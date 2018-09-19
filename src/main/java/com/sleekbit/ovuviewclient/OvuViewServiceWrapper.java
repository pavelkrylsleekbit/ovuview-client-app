package com.sleekbit.ovuviewclient;

import android.os.IBinder;
import android.os.RemoteException;

import com.sleekbit.ovuview.remote.v1.DataSetInfo;
import com.sleekbit.ovuview.remote.v1.IOvuViewService;
import com.sleekbit.ovuview.remote.v1.ResultStatus;
import com.sleekbit.ovuview.remote.v1.TemperatureSymptomValue;

import java.util.List;

/**
 * Wraps the OvuViewService notifying interceptor anytime a service operation is about to execute.
 */
public class OvuViewServiceWrapper implements IOvuViewService {
    private final IOvuViewService delegate;
    private final OperationInterceptor interceptor;

    OvuViewServiceWrapper(IOvuViewService delegate, OperationInterceptor interceptor) {
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public List<DataSetInfo> getOwnedDataSets(ResultStatus resultStatus) throws RemoteException {
        interceptor.ping();
        return delegate.getOwnedDataSets(resultStatus);
    }

    @Override
    public void setTemperatureSymptom(String dataSetId,
                                      int date,
                                      TemperatureSymptomValue value, ResultStatus resultStatus) throws RemoteException {
        interceptor.ping();
        delegate.setTemperatureSymptom(dataSetId, date, value, resultStatus);
    }

    @Override
    public TemperatureSymptomValue getTemperatureSymptom(String dataSetId,
                                                         int date,
                                                         ResultStatus resultStatus) throws RemoteException {
        interceptor.ping();
        return delegate.getTemperatureSymptom(dataSetId, date, resultStatus);
    }

    @Override
    public IBinder asBinder() {
        return delegate.asBinder();
    }

}
