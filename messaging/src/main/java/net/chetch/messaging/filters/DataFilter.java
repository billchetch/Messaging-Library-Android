package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class DataFilter extends MessageFilter {

    public DataFilter(String sender, String requiredFields) {
        super(sender, MessageType.DATA, requiredFields);
    }

    public DataFilter(String sender){
        this(sender, null);
    }

    public DataFilter(String sender, String requiredFields, Object ... requiredValues){
        super(sender, MessageType.DATA, requiredFields, requiredValues);
    }
}
