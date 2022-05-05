package net.chetch.messaging;

public class MessageSchema {
    static public final String COMMAND_HELP = "help";

    static public Message createResponse(Message message){
        Message response = new Message();
        MessageType responseType = MessageType.NOT_SET;
        switch(message.Type){
            case STATUS_REQUEST:
                responseType = MessageType.STATUS_RESPONSE; break;

            case CONNECTION_REQUEST:
                responseType = MessageType.CONNECTION_REQUEST_RESPONSE; break;

            case COMMAND:
                responseType = MessageType.COMMAND_RESPONSE; break;

            case PING:
                responseType = MessageType.PING_RESPONSE; break;
        }
        response.Type = responseType;
        response.ResponseID = message.ID;
        response.Target = message.Sender;

        return response;
    }

    public Message message;

    public MessageSchema(){}

    public MessageSchema(Message message){
        this.message = message;
    }
}
