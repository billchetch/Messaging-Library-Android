package net.chetch.messaging;

import android.os.Handler;
import android.telephony.ServiceState;
import android.util.Log;

import net.chetch.messaging.exceptions.MessagingException;
import net.chetch.messaging.exceptions.MessagingServiceException;
import net.chetch.utilities.Utils;
import net.chetch.webservices.DataStore;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.network.Service;
import net.chetch.webservices.network.Services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public class MessagingViewModel extends WebserviceViewModel implements IMessageHandler, IConnectionHandler {
    public enum MessagingServiceState{
        UNKNOWN,
        NOT_CONNECTED,
        NOT_RESPONDING,
        RESPONDING,
    }

    //A client that this service subscribes to t
    public class MessagingService{
        public boolean subscribed = false;
        public String name;
        public MessagingServiceState state;
        public Calendar firstPingSentOn;
        public Calendar lastMessageReceivedOn;
        public Message lastMessage;
        public Message lastError;
        public Calendar lastErrorReceivedOn;
        public int maxDormantTime = 30;  //wait this many seconds before declaring the services as non responsive
        public int pingInterval = 15; //wait this many seconds after last message received before pinging the service

        public MessagingService(String clientName){
            name = clientName;
            state = MessagingServiceState.UNKNOWN;
        }

        public boolean isResponsive(){
            if(lastMessageReceivedOn == null && firstPingSentOn == null){
                return true;
            } else { //so we can assume at least one ping has been sent
                long useTime = lastMessageReceivedOn == null ? firstPingSentOn.getTimeInMillis() : lastMessageReceivedOn.getTimeInMillis();
                long diff = Calendar.getInstance().getTimeInMillis() - useTime;
                return diff < maxDormantTime * 1000;
            }
        }

        public boolean requiresPinging(){
            if(lastMessageReceivedOn == null){
                return true;
            } else {
                long diff = Calendar.getInstance().getTimeInMillis() - lastMessageReceivedOn.getTimeInMillis();
                return diff > pingInterval * 1000;
            }
        }

        public boolean setState(MessagingServiceState newState){
            if (state != newState){
                state = newState;
                return true;
            } else {
                return false;
            }
        }

        public void reset(){
            firstPingSentOn = null;
            lastMessageReceivedOn = null;
        }
    }

    public static final String CHETCH_MESSAGING_SERVICE = "Chetch Messaging";

    static ClientConnection client;
    static String clientName = "AndroidCMClient";
    static String connectionString;

    public static boolean isClientConnected(){
        return client != null && client.isConnected();
    }
    static public void setClientName(String clName){
        clientName = clName;
    }
    static public void setConnectionString(String cnnString){
        connectionString = cnnString;
    }

    List<MessageFilter> messageFilters = new ArrayList<MessageFilter>();
    Map<String, MessagingService> messagingServices = new HashMap<>(); //Other 'clients' this view model subscribes to via a message filter
    MutableLiveData<MessagingService> liveDataMessagingService = new MutableLiveData<>();


    //timer stuff
    protected int timerDelay = 30;
    Calendar timerStartedOn = null;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            int nextTimer = onTimer();
            if(nextTimer > 0) {
                timerHandler.postDelayed(this, timerDelay * 1000);
            }
        }
    };

    public MessagingViewModel(){
        //empty
    }

    public ClientConnection connectClient(Observer observer) {
        if(!isClientConnected()) {
            try {
                if (clientName == null || clientName.isEmpty())
                    throw new Exception("clientName required");
                if (connectionString == null || connectionString.isEmpty())
                    throw new Exception("connectionString required");

                client = TCPClientManager.connect(connectionString, clientName);
            } catch (Exception e) {
                Log.e("MessagingViewModel", e.getMessage());
                setError(e);
                return null;
            }
        }

        client.addMessageHandler(this);
        client.addConnectionHandler(this);

        for(MessageFilter f : messageFilters) {
            try {
                client.subscribe(f);
            } catch (Exception e){
                Log.e("MessagingViewModel", e.getMessage());
                setError(e);
            }
        }

        for(MessagingService ms : messagingServices.values()){
            try{
                if(!ms.subscribed){
                    client.subscribe(ms.name);
                    ms.subscribed = true;
                }
            } catch (Exception e){
                Log.e("MessagingViewModel", e.getMessage());
                setError(e);
            }
        }

        onClientConnected();
        notifyObserver(observer, clientName);
        startTimer(timerDelay, 1);
        return client;
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

    public void addMessageFilter(MessageFilter f) throws Exception {
        MessagingService ms = null;
        if(f.Sender != null && !messagingServices.containsKey(f.Sender)){
            ms = new MessagingService(f.Sender);
            messagingServices.put(clientName, ms);
        }
        if(messageFilters.contains(f))return;

        messageFilters.add(f);
        if(client != null && client.isConnected()){
            client.subscribe(f);
            if(ms != null)ms.subscribed = true;
        }
    }

    public void addMessagingService(String clientName){
        if(clientName == null || messagingServices.containsKey(clientName))return;

        MessagingService ms = new MessagingService(clientName);
        ms.maxDormantTime = timerDelay + ms.pingInterval;
        messagingServices.put(clientName, ms);

        if(client != null && client.isConnected()){
            try {
                client.subscribe(clientName);
                ms.subscribed = true;
            } catch (Exception e){
                Log.e("MessagingViewModel", e.getMessage());
            }
        }
    }

    protected void onClientConnected(){
        //a hook
    }

    protected int onTimer(){
        //we check how long since the last ping arrived and set service status accordingly
        Calendar now = Calendar.getInstance();
        for(MessagingService ms : messagingServices.values()){
            if(ms.state != MessagingServiceState.NOT_CONNECTED && !ms.isResponsive()){
                if(ms.setState(MessagingServiceState.NOT_RESPONDING)) {
                    setError(new MessagingServiceException("Service " + ms.name + " is not responding after more than " + ms.maxDormantTime + " seconds"));
                    liveDataMessagingService.postValue(ms);
                }
            }

            if(ms.requiresPinging()){
                getClient().sendPing(ms.name);
                if(ms.firstPingSentOn == null)ms.firstPingSentOn = Calendar.getInstance();
                Log.i("MessagingViewModel", "Pinging " + ms.name);
            }
        }
        return timerDelay;
    }

    protected void startTimer(int timerDelay, int postDelay){
        if(timerStartedOn != null)return;
        this.timerDelay = timerDelay;

        timerHandler.postDelayed(timerRunnable, postDelay*1000);
        timerStartedOn = Calendar.getInstance();
    }

    protected void startTimer(int timerDelay){
        startTimer(timerDelay, timerDelay);
    }

    protected void stopTimer(){
        timerHandler.removeCallbacks(timerRunnable);
        timerStartedOn = null;
    }

    @Override
    public void handleReceivedMessage(Message message, ClientConnection cnn) {
        //handle messages intended for a service first
        if(message.Sender != null && messagingServices.containsKey(message.Sender)) {
            MessagingService ms = messagingServices.get(message.Sender);
            Calendar now = Calendar.getInstance();
            MessagingServiceState msState = MessagingServiceState.UNKNOWN;
            switch(message.Type){
                case ERROR:
                    ms.lastErrorReceivedOn = now;
                    ms.lastError = message;
                    msState = MessagingServiceState.RESPONDING;
                    break;

                case NOTIFICATION:
                    switch(message.SubType){
                        case ClientManager.NOTIFICATION_CLIENT_CONNECTED:
                            msState = MessagingServiceState.RESPONDING;
                            break;

                        case ClientManager.NOTIFICATION_CLIENT_DISCONNECTED:
                            msState = MessagingServiceState.NOT_CONNECTED;
                            break;

                        default:
                            msState = MessagingServiceState.RESPONDING;
                            break;
                    }
                    break;

                default:
                    msState = MessagingServiceState.RESPONDING;
                    break;
            }
            ms.lastMessage = message;
            ms.lastMessageReceivedOn = now;

            if(ms.setState(msState)){
                liveDataMessagingService.postValue(ms);
            }
        } //end test for message from a service

        //general message handling
        switch(message.Type){
            case SHUTDOWN:
                for(MessagingService ms : messagingServices.values()) {
                    if(ms.setState(MessagingServiceState.NOT_CONNECTED))liveDataMessagingService.postValue(ms);
                }
                break;

            case ERROR:
                if(message.SubType == ClientManager.ERROR_CLIENT_NOT_CONNECTED){
                    String service = message.getString("IntendedTarget");
                    if(messagingServices.containsKey(service)){
                        MessagingService ms = messagingServices.get(service);
                        if(ms.setState(MessagingServiceState.NOT_CONNECTED))liveDataMessagingService.postValue(ms);
                    }
                }

                String msg = "ChetchMessaging error: " + (message.hasValue() ? message.getValue().toString() : "no error message available");
                Log.e("MessagingViewModel", msg);
                setError(new MessagingException(msg));
                break;
        }
    }


    @Override
    public void handleConnectionError(ClientConnection cnn, Exception e) {
        setError(e);
        Log.e("MessagingViewModel", "Connection error: " + e.getMessage());
    }

    @Override
    public void handleConnectionClosed(ClientConnection cnn) {
        stopTimer();

    }

    @Override
    public void handleReconnect(ClientConnection oldCnn, ClientConnection newCnn) {
        Log.w("MessagingViewModel", "Reconnecting client");
        client = newCnn;

        for(MessagingService ms : messagingServices.values()){
            ms.reset();
        }
        startTimer(timerDelay, 1);
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

    public LiveData<MessagingService> getMessagingService(){
        return liveDataMessagingService;
    }
}

