package net.chetch.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.chetch.utilities.EnumTypeAdapater;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Map;

public class Message{
    static Gson gson = null;

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

    public String getString(String key){
        return getValue(key).toString();
    }

    public int getInt(String key){
        Object val = getValue(key);
        if(val instanceof Double){
            return ((Double)val).intValue();
        } else {
            return Integer.parseInt(val.toString());
        }
    }

    public boolean getBoolean(String key){
        String val = getString(key);
        if(val == "true"){
            return true;
        } else if(val == "false"){
            return false;
        } else {
            int n = getInt(key);
            return n > 0;
        }
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

    static public Message deserialize(String serialized){

        initSerializer();

        Message m = gson.fromJson(serialized, Message.class);
        HashMap<String, Object> vals = gson.fromJson(serialized, HashMap.class);

        for(Map.Entry<String, Object> entry : vals.entrySet()){
            m.addValue(entry.getKey(), entry.getValue());
        }

        return m;
    }

    static public Message createResponse(Message message){
        Message response = new Message();
        MessageType responseType = MessageType.NOT_SET;
        switch(message.Type){
            case STATUS_REQUEST:
                responseType = MessageType.STATUS_RESPONSE; break;

            case CONNECTION_REQUEST:
                responseType = MessageType.CONNECTION_REQUEST_RESPONSE; break;

            case COMMAND:
                responseType = MessageType.COMMAND_RESPONSE; break;

            case PING:
                responseType = MessageType.PING_RESPONSE; break;
        }
        response.Type = responseType;
        response.ResponseID = message.ID;
        response.Target = message.Sender;

        return response;
    }
}

