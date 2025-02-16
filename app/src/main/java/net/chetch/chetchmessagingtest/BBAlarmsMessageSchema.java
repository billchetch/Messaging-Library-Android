package net.chetch.chetchmessagingtest;

import net.chetch.messaging.Message;
import net.chetch.messaging.MessageSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BBAlarmsMessageSchema extends MessageSchema {
    public enum AlarmState{
        OFF,
        ON,
        DISABLED,
        ENABLED
    }

    static public final String SERVICE_NAME = "BBAlarms";

    static public final String COMMAND_ALARM_STATUS = "alarm-status";
    static public final String COMMAND_LIST_ALARMS = "list-alarms";
    static public final String COMMAND_SILENCE = "silence";
    static public final String COMMAND_UNSILENCE = "unsilence";
    static public final String COMMAND_DISABLE_ALARM = "disable-alarm";
    static public final String COMMAND_ENABLE_ALARM = "enable-alarm";
    static public final String COMMAND_TEST_ALARM = "test-alarm";


    public BBAlarmsMessageSchema(Message message){
        super(message);
    }

    public String getDeviceID(){
        return message.getString("DeviceID");
    }

    /*public AlarmState getAlarmState(){
        return message.getEnum("AlarmState", AlarmState.class);
    }*/

    public Map<String, AlarmState> getAlarmStates(){
        return message.getMap("AlarmStates", AlarmState.class);
    }
}
