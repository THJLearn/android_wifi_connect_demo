package com.ismart.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiApi {
    private final String TAG = "hello";
    private Context mContext;
    private WifiManager mWifiManager;
    public String ApName = "";

    private BlueToothServer blueToothServer;


    public WifiApi(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public void setBlueToothServer(BlueToothServer blueToothServer) {
        this.blueToothServer = blueToothServer;
    }

    public void enableWifi(boolean enable) {
        if (enable) {
            mWifiManager.setWifiEnabled(true);
            mWifiManager.startScan();
        } else {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public void setWifiConnect(String ssid, String password) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        // 指定对应的SSID
        config.SSID = "\"" + ssid + "\"";

        config.preSharedKey = "\"" + password + "\"";
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;

        int netId = mWifiManager.addNetwork(config);
        Log.e("TAG", netId + "   ");
        // 这个方法的第一个参数是需要连接wifi网络的networkId，第二个参数是指连接当前wifi网络是否需要断开其他网络
        // 无论是否连接上，都返回true。。。。
        mWifiManager.enableNetwork(netId, true);
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                StringBuilder sb = new StringBuilder();
                sb.append("AT^WIFI:");
                List<ScanResult> mWifis = mWifiManager.getScanResults();
                for (ScanResult result : mWifis) {
                    if (result.SSID.equals("")) {
                        continue;
                    }
                    sb.append(result.SSID);
                    sb.append(",");
                }
                ApName = sb.deleteCharAt(sb.length() - 1).toString();
                if (blueToothServer != null) {
                    blueToothServer.write(ApName.getBytes());
                }
                Log.e(TAG, "" + ApName);
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;
                    Log.e(TAG, "WIFI Connected = " + isConnected);
                    if (isConnected) {
                        if (blueToothServer != null) {
                            blueToothServer.write("AT^CONNECT_OK".getBytes());
                        }
                    }
                }
            }
        }
    };
}
