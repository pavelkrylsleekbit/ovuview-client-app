package com.sleekbit.ovuviewclient;

import com.sleekbit.ovuview.remote.v1.IOvuViewService;

import static com.sleekbit.ovuviewclient.ClientApp.daApp;

/**
 *
 */
public class ConnectionStateCallbackInterceptor implements OvuServiceSimpleConnection.ConnectionStateCallback {
    // members
    private final OvuServiceSimpleConnection.ConnectionStateCallback delegate;
    private final Runnable onStateChange;
    private final String connectingMsg, connectedMsg, appInitiatedDisconnectMsg, unexpectedDisconnectMsg;


    ConnectionStateCallbackInterceptor(OvuServiceSimpleConnection.ConnectionStateCallback delegate,
                                       Runnable stateChangeListener,
                                       String connectingMsg,
                                       String connectedMsg,
                                       String appInitiatedDisconnectMsg,
                                       String unexpectedDisconnectMsg) {
        this.delegate = delegate;
        this.onStateChange = stateChangeListener;
        this.connectingMsg = connectingMsg;
        this.connectedMsg = connectedMsg;
        this.appInitiatedDisconnectMsg = appInitiatedDisconnectMsg;
        this.unexpectedDisconnectMsg = unexpectedDisconnectMsg;
    }

    @Override
    public void onConnecting() {
        daApp.logBuffer.add(connectingMsg);
        onStateChange.run();
        delegate.onConnecting();
    }

    @Override
    public void onConnected(IOvuViewService iOvuViewService) {
        daApp.logBuffer.add(connectedMsg);
        onStateChange.run();
        delegate.onConnected(iOvuViewService);
    }

    @Override
    public void onAppInitiatedDisconnect() {
        daApp.logBuffer.add(appInitiatedDisconnectMsg);
        onStateChange.run();
        delegate.onAppInitiatedDisconnect();
    }

    @Override
    public void onUnexpectedDisconnect() {
        daApp.logBuffer.add(unexpectedDisconnectMsg);
        onStateChange.run();
        delegate.onUnexpectedDisconnect();
    }

}
