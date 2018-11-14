package com.bulan_baru.surf_forecast;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;

import com.bulan_baru.surf_forecast_data.injection.DaggerRepositoryComponent;
import com.bulan_baru.surf_forecast_data.injection.RepositoryComponent;
import com.bulan_baru.surf_forecast_data.utils.Utils;


public class SurfForecastApplication extends Application {

    RepositoryComponent repositoryComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        repositoryComponent = DaggerRepositoryComponent.builder().build();

        String androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        repositoryComponent.getRepository().getClientDevice().setDeviceID(androidID);

        setDeviceNetwork();

        //set default prefs and API Base URL
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String apiBaseURL = sharedPref.getString(SFLocationService.API_BASE_URL, null);
        repositoryComponent.getRepository().getServiceManager().setApiBaseURL(apiBaseURL);
        boolean useDeviceLocation = sharedPref.getBoolean("use_device_location", true);
        repositoryComponent.getRepository().setUseDeviceLocation(useDeviceLocation);
        float maxDistance = sharedPref.getFloat("max_distance", -1);
        repositoryComponent.getRepository().setMaxDistance(maxDistance);

        //add a network change listener to handle network state changes
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    setDeviceNetwork();
                }
            }, intentFilter);
    }

    protected void setDeviceNetwork(){
        Context context = getApplicationContext();

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            repositoryComponent.getRepository().getClientDevice().setDeviceNetwork(wifiInfo.getSSID());
        }
    }


}
