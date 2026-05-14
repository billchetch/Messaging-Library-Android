package net.chetch.messaging;

abstract public class MessageIO<M extends IMessage>  {

    protected MessageQueue<M> qIn;
    protected MessageQueue<M> qOut;


    public MessageIO(String name, Class<M> cls,  Frame.FrameSchema schema, MessageEncoding encoding){
        qIn = new MessageQueue<>("in-" + name, cls, schema, encoding);
        qIn.setDispatchListener(this::onMessageIn);

        qOut = new MessageQueue<>("out-" + name, cls, schema, encoding);
        qOut.setDispatchListener(this::onMessageOut);
    }

    public void setInThrottling(int waitBeforeDispatch, int throttleDispatch){
        qIn.setThrottling(waitBeforeDispatch, throttleDispatch);
    }

    public void setOutThrottling(int waitBeforeDispatch, int throttleDispatch){
        qOut.setThrottling(waitBeforeDispatch, throttleDispatch);
    }

    public void setThrottling(int waitBeforeDispatch, int throttleDispatch){
        setInThrottling(waitBeforeDispatch, throttleDispatch);
        setOutThrottling(waitBeforeDispatch, throttleDispatch);
    }

    protected abstract void onMessageIn(M message);

    protected abstract void onMessageOut(M message);

    public void add(byte[] bytes, int readUntil) throws Exception{
        qIn.add(bytes, readUntil);
    }

    public void add(M message){
        qOut.add(message);
    }

    public void begin(){
        qIn.begin();
        qOut.begin();
    }

    public void end(){
        qIn.end();
        qOut.end();
    }

    public Frame wrap(M message) throws Exception{
        return qOut.wrap(message);
    }
}
