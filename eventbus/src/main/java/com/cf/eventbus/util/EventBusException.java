package com.cf.eventbus.util;

public class EventBusException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    public EventBusException(String detailMessage) {
        super(detailMessage);
    }

    public EventBusException(Throwable cause) {
        super(cause);
    }

    public EventBusException(String message, Throwable cause) {
        super(message, cause);
    }

}
