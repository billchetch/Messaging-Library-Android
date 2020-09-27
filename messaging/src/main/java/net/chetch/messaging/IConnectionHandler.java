package net.chetch.messaging;

public interface IConnectionHandler {
    void handleConnectionError(ClientConnection cnn, Exception e);
    void handleConnectionClosed(ClientConnection cnn);
    void handleReconnect(ClientConnection oldCnn, ClientConnection newCnn);
}
