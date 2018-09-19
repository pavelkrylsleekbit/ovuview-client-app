package com.sleekbit.ovuviewclient;

import android.app.Application;

import com.sleekbit.ovuviewclient.logging.LogBuffer;

import java.util.UUID;

/**
 * This comes handy because of the LogBuffer.
 */
public class ClientApp extends Application {
    // constants
    public static final String TAG = "OVU-CLIENT";
    public static final UUID OVU_CLIENT_APP_ID = UUID.fromString("0b844795-91b7-44d8-ac76-9c840db6c163");
    // 'context'
    public static ClientApp daApp;

    // references to generically usable fields
    public LogBuffer logBuffer;

    @Override
    public void onCreate() {
        super.onCreate();
        // set up the static reference/context
        ClientApp.daApp = this;
        //
        initialize();
    }

    private void initialize() {
        logBuffer = new LogBuffer();
    }


}
