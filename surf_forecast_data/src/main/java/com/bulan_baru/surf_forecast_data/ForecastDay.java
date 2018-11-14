package com.bulan_baru.surf_forecast_data;

import com.bulan_baru.surf_forecast_data.utils.Utils;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForecastDay extends ForecastDetail {
    @SerializedName("tide_extreme_type_1")
    private String tideExtremeType1;

    @SerializedName("tide_extreme_height_1")
    private WeightedDetail tideExtremeHeight1;

    @SerializedName("tide_extreme_time_1")
    private WeightedDetail tideExtremeTime1;

    @SerializedName("tide_extreme_type_2")
    private String tideExtremeType2;

    @SerializedName("tide_extreme_height_2")
    private WeightedDetail tideExtremeHeight2;

    @SerializedName("tide_extreme_time_2")
    private WeightedDetail tideExtremeTime2;

    @SerializedName("tide_extreme_type_3")
    private String tideExtremeType3;

    @SerializedName("tide_extreme_height_3")
    private WeightedDetail tideExtremeHeight3;

    @SerializedName("tide_extreme_time_3")
    private WeightedDetail tideExtremeTime3;

    @SerializedName("tide_extreme_type_4")
    private String tideExtremeType4;

    @SerializedName("tide_extreme_height_4")
    private WeightedDetail tideExtremeHeight4;

    @SerializedName("tide_extreme_time_4")
    private WeightedDetail tideExtremeTime4;

    @SerializedName("first_light")
    private Date firstLight;

    @SerializedName("last_light")
    private Date lastLight;

    public List<TideData> getTideData(){
        WeightedDetail[] weightedHeights = new WeightedDetail[]{tideExtremeHeight1, tideExtremeHeight2, tideExtremeHeight3, tideExtremeHeight4};
        WeightedDetail[] weightedTimes = new WeightedDetail[]{tideExtremeTime1, tideExtremeTime2, tideExtremeTime3, tideExtremeTime4};
        String[] tideExtremes = new String[]{tideExtremeType1, tideExtremeType2, tideExtremeType3, tideExtremeType4};

        DateFormat dateFormat = new SimpleDateFormat(SurfForecastService.DATE_FORMAT);
        List<TideData> tideData = new ArrayList<>();
        for(int i = 0; i < weightedHeights.length; i++) {
            if(tideExtremes[i] == null || tideExtremes[i] == "")continue;

            TideData td = new TideData();
            td.height = Double.parseDouble(weightedHeights[i].getString());
            td.position = tideExtremes[i];
            String dt = Utils.formatDate(date, SurfForecastService.DATE_ONLY_FORMAT);
            String[] keyParts = dt.split(" ");
            dt = keyParts[0] + " " + weightedTimes[i].getString() + " " + keyParts[1];
            try {
                td.time = dateFormat.parse(dt);
                tideData.add(td);
            } catch (Exception e){

            }
        }
        return tideData;
    }


    public Date getFirstLight(){
        return firstLight;
    }
    public Date getLastLight(){
        return lastLight;
    }

}
