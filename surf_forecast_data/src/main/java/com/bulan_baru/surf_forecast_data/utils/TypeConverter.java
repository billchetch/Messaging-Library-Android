package com.bulan_baru.surf_forecast_data.utils;

import java.util.ArrayList;
import java.util.List;

public class TypeConverter<A,B> {

    public B convert(A a){
        return null;
    }

    public List<B> convertList(List<A> list){
        List<B> convertedList = new ArrayList<>();
        for(A a : list){
            convertedList.add(convert(a));
        }
        return convertedList;
    }
}
