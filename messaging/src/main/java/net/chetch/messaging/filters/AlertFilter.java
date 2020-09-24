package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class AlertFilter extends MessageFilter {

    public AlertFilter(String sender, String requiredKeys) {
        super(sender, MessageType.ALERT, requiredKeys);
    }

    public AlertFilter(String sender){
        this(sender, null);
    }

    public AlertFilter(String sender, String requiredFields, Object ... requiredValues){
        super(sender, MessageType.ALERT, requiredFields, requiredValues);
    }
}
