package com.sleekbit.ovuviewclient;

import android.os.RemoteException;

import com.sleekbit.ovuview.remote.v1.DataSetInfo;
import com.sleekbit.ovuview.remote.v1.IOvuViewService;
import com.sleekbit.ovuview.remote.v1.ResultStatus;
import com.sleekbit.ovuview.remote.v1.TemperatureSymptomValue;

/**
 * Wrapper around IOvuViewService working on top of a single dataset.
 * Internally holds dataset identification.
 */
class DataSetServiceImpl implements DataSetService {
    private final IOvuViewService delegate;
    private final DataSetInfo dataSetInfo;

    DataSetServiceImpl(IOvuViewService delegate, DataSetInfo dataSetInfo) {
        this.delegate = delegate;
        this.dataSetInfo = dataSetInfo;
    }

    public String getDataSetId() {
        return dataSetInfo.id;
    }

    public String getDataSetLabel() {
        return dataSetInfo.label;
    }

    @Override
    public void setTemperatureSymptom(int date, TemperatureSymptomValue temp, ResultStatus resultStatus) {
        try {
            delegate.setTemperatureSymptom(dataSetInfo.id, date, temp, resultStatus);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TemperatureSymptomValue getTemperatureSymptomValue(int date, ResultStatus resultStatus) {
        try {
            return delegate.getTemperatureSymptom(dataSetInfo.id, date, resultStatus);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
