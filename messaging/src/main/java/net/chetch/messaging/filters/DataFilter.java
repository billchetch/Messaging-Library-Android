package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class DataFilter extends MessageFilter {

    public DataFilter(String sender, String requiredValues) {
        super(sender, MessageType.DATA, requiredValues);
    }

    public DataFilter(String sender){
        this(sender, null);
    }
}
