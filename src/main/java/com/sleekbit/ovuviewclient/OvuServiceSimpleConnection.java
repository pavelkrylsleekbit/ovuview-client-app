package com.sleekbit.ovuviewclient;

import android.content.Context;
import android.content.ServiceConnection;

import com.google.common.base.Preconditions;
import com.sleekbit.ovuview.remote.v1.IOvuViewService;

/**
 * Limits capabilities of android bound services.
 * The connection cannot be established, dropped and then again re-established automagically (as android does).
 * Instead, once Android reports that the connection is broken, it is kept closedb (no repeated callbacks
 * about again established connection). Programmatically, however, you are allowed to reestablish
 * connection again.
 *
 *
 */
public class OvuServiceSimpleConnection {

    // members
    private ConnectionStateCallback callback;
    private ServiceConnection connection;
    private ConnectionState state = ConnectionState.DISCONNECTED;

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////
    public interface ConnectionStateCallback {

        void onConnecting();

        void onConnected(IOvuViewService iOvuViewService);

        void onAppInitiatedDisconnect();

        void onUnexpectedDisconnect();

    }

    public ConnectionState getState() {
        return state;
    }

    public void connect(Context context, final ConnectionStateCallback callback) {
        Preconditions.checkState(this.callback == null, "already connected");
        Preconditions.checkState(this.connection == null, "already connected");
        Preconditions.checkState(this.state == ConnectionState.DISCONNECTED);
        Preconditions.checkArgument(callback != null);
        // remember application-level callback
        this.callback = callback;
        // set proper state
        setState(ConnectionState.CONNECTING).onConnecting();
        // save the connection
        this.connection = OvuViewServiceBinder.bind(context, new OvuViewServiceBinder.ConnectionCallback() {
            @Override
            public void onConnected(IOvuViewService iOvuViewService) {
                setState(ConnectionState.CONNECTED).onConnected(iOvuViewService);
            }

            @Override
            public void onDisconnected() {
                Preconditions.checkState(OvuServiceSimpleConnection.this.callback != null);
                // cleanup and notify the callback
                cleanupAndUnbind(context).onUnexpectedDisconnect();
            }
        });
    }

    public void disconnect(Context context) {
        // cleanup and notify the callback
        cleanupAndUnbind(context).onAppInitiatedDisconnect();
    }

    ///////////////////////////////////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Clean up the stateful variables and call unbind - to make sure that we do not receive
     * repeated onConnected callback invocations.
     */
    private ConnectionStateCallback cleanupAndUnbind(Context context) {
        // set proper statem, do not report the state change, it will be reported by the upper layer
        setState(ConnectionState.DISCONNECTED);
        // cleanup
        ConnectionStateCallback r = OvuServiceSimpleConnection.this.callback;
        OvuServiceSimpleConnection.this.callback = null;
        ServiceConnection c = OvuServiceSimpleConnection.this.connection;
        OvuServiceSimpleConnection.this.connection = null;
        // unbind from the service
        OvuViewServiceBinder.unbind(context, c);
        // return the old user callback
        return r;
    }

    private ConnectionStateCallback setState(ConnectionState newState) {
        this.state = newState;
        // make the invocation chainable
        return callback;
    }

}
