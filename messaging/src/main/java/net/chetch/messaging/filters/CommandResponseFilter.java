package net.chetch.messaging.filters;

import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class CommandResponseFilter extends MessageFilter {

    public CommandResponseFilter(String sender) {
        super(sender, MessageType.COMMAND_RESPONSE);
    }
}
