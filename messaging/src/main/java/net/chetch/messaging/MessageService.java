package net.chetch.messaging;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import net.chetch.utilities.SLog;

public class MessageService extends Service {
    public class MessageServiceBinder extends Binder {
        public MessageService getService(){
            return MessageService.this;
        }
    }

    static public class Connection implements ServiceConnection {
        private MessageService messageService;
        private boolean bound;

        public MessageService getService(){
            return messageService;
        }

        public boolean isBound(){
            return bound;
        }

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MessageService.MessageServiceBinder binder = (MessageService.MessageServiceBinder) service;
            messageService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    }


    static public final String INTENT_CONNECTION_STRING = "net.chetch.messaging.connection_string";
    static public final String INTENT_CLIENT_NAME = "net.chetch.messaging.client_name";

    private final IBinder serviceBinder = new MessageServiceBinder();

    TCPClientManager cmgr;
    ClientConnection client;

    @Override
    public void onCreate() {
        super.onCreate();

        cmgr = new TCPClientManager();

        if(SLog.LOG)SLog.i("MS", "Service created");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if(SLog.LOG)SLog.i("MS", "Service start called #" + this.hashCode());


        return START_STICKY;
    }


    public void createClient(String connectionString, String clientName) throws Exception{
        if(connectionString == null || connectionString.isEmpty() || clientName == null || clientName.isEmpty()){
            throw new IllegalArgumentException("Must pass a connection string and client name");
        }
        if(SLog.LOG)SLog.i("MS", "Attempting to create and connect " + clientName + " to " + connectionString);
        client = cmgr.connect(connectionString, clientName, 10000);
        if(SLog.LOG)SLog.i("MS", clientName + " connected to " + connectionString);
    }


    @Override
    public IBinder onBind(Intent intent) {
        if(SLog.LOG)SLog.i("MS", "onBind called");
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        if(SLog.LOG)SLog.i("MS", "Service destroyed");
    }

    public String getStatus(){
        return "Not yet implemented";
    }
}
