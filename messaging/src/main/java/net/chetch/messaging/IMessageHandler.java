package net.chetch.messaging;

public interface IMessageHandler {
    void handleReceivedMessage(Message message, ClientConnection cnn);
    //void modifySentMessage(Message message, ClientConnection cnn);
    void handleConnectionError(Exception e, ClientConnection cnn);
}
