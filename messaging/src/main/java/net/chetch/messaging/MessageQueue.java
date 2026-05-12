package net.chetch.messaging;

import android.os.Handler;
import android.os.HandlerThread;

import net.chetch.utilities.SLog;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class MessageQueue<M extends IMessage> implements Frame.IFrameCompleteListener {

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

    int waitBeforeDispatch = 100;

    boolean cancelled = false;


    public MessageQueue(String name, Class<M> cls,  Frame.FrameSchema schema, MessageEncoding encoding){
        this.name = name;
        this.messageClass = cls;
        frame = new Frame(schema, encoding, this);
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
                //SLog.i("MQ-" + name, "Dispatching message");
                if(dispatchListener != null){
                    dispatchListener.onMessageDispatch(message);
                }
                //TODO: add a delay here?
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
