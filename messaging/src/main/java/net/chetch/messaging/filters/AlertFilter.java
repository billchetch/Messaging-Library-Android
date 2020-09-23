package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class AlertFilter extends MessageFilter {

    public AlertFilter(String sender, String requiredValues) {
        super(sender, MessageType.ALERT, requiredValues);
    }

    public AlertFilter(String sender){
        this(sender, null);
    }
}
