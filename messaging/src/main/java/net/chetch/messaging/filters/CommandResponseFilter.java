package net.chetch.messaging.filters;

import net.chetch.messaging.Message;
import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageType;

abstract public class CommandResponseFilter extends MessageFilter {

    String originalCommand;

    public CommandResponseFilter(String sender, String requiredValues, String originalCommand) {
        super(sender, MessageType.COMMAND_RESPONSE, requiredValues);

        this.originalCommand = originalCommand;
    }

    public CommandResponseFilter(String sender, String originalCommand) {
        this(sender, null, originalCommand);

    }

    public CommandResponseFilter(String sender) {
        this(sender, null);
    }

    @Override
    protected boolean matches(Message message) {
        boolean matched = super.matches(message);
        if(!matched)return matched;

        if(originalCommand != null){
            return originalCommand.equalsIgnoreCase(message.getString("OriginalCommand"));
        } else {
            return matched;
        }
    }
}