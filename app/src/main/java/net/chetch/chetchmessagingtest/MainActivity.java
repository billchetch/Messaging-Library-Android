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


public class MainActivity extends Activity implements IMessageHandler {

    Button btnStart, btnSend;
    TextView textStatus;
    int serverPort;
    String serverIP;
    ClientConnection client;
    Map<String, BBAlarmsMessageSchema.AlarmState> alarmStates = new HashMap<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverPort = 12000;
        serverIP = "192.168.1.100";

        try {
            client = TCPClientManager.connect(serverIP + ":" + serverPort, "tolly");
            client.addHandler(this);

            client.subscribe(new AlertFilter(BBAlarmsMessageSchema.SERVICE_NAME){
                @Override
                protected void onMatched(Message message) {
                    BBAlarmsMessageSchema schema = new BBAlarmsMessageSchema(message);

                    BBAlarmsMessageSchema.AlarmState state = schema.getAlarmState();
                    TextView tv = findViewById(R.id.tvAlarmStatus);
                    tv.setText("Alarm " + schema.getDeviceID() + " has state: " + state.name());

                    Log.i("Main", "Message filter matched");
                }
            });

            client.subscribe(new CommandResponseFilter(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_ALARM_STATUS){
                @Override
                protected void onMatched(Message message) {
                    BBAlarmsMessageSchema schema = new BBAlarmsMessageSchema(message);

                    Map<String, BBAlarmsMessageSchema.AlarmState> states = schema.getAlarmStates();
                    if(states != null) {
                        for (Map.Entry<String, BBAlarmsMessageSchema.AlarmState> entry : states.entrySet()) {
                            String key = entry.getKey();
                            if (alarmStates.containsKey(key)) {
                                //add alarm to display
                            } else if (alarmStates.get(key) != states.get(key)) {
                                //update alarm display
                            }
                            alarmStates.put(key, entry.getValue());
                        }
                    }

                    List<String> help = message.getList("Help", String.class);

                    Log.i("Main", "Message filter matched");
                }
            });

            client.sendCommand(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_ALARM_STATUS);
            //client.sendCommand(BBAlarmsMessageSchema.SERVICE_NAME, "help");
        } catch (Exception e){
            Log.e("main", e.getMessage());
        }
    }

    public void handleReceivedMessage(Message message, ClientConnection cnn){
        Log.i("main",  " Received: " + message.toString());
    }

    public void handleConnectionError(Exception e, ClientConnection cnn){
        Log.i("main", "Error: " + e.toString());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.close();
        //unbindService(serviceConnection);
    }
}
