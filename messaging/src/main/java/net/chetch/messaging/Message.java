package net.chetch.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;

import net.chetch.utilities.CalendarTypeAdapater;
import net.chetch.utilities.DelegateTypeAdapterFactory;
import net.chetch.utilities.EnumTypeAdapater;
import net.chetch.utilities.Utils;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Message{
    final static public String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ssXXX";
    static public String dateFormat = DEFAULT_DATE_FORMAT;
    static Gson gson = null;
    static public void setDateFormat(String df){
        dateFormat = df;
    }

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
                Method m = null;
                try {
                    m = cls.getMethod("getValue");
                } catch (NoSuchMethodException e){
                    //faile silently
                    m = null;
                } catch (Exception e){
                    throw e;
                }
                Integer v;
                for(T evalue : cls.getEnumConstants()){
                    if(m != null){
                        v = (Integer)m.invoke(evalue);
                    } else{
                        v = ((Enum)evalue).ordinal();
                    }
                    if(n.equals(v))return (T)evalue;
                }
            } catch(Exception e){
                //try a string
                for(T evalue : cls.getEnumConstants()){
                    if(((Enum)evalue).name() == value.toString())return (T)evalue;
                }
            }
        } else if(cls == Double.class){
            return (T)(Object)Double.parseDouble((value.toString()));
        } else {
            String serialized = gson.toJson(value);
            return gson.fromJson(serialized, cls);
        }
        return null;
    }


    public String ID;
    public String ResponseID;
    public MessageType Type;
    public int SubType;
    public String Target;
    public String Sender;
    public String Tag;
    public String Signature;

    Map<String, Object> Body = new TreeMap<>();

    public Message(){
        ID = generateID();
    }

    private String generateID(){
        return this.hashCode() + "-" + Calendar.getInstance().getTimeInMillis();
    }

    public Map<String, Object> getBody(){
        return Body;
    }

    public Object getValue(String key){
        if(Body.containsKey(key)){
            return Body.get(key);
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
        return Body.containsKey(key);
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

    public Calendar getCalendar(String key){
        return getCalendar(key, null);
    }

    public Calendar getCalendar(String key, String format){
        try {
            String dateString = getString(key);
            if(dateString == null || dateString.isEmpty() || dateString.indexOf("0001-01-01") != -1){
                return null;
            } else {
                if(format == null)format = dateFormat; //DEFAULT_DATE_FORMAT;
                return Utils.parseDate(getString(key), format);
            }
        } catch (Exception e){
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> cls){
        List<Object> l =  (List<Object>)getValue(key);
        if(l == null)return null;
        List<T> l2r = new ArrayList<>();

        for(Object item : l){
            //T value = convert(item, cls);
            T value = getAsClass(cls, item);
            l2r.add(value);
        }

        return l2r;
    }

    public <T> Map<String, T> getMap(String key, Class<T> cls){
        Map<String, Object> map = (Map<String, Object>)getValue(key);
        if(map == null)return null;

        HashMap<String, T> map2return = new HashMap<>();
        for(Map.Entry<String, Object> entry : map.entrySet()){
            //probably replace this with getAsClass
            T value = convert(entry.getValue(), cls);
            map2return.put(entry.getKey(), value);
        }
        return map2return;
    }

    public <T> T getAsClass(Class<T> cls, Object value) {
        String serialized = gson.toJson(value);
        return gson.fromJson(serialized, cls);
    }

    public <T> T getAsClass(String key, Class<T> cls){
        Object value = key == null ? Body : getValue(key);
        return getAsClass(cls, value);
    }

    public <T> T getAsClass(Class<T> cls){
        return getAsClass(cls, Body);
    }

    public void setValue(String key, Object val){
        addValue("key", val);
    }
    public void addValue(String key, Object val){
        Body.put(key, val);
    }

    public String toStringHeader(){
        String lf = System.lineSeparator();
        String result = "ID: " + ID + lf;
        result += "ResponseID: " + ResponseID + lf;
        result += "Type: " + Type + lf;
        result += "SubType: " + SubType + lf;
        result += "Target: " + Target + lf;
        result += "Tag: " + Tag + lf;
        result += "Sender: " + Sender;
        return result;
    }

    public String toString(boolean includeBody){
        String result = toStringHeader();

        if(includeBody) {
            String lf = System.lineSeparator();
            result += lf + lf;
            result += getBody().toString();
        }
        return result;
    }

    @Override
    public String toString(){
        return toString(true);
    }

    static private void initSerializer(){
        if(gson == null){
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(MessageType.class, new EnumTypeAdapater<MessageType>(MessageType.class));
            builder.registerTypeAdapter(Calendar.class, new CalendarTypeAdapater(dateFormat));
            builder.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
            gson = builder.create();
        }
    }

    public String serialize(){

        initSerializer();

        Map<String, Object> vals = new HashMap<>();
        vals.put("ID", ID);
        vals.put("ResponseID", ResponseID);
        vals.put("Type", Type);
        vals.put("SubType", SubType);
        vals.put("Target", Target);
        vals.put("Sender", Sender);
        vals.put("Tag", Tag);
        vals.put("Signature", Signature);

        Map<String, Object> body = new HashMap<>();
        for(Map.Entry<String, Object> entry : Body.entrySet()){
            body.put(entry.getKey(), entry.getValue());
        }
        vals.put("Body", body);

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
        return m;
    }
}

