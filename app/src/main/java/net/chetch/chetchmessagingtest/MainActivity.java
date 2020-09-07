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
import net.chetch.messaging.TCPClientManager;
import net.chetch.messaging.filters.AlertFilter;
import net.chetch.messaging.filters.CommandResponseFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;


public class MainActivity extends AppCompatActivity {

    Button btnStart, btnSend;
    TextView textStatus;
    MessagingViewModel model;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        model = ViewModelProviders.of(this).get(MessagingViewModel.class);
        if(!model.isClientConnected()) {
            try {
                model.setConnectionDetails("192.168.1.100", 12000);
                model.addMessageFilter(onAlarmAlert);
                model.addMessageFilter(onListAlarms);
                model.addMessageFilter(onAlarmStatus);
                model.connectClient(data -> {
                    model.getClient().sendCommand(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_LIST_ALARMS);
                });
            } catch (Exception e) {
                Log.e("Main", e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(serviceConnection);
    }
}
