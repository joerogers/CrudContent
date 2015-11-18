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

package com.example.crudcontent.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;

import com.example.crudcontent.BuildConfig;
import com.example.crudcontent.R;
import com.example.crudcontent.adapter.ToolbarSpinnerAdapter;
import com.example.crudcontent.databinding.CityActivityBinding;
import com.example.crudcontent.fragment.CityListFragment;
import com.example.crudcontent.provider.StateContract;
import com.forkingcode.crudcontent.service.BasicCRUDIntentService;
import com.forkingcode.crudcontent.service.BasicCrudResultReceiver;

import java.lang.ref.WeakReference;

public class CityActivity extends AppCompatActivity
        implements CityListFragment.CityListFragmentListener {

    private static final String STATE_SORT_ORDER = "sortOrder";

    private static boolean createdStates = false;
    private CityActivityBinding binding;
    private int sortOrder = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_city);
        setSupportActionBar(binding.toolBar);

        binding.spinner.setAdapter(new ToolbarSpinnerAdapter(binding.spinner.getContext()));
        binding.setListeners(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle(R.string.title_activity_city_list);
        }

        if (!createdStates) {
            BasicCRUDIntentService
                    .performBulkInsert(this, StateContract.URI)
                    .usingValues(StateContract.buildStateValuesArray())
                    .resultReceiver(new StatesCreatedResultReceiver(this))
                    .start();
        }

        if (savedInstanceState != null) {
            sortOrder = savedInstanceState.getInt(STATE_SORT_ORDER, 0);
        }

        updateSortOrder(sortOrder);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SORT_ORDER, sortOrder);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void editCity(long cityId) {
        startActivity(EditCityActivity.buildIntent(this, cityId));
    }

    @SuppressWarnings("unused")   // Called via data binding
    public void onAddCity(View view) {
        startActivity(EditCityActivity.buildIntent(this));
    }

    @SuppressWarnings("unused")   // Called via data binding
    public void onSortOrderSelected(AdapterView<?> parent, View view, int position, long id) {
        sortOrder = position;
        updateSortOrder(position);
    }

    public void updateSortOrder(int sortOrder) {
        CityListFragment fragment = (CityListFragment) getSupportFragmentManager().findFragmentById(R.id.city_fragment);
        if (fragment != null) {
            fragment.setSortOrder(sortOrder);
        }
    }

    /**
     * Result receiver to handle result of the bulk operation and display a SnackBar. Using
     * a weak reference as the the database operations are asynchronous and do not want to hold
     * a strong reference to the activity in case it is ended before the service returns
     */
    public static class StatesCreatedResultReceiver extends BasicCrudResultReceiver {

        // saving the reference to the city activity. A weak reference allows the garbage
        // collector reclaim the memory if this is the last reference to the activity
        private final WeakReference<CityActivity> cityActivityRef;

        public StatesCreatedResultReceiver(CityActivity cityActivity) {
            super(new Handler());
            cityActivityRef = new WeakReference<>(cityActivity);
        }

        @Override
        protected void onBulkInsertComplete(int rows) {
            // Just marking it was complete. Because sample uses IGNORE conflict
            // resolution rows may be 0 if states were added during a previous run
            // of the application
            CityActivity.createdStates = rows >= 0;

            CityActivity activity = cityActivityRef.get();

            // Ensure the activity still exists. Also check the "bindings", just in case the activity
            // is in process of being destroyed and the bindings have been released but activity has not
            // been garbage collected.
            if (activity != null && activity.binding != null) {
                Snackbar.make(activity.binding.coordinatorLayout, R.string.states_created, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
