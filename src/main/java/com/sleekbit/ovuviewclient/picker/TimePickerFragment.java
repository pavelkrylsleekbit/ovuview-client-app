package com.sleekbit.ovuviewclient.picker;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import com.google.common.base.Preconditions;

/**
 *
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    public static final String KEY_OVU_TIME_HR = "KEY_OVU_TIME_HR";
    public static final String KEY_OVU_TIME_MIN = "KEY_OVU_TIME_MIN";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // retrieve value from args
        Bundle args = getArguments();
        int timeHr = args.getInt(KEY_OVU_TIME_HR);
        int timeMin = args.getInt(KEY_OVU_TIME_MIN);
        return new TimePickerDialog(getActivity(), this, timeHr, timeMin, true);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        ((TimePickerDialog.OnTimeSetListener) getActivity()).onTimeSet(view, hourOfDay, minute);
    }

    public static void show(Activity parentActivity, int timeHr, int timeMin) {
        Bundle b = new Bundle();
        Preconditions.checkState(timeHr >= 0 && timeHr < 24);
        Preconditions.checkState(timeMin >= 0 && timeMin < 60);
        b.putInt(KEY_OVU_TIME_HR, timeHr);
        b.putInt(KEY_OVU_TIME_MIN, timeMin);
        TimePickerFragment f = new TimePickerFragment();
        f.setArguments(b);
        f.show(parentActivity.getFragmentManager(), "timePickerFragment");
    }

}
