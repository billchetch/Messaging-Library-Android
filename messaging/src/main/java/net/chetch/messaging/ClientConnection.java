package net.chetch.messaging;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract public class ClientConnection {
    public static String createSignature(String authToken, String sender) throws Exception{
        if(authToken == null || authToken.isEmpty())
        {
            throw new Exception("Cannot create signature without AuthToken");
        }
        if(sender == null || sender.isEmpty()){
            throw new Exception("Cannot create signature without Sender");
        }
        return authToken + "-" + sender;
    }

    public enum ConnectionState
    {
        NOT_SET,
        OPENING,
        OPENED,
        CONNECTED,
        RECEIVING,
        SENDING,
        CLOSING,
        CLOSED
    }

    public ClientManager mgr = null;

    protected String id;
    protected String name;

    protected InputStream inputStream;
    protected OutputStream outputStream;

    protected boolean remainConnected = false;
    protected int connectionTimeout = 0;
    protected int activityTimeout = 0;
    public boolean reconnect = true;

    private ConnectionState state = ConnectionState.NOT_SET;
    private Thread cnnInboundThread = null;
    private Thread cnnOutboundThread = null;

    private ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    public String serverID;
    public String authToken;
    public boolean signMessage = true;

    private int messagesSent = 0;
    private int messagesReceived = 0;
    private int garbageReceived = 0;

    private List<IMessageHandler> messageHandlers = new ArrayList<>();
    private List<IConnectionHandler> connectionHandlers = new ArrayList<>();
    protected List<String> subscriptions = new ArrayList<>();

    abstract public String getConnectionString();
    abstract public void parseConnectionString(String connectionString) throws Exception;
    abstract public void parseMessage(Message message) throws Exception;
    abstract protected void connect() throws Exception;

    public ClientConnection(){
        //empty
    }

    protected void setState(ConnectionState newState){
        if(newState != state){
            state = newState;

            switch(state){
                case CONNECTED:
                    onConnected();
                    break;
            }
        }
    }

    public boolean isConnected(){
        return state == ConnectionState.CONNECTED || state == ConnectionState.RECEIVING || state == ConnectionState.SENDING;
    }

    public void addMessageHandler(IMessageHandler handler){
        if(!messageHandlers.contains(handler)) {
            messageHandlers.add(handler);
        }
    }

    public void removeMessageHandler(IMessageHandler handler){
        if(messageHandlers.contains(handler)){
            messageHandlers.remove(handler);
        }
    }

    public List<IMessageHandler> getMessageHandlersList(){
        List<net.chetch.messaging.IMessageHandler> temp = new ArrayList<>();
        for(net.chetch.messaging.IMessageHandler h : messageHandlers){
            temp.add(h);
        }
        return temp;
    }

    public void addConnectionHandler(IConnectionHandler handler){
        if(!connectionHandlers.contains(handler)) {
            connectionHandlers.add(handler);
        }
    }

    public void removeConnectionHandler(IConnectionHandler handler){
        if(connectionHandlers.contains(handler)){
            connectionHandlers.remove(handler);
        }
    }

    public List<IConnectionHandler> getConnectionHandlersList(){
        List<IConnectionHandler> temp = new ArrayList<>();
        for(IConnectionHandler h : connectionHandlers){
            temp.add(h);
        }
        return temp;
    }

    public void handleConnectionError(Exception e){
        Log.e("CC", id + " exception: " + e.getMessage());

        //we split in to temp to allow for manipulation of handlers list within a particular handler
        List<IConnectionHandler> temp = getConnectionHandlersList();
        for(IConnectionHandler h : temp) {
            h.handleConnectionError(this, e);
        }

        if(mgr != null){
            mgr.handleConnectionError(this, e);
        }
    }

    public void handleReconnect(ClientConnection newCnn){
        if(newCnn == this)return;

        List<IConnectionHandler> ctemp = getConnectionHandlersList();
        for(IConnectionHandler h : ctemp) {
            h.handleReconnect(this, newCnn);
            newCnn.addConnectionHandler(h);
        }
        List<IMessageHandler> mtemp = getMessageHandlersList();
        for(IMessageHandler h : mtemp) {
            newCnn.addMessageHandler(h);
        }
        for(String clientName : subscriptions){
            try {
                newCnn.subscribe(clientName);
            } catch (Exception e){
                Log.e("CC", "Re-subscribint on reconnect gives " + e.getMessage());
            }
        }
    }

    public void open(){
        //TODO: exception if null etc. to avoid repeat calls

        setState(ConnectionState.OPENING);

        final ClientConnection ccn = this;
        Runnable runnable = new Runnable(){
            public void run(){

                try{
                    ccn.connect();
                } catch (Exception e){
                    ccn.handleConnectionError(e);
                } finally{
                    ccn.close();
                }
            }
        };

        cnnInboundThread = new Thread(runnable);

        runnable = new Runnable(){
            public void run(){

                try{
                    ccn.pollMessageQueue();
                } catch (Exception e){
                    ccn.handleConnectionError(e);
                } finally{
                    ccn.close();
                }
            }
        };
        cnnOutboundThread = new Thread(runnable);

        cnnInboundThread.start();
        cnnOutboundThread.start();
    }

    public void close(){
        setState(ConnectionState.CLOSING);
        Log.i("ClientConnection", "Closing");
        try {
            if(inputStream != null)inputStream.close();
            if(outputStream != null)outputStream.close();
        } catch (Exception e){
            Log.e("CC", "ClientConnection::close " + e.getMessage());
        }
        setState(ConnectionState.CLOSED);

        //we split in to temp to allow for manipulation of handlers list within a particular handler
        List<IConnectionHandler> temp = getConnectionHandlersList();
        for(IConnectionHandler h : temp) {
            h.handleConnectionClosed(this);
        }
    }

    protected void onConnected(){
        if(mgr != null){
            mgr.onConnectionConnected(this);
        }
    }

    protected void write(String str) throws IOException{
        outputStream.write(str.getBytes());
    }

    public String createSignature(String sender) throws Exception{
        return createSignature(authToken, sender == null ? name : sender);
    }

    public void pollMessageQueue() throws Exception{
        Log.i("CC", id + " starting poll messages");
        do{
            if(isConnected() && messageQueue.size() > 0){
                Message message = messageQueue.poll();
                Log.i("CC", id + " poll sending message: " + message.toString());
                sendMessage(message);
            }
            Thread.sleep(200);
        } while(state != ConnectionState.CLOSING && state != ConnectionState.NOT_SET.CLOSED);
        Log.i("CC", id + " exiting poll messages");
    }

    //use this method for general sending
    public void send(Message message){
        messageQueue.add(message);
    }

    //this method is used by the pollQueue method to send a message
    protected void sendMessage(Message message) throws Exception{
        if (name != null && message.Sender == null)
        {
            message.Sender = name;
        }

        if(signMessage){
            message.Signature = createSignature(message.Sender);
        }
        String serialized = message.serialize();
        write(serialized);
        messagesSent++;
    }

    protected String read() throws IOException{
        byte[] buffer = new byte[4096];
        int read = inputStream.read(buffer, 0, 4096); //This is blocking
        String result = "";
        while(read != -1){
            byte[] tempdata = new byte[read];
            System.arraycopy(buffer, 0, tempdata, 0, read);
            String temp  = new String(tempdata);
            result += temp;
            if(inputStream.available() == 0 && result.substring(result.length() - 1).equals("}")){
                break;
            }
            read = inputStream.read(buffer, 0, 4096); //This is blocking
        }
        return result;
    }


    protected void receiveMessage() throws Exception {
        Message message;
        String data;
        try {
            data = read();
            if (data == null || data.isEmpty()) {
                throw new Exception("read returned empty for " + id);
            }
        } catch (IOException e) {
            //Log.e("CC", e.getMessage());
            throw e;
        }

        //Log.i("CC", "Received: " + data);
        List<String> splitted = Message.split(data);
        List<Exception> exceptions = new ArrayList<>();
        for (String serialized : splitted){
            try {
                message = Message.deserialize(serialized);
                messagesReceived++;
            } catch (Exception e) {
                garbageReceived++;
                continue;
            }

            try {
                handleReceivedMessage(message);
            } catch (Exception e) {
                Log.e("CC", e.getMessage() == null ? "no error message" : e.getMessage());
                exceptions.add(e);
            }
        }

        if(exceptions.size() > 0)throw new Exception("Message.receiveMessage: resulted in " + exceptions.size() + " exceptions");
    }

    public void handleReceivedMessage(Message message){
        if(mgr != null){
            mgr.handleReceivedMessage(this, message);
        }

        Message response;
        switch(message.Type){
            case STATUS_REQUEST:
                response = MessageSchema.createResponse(message);
                response.addValue("ConnectionID", id);
                response.addValue("Name", name);
                response.addValue("Context", "CONTROLLER");
                response.addValue("MessagesSent", messagesSent);
                response.addValue("MessagesReceived", messagesReceived);
                response.addValue("GarbageReceived", garbageReceived);
                response.addValue("State", state);
                response.addValue("MessageEncoding", "JSON");
                Log.i("CC", id + " responding to STATUS_REQUEST");

                send(response);
                break;

            case PING:
                response = MessageSchema.createResponse(message);
                send(response);
                break;

            default:
                //we split in to temp to allow for manipulation of handlers list within a particular handler
                List<IMessageHandler> temp = getMessageHandlersList();
                for(IMessageHandler h : temp) {
                    h.handleReceivedMessage(message, this);
                }
                break;
        }
    }

    public void sendPing(String target){
        Message message = new Message();
        message.Type = MessageType.PING;
        if(target != null)message.Target = target;
        send(message);
    }

    public void sendPing(){
        sendPing(null);
    }

    public void requestServerStatus(){
        Message message = new Message();
        message.Type = MessageType.STATUS_REQUEST;
        message.Target = serverID;
        send(message);
    }


    public void sendCommand(String target, String command, Object ...args){
        List<Object> largs = null;
        if(args != null) {
            largs = Arrays.asList(args);
        }
        sendCommand(target, command, largs);
    }

    public void sendCommand(String target, String command, List<Object> arguments){
        Message message = new Message();
        message.Type = MessageType.COMMAND;
        message.Target = target;
        message.setValue(command);
        if(arguments != null){
            message.addValue("Arguments", arguments);
        }
        send(message);
    }

    public void subscribe(MessageFilter messageFilter) throws Exception
    {
        if(messageFilter.Sender == null || messageFilter.Sender == "")
        {
            throw new Exception("To subscribe the message filter must have a Sender value");
        }

        addMessageHandler(messageFilter);
        subscribe(messageFilter.Sender);
    }

    public void subscribe(String clientName) throws Exception{
        if(clientName == null || clientName.isEmpty())throw new Exception("There must be a client to subsribe to");

        Message msg = new Message();
        msg.Type = MessageType.SUBSCRIBE;
        msg.setValue("Subscription request from " + name);
        msg.addValue("Clients", clientName);
        send(msg);

        if(!subscriptions.contains(clientName))subscriptions.add(clientName);
    }
}
