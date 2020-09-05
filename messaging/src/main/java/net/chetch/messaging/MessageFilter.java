package net.chetch.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MessageFilter implements IMessageHandler {

    public String Sender;
    private List<MessageType> types = null;

    abstract protected void onMatched(Message message);

    public MessageFilter(String sender, MessageType type)
    {
        this.Sender = sender;
        this.types = new ArrayList<MessageType>();
        this.types.add(type);
    }

    public MessageFilter(String sender, MessageType[] types)
    {
        this.Sender = sender;
        this.types = Arrays.asList(types);
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

        return matched;
    }
}
