package com.bulan_baru.surf_forecast_data;

import android.location.Location;

import com.bulan_baru.surf_forecast_data.DataObject;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class ClientDevice extends DataObject {

    @SerializedName("device_id")
    private String deviceID;

    @SerializedName("device_network")
    private String deviceNetwork;

    @SerializedName("is_location_set")
    private boolean isLocationSet = false;

    private double latitude = 0;
    private double longitude = 0;

    @SerializedName("location_accuracy")
    private float locationAccuracy = 0;

    @SerializedName("last_updated")
    private Date lastUpdated;

    @SerializedName("location_device_id")
    private String locationDeviceID;

    @SerializedName("location_last_updated")
    private Date locationLastUpdated;

    @Inject
    ClientDevice (){
        //empty constructor for injection purposes
    }

    public void setDeviceID(String deviceID){ this.deviceID = deviceID; }
    public String getDeviceID(){ return deviceID; }

    public void setDeviceNetwork(String deviceNetwork){ this.deviceNetwork = deviceNetwork; }
    public String getDeviceNetwork(){ return deviceNetwork; }

    public void setLatitude(double latitude){this.latitude = latitude; isLocationSet = true; }
    public double getLatitude(){ return this.latitude; }
    public void setLongitude(double longitude){this.longitude = longitude; isLocationSet = true; }
    public double getLongitude(){ return this.longitude; }

    public boolean hasLocation(){ return isLocationSet; }

    public void setLocationAccuracy(float locationAccuracy){ this.locationAccuracy = locationAccuracy; }
    public float getLocationAccuracy(){ return locationAccuracy; }

    public void setLastUpdated(Date lastUpdated){ this.lastUpdated = lastUpdated; }
    public Date getLastUpdated(){ return lastUpdated; }


    public void setLocationDeviceID(String deviceID){ this.locationDeviceID = deviceID; }
    public String getLocationDeviceID(){ return locationDeviceID; }

    public void setLocationLastUpdated(Date lastUpdated){ this.locationLastUpdated = lastUpdated; }
    public Date getLocationLastUpdated(){ return locationLastUpdated; }




}
