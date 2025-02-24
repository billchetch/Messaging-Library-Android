package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class NotificationFilter extends MessageFilter {

    public NotificationFilter(String sender, String requiredFields) {
        super(sender, MessageType.NOTIFICATION, requiredFields);
    }

    public NotificationFilter(String sender){
        this(sender, null);
    }

    public NotificationFilter(String sender, String requiredFields, Object ... requiredValues){
        super(sender, MessageType.NOTIFICATION, requiredFields, requiredValues);
    }
}
