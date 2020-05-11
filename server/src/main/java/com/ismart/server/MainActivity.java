package com.ismart.server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private Context mContext;
    private  WifiApi mWifiApi;
    private final String AT_SCAN_COMMON = "AT^SCAN";
    private final String AT_CONNECT_COMMON = "AT^CONNECT";

    private BluetoothAdapter mBtAdapter;
    private BlueToothServer blueToothServer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
        permission();
    }

    private void permission(){
        int hasPermission= ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            //已获取权限
            initWifi();
            initBlue();
        }else{
            //未获取权限 申请
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }

    private void initWifi(){
        mWifiApi = new WifiApi(mContext);
        //mWifiApi.enableWifi(true);
    }

    private void initBlue(){
//        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
//        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter(); //mBtAdapter

        // 得到本地蓝牙适配器
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();

        // 若当前设备不支持蓝牙功能
        if(mBtAdapter == null){
            Toast.makeText(this,"蓝牙不可用",Toast.LENGTH_LONG).show();
            //finish();
            return;
        }

        if(!mBtAdapter.isEnabled()){
            // 若当前设备蓝牙功能未开启，则开启蓝牙
            boolean isEnable = mBtAdapter.enable();
            Log.d(TAG,"mBtAdapter.enable()-->"+isEnable);

            //setDiscoverableTimeout();

        }
        BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBtAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(true, 0), createAdvertiseData(), mAdvertiseCallback);

    }
    /**
     *广播的一些基本设置
     **/
    public AdvertiseSettings createAdvSettings(boolean connectAble, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(connectAble);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings mAdvertiseSettings = builder.build();
        if (mAdvertiseSettings == null) {
            Toast.makeText(this, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseSettings;
    }
    //广播数据
    public AdvertiseData createAdvertiseData() {
        AdvertiseData.Builder mDataBuilder = new AdvertiseData.Builder();
        mDataBuilder.setIncludeDeviceName(true); //广播名称也需要字节长度
        mDataBuilder.setIncludeTxPowerLevel(true);
        mDataBuilder.addServiceData(ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),new byte[]{1,2});
        AdvertiseData mAdvertiseData = mDataBuilder.build();
        if (mAdvertiseData == null) {
            Toast.makeText(MainActivity.this, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseData;
    }
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (settingsInEffect != null) {
                Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode()
                        + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.e(TAG, "onStartSuccess, settingInEffect is null");
            }
            Log.e(TAG, "onStartSuccess settingsInEffect" + settingsInEffect);

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "onStartFailure errorCode" + errorCode);

            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_data_too_large", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_too_many_advertises", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising because no advertising instance is available.");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_already_started", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertising is already started");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_internal_error", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Operation failed due to an internal error");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_feature_unsupported", Toast.LENGTH_LONG).show();
                Log.e(TAG, "This feature is not supported on this platform");
            }
        }
    };
    private void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 1000);
                switch (state) {

                    case BluetoothAdapter.STATE_ON:
                        Log.d("hello","BluetoothAdapter.STATE_ON");
                        setDiscoverableTimeout(0);
                        blueToothServer = new BlueToothServer(mContext,mBtAdapter);
                        blueToothServer.setOnBlueToothListener(new BlueToothServer.onBlueToothListener() {
                            @Override
                            public void onChangeState(int state) {
                                Log.d("hello","onChangeState:"+state);
                                if(state == BlueToothServer.STATE_CONNECTED){
                                    blueToothServer.write("AT^HELLO".getBytes());
                                }
                        }

                            @Override
                            public void onRecv(byte[] data, int len) {
                                Log.d("hello","recv:"+new String(data));
                                parser(new String(data));
                            }
                        });

                        blueToothServer.start();

                        mWifiApi.setBlueToothServer(blueToothServer);
                        break;
                }
            }
        }
    };

    //AT^SCAN
    private void parser(String cmd){
        if(cmd.equals(AT_SCAN_COMMON))
        {
            Log.d("hello","CMD_AT_SCAN");
            mWifiApi.enableWifi(false);
            mWifiApi.enableWifi(true);
        }else if(cmd.startsWith(AT_CONNECT_COMMON)) {
            String[] temp = cmd.split(":");
            String[] temp1 = temp[1].split(",");
            String ssid = temp1[0];
            String pwd = temp1[1];
            Log.d("hello","ssid:"+temp1[0]+"pwd:"+temp1[1]);
            mWifiApi.setWifiConnect(ssid,pwd);
        }else if(cmd.equals("AT^HELLO")){
            blueToothServer.write("AT^HELLO".getBytes());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1000){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户同意了权限申请
                initWifi();
                initBlue();
            }else{
                //用户拒绝了权限申请，建议向用户解释权限用途
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(blueToothServer!=null){
            blueToothServer.stop();
        }
    }
}
