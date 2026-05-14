package net.chetch.messaging;

import android.os.Handler;
import android.os.HandlerThread;

import net.chetch.utilities.SLog;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class MessageQueue<M extends IMessage> implements Frame.IFrameCompleteListener {
    static final int DEFAULT_WAIT_BEFORE_DISPATCH = 100;
    static final int DEFAULT_THROTTLE_DISPATCH = 10;

    public interface IDispatchListener<M>{
        void onMessageDispatch(M message);
    }

    String name;

    final Object nqLock = new Object();

    HandlerThread dispatchThread;

    Handler dispatchThreadHandler;

    IDispatchListener<M> dispatchListener;

    Frame frame;

    Queue<M> queue = new LinkedList<>();

    Class<M> messageClass;

    int waitBeforeDispatch = DEFAULT_WAIT_BEFORE_DISPATCH;
    int throttleDispatch = 0;

    boolean cancelled = false;


    public MessageQueue(String name, Class<M> cls,  Frame.FrameSchema schema, MessageEncoding encoding, int waitBeforeDispatch, int throttleDispatch){
        this.name = name;
        this.messageClass = cls;
        frame = new Frame(schema, encoding, this);
        setThrottling(waitBeforeDispatch, throttleDispatch);
    }

    public MessageQueue(String name, Class<M> cls,  Frame.FrameSchema schema, MessageEncoding encoding){
        this(name, cls, schema, encoding, DEFAULT_WAIT_BEFORE_DISPATCH, DEFAULT_THROTTLE_DISPATCH);
    }

    public void setThrottling(int waitBeforeDispatch, int throttleDispatch){
        this.waitBeforeDispatch = waitBeforeDispatch;
        this.throttleDispatch = throttleDispatch;
    }

    public void setDispatchListener(IDispatchListener<M> listener){
        dispatchListener = listener;
    }

    public void add(byte[] bytes, int readUntil) throws Exception{
        frame.add(bytes, readUntil);
    }

    public void add(M message){
        synchronized (nqLock) {
            queue.add(message);
        }

        if(dispatchThreadHandler != null) {
            SLog.i("MQ-" + name, "Added " + message + " and now posting to dispatch q-size currently = " + queue.size());
            dispatchThreadHandler.postDelayed(this::dispatch, waitBeforeDispatch);
        }
    }

    @Override
    public void onFrameComplete(byte[] payload) {
        try {
            M m = (M)messageClass.newInstance();
            m.deserialize(payload);
            add(m);

        } catch (Exception e){
            SLog.e("MQ" + name, e.getMessage());
        }
    }


    public void begin(){
        cancelled = false;
        if(dispatchThread == null){
            String threadName = "MQ" + name;
            dispatchThread = new HandlerThread(threadName);
            dispatchThread.start();
            dispatchThreadHandler = new Handler(dispatchThread.getLooper());
        }

        if(dispatchThreadHandler != null) {
            //Run the next bit in a new thread
            dispatchThreadHandler.postDelayed(this::dispatch, waitBeforeDispatch);
        }
    }

    public void end(){
        cancelled = true;
        synchronized (nqLock) {
            queue.clear();
        }
    }

    void dispatch(){
        if(queue.isEmpty())return;

        synchronized(nqLock) {
            while(!queue.isEmpty() && !cancelled) {
                M message = queue.remove();
                SLog.i("MQ-" + name, "Dispatching message " + message + " " + queue.size() + " remaining.");
                if(dispatchListener != null){
                    dispatchListener.onMessageDispatch(message);
                }
                if(!queue.isEmpty() && throttleDispatch > 0){
                    try {
                        Thread.sleep(throttleDispatch);
                    } catch (Exception e){
                        SLog.e("MQ-" + name, "di1patch error: " + e.getMessage());
                    }
                }
            }
        }
    }

    Frame wrap(M message) throws Exception{
        Frame f = new Frame(frame.schema, frame.encoding);
        byte[] payload = message.serialize();
        f.setPayload(payload);
        return f;
    }
}
