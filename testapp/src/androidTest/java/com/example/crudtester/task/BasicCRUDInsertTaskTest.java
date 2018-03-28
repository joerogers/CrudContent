/*
 * Copyright 2016 Joe Rogers
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

package com.example.crudtester.task;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.LocalBroadcastManager;

import com.forkingcode.crudcontent.task.BasicCRUDInsertTask;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CancellationException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test BasicCRUInsertTask, uses a mock content provider as actual interaction
 * with database is not required for these tests.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDInsertTaskTest {


    private static final IntentFilter INTENT_FILTER = new IntentFilter(BasicCRUDInsertTask.INSERT_COMPLETE_ACTION);
    private static final String AUTHORITY = "test";
    private static final String TABLE = "my_table";
    private static final Uri URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath(TABLE)
            .build();

    private Context context;
    private TaskBroadcastReceiver receiver;

    @Before
    public void setup() {
        context = new TaskMockContext(AUTHORITY, InstrumentationRegistry.getTargetContext());
        receiver = new TaskBroadcastReceiver();
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, INTENT_FILTER);
    }

    @After
    public void tearDown() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        receiver = null;
        context = null;
    }

    @Test
    public void test01Insert() throws Exception {

        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));

        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertThat("Intent null", receiver.getIntent(), is(notNullValue()));

        Uri uri = receiver.getIntent().getParcelableExtra(BasicCRUDInsertTask.EXTRA_URI);
        assertThat("Uri null", uri, is(notNullValue()));
        long id = ContentUris.parseId(uri);
        assertThat("Invalid id", id, is(TaskMockContentProvider.INSERT_ID_RESULT));

        int rows = receiver.getIntent().getIntExtra(BasicCRUDInsertTask.EXTRA_ROWS, 0);
        assertThat("Incorrect rows", rows, is(1));
    }

    @Test
    public void test02BulkInsert() throws Exception {

        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));

        ContentValues[] valuesArray = new ContentValues[2];
        valuesArray[0] = new ContentValues();
        valuesArray[1] = new ContentValues();

        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .forUri(URI)
                .usingValues(valuesArray)
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertThat("Intent null", receiver.getIntent(), is(notNullValue()));

        Uri uri = receiver.getIntent().getParcelableExtra(BasicCRUDInsertTask.EXTRA_URI);
        assertThat("Uri not null", uri, is(nullValue()));

        int rows = receiver.getIntent().getIntExtra(BasicCRUDInsertTask.EXTRA_ROWS, 0);
        assertThat("Incorrect rows", rows, is(valuesArray.length));
    }

    @Test
    public void test03InsertCancelled() throws Exception {
        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));

        boolean cancelled = false;
        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .start();

        try {
            assertThat(task.cancel(false), is(true));
            task.get();
        }
        catch (CancellationException e) {
            cancelled = true;
        }

        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));
        assertThat("Task not cancelled", cancelled, is(true));
    }

    @Test
    public void test04InsertWithBroadcastCancelled() throws Exception {
        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));

        boolean cancelled = false;
        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .requestResultBroadcast()
                .start();

        try {
            assertThat(task.cancel(false), is(true));
            task.get();
        }
        catch (CancellationException e) {
            cancelled = true;
        }

        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));
        assertThat("Task not cancelled", cancelled, is(true));
    }

    @Test
    public void test05InsertNoBroadcast() throws Exception {
        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));

        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .start();

        task.get();

        Thread.sleep(5);
        assertThat("Intent not null", receiver.getIntent(), is(nullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void test06NoContentValuesForInsert() {

        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .forUri(URI)
                .start();

        assertThat("Task not null", task, is(nullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void test07NoUriForInsert() {

        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(context)
                .usingValues(new ContentValues())
                .start();

        assertThat("Task not null", task, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void test08NoContextForInsert() {

        // Suppressing warning for passing null validated by Android inspections.
        @SuppressWarnings("ConstantConditions")
        BasicCRUDInsertTask task = new BasicCRUDInsertTask.Builder(null)
                .forUri(URI)
                .usingValues(new ContentValues())
                .start();

        assertThat("Task not null", task, is(nullValue()));
    }
}
