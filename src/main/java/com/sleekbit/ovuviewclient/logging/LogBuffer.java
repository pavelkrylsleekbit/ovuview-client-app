package com.sleekbit.ovuviewclient.logging;

import android.util.Log;

import com.sleekbit.ovuviewclient.ClientApp;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.nio.Buffer;

/**
 *
 */
public class LogBuffer {
    private static final boolean PRINT_TO_CONSOLE = false;
    private static final int BUFFER_SIZE = 4096;
    // container
    private final CircularFifoQueue<LogEntry> entries = new CircularFifoQueue<>(BUFFER_SIZE);
    private Listener listener;

    public int size() {
        return entries.size();
    }

    public CircularFifoQueue<LogEntry> getEntries() {
        return entries;
    }

    public interface Listener {

        void onNewLogEntry(LogEntry logEntry, boolean overflow);

    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void add(String message) {
        LogEntry e = new LogEntry(message);
        boolean overflow = entries.size() == BUFFER_SIZE;
        entries.add(e);
        if (PRINT_TO_CONSOLE) {
            Log.d(ClientApp.TAG, message);
        }
        if (listener != null) listener.onNewLogEntry(e, overflow);
    }

    public void add(String message, Throwable throwable) {
        LogEntry e = new LogEntry(message, throwable);
        boolean overflow = entries.size() == BUFFER_SIZE;
        entries.add(e);
        if (PRINT_TO_CONSOLE) {
            Log.w(ClientApp.TAG, message, throwable);
        }
        if (listener != null) listener.onNewLogEntry(e, overflow);
    }

}
