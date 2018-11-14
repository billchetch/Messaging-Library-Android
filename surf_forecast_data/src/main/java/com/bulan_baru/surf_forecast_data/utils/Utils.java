package com.bulan_baru.surf_forecast_data.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/*
Utils class for surf forecast data xxx
 */

public class Utils {
    public enum Conversions{ METERS_2_FEET, KPH_2_MPH, KM_2_MILES , ROTATE_180, DEG_2_COMPASS}

    //TODO: change this to use resources so its translatable
    final private static String[] COMPASS = new String[]{"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};

    final private static String CHECK_INTERNET_HOST = "www.google.com";
    final private static int CHECK_INTERNET_PORT = 80;

    public static String convert(String val, Conversions conversion, int dp){
        if(val == null)return val;

        switch(conversion){
            case METERS_2_FEET:
                double mtr = Double.parseDouble(val);
                double ft = mtr*100/(2.54*12);
                return round2string(ft, dp);

            case KM_2_MILES:
            case KPH_2_MPH:
                double km = Double.parseDouble(val);
                double mi = km*0.6;
                return round2string(mi, dp);

            case ROTATE_180:
                double deg = Double.parseDouble(val);
                return round2string((deg + 180.0) % 360.0, dp);

            case DEG_2_COMPASS:
                deg = Double.parseDouble(val);
                int idx = (int)round((deg/360.0)*(double)COMPASS.length, 0) % COMPASS.length;
                return COMPASS[idx];

            default:
                return val;

        }
    }
    public static String convert(double val, Conversions conversion, int dp) {
        return convert(Double.toString(val), conversion, dp);
    }
    public static String convert(float val, Conversions conversion, int dp) {
        return convert(Float.toString(val), conversion, dp);
    }

    public static String convert(int val, Conversions conversion) {
        return convert(Float.toString(val), conversion, 0);
    }

    public static String round2string(double v, int dp){
        if(dp > 0 || dp < 0) {
            return Double.toString(round(v, dp));
        } else {
            int i = (int)round(v, 0);
            return Integer.toString(i);
        }
    }

    public static double round(double v, int dp){
        double shift = Math.pow(10, (double)dp);
        if(dp > 0) {
            return ((double) Math.round(v * shift)) / shift;
        } else if(dp == 0) {
            return Math.round(v);
        } else {
            return v;
        }
    }

    /*
    Date and Calendar methods
     */
    public static Calendar calendarSetHour(Calendar cal, int hour){
        Calendar newCal = (Calendar)cal.clone();
        newCal.set(Calendar.HOUR_OF_DAY, hour);
        newCal.set(Calendar.MINUTE, 0);
        newCal.set(Calendar.SECOND, 0);
        newCal.set(Calendar.MILLISECOND, 0);
        return newCal;
    }

    public static Calendar calendarZeroTime(Calendar cal){
        Calendar newCal = (Calendar)cal.clone();
        newCal.set(Calendar.HOUR_OF_DAY, 0);
        newCal.set(Calendar.MINUTE, 0);
        newCal.set(Calendar.SECOND, 0);
        newCal.set(Calendar.MILLISECOND, 0);
        return newCal;
    }

    public static long dateDiff(Calendar cal1, Calendar cal2, TimeUnit timeUnit){
        long duration = cal1.getTimeInMillis() - cal2.getTimeInMillis();
        switch(timeUnit){
            case HOURS:
                return (long)Math.floor(TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS));

            case DAYS:
                return (long)Math.floor(TimeUnit.DAYS.convert(duration, TimeUnit.MILLISECONDS));

            case MINUTES:
                return (long)Math.floor(TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS));

            case SECONDS:
                return (long)Math.floor(TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS));

            default:
                return 0;
        }
    }

    public static long dateDiff(Date d1, Date d2, TimeUnit timeUnit){
        return dateDiff(date2cal(d1), date2cal(d2), timeUnit);
    }
    public static long dateDiff(Calendar cal1, Calendar cal2){
        return dateDiff(calendarZeroTime(cal1), calendarZeroTime(cal2), TimeUnit.DAYS);
    }

    public static boolean dateInRange(Calendar cal, Calendar cal1, Calendar cal2){
        if(cal1.compareTo(cal2) > 0){
            Calendar swap = (Calendar)cal1.clone();
            cal2 = cal1;
            cal1 = swap;
        }
        return cal.compareTo(cal1) >= 0 && cal.compareTo(cal2) <= 0;
    }

    public static boolean isToday(Calendar cal){
        Calendar cal1 = calendarSetHour(Calendar.getInstance(), 0);
        Calendar cal2 = (Calendar)cal1.clone();
        cal2.add(Calendar.DATE, 1);
        return dateInRange(cal, cal1, cal2);
    }

    public static boolean isToday(Date date){
        return isToday(date2cal(date));
    }

    public static boolean isTomorrow(Calendar cal){
        Calendar c = (Calendar)cal.clone();
        c.add(Calendar.DATE, -1);
        return isToday(c);
    }

    public static boolean isTomorrow(Date date){
        return isTomorrow(date2cal(date));
    }

    public static boolean isYesterday(Calendar cal){
        Calendar c = (Calendar)cal.clone();
        c.add(Calendar.DATE, 1);
        return isToday(c);
    }

    public static boolean isYesterday(Date date){
        return isYesterday(date2cal(date));
    }

    public static List<Calendar> getDates(Calendar cal1, Calendar cal2){
        List<Calendar> dates = new ArrayList<>();
        if(cal1.compareTo(cal2) == 0){
            dates.add(cal1);
            return dates;
        } else if(cal1.compareTo(cal2) > 0){
            Calendar swap = (Calendar)cal1.clone();
            cal2 = cal1;
            cal1 = swap;
        }

        long days = dateDiff(cal2, cal1, TimeUnit.DAYS);
        for(int i = 0; i < days; i++){
            Calendar cal = (Calendar)cal1.clone();
            cal.add(Calendar.DATE, i);
            dates.add(cal);
        }
        return dates;
    }

    public static Calendar date2cal(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public static String formatDate(Date date, String format, TimeZone tz){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        if(tz != null){
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MILLISECOND, tz.getOffset(cal.getTimeInMillis()));
            return sdf.format(cal.getTime());
        } else {
            return sdf.format(date);
        }
    }

    public static String formatDate(Calendar cal, String format, TimeZone tz){
        return formatDate(cal.getTime(), format, tz);
    }

    public static String formatDate(Date date, String format){
        return formatDate(date, format, null);
    }

    public static String formatDate(Calendar cal, String format){
        return formatDate(cal, format, null);
    }

    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    public static boolean isInternetAvailable(String host, int port){
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (IOException e) {
            // Either we have a timeout or unreachable host or failed DNS lookup
            return false;
        }
    }

    public static boolean isInternetAvailable(){
        return isInternetAvailable(CHECK_INTERNET_HOST, CHECK_INTERNET_PORT);
    }


}
