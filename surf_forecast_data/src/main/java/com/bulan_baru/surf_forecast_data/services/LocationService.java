package com.bulan_baru.surf_forecast_data.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.bulan_baru.surf_forecast_data.ClientDevice;
import com.bulan_baru.surf_forecast_data.R;
import com.bulan_baru.surf_forecast_data.SurfForecastRepository;
import com.bulan_baru.surf_forecast_data.injection.DaggerRepositoryComponent;
import com.bulan_baru.surf_forecast_data.injection.RepositoryComponent;

import java.util.concurrent.TimeUnit;

abstract public class LocationService extends Service {
    final public static int ONGOING_NOTIFICATION_ID = 1;
    final public static String CHANNEL_ID = "com.bulan_baru.LocationService.N1";
    final public static String CHANNEL_NAME = "Location service notification channel";

    final public static String ACTION_UPDATED_LOCATION = "com.bulan_baru.broadcast.UPDATED_LOCATION";
    final public static String ACTION_REQUEST_PERMISSION = "com.bulan_baru.broadcast.REQUEST_PERMISSION";
    final public static String ACTION_REQUEST_NETWORK_SSID = "com.bulan_baru.broadcast.REQUEST_NETWORK_SSID";
    final public static String ACTION_REQUEST_API_BASE_URL = "com.bulan_baru.broadcast.REQUEST_API_BASE_URL";
    final public static String ACTION_LOCATION_SERVICE_ERROR = "com.bulan_baru.broadcast.LOCATION_SERVICE_ERROR";

    final public static String NETWORK_SSID = "network_ssid";
    final public static String API_BASE_URL = "api_base_url";
    final public static String DEVICE_ID = "device_id";
    final public static String LONGITUDE = "longitude";
    final public static String LATITUDE = "latitude";
    final public static String LOCATION_ACCURACY = "location_accuracy";
    final public static String MIN_TIME = "location_update_min_time";
    final public static String MIN_DISTANCE = "location_update_min_distance";

    final public static String ERROR_TYPE_KEY = "error_type";
    final public static String ERROR_MESSAGE_KEY = "error_message";
    final public static int ERROR_NONE = 0;
    final public static int ERROR_REPOSITORY = 1;
    final public static int ERROR_INITIALISE = 2;


    final public static long DEFAULT_MIN_TIME = 10;
    final public static long DEFAULT_MIN_DISTANCE = 0;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentLocation;
    private SurfForecastRepository repository;
    private long minTime;
    private long minDistance;



    public LocationService(){
        Log.i("LS", "Constructed");
    }

    @Override
    public void onCreate(){
        super.onCreate();

        //create a repository instance to save device and location data
        repository = DaggerRepositoryComponent.builder().build().getRepository();

        repository.serviceError().observeForever(t->{
            handleServiceError(ERROR_REPOSITORY, t);
        });

        //set the current device ID
        String androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        repository.getClientDevice().setDeviceID(androidID);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.createNotificationChannel(channel);
        }

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("BB Location Service")
                        .setContentText("Bulan Baru Location Service")
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LS", "Start service wtih ID " + startId);

        try {
            String ssid = intent.getStringExtra(NETWORK_SSID);
            if (ssid == null) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) ssid = wifiInfo.getSSID();
            }
            if (ssid == null) {
                handleNetworkSSIDRequest();
                Log.i("LS", "No API Base URL found");
                return START_STICKY;
            }
            repository.getClientDevice().setDeviceNetwork(ssid);


            if (requiresUserPermission()) {
                handlePermissionRequest();
                Log.i("LS", "Permission is not granted");
                return START_STICKY;
            }

            String apiBaseURL = intent.getStringExtra(API_BASE_URL);
            if (apiBaseURL == null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                apiBaseURL = preferences.getString(API_BASE_URL, null);
            }
            if (apiBaseURL == null) {
                handleAPIBaseURLRequest();
                Log.i("LS", "No API Base URL found");
                return START_STICKY;
            }
            repository.getServiceManager().setApiBaseURL(apiBaseURL);

            startListeningForLocationUpdates();
        } catch (Throwable t){
            handleServiceError(ERROR_INITIALISE, t);
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    public boolean requiresUserPermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    abstract protected void handleNetworkSSIDRequest();
    abstract protected void handleAPIBaseURLRequest();
    abstract protected void handlePermissionRequest();
    abstract protected void handleServiceError(int errorType, Throwable t);

    protected SurfForecastRepository getRepository() {
        return repository;
    }

    protected Location getCurrentLocation() {
        return currentLocation;
    }

    public static boolean isError(Intent intent){
        return getErrorType(intent) != ERROR_NONE;
    }
    public static int getErrorType(Intent intent){
        return intent.getIntExtra(ERROR_TYPE_KEY, ERROR_NONE);
    }
    public static String getErrorMessage(Intent intent){
        return intent.getStringExtra(ERROR_MESSAGE_KEY);
    }

    protected void startListeningForLocationUpdates() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //location listener on Repository
        repository.updateDeviceLocation(currentLocation).observeForever(clientDevice -> {
            if(clientDevice != null){
                onDeviceLocationUpdated(clientDevice);
            }
        });

        //location listener on Device
        locationListener = new LocationListener() {  //location listener on device
            public void onLocationChanged(Location location) {
                //TODO: some logic here to make sure we have the best current location
                repository.updateDeviceLocation(location);
                currentLocation = location;
                Log.i("LS", "location updated by device " + location.getLongitude() + "/" + location.getLatitude());
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i("LS", provider + " has status " + status);
            }

            public void onProviderEnabled(String provider) {
                Log.i("LS", provider + " enabled");
            }

            public void onProviderDisabled(String provider) {
                Log.i("LS", provider + " disabled");
            }
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        minTime = preferences.getLong(MIN_TIME, DEFAULT_MIN_TIME);
        minDistance = preferences.getLong(MIN_DISTANCE, DEFAULT_MIN_DISTANCE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TimeUnit.SECONDS.toMillis(minTime), minDistance, locationListener);

        Log.i("LS", "Requested location updates");
    }

    protected void stopListeningForLocationUpdates(){
        locationManager.removeUpdates(locationListener);
    }

    protected void onDeviceLocationUpdated(ClientDevice clientDevice){

        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATED_LOCATION);
        intent.putExtra(DEVICE_ID, clientDevice.getDeviceID());
        intent.putExtra(LONGITUDE, clientDevice.getLongitude());
        intent.putExtra(LATITUDE, clientDevice.getLatitude());
        intent.putExtra(LOCATION_ACCURACY, clientDevice.getLocationAccuracy());
        intent.putExtra(API_BASE_URL, repository.getServiceManager().getApiBaseURL());
        intent.putExtra(NETWORK_SSID, clientDevice.getDeviceNetwork());
        intent.putExtra(MIN_TIME, minTime);
        intent.putExtra(MIN_DISTANCE, minDistance);

        sendBroadcast(intent);

        Log.i("LS", "Device Location Updated " + clientDevice.getLongitude() + "/" + clientDevice.getLatitude());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        stopListeningForLocationUpdates();
        Log.i("LS", "Service destroyed");
    }
}
