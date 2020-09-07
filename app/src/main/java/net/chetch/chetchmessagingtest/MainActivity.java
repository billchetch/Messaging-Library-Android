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

import net.chetch.messaging.ClientConnection;
import net.chetch.messaging.IMessageHandler;
import net.chetch.messaging.Message;
import net.chetch.messaging.MessageFilter;
import net.chetch.messaging.MessageService;
import net.chetch.messaging.MessageType;
import net.chetch.messaging.MessagingViewModel;
import net.chetch.messaging.TCPClientManager;
import net.chetch.messaging.filters.AlertFilter;
import net.chetch.messaging.filters.CommandResponseFilter;
import net.chetch.webservices.WebserviceViewModel;
import net.chetch.webservices.network.NetworkRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;


public class MainActivity extends AppCompatActivity {

    Button btnStart, btnSend;
    TextView textStatus;
    MViewModel model;
    Map<String, BBAlarmsMessageSchema.AlarmState> alarmStates = new HashMap<>();

    AlertFilter onAlarmAlert = new AlertFilter(BBAlarmsMessageSchema.SERVICE_NAME){
        @Override
        protected void onMatched(Message message) {

            Log.i("Main", "On Alarm Alert");
        }
    };

    CommandResponseFilter onListAlarms = new CommandResponseFilter(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_LIST_ALARMS){
        @Override
        protected void onMatched(Message message) {
            Log.i("Main", "On List Alarms");
            if(model != null && model.isClientConnected()){
                model.getClient().sendCommand(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_ALARM_STATUS);
            }
        }
    };

    CommandResponseFilter onAlarmStatus = new CommandResponseFilter(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_ALARM_STATUS){
        @Override
        protected void onMatched(Message message) {
            Log.i("Main", "On Alarm Status");
        }
    };

    Observer dataLoadProgress  = obj -> {
        WebserviceViewModel.LoadProgress progress = (WebserviceViewModel.LoadProgress) obj;
        String state = progress.startedLoading ? "Loading" : "Loaded";
        String progressInfo = state + (progress.info == null ? "" : " " + progress.info.toLowerCase());
        Log.i("Main", progressInfo);
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            //String apiBaseURL = "http://192.168.43.123:8001/api/";
            String apiBaseURL = "http://192.168.1.100:8001/api/";
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
        } catch (Exception e) {
            Log.e("MVM", e.getMessage());
            return;
        }

        model = ViewModelProviders.of(this).get(MViewModel.class);

        model.loadData(dataLoadProgress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(serviceConnection);
    }
}
