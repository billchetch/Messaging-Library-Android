package net.chetch.messaging;

import static java.lang.Math.min;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class MessageLog<T extends MessageLog.ILogItem> {

    public interface ILogItem{
        int id = 0;

        LocalDateTime created = null;

        IMessage message = null;
    }

    public interface IFilter<T>{
        boolean matches(T logItem);
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

    public boolean matches(T item, Collection<IFilter<T>> filters){
        boolean matches = true;
        if(filters != null) {
            for (IFilter filter : filters) {
                if (filter != null && !filter.matches(item)) {
                    matches = false;
                    break;
                }
            }
        }
        return matches;
    }

    public void copyTo(Collection<T> target, boolean reverse, Collection<IFilter<T>> filters){
        for (int i = 0; i < count; i++) {
            int idx = reverse ? count - 1 - i : i;
            T item = get(idx);
            if(matches(item, filters)) {
                target.add(item);
            }
        }
    }

    public void copyTo(Collection<T> target){
        copyTo(target, false, null);
    }

}
