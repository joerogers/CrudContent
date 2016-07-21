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
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.LocalBroadcastManager;

import com.forkingcode.crudcontent.task.BasicCRUDUpdateTask;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test BasicCRUUpdateTask, uses a mock content provider as actual interaction
 * with database is not required for these tests.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDUpdateTaskTest {


    private static final IntentFilter INTENT_FILTER = new IntentFilter(BasicCRUDUpdateTask.UPDATE_COMPLETE_ACTION);
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
    public void test01UpdateAllRows() throws Exception {

        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertNotNull("Intent null", receiver.getIntent());

        int rows = receiver.getIntent().getIntExtra(BasicCRUDUpdateTask.EXTRA_ROWS, 0);
        assertEquals("Incorrect rows", TaskMockContentProvider.UPDATE_ALL_RESULT, rows);
    }

    @Test
    public void test02UpdateById() throws Exception {

        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .whereMatchesId(2)
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertNotNull("Intent null", receiver.getIntent());

        int rows = receiver.getIntent().getIntExtra(BasicCRUDUpdateTask.EXTRA_ROWS, 0);
        assertEquals("Incorrect rows", 1, rows);
    }

    @Test
    public void test03UpdateBySelection() throws Exception {
        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .whereMatchesSelection("columnx = ?", "5")
                .requestResultBroadcast()
                .start();


        task.get();

        Thread.sleep(5);
        assertNotNull("Intent null", receiver.getIntent());

        int rows = receiver.getIntent().getIntExtra(BasicCRUDUpdateTask.EXTRA_ROWS, 0);
        assertEquals("Incorrect rows", TaskMockContentProvider.UPDATE_SELECTION_RESULT, rows);
    }

    public void test04UpdateNoBroadcastCancellation() throws Exception {
        boolean cancelled = false;

        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .start();

        try {
            assertTrue(task.cancel(false));
            task.get();
        }
        catch (CancellationException e) {
            cancelled = true;
        }

        assertNull("Intent not null", receiver.getIntent());
        assertTrue("Task not cancelled", cancelled);
    }

    @Test
    public void test05UpdateWithBroadcastCancelled() throws Exception {
        assertNull("Intent not null", receiver.getIntent());

        boolean cancelled = false;
        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .requestResultBroadcast()
                .start();

        try {
            assertTrue(task.cancel(false));
            task.get();
        }
        catch (CancellationException e) {
            cancelled = true;
        }

        assertNull("Intent not null", receiver.getIntent());
        assertTrue("Task not cancelled", cancelled);
    }

    @Test
    public void test06UpdateNoBroadcast() throws Exception {
        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .usingValues(new ContentValues())
                .start();

        task.get();

        Thread.sleep(5);
        assertNull("Intent not null", receiver.getIntent());
    }

    @Test(expected = IllegalStateException.class)
    public void test07NoContentValuesForUpdate() throws Exception {

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .forUri(URI)
                .start();

        assertNull("Task not null", task);
    }

    @Test(expected = IllegalStateException.class)
    public void test08NoUriForUpdate() throws Exception {

        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .usingValues(new ContentValues())
                .start();

        assertNull("Task not null", task);
    }

    @Test(expected = NullPointerException.class)
    public void test09NoContextForUpdate() throws Exception {

        // Suppressing warning for passing null validated by Android inspections.
        @SuppressWarnings("ConstantConditions")
        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(null)
                .forUri(URI)
                .usingValues(new ContentValues())
                .start();

        assertNull("Task not null", task);
    }

    @Test(expected = IllegalStateException.class)
    public void test10BothIdAndSelectionInUpdate() throws Exception {
        BasicCRUDUpdateTask task = new BasicCRUDUpdateTask.Builder(context)
                .usingValues(new ContentValues())
                .whereMatchesId(3)
                .whereMatchesSelection("column1 = ?", "test")
                .start();

        assertNull("Task not null", task);
    }
}
