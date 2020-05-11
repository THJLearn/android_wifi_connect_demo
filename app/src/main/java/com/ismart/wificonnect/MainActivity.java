package com.ismart.wificonnect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ismart.wificonnect.bluetool.BluetoothUtil;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "hello";
    private ListView lv_devices;





    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    public static String EXTRA_DEVICE_ADDRESS="device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String>mNewDevicesArrayAdapter;
    private IntentFilter filter = new IntentFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        permission();
    }

    private void initViews(){
        lv_devices = (ListView)findViewById(R.id.new_devices);
        mNewDevicesArrayAdapter=new ArrayAdapter<String>(this,
                R.layout.list_item);
        lv_devices.setAdapter(mNewDevicesArrayAdapter);
        lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String titles = mNewDevicesArrayAdapter.getItem(i);
                String [] name = titles.split("\n");
                //Toast.makeText(MainActivity.this,""+name[1],Toast.LENGTH_SHORT).show();
                //connectDevice(name[1]);
                Intent intent = new Intent(MainActivity.this,SelectWifiActivity.class);
                intent.putExtra(EXTRA_DEVICE_ADDRESS,name[1]);
                startActivity(intent);

            }
        });
        lv_devices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    BluetoothUtil.sendData("AT^CONNECT:ismart1_2.4G,www.ismart1.cn\n".getBytes(),socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });
    }

    private void permission(){
        int hasPermission= ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            //已获取权限
            init();
        }else{
            //未获取权限 申请
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }




    BluetoothSocket socket;
    private void connectDevice(String address){
        if(TextUtils.isEmpty(address))return;
        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        if(device == null ) return;
        try {
            socket = BluetoothUtil.getSocket(device);
        }catch (Exception e){
            Log.d("hello",e.getMessage());
        }

        /*
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            Toast.makeText(this, "发送蓝牙配对请求", Toast.LENGTH_SHORT).show();
            //这里只需要createBond就行了
            try {
                ClsUtils.createBond(device.getClass(), device);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else if(device.getBondState() == BluetoothDevice.BOND_BONDED){
            Toast.makeText(this, " 正在连接...",  Toast.LENGTH_SHORT).show();
        }

        */

    }


    private void init(){
        // 得到本地蓝牙适配器
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();

        // 若当前设备不支持蓝牙功能
        if(mBtAdapter == null){
            Toast.makeText(this,"蓝牙不可用",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver,filter);

        if(!mBtAdapter.isEnabled()){
            // 若当前设备蓝牙功能未开启，则开启蓝牙
            boolean isEnable = mBtAdapter.enable();
            Log.d(TAG,"mBtAdapter.enable()-->"+isEnable);
            if(isEnable){
                doDiscovery();
            }
        } else{
            doDiscovery();
        }



    }

    private void doDiscovery(){
        Log.d(TAG,"doDiscovery");
        if(mBtAdapter.isDiscovering())
            mBtAdapter.cancelDiscovery();
        mBtAdapter.startDiscovery();
    }


    private boolean isInList(BluetoothDevice device){
        int count = mNewDevicesArrayAdapter.getCount();
        if(count>0) {
            for (int i = 0; i < count; i++) {
                String temp = mNewDevicesArrayAdapter.getItem(i);
                if (temp.endsWith(device.getAddress())) {
                   return true;
                }
            }
        }
        return false;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Log.d("hello","ACTION_FOUND");
                BluetoothDevice  device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("hello","ACTION_FOUND:"+device.toString());

                if(!isInList(device)){
                    mNewDevicesArrayAdapter.add(device.getName()+"\n" +
                            device.getAddress());
                }else{
                    Log.d("hello","ACTION_FOUND:"+device.toString()+" in list");
                }

            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d("hello","ACTION_DISCOVERY_FINISHED");
                Toast.makeText(MainActivity.this,"搜索完毕",Toast.LENGTH_SHORT).show();

            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1000){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户同意了权限申请
                init();
            }else{
                //用户拒绝了权限申请，建议向用户解释权限用途
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if(mBtAdapter!=null){
            mBtAdapter.cancelDiscovery();
        }
    }
}
