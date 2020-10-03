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


    public MViewModel(){

        setClientName("RoundhouseBilly");
        addMessagingService("BBAlarms");
    }

    @Override
    public void onClientConnected() {
        super.onClientConnected();

    }
}
