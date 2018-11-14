package com.bulan_baru.surf_forecast;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;


import com.bulan_baru.surf_forecast_data.ClientDevice;
import com.bulan_baru.surf_forecast_data.Forecast;
import com.bulan_baru.surf_forecast_data.Location;
import com.bulan_baru.surf_forecast_data.utils.TypeConverter;
import com.bulan_baru.surf_forecast_data.utils.Utils;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MainActivity extends GenericActivity {
    private static final String LOG_TAG = "Main";
    private Forecast currentForecast;
    private SurfConditionsFragmentAdapter surfConditionsAdapter;
    private Calendar forecastLastDisplayed;
    private Calendar pauseLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        surfConditionsAdapter = new SurfConditionsFragmentAdapter(this, getSupportFragmentManager());

        hideSurfConditions();
        hideProgress();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        hideSurfConditions();
    }


    @Override
    public void onRestart(){
        super.onRestart();

        showSurfConditions();
    }

    private void showSurfConditions(int visibility){
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setVisibility(visibility);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setVisibility(visibility);
    }

    private void showSurfConditions(){ showSurfConditions(View.VISIBLE); }
    private void hideSurfConditions(){ showSurfConditions(View.INVISIBLE); }


    /*
        Assign the particular view model and call the parent
     */
    @Override
    protected void initViewModel(Bundle savedInstanceState){
        viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        super.initViewModel(savedInstanceState);
    }

    @Override
    protected void onTimer(){
        super.onTimer();
    }

    @Override
    public void onDeviceLocationUpdated(ClientDevice device){
        if(pauseLocationUpdates != null && Utils.dateDiff(Calendar.getInstance(), pauseLocationUpdates, TimeUnit.SECONDS) < 30){
            Log.i(LOG_TAG,"Location updates paused");
            return;
        }


        ((MainViewModel)viewModel).getLocationsNearby().observe(this, locations -> {
            if(locations.size() > 0) {
                TypeConverter<Location,String> tc = new TypeConverter<Location,String>(){
                    @Override
                    public String convert(Location loc){
                        String dist = Utils.convert(loc.getDistance(), Utils.Conversions.KM_2_MILES, 1);
                        return loc.getLocation() + " (" + dist + " miles)";
                    }
                };
                List<String> spinnerList = tc.convertList(locations);

                Spinner locationsView = findViewById(R.id.surfLocation);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, spinnerList);
                locationsView.setAdapter(adapter);

                locationsView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        if(currentForecast != null && position < locations.size() && currentForecast.getLocationID() != locations.get(position).getID()) {
                            pauseLocationUpdates = Calendar.getInstance();
                            getForecastForLocation(locations.get(position).getID());
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // your code here
                    }

                });

                pauseLocationUpdates = null;
                getForecastForLocation(locations.get(0).getID());
            }
        });
    }

    protected void getForecastForLocation(int locationID){
        if(currentForecast != null && currentForecast.getLocationID() == locationID && Utils.dateDiff(Calendar.getInstance(), forecastLastDisplayed, TimeUnit.MINUTES) < 20){
            return;
        }

        hideSurfConditions();
        showProgress();
        ((MainViewModel)viewModel).getForecast(locationID).observe(this, forecast -> {

            //save forecast for use elsewhere and update forecast freshness
            setCurrentForecast(forecast);

        });
    }

    protected void setCurrentForecast(Forecast forecast){
        boolean newForecast = (currentForecast == null || currentForecast.getFeedRunID() != forecast.getFeedRunID() || currentForecast.getLocationID() != forecast.getLocationID());
        Calendar now = Calendar.getInstance();
        if(!newForecast && Utils.dateDiff(now, forecastLastDisplayed, TimeUnit.MINUTES) < 20){
            showSurfConditions();
            hideProgress();
            return;
        }

        String s = "Updated ";
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(tz);
        long hoursDiff = Utils.dateDiff(cal, Utils.date2cal(forecast.getCreated()), TimeUnit.HOURS);
        if(hoursDiff <= 0){
            s+= "just now";
        } else if (hoursDiff < 24) {
            s+= Utils.round2string(hoursDiff, 0);
            s+= hoursDiff == 1 ? " hour" : " hours";
            s+= " ago";
        } else if(hoursDiff >= 24){
            double days = Math.floor(hoursDiff/24);
            hoursDiff = hoursDiff - (long)days*24;
            s += Utils.round2string(days, 0) + (days > 1 ? " days" : " day");
            s += " and " + hoursDiff + " hours ago";
        }

        boolean expired = Utils.dateDiff(cal, Utils.date2cal(forecast.getForecastTo())) > 0;
        if(expired){
            s = "Forecast expired ... " + s;
        }

        TextView forecastInfo = findViewById(R.id.forecastInfo);
        forecastInfo.setText(s);

        hideProgress();
        if(!expired) {
            ViewPager viewPager = findViewById(R.id.viewPager);
            int cp = viewPager.getCurrentItem();
            surfConditionsAdapter.setForecast(forecast);
            if (currentForecast == null) { //first time this is ever called
                //get the view pager for the surf conditions
                viewPager.setAdapter(surfConditionsAdapter);
                viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int i, float v, int i1) {

                    }

                    @Override
                    public void onPageSelected(int i) {
                        pauseLocationUpdates = Calendar.getInstance();
                    }

                    @Override
                    public void onPageScrollStateChanged(int i) {

                    }
                });

                TabLayout tabLayout = findViewById(R.id.tabLayout);
                tabLayout.setupWithViewPager(viewPager);
            } else {
                viewPager.setCurrentItem(cp);
            }

            showSurfConditions();
        }

        currentForecast = forecast;
        forecastLastDisplayed = now;
    }
}
