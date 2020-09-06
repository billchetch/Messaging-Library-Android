package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class DataFilter extends MessageFilter {

    public DataFilter(String sender) {
        super(sender, MessageType.DATA);
    }
}
