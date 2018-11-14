package com.bulan_baru.surf_forecast_data;

public class SurfForecastServiceException extends Exception {
    private int code;

    SurfForecastServiceException(String message, int code){
        super(message);
        this.code = code;
    }

    public int getCode(){ return code; }
}
