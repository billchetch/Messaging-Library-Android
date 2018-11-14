package com.bulan_baru.surf_forecast;

import android.content.Intent;
import android.util.Log;

import com.bulan_baru.surf_forecast_data.services.LocationService;

public class SFLocationService extends LocationService {
    @Override
    protected void handleServiceError(int errorType, Throwable t){
        Log.e("SFLS", t.getMessage());
        Intent intent = new Intent();
        intent.setAction(ACTION_LOCATION_SERVICE_ERROR);
        intent.putExtra(ERROR_TYPE_KEY, errorType);
        intent.putExtra(ERROR_MESSAGE_KEY, t.getMessage());
        sendBroadcast(intent);
    }

    @Override
    protected void handleNetworkSSIDRequest(){
        Intent intent = new Intent();
        intent.setAction(ACTION_REQUEST_NETWORK_SSID);
        sendBroadcast(intent);
    }

    @Override
    protected void handleAPIBaseURLRequest(){
        Intent intent = new Intent();
        intent.setAction(ACTION_REQUEST_API_BASE_URL);
        sendBroadcast(intent);
    }

    @Override
    protected void handlePermissionRequest(){
        Intent intent = new Intent();
        intent.setAction(ACTION_REQUEST_PERMISSION);
        sendBroadcast(intent);
    }
}
