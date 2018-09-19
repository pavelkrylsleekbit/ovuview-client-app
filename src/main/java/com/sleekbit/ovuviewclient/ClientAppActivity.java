package com.sleekbit.ovuviewclient;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.sleekbit.ovuview.remote.v1.DataSetInfo;
import com.sleekbit.ovuview.remote.v1.IOvuViewService;
import com.sleekbit.ovuview.remote.v1.OvuUtil;
import com.sleekbit.ovuview.remote.v1.ResultStatus;
import com.sleekbit.ovuview.remote.v1.TemperatureSymptomValue;
import com.sleekbit.ovuviewclient.logging.LogMessageHolder;
import com.sleekbit.ovuviewclient.logging.LogMessageHolderAdapter;
import com.sleekbit.ovuviewclient.logging.NpaLinearLayoutManager;
import com.sleekbit.ovuviewclient.picker.DatePickerFragment;
import com.sleekbit.ovuviewclient.picker.TimePickerFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.sleekbit.ovuviewclient.ClientApp.daApp;

public class ClientAppActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, OvuServiceRxConnection.StateListener {
    // static references to make things easier
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private static SimpleDateFormat TWENTYFOUR_HOUR_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static OvuServiceRxConnection rxConnection = new OvuServiceRxConnection();
    // controls/visuals
    private Button btnReadTemp;
    private Button btnWriteTemp;
    private EditText etTemperature;
    private CheckBox chTrueBbt;
    private TextView tvDate;
    private TextView tvTime;
    private RecyclerView tvConsole;
    private View progress;
    private RecyclerView.Adapter<LogMessageHolder> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // extract references to control elements
        btnReadTemp = findViewById(R.id.btnReadTemp);
        btnReadTemp.setOnClickListener(view -> readTemp(OvuUtil.calendarToOvuDate(parseDate(tvDate.getText().toString()))));
        btnWriteTemp = findViewById(R.id.btnWriteTemp);
        btnWriteTemp.setOnClickListener(view -> {
            // retrieve temperature value from controls
            TemperatureSymptomValue tv = new TemperatureSymptomValue();
            int[] hrMin = parseTime(tvTime.getText().toString());
            tv.setTime(hrMin[0], hrMin[1]);
            String tempText = etTemperature.getText().toString();
            tv.value = tempText.length() == 0 ? Double.MAX_VALUE : Float.parseFloat(tempText);
            if (tv.value <= Constants.MIN_TEMPERATURE_CELSIUS || tv.value >= Constants.MAX_TEMPERATURE_CELSIUS) {
                Toast.makeText(daApp, "Invalid temperature value", Toast.LENGTH_LONG).show();
                // you might remove this return to see how the service validates values
                return;
            }
            tv.trueBbt = chTrueBbt.isChecked();
            tv.originAppId = ClientApp.OVU_CLIENT_APP_ID;
            writeTemp(OvuUtil.calendarToOvuDate(parseDate(tvDate.getText().toString())), tv);
        });
        etTemperature = findViewById(R.id.etTemperature);
        chTrueBbt = findViewById(R.id.chTrueBbt);
        tvDate = findViewById(R.id.tvDate);
        tvDate.setOnClickListener((ignore) -> showDatePickerDialog());
        tvTime = findViewById(R.id.tvTime);
        tvTime.setOnClickListener((ignore) -> showTimePickerDialog());
        tvConsole = findViewById(R.id.tvConsole);
        progress = findViewById(R.id.indeterminateBar);
        // set up the listview
        initializeAdapter();
        tvConsole.setLayoutManager(new NpaLinearLayoutManager(this));
        tvConsole.setAdapter(adapter);
        tvConsole.setHasFixedSize(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // fill the defaults
        if (tvDate.getText().length() == 0) {
            // fill in the current date in DD-MM-YYYY format
            Calendar now = Calendar.getInstance();
            tvDate.setText(formatDate(now));
            // fill in the current time in HH:MM format
            tvTime.setText(formatTime(now));
        }
        // register listeners
        daApp.logBuffer.setListener((e,o) -> updateConsoleLog(o));
        rxConnection.setListener(this);
        updateUi();
    }

    @Override
    public void onStateChanged(ConnectionState state) {
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // reset the listener - to not try to update the UI
        daApp.logBuffer.setListener(null);
        rxConnection.setListener(null);
    }


    private void updateUi() {
        progress.setVisibility(rxConnection.getState() == ConnectionState.DISCONNECTED ?
                View.GONE : View.VISIBLE);
    }

    private void updateConsoleLog(boolean overflow) {
        int lastIdx = daApp.logBuffer.size() - 1;
        if (overflow) {
            adapter.notifyDataSetChanged();
            tvConsole.scrollToPosition(lastIdx);
        } else {
            // the log buffer is now one item bigger
            adapter.notifyItemInserted(lastIdx);
        }
        int range = tvConsole.computeVerticalScrollRange();
        int verticalOffset = tvConsole.computeVerticalScrollOffset();
        int verticalExtent = tvConsole.computeVerticalScrollExtent();
        if (verticalExtent + verticalOffset >= (range - verticalExtent * 0.1)) {
            // it seems that the user is viewing tail of the log
            tvConsole.scrollToPosition(lastIdx);
            tvConsole.setScrollbarFadingEnabled(false);
        } else {
            // hide the scrollbar - cause we cannot force android to do recalculation on the fly
            tvConsole.setScrollbarFadingEnabled(true);
        }
    }

    private void initializeAdapter() {
        adapter = new LogMessageHolderAdapter(daApp.logBuffer);
    }


    @NonNull
    private DataSetServiceImpl getFirstDataSetService(IOvuViewService conn, List<DataSetInfo> dataSets) {
        if (!dataSets.isEmpty()) {
            return new DataSetServiceImpl(conn, dataSets.get(0));
        } else {
            throw new RuntimeException("there are no datasets!");
        }
    }

    private void checkResultStatus(ResultStatus resultStatus, String errMsgPrefix) {
        if (resultStatus.status != ResultStatus.STATUS_OK) {
            throw new RuntimeException(errMsgPrefix + " " + resultStatus.explanation);
        } // else: OK
    }

    private <T> T checkAndReturnValue(T value, ResultStatus resultStatus, String errMsgPrefix) {
        if (resultStatus.status != ResultStatus.STATUS_OK) {
            throw new RuntimeException(errMsgPrefix + " " + resultStatus.explanation);
        } // else: OK
        return value;
    }

    private Disposable readTemp(int ovuDate) {
        return rxConnection.getAliveConnection()
                // we are on the main application thread
                .doOnSuccess(conn -> daApp.logBuffer.add("** READING TEMP **"))
                // switch to IO thread - we will do service invocation
                .observeOn(Schedulers.io())
                .map(conn -> {
                    ResultStatus resultStatus = new ResultStatus();
                    List<DataSetInfo> dataSets = conn.getOwnedDataSets(resultStatus);
                    return getFirstDataSetService(conn,
                            checkAndReturnValue(dataSets, resultStatus, "problem retrieving datasets"));
                })
                // retrieve the temperature
                // switch back to main thread - we are modifying UI
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(dsService -> daApp.logBuffer.add("using dataset service: " + dsService.getDataSetLabel() + "@" + dsService.getDataSetId()))
                // switch back to IO thread
                .observeOn(Schedulers.io())
                .map(dsService -> {
                    // retrieve date from controls
                    ResultStatus resultStatus = new ResultStatus();
                    TemperatureSymptomValue value = dsService.getTemperatureSymptomValue(ovuDate, resultStatus);
                    return checkAndReturnValue(value, resultStatus, "problem retrieving temperature symptom value");
                })
                // switch back to UI thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(t -> {
                    // print the temperature value
                    if (t == null) {
                        daApp.logBuffer.add("temperature: NULL");
                    } else {
                        daApp.logBuffer.add("retrieved temperature: temp = " + String.format(Locale.getDefault(), "%.2f", t.value)
                                + ", trueBbt = " + t.trueBbt
                                + ", time = " + formatTime(t.time)
                                + ", originAppId = " + t.originAppId);
                    }
                }, exc -> daApp.logBuffer.add("problem while reading temperature", exc));
    }

    private Disposable writeTemp(int ovuDate, TemperatureSymptomValue temperatureSymptomValue) {
        return rxConnection.getAliveConnection()
                // we are on the main application thread
                .doOnSuccess(conn -> daApp.logBuffer.add("** WRITING TEMP **"))
                // switch to IO thread - we will do service invocation
                .observeOn(Schedulers.io())
                .map(conn -> {
                    ResultStatus resultStatus = new ResultStatus();
                    List<DataSetInfo> dataSets = conn.getOwnedDataSets(resultStatus);
                    return getFirstDataSetService(conn,
                            checkAndReturnValue(dataSets, resultStatus, "problem retrieving datasets"));
                })
                // retrieve the temperature
                // switch back to main thread - we are logging
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(dsService -> daApp.logBuffer.add("using dataset service: " + dsService.getDataSetLabel() + "@" + dsService.getDataSetId()))
                // switch back to IO thread
                .observeOn(Schedulers.io())
                .map(dsService -> {
                    // invoke the service
                    ResultStatus resultStatus = new ResultStatus();
                    dsService.setTemperatureSymptom(ovuDate, temperatureSymptomValue, resultStatus);
                    checkResultStatus(resultStatus, "problem writing temperature symptom value");
                    return temperatureSymptomValue;
                })
                // switch back to UI thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(t ->
                        daApp.logBuffer.add("temperature written: " + String.format(Locale.getDefault(), "%.2f", t.value)
                            + ", trueBbt = " + t.trueBbt
                            + ", time = " + formatTime(t.time)
                            + ", originAppId = " + t.originAppId)
                , exc -> daApp.logBuffer.add("problem while writing temperature", exc));
    }


    public void showDatePickerDialog() {
        // there is always something filled in the tvDate
        Preconditions.checkState(tvDate.getText().length() > 0);
        // retrieve the date from textview
        DatePickerFragment.show(this, parseDate(tvDate.getText().toString()));
    }

    public void showTimePickerDialog() {
        // there is always something filled in the tvTime
        Preconditions.checkState(tvTime.getText().length() > 0);
        // retrieve the date from textview
        int[] hrsMins = parseTime(tvTime.getText().toString());
        TimePickerFragment.show(this, hrsMins[0], hrsMins[1]);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        tvDate.setText(formatDate(cal));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        tvTime.setText(formatTime(OvuUtil.ovuTimeFromHoursMinutes(hourOfDay, minute)));
    }


    ///////////////////////////////////////////////////////////////////////////
    // parsing/formatting utility methods
    ///////////////////////////////////////////////////////////////////////////
    private static String formatDate(Calendar cal) {
        return DATE_FORMAT.format(cal.getTime());
    }

    private static String formatTime(int ovuTime) {
        final int[] hm = new int[2];
        OvuUtil.ovuTimeToHoursMinutes(ovuTime, hm);
        //noinspection deprecation
        return TWENTYFOUR_HOUR_FORMAT.format(new Date(0, 0, 0, hm[0], hm[1]));
    }

    private static String formatTime(Calendar cal) {
        //noinspection deprecation
        return TWENTYFOUR_HOUR_FORMAT.format(new Date(cal.getTimeInMillis()));
    }

    private static Calendar parseDate(String formattedDate) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(DATE_FORMAT.parse(formattedDate));
            return cal;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] parseTime(String formattedTime) {
        String hm[] = formattedTime.split(":");
        //
        int[] hrMin = new int[2];
        try {
            hrMin[0] = Integer.parseInt(hm[0]);
            hrMin[1] = Integer.parseInt(hm[1]);
            return hrMin;
        } catch (NumberFormatException e) {
            throw new RuntimeException("wrong time format: " + formattedTime, e);
        }
    }

}
