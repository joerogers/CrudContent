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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.crudcontent.BuildConfig;
import com.example.crudcontent.R;
import com.example.crudcontent.databinding.EditCityActivityBinding;
import com.example.crudcontent.fragment.EditCityFragment;
import com.example.crudcontent.provider.CityContract;
import com.forkingcode.crudcontent.task.BasicCRUDDeleteTask;
import com.forkingcode.crudcontent.task.BasicCRUDInsertTask;
import com.forkingcode.crudcontent.task.BasicCRUDUpdateTask;

public class EditCityActivity extends AppCompatActivity
        implements EditCityFragment.EditCityFragmentListener {

    private static final String EXTRA_CITY_ID = BuildConfig.APPLICATION_ID + ".activity.EditCityActivity.extra.cityId";
    private static final String EXTRA_MENU_RES = BuildConfig.APPLICATION_ID + ".activity.EditCityActivity.extra.menu";
    private static final String EXTRA_TITLE_RES = BuildConfig.APPLICATION_ID + ".activity.EditCityActivity.extra.title";

    private long cityId;

    public static Intent buildIntent(Context context) {
        Intent intent = new Intent(context, EditCityActivity.class);
        intent.putExtra(EXTRA_TITLE_RES, R.string.title_activity_add_city);
        return intent;
    }

    public static Intent buildIntent(Context context, long cityId) {
        Intent intent = new Intent(context, EditCityActivity.class);
        intent.putExtra(EXTRA_CITY_ID, cityId);
        intent.putExtra(EXTRA_TITLE_RES, R.string.title_activity_edit_city);
        intent.putExtra(EXTRA_MENU_RES, R.menu.menu_edit_city);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EditCityActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_city);
        setSupportActionBar(binding.toolBar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getIntent().getIntExtra(EXTRA_TITLE_RES, R.string.add_city));
        }
        cityId = getIntent().getLongExtra(EXTRA_CITY_ID, CityContract.NO_CITY_ID);

        if (savedInstanceState == null) {
            EditCityFragment fragment = EditCityFragment.newInstance(cityId);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment, "frag")
                    .commitNow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuRes = getIntent().getIntExtra(EXTRA_MENU_RES, 0);
        if (menuRes == 0) {
            return super.onCreateOptionsMenu(menu);
        }
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(menuRes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_delete:
                new BasicCRUDDeleteTask.Builder(this)
                        .forUri(CityContract.URI)
                        .whereMatchesId(cityId)
                        .start();

                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void submitCity(ContentValues cityValues) {

        if (cityId == CityContract.NO_CITY_ID) {
            new BasicCRUDInsertTask.Builder(this)
                    .forUri(CityContract.URI)
                    .usingValues(cityValues)
                    .start();
        }
        else {
            new BasicCRUDUpdateTask.Builder(this)
                    .forUri(CityContract.URI)
                    .whereMatchesId(cityId)
                    .usingValues(cityValues)
                    .start();
        }

        // finish activity...
        finish();
    }
}
