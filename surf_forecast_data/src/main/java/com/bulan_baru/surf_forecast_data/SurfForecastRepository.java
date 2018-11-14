package com.bulan_baru.surf_forecast_data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class SurfForecastRepository{
    private static final String LOG_TAG = "SF Repo";

    private SurfForecastServiceManager serviceManager;
    private ClientDevice device;
    private boolean serviceAvailable = true;
    private boolean useDeviceLocation = true;
    private float maxDistance = -1;

    //so that we can observe over time
    private final MutableLiveData<Throwable> liveDataServiceError = new MutableLiveData<>();
    private final MutableLiveData<ClientDevice> liveDataDevice = new MutableLiveData<>();

    @Inject
    SurfForecastRepository(ClientDevice device, SurfForecastServiceManager serviceManager){
        this.device = device;
        this.serviceManager = serviceManager;

        liveDataDevice.setValue(device);

        serviceError().observeForever(t->{
            handleServiceError(t);
        });
    }

    protected void handleServiceError(Throwable t){
        if(t instanceof SocketTimeoutException){
            serviceAvailable = false;
        }
        if(t instanceof SurfForecastServiceException){
            serviceAvailable = false;
        }
        Log.e(LOG_TAG, t.getMessage() != null ? t.getMessage() : "no error message available");
    }

    protected boolean isServiceAvailable(){ return serviceAvailable; }

    public void flush(){
        serviceManager.cancelAllCalls();
        this.device.setID(0);
    }

    public SurfForecastServiceManager getServiceManager() {
        return serviceManager;
    }

    public SurfForecastService getService(){
        return isServiceAvailable() ? getServiceManager().getService() : null;
    }

    public ClientDevice getClientDevice(){ return this.device; }

    public LiveData<Throwable> serviceError(){ return liveDataServiceError; }

    public void setUseDeviceLocation(boolean useDeviceLocation){ this.useDeviceLocation = useDeviceLocation; }
    public void setMaxDistance(float maxDistance){ this.maxDistance = maxDistance; }

    public LiveData<ClientDevice> updateDeviceLocation(android.location.Location location){
        if(device != null) {
            if(location != null) {
                device.setLatitude(location.getLatitude());
                device.setLongitude(location.getLongitude());
                device.setLocationAccuracy(location.getAccuracy());
                device.setLastUpdated(new Date());
            }

            //we set this directly rather than saving to server
            liveDataDevice.setValue(device);
        }
        return liveDataDevice;
    }

    public LiveData<List<Location>> getLocationsNearby(){
        final MutableLiveData<List<Location>> locations = new MutableLiveData<>();

        SurfForecastService service = getService();
        if(service != null) {
            if(useDeviceLocation){
                service.getLocations(device.getLatitude(), device.getLongitude(), maxDistance).enqueue(
                        new SurfForecastServiceCallback<List<Location>>(liveDataServiceError) {
                            @Override
                            public void handleResponse(Call<List<Location>> call, Response<List<Location>> response) {
                                locations.setValue(response.body());
                            }
                        }
                );
            } else {
                //since not using the location of the device, we choose to use the location provided by the server ...
                service.getLocationsNearby(maxDistance).enqueue(
                        new SurfForecastServiceCallback<List<Location>>(liveDataServiceError) {
                            @Override
                            public void handleResponse(Call<List<Location>> call, Response<List<Location>> response) {
                                locations.setValue(response.body());
                            }
                        }
                );
            }
        }
        return locations;
    }


    public LiveData<Forecast> getForecast(int locationID){
        final MutableLiveData<Forecast> forecast = new MutableLiveData<>();

        SurfForecastService service = getService();
        if(service != null) {
            service.getForecast(locationID).enqueue(
                    new SurfForecastServiceCallback<Forecast>(liveDataServiceError) {
                        @Override
                        public void handleResponse(Call<Forecast> call, Response<Forecast> response) {
                            forecast.setValue(response.body());
                        }
                    }
            );
        }
        return forecast;
    }
}
