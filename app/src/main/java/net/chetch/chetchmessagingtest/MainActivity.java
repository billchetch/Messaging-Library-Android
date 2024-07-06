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
import net.chetch.webservices.ConnectManager;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.network.NetworkRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;


public class MainActivity extends GenericActivity {



    Button btnStart, btnSend;
    TextView textStatus;
    MViewModel model;
    Map<String, BBAlarmsMessageSchema.AlarmState> alarmStates = new HashMap<>();
    ConnectManager connectManager = new ConnectManager();

    Observer connectProgress  = obj -> {
        String progressInfo = "...";
        String state;
        if(obj instanceof ClientConnection){
            ClientConnection cnn = (ClientConnection)obj;
            progressInfo = "Client " + cnn.getName() + " connected";
        } else if(obj instanceof ConnectManager) {
            ConnectManager cm = (ConnectManager)obj;
            TextView tv = findViewById(R.id.connectionStatus);
            switch(cm.getState()){
                case CONNECT_REQUEST:
                    if(cm.fromError()){
                        tv.setText("There was an error ... retrying...");
                    } else {
                        tv.setText("Connecting...");
                    }
                    break;

                case RECONNECT_REQUEST:
                    tv.setText("Disconnected!... Attempting to reconnect...");
                    break;

                case CONNECTED:
                    tv.setText("Connected!");
                    break;
            }

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
            //String apiBaseURL = "http://192.168.0.52:8001/api/";
            String apiBaseURL = "http://192.168.2.88:8001/api/";
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
        } catch (Exception e) {
            Log.e("MVM", e.getMessage());
            return;
        }

        model = ViewModelProviders.of(this).get(MViewModel.class);
        model.getError().observe(this, throwable -> {
            Log.e("error", "Some error");
        });

        try {
            model.setClientName("RoundhouseBilly");
            connectManager.addModel(model);
            connectManager.requestConnect(connectProgress);
        } catch (Exception e){
            Log.e("Main",  e.getMessage());
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        connectManager.pause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        connectManager.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(serviceConnection);
    }
}
