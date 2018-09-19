package com.sleekbit.ovuviewclient.logging;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.sleekbit.ovuviewclient.R;

/**
 *
 */
public class LogMessageHolder extends RecyclerView.ViewHolder {
    // views
    private TextView msgText;

    LogMessageHolder(View itemView) {
        super(itemView);
        // extract references
        msgText = itemView.findViewById(R.id.text);
    }

    void bind(String msgText) {
        this.msgText.setText(msgText);
    }

}

