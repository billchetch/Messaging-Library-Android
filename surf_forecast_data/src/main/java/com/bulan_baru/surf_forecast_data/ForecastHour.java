package com.bulan_baru.surf_forecast_data;

import com.google.gson.annotations.SerializedName;

public class ForecastHour extends ForecastDetail {
    @SerializedName("swell_height")
    private WeightedDetail swellHeight;

    @SerializedName("swell_period")
    private WeightedDetail swellPeriod;

    @SerializedName("swell_direction")
    private WeightedDetail swellDirection;

    @SerializedName("wind_direction")
    private WeightedDetail windDirection;

    @SerializedName("wind_speed")
    private WeightedDetail windSpeed;

    @SerializedName("tide_height")
    private WeightedDetail tideHeight;

    @SerializedName("tide_position")
    private Integer tidePosition;

    @SerializedName("rating")
    private WeightedDetail rating;

    public String getSwellHeight(){
        return swellHeight.getString();
    }

    public String getSwellPeriod(){
        return swellPeriod.getString();
    }

    public String getSwellDirection(){
        return swellDirection.getString();
    }

    public String getWindSpeed(){
        return windSpeed.getString();
    }

    public String getWindDirection(){
        return windDirection.getString();
    }

    public String getTideHeight(){
        return tideHeight.getString();
    }

    public Integer getTidePosition(){ return tidePosition; }

    public String getRating(){ return rating.getString(); }
}
