package net.chetch.messaging;

public enum MessageType {
    NOT_SET,

    //BROADCAST Bit 4 = 0 (mask 0xxx)
    WARNING,
    ERROR,
    NOTIFICATION,
    ALERT,
    PRESENCE,
    DATA,
    XDATA,

    //TARGETED Bit 4 = 1 (mask 1xxx)
    PING,
    STATUS_REQUEST,
    COMMAND,
    INITIALISE,
    CONFIGURE,
    ERROR_TEST, //Intended for ERROR as a response
    RESET,
    FINALISE,

    //RESPONSES Bit 5 = 1 and 4 = 0 (mask 10xxx)
    PING_RESPONSE,
    STATUS_RESPONSE,
    COMMAND_RESPONSE,
    INITIALISE_RESPONSE,
    CONFIGURE_RESPONSE,
    RESET_RESPONSE,
    CONNECTION_REQUEST_RESPONSE,
    SUBSCRIBE_RESPONSE,

    //MISC Bit 5 = 1 and 4 = 1 (mask 11xxx)
    INFO,
    ECHO,
    ECHO_RESPONSE,

    SHUTDOWN,
    SUBSCRIBE,
    UNSUBSCRIBE,
    TRACE,
    REGISTER_LISTENER,
    CONNECTION_REQUEST
}
