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
            String sz = "{\"ID\":\"4076-46104728-202009282018322236\",\"ResponseID\":null,\"Target\":null,\"Sender\":null,\"Type\":3,\"SubType\":0,\"Signature\":null,\"D\":\"2020-09-28 21:05:09 +08:00\"}";
            Message msg = Message.deserialize(sz);

            //String apiBaseURL = "http://192.168.43.123:8001/api/";
            //String apiBaseURL = "http://192.168.1.100:8001/api/";
            String apiBaseURL = "http://192.168.0.123:8001/api/";
            NetworkRepository.getInstance().setAPIBaseURL(apiBaseURL);
        } catch (Exception e) {
            Log.e("MVM", e.getMessage());
            return;
        }

        model = ViewModelProviders.of(this).get(MViewModel.class);

        model.addMessagingService("BBAlarms");
        model.loadData(dataLoadProgress);

        model.getMessagingService().observe(this, ms->{
            Log.i("Main", ms.name + " has state " + ms.state);
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(serviceConnection);
    }
}
