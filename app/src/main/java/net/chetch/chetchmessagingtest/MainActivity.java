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
import net.chetch.messaging.MessageService;
import net.chetch.messaging.MessageType;
import net.chetch.messaging.TCPClientManager;


public class MainActivity extends Activity implements IMessageHandler {

    Button btnStart, btnSend;
    TextView textStatus;
    int serverPort;
    String serverIP;
    ClientConnection client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverPort = 12000;
        serverIP = "192.168.1.100";

        try {
            client = TCPClientManager.connect(serverIP + ":" + serverPort, "tolly");
            client.addHandler(this);
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
