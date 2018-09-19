package com.sleekbit.ovuviewclient;

import com.sleekbit.ovuview.remote.v1.ResultStatus;
import com.sleekbit.ovuview.remote.v1.TemperatureSymptomValue;

/**
 *
 */
public interface DataSetService {

    String getDataSetId();

    void setTemperatureSymptom(int date, TemperatureSymptomValue temp, ResultStatus resultStatus);

    TemperatureSymptomValue getTemperatureSymptomValue(int date, ResultStatus resultStatus);

}
