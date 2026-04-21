package net.chetch.messaging;

import static java.lang.Math.min;

import java.time.LocalDateTime;
import java.util.Collection;

public class MessageLog<T extends MessageLog.ILogItem> {

    public interface ILogItem{
        int id = 0;

        LocalDateTime created = null;

        IMessage message = null;
    }


    private final T[] data;
    final private int size;
    private int head = 0; // Point to start of data
    private int tail = 0; // Point to next empty spot
    private int count = 0; // Number of items currently stored


    @SuppressWarnings("unchecked")
    public MessageLog(int size) {
        this.size = size;
        this.data = (T[]) new ILogItem[size];
    }


    public void add(T value) {
        data[tail] = value;
        tail = (tail + 1) % size; // Wraps back to 0 if at the end
        if (count < size) {
            count++;
        } else {
            head = (head + 1) % size; // Overwrite oldest data
        }
    }

    public T get(int index) {
        // Map logical index to physical circular index
        return data[(head + index) % size];
    }

    public T getFirst(){
        return get(0);
    }

    public T getLast(){
        return get(count);
    }

    public int size(){
        return count;
    }

    public void copyTo(Collection<T> target, boolean reverse){
        if(reverse){
            for (int i = count - 1; i >= 0; i--) {
                target.add(get(i));
            }
        } else {
            for (int i = 0; i < count; i++) {
                target.add(get(i));
            }
        }
    }

    public void copyTo(Collection<T> target){
        copyTo(target, false);
    }

}
