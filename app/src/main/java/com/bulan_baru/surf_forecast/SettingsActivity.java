package com.bulan_baru.surf_forecast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bulan_baru.surf_forecast_data.SurfForecastServiceManager;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String LOG_TAG = "Settings";

    private boolean restartApplicationOnFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onDestroy(){
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();


    }

    @Override
    protected void onStop(){
        super.onStop();
        if(restartApplicationOnFinish){
            Intent intent = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            ((SurfForecastApplication)getApplication()).repositoryComponent.getRepository().flush();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


        if(key.equals(SFLocationService.API_BASE_URL)) {
            String apiBaseURL = sharedPreferences.getString(key, null);
            SurfForecastServiceManager serviceManager = ((SurfForecastApplication)getApplication()).repositoryComponent.getRepository().getServiceManager();
            serviceManager.setApiBaseURL(apiBaseURL.toLowerCase());
            restartApplicationOnFinish = true;
        }

        if(key.equals(SFLocationService.MIN_TIME)){
            restartApplicationOnFinish = true;
        }

        if(key.equals(SFLocationService.MIN_DISTANCE)){
            restartApplicationOnFinish = true;
        }

        if(key.equals("use_device_location")){
            boolean useDeviceLocation = sharedPreferences.getBoolean(key, true);
            ((SurfForecastApplication)getApplication()).repositoryComponent.getRepository().setUseDeviceLocation(useDeviceLocation);
            restartApplicationOnFinish = true;
        }

        if(key.equals("max_distance")){
            float maxDistance = sharedPreferences.getFloat(key, -1);
            ((SurfForecastApplication)getApplication()).repositoryComponent.getRepository().setMaxDistance(maxDistance);
        }
    }
}
