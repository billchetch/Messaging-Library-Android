package net.chetch.messaging.exceptions;

import net.chetch.messaging.Frame;

public class FrameException extends Exception {

    public Frame.FrameError error;

    public FrameException(Frame.FrameError error){
        super();
        this.error = error;
    }

    public FrameException(Frame.FrameError error, String message){
        super(message);
        this.error = error;
    }
}
