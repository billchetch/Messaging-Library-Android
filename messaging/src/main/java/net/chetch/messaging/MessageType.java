package net.chetch.messaging;

public enum MessageType {
    NOT_SET,
    REGISTER_LISTENER,
    CUSTOM,
    INFO,
    WARNING,
    ERROR,
    PING,
    PING_RESPONSE,
    STATUS_REQUEST,
    STATUS_RESPONSE,
    COMMAND,
    ERROR_TEST,
    ECHO,
    ECHO_RESPONSE,
    CONFIGURE,
    CONFIGURE_RESPONSE,
    RESET,
    INITIALISE,
    DATA,
    CONNECTION_REQUEST,
    CONNECTION_REQUEST_RESPONSE,
    SHUTDOWN,
    SUBSCRIBE,
    UNSUBSCRIBE,
    COMMAND_RESPONSE,
    TRACE,
    NOTIFICATION,
    SUBSCRIBE_RESPONSE,
    INITIALISE_RESPONSE,
    ALERT,
    FINALISE
}
