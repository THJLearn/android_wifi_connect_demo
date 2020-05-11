package com.ismart.wificonnect.bluetool;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BlueToothService extends Service {


    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;
    private BluetoothAdapter mBtAdapter;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECT = 4;
    private int mState;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("hello","BlueToothService onCreate!");
    }


    private OnBlueToothListener onBlueToothListener;
    private synchronized void setState(int state){
        mState = state;
        if(onBlueToothListener!=null){
            onBlueToothListener.onChangeState(mState);
        }
    }


    public void setOnBlueToothListener(OnBlueToothListener onBlueToothListener) {
        this.onBlueToothListener = onBlueToothListener;
    }

    public interface  OnBlueToothListener{
        void onChangeState(int state);
        void onRecv(byte[]data,int len);
    }

    public class BlueToothBind extends Binder {

        public void setOnBlueToothListener(OnBlueToothListener listener) {
            onBlueToothListener = listener;
        }
        public void connect(String address) throws IOException {
            Log.d("hello","BlueToothBind --> connect:"+address);
            // 得到本地蓝牙适配器
            mBtAdapter= BluetoothAdapter.getDefaultAdapter();
            if(TextUtils.isEmpty(address))return;
            BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
            if(device == null ) return;


            if(mConnectThread !=null){
                mConnectThread.cancel();
                mConnectThread=null;
            }

            if(mConnectedThread !=null){
                mConnectedThread.cancel();
                mConnectedThread=null;
            }

            mConnectThread=new ConnectThread(device);
            mConnectThread.start();

        }

        public void write(byte[]out){
            if(mConnectedThread!=null){
                mConnectedThread.write(out);
            }

        }

        public void disconnect(){
            if(mConnectThread !=null){
                mConnectThread.cancel();
                mConnectThread=null;
            }

            if(mConnectedThread !=null){
                mConnectedThread.cancel();
                mConnectedThread=null;
            }
        }

        public int getState(){
            return -1;
        }

    }

    // 开启一个ConnectThread来管理对应的当前连接。之前取消任意现存的mConnectThread
    // mConnectThread，mAcceptThread线程，然后开启新的mConnectThread，传入当前
    // 刚刚接受的socket连接，最后通过Handler来通知UI连接
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device){
        if(mConnectThread !=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }

        if(mConnectedThread !=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }

        mConnectedThread=new ConnectedThread(socket);
        mConnectedThread.start();

    }

    // 当连接失去的时候，设为STATE_LISTEN
    private void connectionLost(){
        //setState(STATE_DISCONNECT);
        //this.start();
    }


    // 连接线程，专门用来对外发出连接对方蓝牙的请求并进行处理
    // 构造函数里通过BluetoothDevice.createRfcommSocketToServiceRecord(),
    // 从待连接的device产生BluetoothSocket，然后在run方法中connect
    // 成功后调用 BluetoothChatService的connnected（）方法，定义cancel（）在关闭线程时能关闭socket
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            mmDevice=device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try{
                // MY_UUID is the app's UUID string, also used by the server code
                Log.d("hello","ConnectThread --> createInsecureRfcommSocketToServiceRecord:"+PRINTER_UUID);
                tmp =  device.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID);
            }catch (IOException e){}
            mmSocket = tmp;
        }

        public void run(){
            // Cancel discovery because it will slow down the connection
            //mAdapter.cancelDiscovery();
            try{
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            }catch (IOException e){
                //connectionFailed();
                setState(STATE_DISCONNECT);
                // Unable to connect; close the socket and get out
                Log.e("hello","ConnectThread --> connectionFailed！！");

                try{
                    mmSocket.close();
                }catch (IOException e2){}

                //ChatService.this.start();
                return;
            }
            setState(STATE_CONNECTED);
            connected(mmSocket,mmDevice);
        }

        public void cancel(){
           /* try{
                mmSocket.close();
            }catch (IOException e){}*/
        }
    }

    // 双方蓝牙连接后一直运行的线程。构造函数中设置输入输出流。
    // Run方法中使用阻塞模式的InputStream.read()循环读取输入流
    // 然后psot到UI线程中更新聊天信息。也提供了write()将聊天消息写入输出流传输至对方，
    // 传输成功后回写入UI线程。最后cancel()关闭连接的socket

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut=null;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try{
                tmpIn=mmSocket.getInputStream();
                tmpOut=mmSocket.getOutputStream();
            }catch (IOException e){}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[]buffer=new byte[1024];
            int bytes;
            while (true){
                try{
                    bytes = mmInStream.read(buffer);
                    //mHandler.obtainMessage(ChatService.MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    byte []mData = new byte[bytes];
                    System.arraycopy(buffer, 0, mData, 0, bytes);
                    Log.e("hello","ConnectedThread --> recv:"+new String(mData));

                    if(onBlueToothListener!=null){
                        onBlueToothListener.onRecv(mData,bytes);
                    }
                }catch (IOException e){
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[]buffer){
            try{
                mmOutStream.write(buffer);
            }catch (IOException e){
                Log.d("MainActivity","Send Fail");
            }
            //mHandler.obtainMessage(ChatService.MESSAGE_WRITE,buffer).sendToTarget();
        }

        public void cancel(){
            try{
                mmSocket.close();
                setState(STATE_DISCONNECT);
            }catch (IOException e){}
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new BlueToothBind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mConnectThread !=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }

        if(mConnectedThread !=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }
    }
}
