package com.sleekbit.ovuviewclient.logging;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sleekbit.ovuviewclient.R;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 *
 */
public class LogMessageHolderAdapter extends RecyclerView.Adapter<LogMessageHolder> {
        // view types
        private static final int TYPE_NORMAL = 0;
        private static final int TYPE_EXCEPTION = 1;

        private final LogBuffer logBuffer;

        public LogMessageHolderAdapter(LogBuffer logBuffer) {
            this.logBuffer = logBuffer;
        }

        @NonNull
        @Override
        public LogMessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view;
            int resId;
            switch (viewType) {
                case TYPE_NORMAL:
                    resId = R.layout.log_message_debug;
                    break;
                case TYPE_EXCEPTION:
                    resId = R.layout.log_message_exception;
                    break;
                default:
                    throw new IllegalStateException();
            }
            view = inflater.inflate(resId, parent, false);
            view.setTag(viewType);
            return new LogMessageHolder(view);
        }

        @Override
        public int getItemViewType(int position) {
            return getLogEntry(position).hasException() ? TYPE_EXCEPTION : TYPE_NORMAL;
        }

        private LogEntry getLogEntry(int position) {
            return logBuffer.getEntries().get(position);
        }


        @Override
        public void onBindViewHolder(@NonNull LogMessageHolder holder, int position) {
            LogEntry logEntry = getLogEntry(position);
            StringBuilder sb = new StringBuilder(logEntry.message);
            if (logEntry.hasException()) {
                // change to 'false' if you are interested in the full stacktrace
                //noinspection ConstantIfStatement
                if (true) {
                    //noinspection ConstantConditions
                    sb.append(": ").append(logEntry.exception.getMessage());
                } else {
                    sb.append(": ");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream stream = new PrintStream(baos);
                    //noinspection ConstantConditions
                    logEntry.exception.printStackTrace(stream);
                    try {
                        sb.append(baos.toString("utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            holder.bind(sb.toString());
        }

        @Override
        public int getItemCount() {
            return logBuffer.getEntries().size();
        }


}
