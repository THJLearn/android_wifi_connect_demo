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

/**
 * author : zhaoyoulu
 * date : 2020/5/9 15:25
 * description ： TODO:类的作用
 */
class WiFiManagent {
    private static final String TAG = "thj";
    private WifiManager wifiManager = null;
    private ArrayList<ScanResult> wifiList = new ArrayList<>();
    public String ApName = "";
    public  interface  Callback {
        void result(String string);
    }
    public  Callback wifiCallBack;

    public void setWifiCallBack(Callback wifiCallBack) {
        this.wifiCallBack = wifiCallBack;
    }

    WiFiManagent(Context context){
        wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager !=null;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);
    }
    private void checkAndOpenWiFi(){
        if (!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }
    }
    private void startScan(){
        wifiManager.startScan();
        wifiList = (ArrayList<ScanResult>) wifiManager.getScanResults();
    }

    /**
     * 打开并扫描WiFi信号，将结果存储在列表里
     */
    void scanWiFi(){
        checkAndOpenWiFi();
        startScan();
    }

    /**
     * @return 信号列表
     */
    ArrayList<ScanResult>getWifiList(){
        return wifiList;
    }

    /**
     * @return Mac地址+信号强度+名称 的数组列表
     */
    ArrayList<String> getBasicInfo(){
        if (wifiList.isEmpty())
            return null;
        ArrayList<String>wifiInfo = new ArrayList<>();
        for (ScanResult e:wifiList){
            wifiInfo.add(e.BSSID+ " \nSSID编号 "+ e.SSID + " 信号强度:" + e.level );
        }
        return wifiInfo;
    }

    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                StringBuilder sb = new StringBuilder();
                sb.append("AT^WIFI:");
                List<ScanResult> mWifis = wifiManager.getScanResults();

                for (ScanResult result : mWifis) {
                    if (result.SSID.equals("")) {
                        continue;
                    }
                    sb.append(result.SSID);
                    sb.append(",");
                }

                ApName = sb.deleteCharAt(sb.length() - 1).toString();
                if (wifiCallBack != null) {
                    wifiCallBack.result(ApName);
                }
                Log.e(TAG, "" + ApName);
            }
//            else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
//                Parcelable parcelableExtra = intent
//                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                if (null != parcelableExtra) {
//                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
//                    NetworkInfo.State state = networkInfo.getState();
//                    boolean isConnected = state == NetworkInfo.State.CONNECTED;
//                    Log.e(TAG, "WIFI Connected = " + isConnected);
//                    if (isConnected) {
//                        if (wifiCallBack != null) {
//                            wifiCallBack.result("AT^CONNECT_OK");
//                        }
//                    }
//                }
//            }
        }
    };
    /**
     * 连接有密码的wifi.
     *
     * @param SSID     ssid
     * @param Password Password
     * @return apConfig
     */
    private WifiConfiguration setWifiParamsPassword(String SSID, String Password) {
        WifiConfiguration apConfig = new WifiConfiguration();
        apConfig.SSID = "\"" + SSID + "\"";
        apConfig.preSharedKey = "\"" + Password + "\"";
        //不广播其SSID的网络
        apConfig.hiddenSSID = true;
        apConfig.status = WifiConfiguration.Status.ENABLED;
        //公认的IEEE 802.11验证算法。
        apConfig.allowedAuthAlgorithms.clear();
        apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        //公认的的公共组密码
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        //公认的密钥管理方案
        apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        //密码为WPA。
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        //公认的安全协议。
        apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return apConfig;
    }

    /**
     * 连接没有密码wifi.
     *
     * @param ssid ssid
     * @return configuration
     */
    private WifiConfiguration setWifiParamsNoPassword(String ssid) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = "\"" + ssid + "\"";
        configuration.status = WifiConfiguration.Status.ENABLED;
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        configuration.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.TKIP);
        configuration.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.CCMP);
        configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return configuration;
    }

    public static final int WIFI_NO_PASS = 0;
    private static final int WIFI_WEP = 1;
    private static final int WIFI_PSK = 2;
    private static final int WIFI_EAP = 3;

    /**
     * 判断是否有密码.
     *
     * @param result ScanResult
     * @return 0
     */
    public static int getSecurity(ScanResult result) {
        if (null != result && null != result.capabilities) {
            if (result.capabilities.contains("WEP")) {
                return WIFI_WEP;
            } else if (result.capabilities.contains("PSK")) {
                return WIFI_PSK;
            } else if (result.capabilities.contains("EAP")) {
                return WIFI_EAP;
            }
        }
        return WIFI_NO_PASS;
    }

}
