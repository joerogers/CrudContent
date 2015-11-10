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

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.crudcontent.BuildConfig;
import com.example.crudcontent.R;
import com.example.crudcontent.databinding.CityActivityBindings;
import com.example.crudcontent.fragment.CityListFragment;
import com.example.crudcontent.provider.StateContract;
import com.forkingcode.crudcontent.service.BasicCRUDIntentService;
import com.forkingcode.crudcontent.service.BasicCrudResultReceiver;

import java.lang.ref.WeakReference;

public class CityActivity extends AppCompatActivity
        implements CityListFragment.CityListFragmentListener {

    private static boolean createdStates = false;
    private CityActivityBindings bindings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }

        bindings = DataBindingUtil.setContentView(this, R.layout.activity_city);
        setSupportActionBar(bindings.toolBar);

        bindings.setListeners(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle(R.string.title_activity_city_list);
        }

        if (!createdStates) {
            Intent intent = BasicCRUDIntentService.IntentBuilder
                    .buildForBulkInsert(this, StateContract.URI)
                    .usingValues(StateContract.buildStateValuesArray())
                    .setResultReceiver(new StatesCreatedResultReceiver(this))
                    .build();

            // Ensure state data exists
            startService(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bindings = null;
    }

    @SuppressWarnings("unused")   // Called via data binding
    public void onAddCity(View view) {
        startActivity(EditCityActivity.buildIntent(this));
    }

    @Override
    public void editCity(long cityId) {
        startActivity(EditCityActivity.buildIntent(this, cityId));
    }


    /**
     * Result receiver to handle result of the bulk operation and display a SnackBar. Using
     * a weak reference as the the database operations are asynchronous and do not want to hold
     * a strong reference to the activity in case it is ended before the service returns
     */
    public static class StatesCreatedResultReceiver extends BasicCrudResultReceiver {

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

            // Also check the "bindings", just in case the activity is in process of being destroyed
            // and the bindings have been released.
            if (activity != null && activity.bindings != null) {
                Snackbar.make(activity.bindings.coordinatorLayout, R.string.states_created, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
