/*
 * Copyright (C) 2013 T.Ishii
 *
 * ※Gooleのサンプルプログラムがベース
 *
 */

package trifort.jp.blepasswordsystem.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

/**
 * BLESerial（浅草ギ研製BLE通信モジュール）との接続と通信を行うサービス。
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    //BLESerial サービスUUID
    public static String UUID_BLESERIAL_SERVICE = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    //　BLESerial　受信UUID （Notify)
    public static String UUID_BLESERIAL_RX = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    // BLESerial 送信UUID （write without response)
    public static String UUID_BLESERIAL_TX = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    //　キャラクタリスティック設定UUID
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public final static String ACTION_GATT_CONNECTED =
            "com.robotsfx.bleserialtest.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.robotsfx.bleserialtest.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.robotsfx.bleserialtest.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.robotsfx.bleserialtest.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.robotsfx.bleserialtest.EXTRA_DATA";

    /**
     * GATTイベントコールバック
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        //　接続状態変化イベント
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // 接続成功後に、サービスを検索
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        //　サービス一覧取得終了イベント
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                    //RXにNotificateをセット
                    BluetoothGattService myService = mBluetoothGatt.getService(UUID.fromString(UUID_BLESERIAL_SERVICE));
                    BluetoothGattCharacteristic characteristic = myService.getCharacteristic(UUID.fromString(UUID_BLESERIAL_RX));
                    mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    //GATTにRXのNotifiを設定
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //　CharacteristicReadイベント
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        //　受信イベント
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     * 接続、切断、サービス一覧取得をブロードキャスト
     * （SerialComActivityで受信）
     *
     * @param action
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * onCharacteristicChanged又はonCharacteristicReadのときに
     * 受信データをブロードキャスト（SerialComActivityで受信）
     *
     * @param action
     * @param characteristic
     */
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (UUID.fromString(UUID_BLESERIAL_RX).equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(EXTRA_DATA, data);        // 	受信データをインテントにセット
            }
        }
        sendBroadcast(intent);    //インテント送信
    }

    //　－－－以降４つはBind関連－－－

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //　BLEデバイスとの接続を終了する時には、必ずBluetoothGatt.close()する。
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * BluetoothAdapter 初期化.
     *
     * @return true：初期化成功
     */
    public boolean initialize() {
        // API18以上必要
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * GATT server（BLESerial） と接続
     *
     * @param address ：Device Address
     * @return true：接続成功
     * 結果は非同期で、BluetoothGattCallback（冒頭部分）で報告される。
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // 前回接続したデバイスだった場合は再接続
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        //　接続
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // デバイスと直接通信するので、autoConnectパラメータ（２個目の引数）はfalse
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * 切断
     * 結果は非同期でBluetoothGattCallbackで報告される
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 指定したCharacteristicをread.
     * 結果は非同期でBluetoothGattCallback#onCharacteristicReadで報告される
     *
     * @param ：読み込むキャラクタリスティックを指定.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 送信
     *
     * @param bytes 　：送信データ
     */
    public void sendData(byte[] bytes) {
        BluetoothGattService myService = mBluetoothGatt.getService(UUID.fromString(UUID_BLESERIAL_SERVICE));
        BluetoothGattCharacteristic characteristic = myService.getCharacteristic(UUID.fromString(UUID_BLESERIAL_TX));
        characteristic.setValue(bytes);
        mBluetoothGatt.writeCharacteristic(characteristic);

    }

}
