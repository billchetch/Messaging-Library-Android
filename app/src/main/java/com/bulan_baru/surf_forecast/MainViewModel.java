package com.bulan_baru.surf_forecast;

import android.arch.lifecycle.LiveData;

import com.bulan_baru.surf_forecast_data.Forecast;
import com.bulan_baru.surf_forecast_data.Location;

import java.util.List;

public class MainViewModel extends GenericViewModel {

    public LiveData<List<Location>> getLocationsNearby(){
        return surfForecastRepository.getLocationsNearby();
    }

    public LiveData<Forecast> getForecast(int locationID){
        return surfForecastRepository.getForecast(locationID);
    }

}
