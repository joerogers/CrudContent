/*
 * Copyright 2015 Joe Rogers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.crudcontent.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

public class DatePickerDialogFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private static final String ARG_DATE = "DatePickerDialogFragment.arg.date";

    private DatePickerDialogFragmentListener listener;
    private Date date;

    public interface DatePickerDialogFragmentListener {
        void onDateSet(@NonNull Date date);
    }

    public static DatePickerDialogFragment newInstance(Date date) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DATE, date.getTime());
        fragment.setArguments(args);
        return fragment;
    }

    public DatePickerDialogFragment() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // In the app this dialog is never called by an activity, but
        // here is an example of how to dynamically handle either a parent fragment
        // or an activity for our listener
        Fragment parent = getParentFragment();
        Object objectToCast = parent != null ? parent : context;
        listener = (DatePickerDialogFragmentListener) objectToCast;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            date = new Date(args.getLong(ARG_DATE));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Context context = getContext();
        if (context == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        // Initialize our date picker dialog with the last birthday of the user...
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        DatePickerDialog dialog = new DatePickerDialog(context, this,
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        // No birthdays allowed in the future...
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // One note. The DatePickerDialog is managing the saved state for us. This is why
        // this fragment isn't trying to do that. It is nice when that happens, but you
        // should always verify expected behavior.
        return dialog;
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
        listener.onDateSet(cal.getTime());
    }
}
