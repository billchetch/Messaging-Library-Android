package net.chetch.messaging.exceptions;

import net.chetch.messaging.Message;

public class ConnectClientException extends MessagingException{

    public ConnectClientException(String details, Message message) {
        super(details, message);
    }
}
