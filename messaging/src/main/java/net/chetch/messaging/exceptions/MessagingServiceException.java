package net.chetch.messaging.exceptions;

import net.chetch.messaging.Message;
import net.chetch.messaging.MessagingViewModel;

public class MessagingServiceException extends MessagingException {
    public MessagingViewModel.MessagingService messagingService;

    public MessagingServiceException(String message){
        super(message, null);
    }
    public MessagingServiceException(MessagingViewModel.MessagingService ms, String message){
        this(message);
        messagingService = ms;
    }

    public MessagingServiceException(MessagingViewModel.MessagingService ms, String message, Message msg){
        super(message, msg);
        messagingService = ms;
    }
}
