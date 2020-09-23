package net.chetch.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MessageFilter implements IMessageHandler {

    public String Sender;
    private List<MessageType> types = null;
    private List<String> requiredValues = null;

    abstract protected void onMatched(Message message);

    public MessageFilter(String sender, MessageType type, String requiredVals)
    {
        this.Sender = sender;
        this.types = new ArrayList<MessageType>();
        this.types.add(type);
        if(requiredVals != null){
            requiredValues = new ArrayList<>();
            String[] rvals = requiredVals.split(",");
            for(String rv : rvals){
                requiredValues.add(rv.trim());
            }
        }
    }

    public MessageFilter(String sender, MessageType[] types, String requiredVals)
    {
        this.Sender = sender;
        this.types = Arrays.asList(types);
        if(requiredVals != null){
            requiredValues = new ArrayList<>();
            String[] rvals = requiredVals.split(",");
            for(String rv : rvals){
                requiredValues.add(rv.trim());
            }
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

    @Override
    public void handleConnectionError(Exception e, ClientConnection cnn) {
        //empty
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

        if(requiredValues != null && requiredValues.size() > 0)
        {
            for(String s : requiredValues){
                if(!message.hasValue(s))return false;
            }
        }

        return matched;
    }
}
