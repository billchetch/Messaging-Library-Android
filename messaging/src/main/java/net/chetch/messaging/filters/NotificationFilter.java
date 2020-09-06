package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class NotificationFilter extends MessageFilter {

    public NotificationFilter(String sender) {
        super(sender, MessageType.NOTIFICATION);
    }
}
