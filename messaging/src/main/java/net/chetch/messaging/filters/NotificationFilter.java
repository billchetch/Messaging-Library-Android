package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class NotificationFilter extends MessageFilter {

    public NotificationFilter(String sender, String requiredValues) {
        super(sender, MessageType.NOTIFICATION, requiredValues);
    }

    public NotificationFilter(String sender){
        this(sender, null);
    }
}
