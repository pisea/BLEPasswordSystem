package trifort.jp.blepasswordsystem.view.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import trifort.jp.blepasswordsystem.R;
import trifort.jp.blepasswordsystem.view.dialog.AlertDialogFragment;
import trifort.jp.blepasswordsystem.view.dialog.ProgressDialogFragment;

public class SplashActivity extends AppCompatActivity {

    private static final String TARGET_NAME = "Native_BLE_Test";
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 3000;    //スキャンを始めてからSCAN_PERIOD mS後にスキャンを自動停止


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);

        mHandler = new Handler();

        // 対象デバイスがBLEをサポートしているかのチェック。
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "このデバイスではBLEはサポートされておりません。", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bluetooth adapter 初期化.  （API 18以上が必要)
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 対象デバイスがBluetoothをサポートしているかのチェック.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetoothがサポートされておりません。", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    //　画面が出る時
    @Override
    protected void onResume() {
        super.onResume();

        // Bluetooth機能が有効になっているかのチェック。無効の場合はダイアログを表示して有効をうながす。(intentにて)
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        scanLeDevice(true);
    }

    //　intent戻り時
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //　画面が消える時
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    //　スキャンメソッド
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            try {
                ProgressDialogFragment.getInstance(getString(R.string.dialog_search_Device))
                        .showDialog(getSupportFragmentManager());
            } catch (IllegalStateException e) {
            }
            // 一定時間後にスキャンを停止（をセット）（３）
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressDialogFragment.dismissDialog(getSupportFragmentManager());
                    } catch (IllegalStateException e) {
                    }
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    AlertDialogFragment.getInstance(getString(R.string.EA01),
                            getString(R.string.dialog_rescan)).showDialog(getSupportFragmentManager(),
                            new AlertDialogFragment.SimpleCallback() {
                                @Override
                                public void onPositive() {
                                    scanLeDevice(true);
                                }

                                @Override
                                public void onNegative() {
                                }
                            });
                }
            }, SCAN_PERIOD);
            //　スキャン開始（２）
            mScanning = true;
            //(*1)
            //ここは本来はBLESerialのUUIDを指定してstartLeScan(UUID[],Callback)
            //の形でBLESerial対応デバイスのみを検索するが、2013.10現在で
            //UUID[]が独自128bit値の場合は検索されないというAndroidのバグがあり
            //とりあえずはLeScanCallbackのonLeScan時にデバイス名（"BLESerial"の文字）で判定している。
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // スキャンのコールバック
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                //　デバイスが発見された時
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //(*1)参照
                            if (device != null &&
                                    device.getName() != null &&
                                    device.getName().equals(TARGET_NAME)) {

                                if (mScanning) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;
                                }
                                try {
                                    ProgressDialogFragment.dismissDialog(getSupportFragmentManager());
                                } catch (IllegalStateException e) {
                                }
                                AlertDialogFragment.getInstance(getString(R.string.MA01),
                                        getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(),
                                        new AlertDialogFragment.SimpleCallback() {
                                            @Override
                                            public void onPositive() {
                                                Intent intent = new Intent(SplashActivity.this, PasswordActivity.class);
                                                intent.putExtra(PasswordActivity.EXTRAS_DEVICE_NAME, device.getName());
                                                intent.putExtra(PasswordActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                                                startActivity(intent);
                                                finish();
                                            }

                                            @Override
                                            public void onNegative() {
                                            }
                                        });
                            }
                        }
                    });
                }
            };

}
