package com.sleekbit.ovuviewclient.logging;

/**
 *
 */
public class LogEntry {
    public final String message;
    public final Throwable exception;

    LogEntry(String message) {
        this.message = message;
        this.exception = null;
    }

    LogEntry(String message, Throwable exception) {
        this.message = message;
        this.exception = exception;
    }

    public boolean hasException() {
        return exception != null;
    }

}
