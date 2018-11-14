package com.bulan_baru.surf_forecast_data.injection;

import com.bulan_baru.surf_forecast_data.SurfForecastRepository;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component
public interface RepositoryComponent {
    SurfForecastRepository getRepository();
}
