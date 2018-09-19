package com.sleekbit.ovuviewclient.picker;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 *
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    public static final String KEY_MILLIS = "KEY_OVU_TIME";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // retrieve value from args
        Bundle args = getArguments();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(args.getLong(KEY_MILLIS));
        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        ((DatePickerDialog.OnDateSetListener) getActivity()).onDateSet(view, year, month, dayOfMonth);
    }

    public static void show(Activity parentActivity, Calendar date) {
        Bundle b = new Bundle();
        b.putLong(KEY_MILLIS, date.getTimeInMillis());
        DatePickerFragment f = new DatePickerFragment();
        f.setArguments(b);
        f.show(parentActivity.getFragmentManager(), "datePickerFragment");
    }

}
