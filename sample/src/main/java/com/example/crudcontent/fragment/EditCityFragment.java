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


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.example.crudcontent.R;
import com.example.crudcontent.adapter.StateAdapter;
import com.example.crudcontent.databinding.EditCityFragmentBindings;
import com.example.crudcontent.loader.CityLoaderCallbacks;
import com.example.crudcontent.loader.StateLoaderCallbacks;
import com.example.crudcontent.provider.CityContract;

import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class EditCityFragment extends Fragment
        implements Handler.Callback,
        CityLoaderCallbacks.CityLoadListener,
        StateLoaderCallbacks.StateLoadListener,
        DatePickerDialogFragment.DatePickerDialogFragmentListener {

    private static final String STATE_CITY = "EditCityFragment.city";
    private static final String STATE_STATE_ID = "EditCityFragment.stateId";
    private static final String STATE_DATE_VISITED = "EditCityFragment.dateOfVisit";
    private static final String STATE_NOTES = "EditCityFragment.notes";

    private static final String[] CITY_PROJECTION = new String[]{
            CityContract.Columns.NAME,
            CityContract.Columns.STATE_ID,
            CityContract.Columns.DATE_VISITED,
            CityContract.Columns.NOTES
    };

    private static final int NAME_POS = 0;
    private static final int STATE_ID_POS = 1;
    private static final int DATE_VISITED_POS = 2;
    private static final int NOTES_POS = 3;

    private static final int ENABLE_ANIMATION = 1;


    private static final String ARG_CITY_ID = "cityId";

    private EditCityFragmentListener listener;
    private long cityId = CityContract.NO_CITY_ID;
    private String city;
    private long stateId = -1;
    private Date dateOfVisit = new Date();
    private String notes;
    private Handler handler;
    private boolean animationEnabled = false;

    public interface EditCityFragmentListener {
        void submitCity(ContentValues values);
    }

    public static EditCityFragment newInstance(long cityId) {
        EditCityFragment fragment = new EditCityFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CITY_ID, cityId);
        fragment.setArguments(args);
        return fragment;
    }

    public EditCityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler(this);
        animationEnabled = false;
        Fragment parent = getParentFragment();
        Object objectToCast = parent != null ? parent : context;
        try {
            listener = (EditCityFragmentListener) objectToCast;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(objectToCast.getClass().getSimpleName()
                    + " must implement EditCityFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // City id can not be changed, so always fetch from arguments
        Bundle args = getArguments();
        if (args != null) {
            cityId = args.getLong(ARG_CITY_ID, CityContract.NO_CITY_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        EditCityFragmentBindings bindings = EditCityFragmentBindings.inflate(inflater, container, false);
        bindings.setListeners(this);
        bindings.stateSpinner.setAdapter(new StateAdapter(getContext()));
        if (cityId != CityContract.NO_CITY_ID) {
            updateDateVisitedView(bindings);
        }

        return bindings.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        StateLoaderCallbacks.initLoader(getActivity(), getLoaderManager(), this, StateAdapter.PROJECTION);

        // Only initialize loader, if editing a city and the saved instance state is null
        // otherwise, the user may have edited data that is now tracked in the save state
        if (cityId != CityContract.NO_CITY_ID && savedInstanceState == null) {
            CityLoaderCallbacks.initLoader(getActivity(), getLoaderManager(), this, CITY_PROJECTION, cityId);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            city = savedInstanceState.getString(STATE_CITY);
            stateId = savedInstanceState.getLong(STATE_STATE_ID);
            dateOfVisit = new Date(savedInstanceState.getLong(STATE_DATE_VISITED));
            notes = savedInstanceState.getString(STATE_NOTES);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings == null) return;
        bindings.cityView.setText(city);
        updateSpinnerSelection(bindings);
        updateDateVisitedView(bindings);
        bindings.notesView.setText(notes);

        if (!animationEnabled) {
            handler.sendEmptyMessageDelayed(ENABLE_ANIMATION, 50);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings == null) return;
        city = bindings.cityView.getText().toString();
        notes = bindings.notesView.getText().toString();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CITY, city);
        outState.putLong(STATE_STATE_ID, stateId);
        outState.putLong(STATE_DATE_VISITED, dateOfVisit.getTime());
        outState.putString(STATE_NOTES, notes);
    }

    @Override
    public void onDetach() {
        listener = null;
        handler.removeMessages(ENABLE_ANIMATION);
        handler = null;
        super.onDetach();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ENABLE_ANIMATION:
                EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
                if (bindings == null) return false;
                bindings.cityTextLayout.setHintAnimationEnabled(true);
                bindings.notesTextLayout.setHintAnimationEnabled(true);
                animationEnabled = true;
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCityLoadComplete(Cursor cursor) {
        // If cursor is null, loader must be resetting
        // since we are not holding onto the cursor, all is good
        if (cursor == null) return;

        EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings == null) return;

        // Move to first row.
        if (cursor.moveToFirst()) {
            bindings.cityView.setText(cursor.getString(NAME_POS));
            stateId = cursor.getLong(STATE_ID_POS);
            updateSpinnerSelection(bindings);
            dateOfVisit = new Date(cursor.getLong(DATE_VISITED_POS));
            updateDateVisitedView(bindings);
            bindings.notesView.setText(cursor.getString(NOTES_POS));
        }
    }

    @Override
    public void onStateLoadComplete(Cursor cursor) {
        EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings == null) return;

        StateAdapter adapter = (StateAdapter) bindings.stateSpinner.getAdapter();
        adapter.swapCursor(cursor);
        if (cursor != null) {
            updateSpinnerSelection(bindings);
        }
    }

    @Override
    public void onDateSet(@NonNull Date date) {
        dateOfVisit = date;
        EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings != null) {
            updateDateVisitedView(bindings);
        }
    }

    @SuppressWarnings("unused") // called by data binding
    public void onDateVisitedClick(View v) {
        DatePickerDialogFragment fragment = DatePickerDialogFragment.newInstance(dateOfVisit);
        fragment.show(getChildFragmentManager(), "DIALOG");
    }

    @SuppressWarnings("unused") // called by data binding
    public void onSubmitClick(View v) {
        EditCityFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings == null) return;

        if (TextUtils.isEmpty(bindings.cityView.getText().toString())) {
            bindings.cityTextLayout.setErrorEnabled(true);
            bindings.cityTextLayout.setError(getString(R.string.error_no_city));
            return;
        }

        listener.submitCity(buildContentValues(bindings));
    }

    @SuppressWarnings("unused") // called by data binding
    public void onStateSelected(AdapterView<?> parent, View view, int position, long id) {
        stateId = id;
    }

    private void updateSpinnerSelection(@NonNull EditCityFragmentBindings bindings) {
        if (bindings.stateSpinner.getCount() == 0 ||
                stateId == bindings.stateSpinner.getSelectedItemId()) return;

        int size = bindings.stateSpinner.getCount();
        for (int i = 0; stateId > 0 && i < size; ++i) {
            if (stateId == bindings.stateSpinner.getItemIdAtPosition(i)) {
                bindings.stateSpinner.setSelection(i);
                return;
            }
        }

        bindings.stateSpinner.setSelection(0);
    }

    private void updateDateVisitedView(EditCityFragmentBindings bindings) {
        bindings.dateVisited.setText(DateFormat.getMediumDateFormat(getActivity()).format(dateOfVisit));
    }


    private ContentValues buildContentValues(@NonNull EditCityFragmentBindings bindings) {
        ContentValues values = new ContentValues();
        values.put(CityContract.Columns.NAME, bindings.cityView.getText().toString());
        values.put(CityContract.Columns.STATE_ID, stateId);
        int position = bindings.stateSpinner.getSelectedItemPosition();
        StateAdapter adapter = (StateAdapter) bindings.stateSpinner.getAdapter();
        values.put(CityContract.Columns.STATE_ABBREVIATION, adapter.getAbbreviation(position));
        values.put(CityContract.Columns.DATE_VISITED, dateOfVisit.getTime());
        values.put(CityContract.Columns.NOTES, bindings.notesView.getText().toString());
        return values;
    }
}
