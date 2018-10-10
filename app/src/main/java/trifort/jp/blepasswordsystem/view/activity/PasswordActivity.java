package trifort.jp.blepasswordsystem.view.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;

import trifort.jp.blepasswordsystem.R;
import trifort.jp.blepasswordsystem.service.BluetoothLeService;
import trifort.jp.blepasswordsystem.view.dialog.AlertDialogFragment;
import trifort.jp.blepasswordsystem.view.dialog.ProgressDialogFragment;

public class PasswordActivity extends AppCompatActivity {

    public static long lastClickTime = 0;
    public static final int BUTTON_DELAY = 2000;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private EditText edtInputPassword;
    private EditText edtResetPassword;
    private Button btnRun;
    private LayoutState currentState = LayoutState.INPUT;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private String inputPasswordStr;
    private String resetPasswordStr;
    private byte[] passwordByte;
    private boolean isConnectSuccessDialogFlg = false;

    private enum LayoutState {
        INPUT, RESET
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_password);
        initView();
        //　BTデバイス名とアドレス引継ぎ
        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

    }

    private void initBLEService() {
        //　サービス接続（BluetoothLeService）
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void initView() {

        edtInputPassword = findViewById(R.id.edtInputPassword);
        edtResetPassword = findViewById(R.id.edtResetPassword);

        edtInputPassword.addTextChangedListener(new EditTextWatcher());
        edtResetPassword.addTextChangedListener(new EditTextWatcher());

        btnRun = findViewById(R.id.btnRun);
        btnRun.setOnClickListener(new RunButtonClickListener());
        btnRun.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            ProgressDialogFragment.getInstance(getString(R.string.dialog_connect_Device))
                    .showDialog(getSupportFragmentManager());
        } catch (IllegalStateException e) {
        }
        initBLEService();
        //　イベントレシーバの登録と、GATTサーバ接続
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        clearBLEService();
        super.onPause();
    }

    private void clearBLEService() {
        //　各種終了処理
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        } catch (IllegalArgumentException e) {
        }
    }

    // BLEサービスからのイベントを処理
    // ACTION_GATT_CONNECTED: GATT server　接続.
    // ACTION_GATT_DISCONNECTED: GATT server　切断.
    // ACTION_GATT_SERVICES_DISCOVERED: GATT services　取得.
    // ACTION_DATA_AVAILABLE: 受信データあり.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ProgressDialogFragment.dismissDialog(getSupportFragmentManager());
            } catch (IllegalStateException e) {
            }
            final String action = intent.getAction();
            //　GATT接続時
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            }

            //　GATT切断時
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

            }

            //　GATTサービス一覧取得時
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

            }

            //　データ受信時
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                //受信データ受け取り（byte配列）
                byte[] rsvData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //　受信内容表示
                if (rsvData != null) {
                    try {
                        String result = new String(rsvData, "UTF-8");

                        if ("ES01".equals(result)) {
                            if (currentState != LayoutState.RESET) {
                                AlertDialogFragment.getInstance(getString(R.string.ES01),
                                        getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(),
                                        new AlertDialogFragment.SimpleCallback() {
                                            @Override
                                            public void onPositive() {
                                                if (currentState != LayoutState.RESET) {
                                                    currentState = LayoutState.RESET;
                                                    changeLayout();
                                                }
                                            }

                                            @Override
                                            public void onNegative() {
                                            }
                                        });
                            }
                        }

                        if ("ES02".equals(result)) {
                            AlertDialogFragment.getInstance(getString(R.string.ES02),
                                    getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(),
                                    new AlertDialogFragment.SimpleCallback() {
                                        @Override
                                        public void onPositive() {
                                            if (currentState != LayoutState.INPUT) {
                                                currentState = LayoutState.INPUT;
                                                changeLayout();
                                            }
                                            edtInputPassword.setEnabled(true);
                                        }

                                        @Override
                                        public void onNegative() {
                                        }
                                    });
                        }

                        if ("MS01".equals(result)) {
                            if (isConnectSuccessDialogFlg) {
                                AlertDialogFragment.getInstance(getString(R.string.MS01),
                                        getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(),
                                        new AlertDialogFragment.SimpleCallback() {
                                            @Override
                                            public void onPositive() {
                                                if (currentState != LayoutState.INPUT) {
                                                    currentState = LayoutState.INPUT;
                                                    changeLayout();
                                                }
                                            }

                                            @Override
                                            public void onNegative() {
                                            }
                                        });
                                isConnectSuccessDialogFlg = false;
                            }
                        }

                        if ("MS02".equals(result)) {
                            AlertDialogFragment.getInstance(getString(R.string.MS02),
                                    getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(),
                                    new AlertDialogFragment.SimpleCallback() {
                                        @Override
                                        public void onPositive() {
                                            if (currentState != LayoutState.INPUT) {
                                                currentState = LayoutState.INPUT;
                                                changeLayout();
                                            }
                                        }

                                        @Override
                                        public void onNegative() {
                                        }
                                    });

                        }


                        if ("MS03".equals(result)) {
                            AlertDialogFragment.getInstance(getString(R.string.MS03),
                                    getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(),
                                    new AlertDialogFragment.SimpleCallback() {
                                        @Override
                                        public void onPositive() {
                                            clearBLEService();
                                            finish();
                                        }

                                        @Override
                                        public void onNegative() {
                                        }
                                    });
                        }

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private void changeLayout() {

        TextView txtTitle = findViewById(R.id.txtTitle);
        TextView txtMessage = findViewById(R.id.txtMessage);
        LinearLayout passwordCheckLayout = findViewById(R.id.passwordCheckLayout);
        Button btnRun = findViewById(R.id.btnRun);

        switch (currentState) {
            case INPUT:
                txtTitle.setText(R.string.password_input_title);
                txtMessage.setText(R.string.password_input_message);
                passwordCheckLayout.setVisibility(View.GONE);
                btnRun.setText(R.string.password_send_button_text);
                break;
            case RESET:
                txtTitle.setText(R.string.password_reset_title);
                txtMessage.setText(R.string.password_reset_message);
                passwordCheckLayout.setVisibility(View.VISIBLE);
                btnRun.setText(R.string.password_reset_button_text);
                break;
            default:
                break;

        }
    }

    /**
     * GATTインテントフィルタを作る
     *
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //　サービスコネクション（BluetoothLeService間）
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, final IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // BTアダプタへの参照の初期化が成功したら、接続動作を開始
            mBluetoothLeService.connect(mDeviceAddress);
            isConnectSuccessDialogFlg = true;
            try {
                ProgressDialogFragment.dismissDialog(getSupportFragmentManager());
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            try {
                ProgressDialogFragment.dismissDialog(getSupportFragmentManager());
            } catch (IllegalStateException e) {
            }
            mBluetoothLeService = null;
        }
    };

    public class RunButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (SystemClock.elapsedRealtime() - lastClickTime < BUTTON_DELAY) {
                return;
            }
            lastClickTime = SystemClock.elapsedRealtime();
            try {
                ProgressDialogFragment.getInstance(getString(R.string.dialog_connect_Device))
                        .showDialog(getSupportFragmentManager());
            } catch (IllegalStateException e) {
            }
            switch (currentState) {
                case INPUT:
                    edtInputPassword.clearFocus();
                    inputPasswordStr = edtInputPassword.getText().toString();

                    passwordByte = ("MS03@" + inputPasswordStr).getBytes();
                    mBluetoothLeService.sendData(passwordByte);    //送信

                    inputPasswordStr = "";
                    edtInputPassword.setText("");
                    edtInputPassword.setEnabled(false);

                    break;

                case RESET:
                    edtInputPassword.clearFocus();
                    edtResetPassword.clearFocus();

                    inputPasswordStr = edtInputPassword.getText().toString();
                    resetPasswordStr = edtResetPassword.getText().toString();

                    if (inputPasswordStr.equals(resetPasswordStr)) {
                        passwordByte = ("MS02@" + inputPasswordStr).getBytes();
                        mBluetoothLeService.sendData(passwordByte);    //送信

                        inputPasswordStr = "";
                        resetPasswordStr = "";
                        edtInputPassword.setText("");
                        edtResetPassword.setText("");

                    } else {
                        AlertDialogFragment.getInstance(getString(R.string.EA03),
                                getString(R.string.dialog_ok)).showDialog(getSupportFragmentManager(), null);
                    }

                    break;

                default:
                    break;
            }

        }
    }

    public class EditTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            switch (currentState) {
                case INPUT:
                    if (s.length() == 0) {
                        btnRun.setEnabled(false);
                    } else {
                        btnRun.setEnabled(true);
                    }
                    break;

                case RESET:
                    if (edtInputPassword.length() == 0) {
                        btnRun.setEnabled(false);
                    } else {
                        if (edtResetPassword.length() == 0) {
                            btnRun.setEnabled(false);
                        } else {
                            btnRun.setEnabled(true);
                        }
                    }
                    break;
            }


        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

}
