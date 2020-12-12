package net.chetch.messaging.exceptions;

import net.chetch.messaging.Message;

public class MessagingException extends Exception {
    public Message message;

    public MessagingException(String details, Message message){
        super(details);

        this.message = message;
    }
}
