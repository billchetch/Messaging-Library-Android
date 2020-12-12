package net.chetch.messaging.exceptions;

public class MessagingServiceException extends MessagingException {
    public MessagingServiceException(String message){
        super(message, null);
    }
}
