package net.chetch.messaging;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import net.chetch.messaging.exceptions.ConnectClientException;
import net.chetch.utilities.SLog;

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

    static public SharedPreferences tokenStore;

    public class ConnectionRequest{
        public String id;
        public String name;
        public Message request;
        public boolean requested = false;
        public boolean succeeded = false;
        public boolean failed = false;
        public String reason4failure;
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
    boolean keepAlivePaused = false;

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
        keepAlivePaused = false;
    }

    protected void stopKeepAlive(){
        keepAliveHandler.removeCallbacks(keepAliveRunnable);
        keepAliveStarted = false;
        keepAlivePaused = false;
    }

    public void pauseKeepAlive(){
        keepAlivePaused = true;
    }

    public void resumeKeepAlive(){
        keepAlivePaused = false;
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
        if(SLog.LOG) SLog.e("CMGR", "handleConnectionError: " + e.getMessage());

        if(cnn == primaryConnection && currentRequest != null){
            currentRequest.failed = true;
            if(SLog.LOG)SLog.i("CMGR", "Setting current request as failed");
        }

        if(cnn != primaryConnection && connections.containsKey(cnn.id)){
            if(SLog.LOG)SLog.i("CMGR", "removing " + cnn.id + " and adding to reconnect list");
            connections.remove(cnn.id);
            reconnect.put(cnn.name, cnn);
            stopKeepAlive();
            startKeepAlive(1000);
        }
    }

    public ClientConnection connect(String connectionString, String name, int timeout, String authToken) throws Exception {
        //here we create a connection request and
        if(currentRequest != null){
            throw new ConnectClientException("There is still an ongoing connection request", currentRequest.request);
        }

        //check if there is already a connection with that name ... if it's good to go then just return it otherwise barf
        ClientConnection cnn = getConnection(name);
        if(cnn != null){
            if(cnn.isConnected()){
                startKeepAlive(10000);
                return cnn;
            } else {
                throw new ConnectClientException("There is already an existing connection for " + name + " of state " + cnn.getState(), null);
            }
        }

        if(authToken == null && tokenStore != null){ //check if we have an auth token for this connection
            authToken = tokenStore.getString(name + "-AuthToken", null);
        }

        //create a request object to keep track of the request
        ConnectionRequest cnnreq = new ConnectionRequest();
        cnnreq.name = name;

        //create the message to send
        Message request = new Message();
        request.Type = MessageType.CONNECTION_REQUEST;
        request.Sender = name;
        if(authToken != null){
            request.Signature = ClientConnection.createSignature(authToken, name);
            if(SLog.LOG)SLog.i("CMGR", "Connect using auth token " + authToken);
        }
        cnnreq.request = request;

        //record this as the current request
        currentRequest = cnnreq;

        initialisePrimaryConnection(connectionString);

        primaryConnection.open();

        try {
            long start = Calendar.getInstance().getTimeInMillis();
            do {
                Thread.sleep(250);
                long elapsed = Calendar.getInstance().getTimeInMillis() - start;
                if (timeout > 0 && elapsed > timeout) {
                    throw new ConnectClientException("ClientConection::connect Timeout occurred", currentRequest.request);
                }
            } while (!currentRequest.isFinished());

            if (currentRequest.failed) {
                throw new ConnectClientException("Connection request failed " + currentRequest.reason4failure, currentRequest.request);
            }
        } catch (Exception e){
            currentRequest = null;
            throw e;
        }

        cnn = null;
        if(currentRequest.succeeded){
            cnn = currentRequest.connection;
        }
        currentRequest = null;
        startKeepAlive(10000);
        return cnn;
    }


    public ClientConnection connect(String connectionString, String name, int timeout) throws Exception {
        return connect(connectionString, name, timeout, null);
    }

    protected int keepAlive(){
        if(keepAlivePaused)return 0;

        if(SLog.LOG)SLog.i("CMGR", "Keep Alive Called!");

        for(Map.Entry<String, ClientConnection> entry : connections.entrySet()){
            ClientConnection cnn = entry.getValue();
            if(cnn.isConnected()){
                cnn.sendPing();
            }
        }

        List<String> toRemove = new ArrayList<>();
        for(Map.Entry<String, ClientConnection> entry : reconnect.entrySet()){
            try {
                if(SLog.LOG)SLog.i("CC", "reconnecting " + entry.getKey() + " to " + entry.getValue());
                ClientConnection oldCnn = entry.getValue();
                ClientConnection newCnn = connect(oldCnn.getConnectionString(), entry.getKey(), 10000, oldCnn.authToken);
                oldCnn.handleReconnect(newCnn);
                toRemove.add(entry.getKey());
            } catch (Exception e){
                if(SLog.LOG)SLog.e("CMGR", "keepAlive: " + e.getMessage());
            }
        }

        for(String key : toRemove){
            reconnect.remove(key);
        }

        return reconnect.size() > 0 ? 5000 : 10*1000;
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
            if(SLog.LOG)SLog.i("CMGR","Sending connection reqeust");
            cnn.send(currentRequest.request);
        } else {
            if(cnn == currentRequest.connection){
                if(SLog.LOG)SLog.i("CMGR", "Connection request succeeded");
                currentRequest.succeeded = true;
            }
        }
    }

    public void handleReceivedMessage(ClientConnection cnn, Message message){
        switch(message.Type){
            case CONNECTION_REQUEST_RESPONSE:
                if(cnn != primaryConnection){
                    //throw  exception...?
                    if(SLog.LOG)SLog.e("ClientManager", "Cannot process connection requests other than on primary connection");
                    return;
                }
                currentRequest.requested = true;

                if(SLog.LOG)SLog.i("CMGR","Received connection request response");

                boolean granted = message.getBoolean("Granted");
                if(granted) {
                    //so we create a new connection and attempt to connect it
                    ClientConnection newCnn;
                    newCnn = createConnection(message);
                    newCnn.name = currentRequest.name;
                    connections.put(newCnn.id, newCnn);
                    newCnn.open();
                    if(SLog.LOG)SLog.i("CMGR", "Opening new connection");
                    currentRequest.connection = newCnn;
                } else {
                    currentRequest.failed = true;
                    currentRequest.reason4failure = "Connection request declined ... " + (message.hasValue("Declined") ? message.getValue("Declined") : "Server did not provide a reason");
                    if(SLog.LOG)SLog.i("CMGR",currentRequest.reason4failure);
                }
                break;


            default:
                if(SLog.LOG)SLog.i("CMGR", "Received message " + (message.ID == null ? "NULL ID!" : message.ID) + " " + message.Type);
                break;
        }
    }
}
