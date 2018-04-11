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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.example.crudcontent.BuildConfig;
import com.example.crudcontent.R;
import com.example.crudcontent.adapter.ToolbarSpinnerAdapter;
import com.example.crudcontent.databinding.CityActivityBinding;
import com.example.crudcontent.fragment.CityListFragment;
import com.example.crudcontent.provider.StateContract;
import com.forkingcode.crudcontent.task.BasicCRUDInsertTask;

public class CityActivity extends AppCompatActivity
        implements CityListFragment.CityListFragmentListener {

    private static final String STATE_SORT_ORDER = "sortOrder";

    private static final IntentFilter INSERT_FILTER = new IntentFilter(BasicCRUDInsertTask.INSERT_COMPLETE_ACTION);
    private static boolean createdStates = false;
    private CityActivityBinding binding;
    private BroadcastReceiver receiver;
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

        if (savedInstanceState != null) {
            sortOrder = savedInstanceState.getInt(STATE_SORT_ORDER, 0);
        }

        updateSortOrder(sortOrder);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!createdStates) {
            receiver = new StatesCreatedResultReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, INSERT_FILTER);
            new BasicCRUDInsertTask.Builder(this)
                    .forUri(StateContract.URI)
                    .usingValues(StateContract.buildStateValuesArray())
                    .requestResultBroadcast()
                    .start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (receiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SORT_ORDER, sortOrder);
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }

    @Override
    public void editCity(long cityId) {
        startActivity(EditCityActivity.buildIntent(this, cityId));
    }

    public void onAddCity() {
        startActivity(EditCityActivity.buildIntent(this));
    }

    // Called via data binding
    public void onSortOrderSelected(int position) {
        sortOrder = position;
        updateSortOrder(position);
    }

    private void updateSortOrder(int sortOrder) {
        CityListFragment fragment = (CityListFragment) getSupportFragmentManager().findFragmentById(R.id.city_fragment);
        if (fragment != null) {
            fragment.setSortOrder(sortOrder);
        }
    }

    /**
     * Broadcast receiver to handle result of the state insert bulk operation and display a SnackBar.
     */
    public class StatesCreatedResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int rows = intent.getIntExtra(BasicCRUDInsertTask.EXTRA_ROWS, -1);

            // Just marking it was complete. Because sample uses IGNORE conflict
            // resolution rows may be 0 if states were added during a previous run
            // of the application
            createdStates = rows >= 0;


            // Ensure the activity still exists. Also check the "bindings", just in case the activity
            // is in process of being destroyed and the bindings have been released but activity has not
            // been garbage collected.
            if (binding != null) {
                Snackbar.make(binding.coordinatorLayout, R.string.states_created, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
