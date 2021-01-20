package net.chetch.chetchmessagingtest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import net.chetch.appframework.GenericActivity;
import net.chetch.messaging.ClientConnection;
import net.chetch.messaging.IMessageHandler;
import net.chetch.messaging.Message;
import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageService;
import net.chetch.messaging.MessageType;
import net.chetch.messaging.MessagingViewModel;
import net.chetch.messaging.TCPClientManager;
import net.chetch.messaging.exceptions.MessagingException;
import net.chetch.messaging.exceptions.MessagingServiceException;
import net.chetch.messaging.filters.AlertFilter;
import net.chetch.messaging.filters.CommandResponseFilter;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.network.NetworkRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;


public class MainActivity extends GenericActivity {

    enum ConnectState{
        NOT_SET,
        CONNECT_REQUEST,
        CONNECTING,
        CONNECTED,
        RECONNECT_REQUEST,
        ERROR
    }

    Button btnStart, btnSend;
    TextView textStatus;
    MViewModel model;
    Map<String, BBAlarmsMessageSchema.AlarmState> alarmStates = new HashMap<>();
    ConnectState connectState = ConnectState.NOT_SET;

    Observer dataLoadProgress  = obj -> {
        String progressInfo;
        String state;
        if(obj instanceof ClientConnection){
            ClientConnection cnn = (ClientConnection)obj;
            progressInfo = "Client " + cnn.getName() + " connected";
        } else {
            WebserviceViewModel.LoadProgress progress = (WebserviceViewModel.LoadProgress) obj;
            state = progress.startedLoading ? "Loading" : "Loaded";
            progressInfo = state + (progress.info == null ? "" : " " + progress.info.toLowerCase());
        }
        Log.i("Main", progressInfo);
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            //String apiBaseURL = "http://192.168.43.123:8001/api/";
            //String apiBaseURL = "http://192.168.1.100:8001/api/";
            //String apiBaseURL = "http://192.168.0.123:8001/api/";
            //String apiBaseURL = "http://192.168.0.150:8001/api/";
            String apiBaseURL = "http://192.168.0.52:8001/api/";
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
        } catch (Exception e) {
            Log.e("MVM", e.getMessage());
            return;
        }

        model = ViewModelProviders.of(this).get(MViewModel.class);

        model.getError().observe(this, throwable -> {
            handleModelErrors(throwable);
        });

        //observe state
        model.observeMessagingServices(this, ms->{
            Log.i("Main", ms.name + " has state " + ms.state);
        });

        model.addMessagingService("BBAlarms");

        connectState = ConnectState.CONNECT_REQUEST;
        startTimer(1, 1);
    }


    @Override
    protected int onTimer() {
        switch(connectState){
            case CONNECT_REQUEST:
            case RECONNECT_REQUEST:
                stopTimer();
                try {
                    Log.i("Main", "Attempting to connect it all...");
                    connectState = ConnectState.CONNECTING;
                    model.loadDataForClient(this, "Roundhousebilly", dataLoadProgress);

                    connectState = ConnectState.CONNECTED;
                } catch (Exception e){
                    connectState = ConnectState.ERROR;
                    Log.e("Main", e.getMessage());
                } finally {
                    startTimer(1);
                }
                break;

            case ERROR:
                connectState = connectState == ConnectState.CONNECTED ? ConnectState.RECONNECT_REQUEST : ConnectState.CONNECT_REQUEST;
                break;
        }

        return super.onTimer();
    }

    private void handleModelErrors(Throwable t){
        connectState = ConnectState.ERROR;

        Log.e("MODEL ERROR >>>>>>> ", t.getMessage());
    }

    @Override
    protected void onStop() {
        super.onStop();
        model.pausePingServices();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        model.resumePingServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(serviceConnection);
    }
}
