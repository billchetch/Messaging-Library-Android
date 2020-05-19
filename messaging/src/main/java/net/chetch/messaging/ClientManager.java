package net.chetch.messaging;

import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

public class ClientManager<T extends ClientConnection> {

    public class ConnectionRequest{
        public String id;
        public String name;
        public Message request;
        public boolean requested = false;
        public boolean succeeded = false;
        public boolean failed = false;
        public ClientConnection connection;

        public boolean isFinished(){
            return failed || succeeded;
        }
    }


    Class<T> classOfT;
    ClientConnection primaryConnection = null;

    ConnectionRequest currentRequest = null;
    HashMap<String, ClientConnection> connections = new HashMap<>();

    int incrementFrom = 1;

    public ClientManager(Class<T> cls){

        classOfT = cls;
    }


    public ClientConnection getConnection(String idOrName){

        if(connections.containsKey(idOrName)){
            return connections.get(idOrName);
        }

        for(HashMap.Entry<String, ClientConnection> entry : connections.entrySet()){
            ClientConnection cnn = entry.getValue();
            if(cnn != null && cnn.name != null && cnn.name.equals(idOrName)){
                return cnn;
            }
        }

        return null;
    }

    protected String createNewConnectionID(){
        return "CMGR-" + this.hashCode() + "-" + incrementFrom++;
    }

    protected void initialisePrimaryConnection(String connectionString){
        if(primaryConnection == null){
            primaryConnection = createPrimaryConnection(connectionString);
        } else {
            //TODO: allow for change of connection string and check that the primary connection is in a valid
            //state to do so
        }
    }

    public void handleConnectionError(ClientConnection cnn, Exception e){
        Log.e("CMGR", "handleConnectionError: " + e.getMessage());

        if(cnn == primaryConnection && currentRequest != null){
            currentRequest.failed = true;
            Log.i("CMGR", "Setting current request as failed");
        }
    }

    public ClientConnection connect(String connectionString, String name, int timeout) throws Exception {
        //here we create a connection request and
        if(currentRequest != null){
            throw new Exception("There is still an ongoing connection request");
        }
        ConnectionRequest cnnreq = new ConnectionRequest();
        cnnreq.name = name;
        Message request = new Message();
        request.Type = MessageType.CONNECTION_REQUEST;
        request.Sender = name;
        cnnreq.request = request;
        currentRequest = cnnreq;

        initialisePrimaryConnection(connectionString);

        primaryConnection.open();

        long start = Calendar.getInstance().getTimeInMillis();
        do{
            Thread.sleep(250);
            long elapsed = Calendar.getInstance().getTimeInMillis() - start;
            if(timeout > 0 && elapsed > timeout){
                throw new TimeoutException("ClientConection::connect Timeout occurred");
            }
        } while(!currentRequest.isFinished());

        if(currentRequest.failed){
            throw new Exception("Connection request failed");
        }

        ClientConnection cnn = null;
        if(currentRequest.succeeded){
            cnn = currentRequest.connection;
        }
        currentRequest = null;
        return cnn;
    }

    protected void keepAlive(){

    }

    protected T createPrimaryConnection(String connectionString){
        try {
            T t = classOfT.newInstance();
            t.parseConnectionString(connectionString);
            t.remainConnected = false;
            t.mgr = this;
            t.id = "PC-" + createNewConnectionID();
            t.signMessage = false;
            return t;
        } catch(Exception e){
            return null;
        }
    }

    protected T createConnection(Message message){
        try {
            T t = classOfT.newInstance();
            t.parseMessage(message);
            t.remainConnected = true;
            t.mgr = this;
            t.id = "CC-" + createNewConnectionID();
            t.serverID = message.Sender;
            if (message.hasValue("AuthToken"))
            {
                t.authToken = message.getString("AuthToken");
                t.signMessage = true;
            }

            return t;
        } catch(Exception e){
            return null;
        }
    }

    protected void onConnectionConnected(ClientConnection cnn){
        if(cnn == primaryConnection){
            Log.i("CMGR","Sending connection reqeust");
            cnn.send(currentRequest.request);
        } else {
            if(cnn == currentRequest.connection){
                Log.i("CMGR", "Connection request succeeded");
                currentRequest.succeeded = true;
            }
        }
    }

    public void handleReceivedMessage(ClientConnection cnn, Message message){
        switch(message.Type){
            case CONNECTION_REQUEST_RESPONSE:
                if(cnn != primaryConnection){
                    //throw  exception...?
                    Log.e("ClientManager", "Cannot process connection requests other than on primary connection");
                    return;
                }
                currentRequest.requested = true;

                Log.i("CMGR","Received connection request response");

                boolean granted = message.getBoolean("Granted");
                if(granted){
                    //so we create a new connection and attempt to connect it
                    ClientConnection newCnn = createConnection(message);
                    newCnn.name = currentRequest.name;
                    connections.put(newCnn.id, newCnn);
                    Log.i("CMGR", "Opening new connection");
                    newCnn.open();
                    currentRequest.connection = newCnn;
                } else {
                    Log.i("CMGR","Connection request not granted");
                    currentRequest.failed = true;
                }
                break;


            default:
                Log.i("CMGR", "Received message " + message.ID);
                break;
        }
    }
}
