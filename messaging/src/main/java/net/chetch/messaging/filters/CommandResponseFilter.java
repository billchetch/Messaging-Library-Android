package net.chetch.messaging.filters;

import net.chetch.messaging.Message;
import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class CommandResponseFilter extends MessageFilter {

    public CommandResponseFilter(String sender, String originalCommand) {
        super(sender, MessageType.COMMAND_RESPONSE, "OriginalCommand", originalCommand);
    }

    public CommandResponseFilter(String sender, String originalCommand, String requiredKeys, Object ... requiredVals) {
        super(sender, MessageType.COMMAND_RESPONSE, (requiredKeys != null && requiredKeys.length() > 0 ? requiredKeys + "," : "") + "OriginalCommand", requiredVals);
    }

}