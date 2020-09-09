package net.chetch.chetchmessagingtest;

import android.util.Log;

import net.chetch.messaging.Message;
import net.chetch.messaging.MessagingViewModel;
import net.chetch.messaging.filters.AlertFilter;
import net.chetch.messaging.filters.CommandResponseFilter;
import net.chetch.webservices.DataStore;
import net.chetch.webservices.network.Service;
import net.chetch.webservices.network.Services;

import java.util.List;
import java.util.Map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public class MViewModel extends MessagingViewModel {

    MutableLiveData<List<String>> alarms = new MutableLiveData<>();
    MutableLiveData<Map<String, BBAlarmsMessageSchema.AlarmState>> alarmStates = new MutableLiveData<>();

    public AlertFilter onAlarmAlert = new AlertFilter(BBAlarmsMessageSchema.SERVICE_NAME){
        @Override
        protected void onMatched(Message message) {

            Log.i("Main", "On Alarm Alert");
        }
    };

    public CommandResponseFilter onListAlarms = new CommandResponseFilter(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_LIST_ALARMS){
        @Override
        protected void onMatched(Message message) {
            List<String> l = message.getList("Alarms", String.class);
            Log.i("Main", "On List Alarms gives " + l.size() + " alarms");

            alarms.postValue(l); //aware of thread safety

            if(isClientConnected()){
                getClient().sendCommand(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_ALARM_STATUS);
            }
        }
    };

    public CommandResponseFilter onAlarmStatus = new CommandResponseFilter(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_ALARM_STATUS){
        @Override
        protected void onMatched(Message message) {
            Log.i("Main", "On Alarm Status");
            BBAlarmsMessageSchema schema = new BBAlarmsMessageSchema(message);
            alarmStates.postValue(schema.getAlarmStates()); //beware of thread safety
        }
    };

    public LiveData<List<String>> getAlarms(){
        return alarms;
    }

    public LiveData<Map<String, BBAlarmsMessageSchema.AlarmState>> getAlarmStates(){
        return alarmStates;
    }

    public MViewModel(){
        setClientName("RoundhouseBilly");
        try {
            addMessageFilter(onAlarmAlert);
            addMessageFilter(onAlarmStatus);
            addMessageFilter(onListAlarms);
        } catch (Exception e){
            Log.e("MVMM", e.getMessage());
        }
    }

    @Override
    public void onClientConnected() {
        super.onClientConnected();

        getClient().sendCommand(BBAlarmsMessageSchema.SERVICE_NAME, BBAlarmsMessageSchema.COMMAND_LIST_ALARMS);
    }
}
