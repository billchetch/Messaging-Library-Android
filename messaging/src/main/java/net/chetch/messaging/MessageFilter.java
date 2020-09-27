package net.chetch.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MessageFilter implements IMessageHandler {

    public String Sender;
    private List<MessageType> types = null;
    private List<String> requiredKeys= null;
    private List<Object> requiredValues = null;

    abstract protected void onMatched(Message message);

    public MessageFilter(String sender, MessageType type, String reqKeys, Object ...requiredVals)
    {
        this.Sender = sender;
        this.types = new ArrayList<MessageType>();
        this.types.add(type);
        if(reqKeys != null){
            requiredKeys = new ArrayList<>();
            String[] rkeys = reqKeys.split(",");
            for(String rk : rkeys){
                requiredKeys.add(rk.trim());
            }
        }

        if(requiredVals != null && requiredVals.length > 0 && requiredVals.length == requiredKeys.size()) {
            requiredValues = Arrays.asList(requiredVals);
        }
    }

    public MessageFilter(String sender, MessageType[] types, String reqKeys, Object ...requiredVals)
    {
        this.Sender = sender;
        this.types = Arrays.asList(types);
        if(reqKeys != null){
            requiredKeys = new ArrayList<>();
            String[] rkeys = reqKeys.split(",");
            for(String rk : rkeys){
                requiredKeys.add(rk.trim());
            }
        }

        if(requiredVals != null && requiredVals.length > 0) {
            requiredValues = Arrays.asList(requiredVals);
        }
    }

    public MessageFilter(String sender, MessageType type){
        this(sender, type, null);
    }

    public MessageFilter(String sender, MessageType[] types)
    {
        this(sender, types, null);
    }


    @Override
    public void handleReceivedMessage(Message message, ClientConnection cnn) {
        if (matches(message))
        {
            onMatched(message);
        }
    }

    protected boolean matches(Message message)
    {
        boolean matched = true;
        if (Sender != null && message.Sender != null)
        {
            matched = Sender.equals(message.Sender);
            if (!matched) return false;
        }

        if (types != null && types.size() > 0)
        {
            matched = types.contains(message.Type);
            if (!matched) return false;
        }

        if(requiredKeys != null && requiredKeys.size() > 0)
        {
            for(String k : requiredKeys){
                if(!message.hasValue(k))return false;
            }
        }

        if(requiredValues != null && requiredValues.size() > 0)
        {
            for(int i = 0; i < requiredKeys.size(); i++){
                String k = requiredKeys.get(i);
                Object v = requiredValues.get(i);
                Object mv = message.getValue(k);

                if((v == null && mv != null) || (v != null && !v.equals(mv)))return false;
            }
        }

        return matched;
    }
}
