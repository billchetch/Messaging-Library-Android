package net.chetch.messaging;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

public class MessagingViewModel extends ViewModel implements IMessageHandler {

    List<MessageFilter> messageFilters = new ArrayList<MessageFilter>();
    ClientConnection client;
    String clientName = "AndroidCMClient";
    String serverIP = "192.168.1.100";;
    int serverPort = 12000;

    public MessagingViewModel(){
        //empty
    }

    public boolean isClientConnected(){
        return client != null && client.isConnected();
    }

    public void setClientName(String clientName){
        this.clientName = clientName;
    }
    public void setConnectionDetails(String serverIP, int serverPort){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public ClientConnection connectClient(Observer observer) throws Exception{
        if(isClientConnected()){
            return client;
        }
        try {
            if(clientName == null || clientName == "")throw new Exception("clientName required");
            if(serverIP == null || serverIP == "")throw new Exception("serverIP required");
            if(serverPort <= 0)throw new Exception("A valid serverPort required");

            client = TCPClientManager.connect(serverIP + ":" + serverPort, clientName);
            client.addHandler(this);
            for(MessageFilter f : messageFilters) {
                client.subscribe(f);
            }
            onClientConnected();
            if(observer != null){
                observer.onChanged(clientName);
            }
            return client;
        } catch (Exception e){
            Log.e("main", e.getMessage());
            throw e;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if(isClientConnected()){
            try {
                client.close();
            } catch (Exception e){
                Log.e("MessagingViewModel", e.getMessage());
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
        //a hook
    }

    @Override
    public void handleConnectionError(Exception e, ClientConnection cnn) {
        //a hook
    }
}

