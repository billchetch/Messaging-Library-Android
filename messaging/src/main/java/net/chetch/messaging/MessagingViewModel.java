package net.chetch.messaging;

import android.util.Log;

import net.chetch.webservices.DataStore;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.network.Service;
import net.chetch.webservices.network.Services;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.Observer;

public class MessagingViewModel extends WebserviceViewModel implements IMessageHandler {
    public static final String CHETCH_MESSAGING_SERVICE = "Chetch Messaging";

    List<MessageFilter> messageFilters = new ArrayList<MessageFilter>();
    ClientConnection client;
    String clientName = "AndroidCMClient";
    String connectionString;

    public MessagingViewModel(){
        //empty
    }

    public boolean isClientConnected(){
        return client != null && client.isConnected();
    }

    public void setClientName(String clientName){
        this.clientName = clientName;
    }
    public void setConnectionString(String connectionString){
        this.connectionString = connectionString;
    }

    public ClientConnection connectClient(Observer observer) {
        if(isClientConnected()){
            return client;
        }
        try {
            if(clientName == null || clientName.isEmpty())throw new Exception("clientName required");
            if(connectionString == null || connectionString.isEmpty())throw new Exception("connectionString required");

            client = TCPClientManager.connect(connectionString, clientName);
            client.addHandler(this);
            for(MessageFilter f : messageFilters) {
                client.subscribe(f);
            }
            onClientConnected();
            notifyObserver(observer, clientName);
            return client;
        } catch (Exception e){
            Log.e("MessagingViewModel", e.getMessage());
            setError(e);
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if(isClientConnected()){
            try {
                client.close();
            } catch (Exception e){
                Log.e("MessagingViewModel", "finalize: " + e.getMessage());
            }
        }
    }

    public ClientConnection getClient(){ return client; }

    public void addMessageFilter(MessageFilter f) throws Exception{
        if(messageFilters.contains(f))return;

        messageFilters.add(f);
        if(client != null && client.isConnected()){
            client.subscribe(f);
        }
    }

    public void onClientConnected(){
        //a hook
    }

    @Override
    public void handleReceivedMessage(Message message, ClientConnection cnn) {
        if(message.Type == MessageType.ERROR){
            String msg = "ChetchMessaging error: " + (message.hasValue() ? message.getValue().toString() : "no error message available");
            Log.e("MessagingViewModel", msg);
            setError(new Exception(msg));
        }
    }


    @Override
    public void handleConnectionError(Exception e, ClientConnection cnn) {
        setError(e);
    }

    @Override
    public DataStore loadData(Observer observer) {
        DataStore<?> dataStore = super.loadData(observer);
        dataStore.observe(services->{
            Log.i("MessagingViewModel", "Loaded data...");
            try {
                connectClient(observer);
            } catch (Exception e){
                Log.e("MessagingViewModel", "Client connection error: " + e.getMessage());
            }
        });
        return dataStore;
    }

    @Override
    protected boolean configureServices(Services services) {
        boolean configured = super.configureServices(services);
        if(configured && services.hasService(CHETCH_MESSAGING_SERVICE)){
            Service cms = services.getService(CHETCH_MESSAGING_SERVICE);
            setConnectionString(cms.getLanIP() + ":" + cms.getEndpointPort());
        }
        return configured;
    }
}

