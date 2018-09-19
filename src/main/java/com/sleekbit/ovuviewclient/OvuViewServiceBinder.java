package com.sleekbit.ovuviewclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.sleekbit.ovuview.remote.v1.IOvuViewService;

/**
 * Provides utility methods for binding and unbinding to OvuRemoteService.
 */
public class OvuViewServiceBinder {

    private static final String SERVICE_CLASS = "com.sleekbit.ovuview.remote.OvuRemoteService";
    private static final String SERVICE_APP_PACKAGE = "com.sleekbit.ovuview";

    public interface ConnectionCallback {

        void onConnected(IOvuViewService iOvuViewService);

        void onDisconnected();

    }

    public static ServiceConnection bind(Context context, final ConnectionCallback connectionCallback) {
        Intent intent = new Intent();
        // set package name and class directly
        intent.setComponent(new ComponentName(SERVICE_APP_PACKAGE, SERVICE_CLASS));
        // bind to the service
        ServiceConnection connection;
        context.bindService(intent, connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // ignore component
                connectionCallback.onConnected(IOvuViewService.Stub.asInterface(service));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // ignore component
                connectionCallback.onDisconnected();
            }
        }, Context.BIND_AUTO_CREATE);
        return connection;
    }

    public static void unbind(Context context, ServiceConnection connection) {
        context.unbindService(connection);
    }

}
