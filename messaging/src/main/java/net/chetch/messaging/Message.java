package net.chetch.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.chetch.utilities.CalendarTypeAdapater;
import net.chetch.utilities.DelegateTypeAdapterFactory;
import net.chetch.utilities.EnumTypeAdapater;
import net.chetch.utilities.Utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class Message{
    static public String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    static Gson gson = null;
    //basic conversions based on expected results after deserialization (such as all numbers becom doubles)
    //or enums are usually their ordinal value but might be their string representation
    static private <T> T convert(Object value, Class<T> cls){
        if(value == null)return null;

        if(cls == Boolean.class){
            Boolean b;
            if(value.toString() == "true"){
                b = true;
            } else if(value.toString() == "false"){
                b = false;
            } else {
                Integer n = convert(value, Integer.class);
                b =  n > 0;
            }
            return (T)b;
        } else if(cls == Integer.class){
            Integer n;
            if(value instanceof Double){
                n = ((Double)value).intValue();
            } else {
                n = Integer.parseInt(value.toString());
            }
            return (T)n;
        } else if(cls == Long.class){
            Long n;
            if(value instanceof Double){
                n = ((Double)value).longValue();
            } else {
                n = Long.parseLong(value.toString());
            }
            return (T)n;
        } else if(cls == String.class){
            return (T)value.toString();
        } else if(Enum.class.isAssignableFrom(cls)){
            try{
                Integer n = convert(value, Integer.class);
                for(T evalue : cls.getEnumConstants()){
                    if(((Enum)evalue).ordinal() == n )return (T)evalue;
                }
            } catch(Exception e){
                //try a string
                for(T evalue : cls.getEnumConstants()){
                    if(((Enum)evalue).name() == value.toString())return (T)evalue;
                }
            }
        } else if(cls == Double.class){
            return (T)(Object)Double.parseDouble((value.toString()));
        }
        return null;
    }


    public String ID;
    public String ResponseID;
    public MessageType Type;
    int SubType;
    public String Target;
    public String Sender;
    public String Signature;

    HashMap<String, Object> Values = new HashMap<>();

    public Message(){
        ID = generateID();
    }

    private String generateID(){
        return this.hashCode() + "-" + Calendar.getInstance().getTimeInMillis();
    }

    public Object getValue(String key){
        if(Values.containsKey(key)){
            return Values.get(key);
        } else {
            return null;
        }
    }

    public Object getValue(){
        return getValue("Value");
    }

    public void setValue(Object val){
        addValue("Value", val);
    }

    public boolean hasValue(String key){
        return Values.containsKey(key);
    }

    public boolean hasValue(){ return hasValue("Value"); }

    public String getString(String key){
        Object v = getValue(key);
        return v == null ? null : v.toString();
    }

    public int getInt(String key){
        Object val = getValue(key);
        return convert(val, Integer.class);
    }

    public boolean getBoolean(String key){
        Object val = getValue(key);
        return convert(val, Boolean.class);
    }

    public long getLong(String key){
        Object val = getValue(key);
        return convert(val, Long.class);
    }

    public double getDouble(String key){
        Object val = getValue(key);
        return convert(val, Double.class);
    }

    public <T extends Enum> T getEnum(String key, Class<T> cls){
        return getEnum(key, cls, null);
    }

    public <T extends Enum> T getEnum(String key, Class<T> cls, T defaultValue){
        Object value = getValue(key);
        T t = convert(value, cls);
        return t == null ? defaultValue : t;
    }

    public Calendar getCalendar(String key){
        try {
            return Utils.parseDate(getString(key), DEFAULT_DATE_FORMAT);
        } catch (Exception e){
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> cls){
        List<Object> l =  (List<Object>)getValue(key);
        if(l == null)return null;
        List<T> l2r = new ArrayList<>();

        for(Object item : l){
            T value = convert(item, cls);
            l2r.add(value);
        }

        return l2r;
    }

    public <T> Map<String, T> getMap(String key, Class<T> cls){
        Map<String, Object> map = (Map<String, Object>)getValue(key);
        if(map == null)return null;

        HashMap<String, T> map2return = new HashMap<>();
        for(Map.Entry<String, Object> entry : map.entrySet()){
            T value = convert(entry.getValue(), cls);
            map2return.put(entry.getKey(), value);
        }
        return map2return;
    }

    public void addValue(String key, Object val){
        Values.put(key, val);
    }

    public String toStringHeader(){
        String lf = System.lineSeparator();
        String result = "ID: " + ID + lf;
        result += "ResponseID: " + ResponseID + lf;
        result += "Type: " + Type + lf;
        result += "SubType: " + SubType + lf;
        result += "Target: " + Target + lf;
        result += "Sender: " + Sender;
        return result;

    }

    @Override
    public String toString(){
        String result = toStringHeader();

        return result;
    }

    static private void initSerializer(){
        if(gson == null){
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(MessageType.class, new EnumTypeAdapater<MessageType>(MessageType.class));
            gson = builder.create();
        }
    }

    public String serialize(){

        initSerializer();

        HashMap<String, Object> vals = new HashMap<>();
        vals.put("ID", ID);
        vals.put("ResponseID", ResponseID);
        vals.put("Type", Type);
        vals.put("SubType", SubType);
        vals.put("Target", Target);
        vals.put("Sender", Sender);
        vals.put("Signature", Signature);

        for(Map.Entry<String, Object> entry : Values.entrySet()){
            vals.put(entry.getKey(), entry.getValue());
        }

        String serialized = gson.toJson(vals);
        return serialized;
    }

    static private Object extractValue(HashMap<String, Object> vals, String key){
        Object val = vals.get(key);
        vals.remove(key);
        return val;
    }

    static public List<String> split(String serialized){
        List<String> splitted = new ArrayList<>();
        if (serialized.indexOf("}{") >= 0) {
            serialized = "}" + serialized + "{";
            String[] parts = serialized.split("\\}\\{");
            for(String part : parts)
            {
                if(part == null)continue;
                part = part.trim();
                if(part.isEmpty())continue;
                splitted.add("{" + part + "}");
            }
        } else {
            splitted.add(serialized);
        }

        return splitted;
    }

    static public Message deserialize(String serialized){

        initSerializer();

        Message m = gson.fromJson(serialized, Message.class);
        HashMap<String, Object> vals = gson.fromJson(serialized, HashMap.class);

        for(Map.Entry<String, Object> entry : vals.entrySet()){
            m.addValue(entry.getKey(), entry.getValue());
        }

        return m;
    }
}

