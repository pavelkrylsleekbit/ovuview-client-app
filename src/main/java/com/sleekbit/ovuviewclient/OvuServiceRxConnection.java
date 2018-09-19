package com.sleekbit.ovuviewclient;

import com.google.common.base.Preconditions;
import com.sleekbit.ovuview.remote.v1.IOvuViewService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static com.sleekbit.ovuviewclient.ClientApp.daApp;

/**
 * Reactive connection manager.
 * Wraps a simple ovu service connection.
 * Returns a Single providing the connected service (once subscribed).
 * Moreover, implements an idle timeout logic which closes the connection automatically after period of inactivity.
 *
 */
public class OvuServiceRxConnection {
    private static final int SERVICE_IDLE_TIMEOUT = 5;    // seconds
    // members
    private OvuServiceSimpleConnection simpleConnection;
    private Single<IOvuViewService> ovuServiceSingle;
    private Disposable watchDogSubscription;
    private ObservableEmitter<Integer> watchDogEmitter;
    private StateListener listener;

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////
    public interface StateListener {

        void onStateChanged(ConnectionState state);

    }

    @SuppressWarnings("WeakerAccess")
    public OvuServiceRxConnection() {
        this.simpleConnection = new OvuServiceSimpleConnection();
    }

    public void setListener(StateListener listener) {
        this.listener = listener;
    }

    public ConnectionState getState() {
        return simpleConnection.getState();
    }


    public Single<IOvuViewService> getAliveConnection() {
        ConnectionState connState = simpleConnection.getState();
        if (ovuServiceSingle != null) {
            // return cached (and not yet closed/disconnected) instance
            return ovuServiceSingle;
        } // else:
        Preconditions.checkState(connState == ConnectionState.DISCONNECTED);
        OvuServiceSimpleConnection myConnector = simpleConnection;
        AtomicBoolean successReported = new AtomicBoolean(false);
        ovuServiceSingle = Single.<IOvuViewService>create(emitter -> {
            // connect with application context scope
            simpleConnection.connect(ClientApp.daApp, new ConnectionStateCallbackInterceptor(
                    new OvuServiceSimpleConnection.ConnectionStateCallback() {

                @Override
                public void onConnecting() {
                    // nothing
                }

                @Override
                public void onConnected(IOvuViewService iOvuViewService) {
                    if (!emitter.isDisposed()) {
                        //
                        successReported.set(true);
                        // setup a watchdog
                        watchDogSubscription = Observable.<Integer>create(emitter2 -> {
                            watchDogEmitter = emitter2;
                            // emit initial value - start timeout countdown now
                            watchDogEmitter.onNext(1);
                        })
                                .throttleWithTimeout(SERVICE_IDLE_TIMEOUT, TimeUnit.SECONDS)
                                // make sure that we initiate the disconnect from the main application thread
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(ignore -> {
                                    daApp.logBuffer.add("service idle timeout expired");
                                    // close the connection once the timeout has expired
                                    if (myConnector.getState() == ConnectionState.CONNECTED) {
                                        myConnector.disconnect(ClientApp.daApp);
                                    }
                                });
                        // report that the service has been created
                        emitter.onSuccess(new OvuViewServiceWrapper(iOvuViewService, () -> {
                            if (!watchDogEmitter.isDisposed())
                                // notify watchdog that the user has interacted with the service
                                watchDogEmitter.onNext(1);
                        }));
                    } else
                        // close the connection now
                        simpleConnection.disconnect(ClientApp.daApp);
                }

                @Override
                public void onAppInitiatedDisconnect() {
                    cleanup();
                }

                @Override
                public void onUnexpectedDisconnect() {
                    cleanup();
                    // report error if we did not report success
                    if (!successReported.get()) {
                        emitter.onError(new Exception("unexpected disconnect"));
                    }
                }

            }, this::propagateStateChange,
                    "connecting", "connected", "app initiated disconnect", "unexpected disconnect"
                    ));
        }).cache()
                // report the result on the main application thread
                .observeOn(AndroidSchedulers.mainThread());
        return ovuServiceSingle;
    }

    ///////////////////////////////////////////////////////////////////////////
    // implementation
    ///////////////////////////////////////////////////////////////////////////
    private void propagateStateChange() {
        if (listener != null) listener.onStateChanged(simpleConnection.getState());
    }

    private void cleanup() {
        // unset the Single observable
        ovuServiceSingle = null;
        // dispose the watchDogSubscription
        if (watchDogSubscription != null) watchDogSubscription.dispose();
    }

}
