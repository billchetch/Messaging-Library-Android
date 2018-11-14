package com.bulan_baru.surf_forecast_data;

import java.util.Date;

public class ForecastDetail extends DataObject {
    public Date date;

    class WeightedDetail{
        private String weighted_values;
        private String weighted_sum;
        private String weighted_average;

        public String getString(){
            return weighted_average;
        }
    }

    public class TideData{
        public Date time;
        public double height;
        public String position;
        public boolean interpolated = false;
    }
}
