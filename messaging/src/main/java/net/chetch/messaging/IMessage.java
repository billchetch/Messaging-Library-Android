package net.chetch.messaging;

public interface IMessage {
    MessageType Type = MessageType.NOT_SET;

    void deserialize(byte[] bytes) throws Exception;

    byte[] serialize() throws Exception;
}
