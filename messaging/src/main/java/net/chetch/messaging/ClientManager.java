package net.chetch.messaging;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

public class ClientManager<T extends ClientConnection> {
    static final public int ERROR_CLIENT_NOT_CONNECTED = 1;
    static final public int NOTIFICATION_CLIENT_CONNECTED = 1;
    static final public int NOTIFICATION_CLIENT_DISCONNECTED = 2;

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
    HashMap<String, ClientConnection> reconnect = new HashMap<>();

    boolean keepAliveStarted = false;
    Handler keepAliveHandler = new Handler();
    Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            int nextKeepAlive = keepAlive();
            if(nextKeepAlive > 0) {
                keepAliveHandler.postDelayed(this, nextKeepAlive);
            }
        }
    };

    int incrementFrom = 1;

    public ClientManager(Class<T> cls){

        classOfT = cls;
    }

    protected void startKeepAlive(int timerDelay){
        if(keepAliveStarted)return;
        keepAliveHandler.postDelayed(keepAliveRunnable, timerDelay);
        keepAliveStarted = true;
    }

    protected void stopKeepAlive(){
        keepAliveHandler.removeCallbacks(keepAliveRunnable);
        keepAliveStarted = false;
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

        if(cnn != primaryConnection && connections.containsKey(cnn.id)){
            Log.i("CMGR", "removing " + cnn.id + " and adding to reconnect list");
            connections.remove(cnn.id);
            reconnect.put(cnn.name, cnn);
            stopKeepAlive();
            startKeepAlive(1000);
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

        try {
            long start = Calendar.getInstance().getTimeInMillis();
            do {
                Thread.sleep(250);
                long elapsed = Calendar.getInstance().getTimeInMillis() - start;
                if (timeout > 0 && elapsed > timeout) {
                    throw new TimeoutException("ClientConection::connect Timeout occurred");
                }
            } while (!currentRequest.isFinished());

            if (currentRequest.failed) {
                throw new Exception("Connection request failed");
            }
        } catch (Exception e){
            currentRequest = null;
            throw e;
        }

        ClientConnection cnn = null;
        if(currentRequest.succeeded){
            cnn = currentRequest.connection;
        }
        currentRequest = null;
        startKeepAlive(10000);
        return cnn;
    }

    protected int keepAlive(){

        Log.i("CMGR", "Keep Alive Called!");

        for(Map.Entry<String, ClientConnection> entry : connections.entrySet()){
            ClientConnection cnn = entry.getValue();
            if(cnn.isConnected()){
                cnn.sendPing();
            }
        }

        List<String> toRemove = new ArrayList<>();
        for(Map.Entry<String, ClientConnection> entry : reconnect.entrySet()){
            try {
                Log.i("CC", "reconnecting " + entry.getKey() + " to " + entry.getValue());
                ClientConnection oldCnn = entry.getValue();
                ClientConnection newCnn = connect(oldCnn.getConnectionString(), entry.getKey(), 10000);
                oldCnn.handleReconnect(newCnn);
                toRemove.add(entry.getKey());
            } catch (Exception e){
                Log.e("CMGR", "keepAlive: " + e.getMessage());
            }
        }

        for(String key : toRemove){
            reconnect.remove(key);
        }

        return 60*1000;
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
                Log.i("CMGR", "Received message " + (message.ID == null ? "NULL ID!" : message.ID));
                break;
        }
    }
}
