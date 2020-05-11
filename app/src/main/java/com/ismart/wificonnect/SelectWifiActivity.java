package com.ismart.wificonnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ismart.wificonnect.bluetool.BlueToothService;
import com.ismart.wificonnect.utils.ProgressDialogUtils;

public class SelectWifiActivity extends AppCompatActivity {

    private BluetoothAdapter mBtAdapter;
    private  String address = null;

    private BlueToothService.BlueToothBind mBlueToothClient;
    private Intent mBlueToothService;

    private ArrayAdapter<String>mNewDevicesArrayAdapter;
    private ListView lv_devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        bindService();
        init();
    }

    private void initViews(){
        lv_devices = (ListView)findViewById(R.id.new_devices);
        mNewDevicesArrayAdapter=new ArrayAdapter<String>(this,
                R.layout.list_item);
        lv_devices.setAdapter(mNewDevicesArrayAdapter);
        lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String wifi = mNewDevicesArrayAdapter.getItem(i);
                inputPassword(wifi);
            }
        });
    }

    private void bindService(){
        mBlueToothService = new Intent(this,BlueToothService.class);
        startService(mBlueToothService);
        bindService(mBlueToothService, conn, Context.BIND_AUTO_CREATE);

    }

    private void init(){
        Intent data = getIntent();
        address=data.getExtras().getString(MainActivity.EXTRA_DEVICE_ADDRESS);
        ProgressDialogUtils.showProgressDialog(this,"正在获取WIFI列表...");
    }

    private void sendData(String cmd){
        Log.d("hello","sendData:"+cmd);
        try{
            if(mBlueToothClient!=null){
                mBlueToothClient.write(cmd.getBytes());
            }
        }catch (Exception e){
            Log.d("hello",e.getMessage());
        }
    }


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    sendData("AT^HELLO");

                    handler.sendEmptyMessageDelayed(1,5000);
                    break;

                case 2:
                    sendData("AT^SCAN");

                    break;
            }
        }
    };

    private void inputPassword(final String wifiName){
        final EditText editText = new EditText(this);
        editText.setHint("请输入WIFI密码");
        new AlertDialog.Builder(this).setTitle(wifiName).setIcon(
                android.R.drawable.ic_dialog_info).setView(
                editText).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
               String password =  editText.getText().toString().trim();
                sendWifiInfo(wifiName,password);
               Toast.makeText(SelectWifiActivity.this,""+password,Toast.LENGTH_SHORT).show();
            }
        })
                .setNegativeButton("取消", null).show();
    }

    private void sendWifiInfo(String wifi,String password){
        //AT^CONNECT:123,456
        String cmd = "AT^CONNECT:"+wifi+","+password;
        sendData(cmd);
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("hello","onServiceConnected:"+name);
            mBlueToothClient = (BlueToothService.BlueToothBind) service;
            mBlueToothClient.setOnBlueToothListener(blueToothListener);
            try{

                if(mBlueToothClient!=null){
                    mBlueToothClient.connect(address);
                }

            }catch (Exception e){
                Log.d("hello",e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("hello","onServiceDisconnected:"+name);
        }
    };

    private BlueToothService.OnBlueToothListener blueToothListener = new BlueToothService.OnBlueToothListener() {
        @Override
        public void onChangeState(int state) {
            switch (state){
                case BlueToothService.STATE_CONNECTED:
                    Log.e("hello","OnBlueToothListener:STATE_CONNECTED");
                    handler.sendEmptyMessageDelayed(1,3000);
                    handler.sendEmptyMessageDelayed(2,1000);
                    //sendData("AT^SCAN");
                break;

                case BlueToothService.STATE_DISCONNECT:
                    Log.e("hello","OnBlueToothListener:STATE_DISCONNECT");
                    break;

            }
        }

        @Override
        public void onRecv(byte[] data, int len) {
            Log.e("hello","onRecv:"+new String(data));
            parser(new String(data));
            ProgressDialogUtils.dismissProgressDialog();
        }
    };

    private void parser(String data){
        if(data.startsWith("AT^WIFI")){
            String[]cmds = data.split(":");
            if(cmds==null||cmds.length==0)return;
            Log.d("hello","wifis:"+cmds[1]);
            final String [] wifis = cmds[1].split(",");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mNewDevicesArrayAdapter.clear();
                    for(String s:wifis){
                        mNewDevicesArrayAdapter.add(s);
                    }
                }
            });
        }
        else if(data.startsWith("AT^CONNECT_OK")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(SelectWifiActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                }
            });
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeMessages(1);
        unbindService(conn);
        stopService(mBlueToothService);
    }
}
