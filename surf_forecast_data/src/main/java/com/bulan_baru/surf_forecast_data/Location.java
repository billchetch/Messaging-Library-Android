package com.bulan_baru.surf_forecast_data;

public class Location extends DataObject {
    public static final int TO_STRING_OPTION_LOCATION = 2;
    public static final int TO_STRING_OPTION_DISTANCE = 4;

    private String location;
    private double latitude;
    private double longitude;
    private double distance;
    private String timezone;

    public void setLocation(String location){
        this.location = location;
    }
    public String getLocation(){
        return location;
    }

    public void setLatitude(double latitude){ this.latitude = latitude; }
    public double getLatitude(){ return latitude;}
    public void setLongitude(double longitude){ this.longitude = longitude; }
    public double getLongitude(){ return longitude; }

    public void setDistance(double distance){ this.distance = distance; }
    public double getDistance(){ return distance; }

    public String getTimezone(){ return timezone; }

    @Override
    public String toString(){
        return "ID: " + getID() + " " + location + " lat/lon: " + Double.toString(latitude) + "," + Double.toString(longitude);
    }
}
