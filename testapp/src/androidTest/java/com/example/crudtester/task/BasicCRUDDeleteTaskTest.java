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
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.LocalBroadcastManager;

import com.forkingcode.crudcontent.task.BasicCRUDDeleteTask;

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
 * Test BasicCRUDeleteTask, uses a mock content provider as actual interaction
 * with database is not required for these tests.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDDeleteTaskTest {


    private static final IntentFilter INTENT_FILTER = new IntentFilter(BasicCRUDDeleteTask.DELETE_COMPLETE_ACTION);
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
    public void test01DeleteAllRows() throws Exception {

        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .forUri(URI)
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertNotNull("Intent null", receiver.getIntent());

        int rows = receiver.getIntent().getIntExtra(BasicCRUDDeleteTask.EXTRA_ROWS, 0);
        assertEquals("Incorrect rows", TaskMockContentProvider.DELETE_ALL_RESULT, rows);
    }

    @Test
    public void test02DeleteById() throws Exception {

        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .forUri(URI)
                .whereMatchesId(4)
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertNotNull("Intent null", receiver.getIntent());

        int rows = receiver.getIntent().getIntExtra(BasicCRUDDeleteTask.EXTRA_ROWS, 0);
        assertEquals("Incorrect rows", 1, rows);
    }

    @Test
    public void test03DeleteBySelection() throws Exception {

        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .forUri(URI)
                .whereMatchesSelection("column1 = ?", "test")
                .requestResultBroadcast()
                .start();

        task.get();

        Thread.sleep(5);
        assertNotNull("Intent null", receiver.getIntent());

        int rows = receiver.getIntent().getIntExtra(BasicCRUDDeleteTask.EXTRA_ROWS, 0);
        assertEquals("Incorrect rows", TaskMockContentProvider.DELETE_SELECTION_RESULT, rows);
    }

    @Test
    public void test04DeleteCancelled() throws Exception {
        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .forUri(URI)
                .start();

        boolean cancelled = false;
        try {
            task.cancel(true);
            task.get();
        }
        catch (CancellationException e) {
            cancelled = true;
        }

        assertNull("Intent not null", receiver.getIntent());
        assertTrue("Task not cancelled", cancelled);
    }

    @Test
    public void test05DeleteWithBroadcastCancelled() throws Exception {
        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .forUri(URI)
                .requestResultBroadcast()
                .start();

        boolean cancelled = false;
        try {
            task.cancel(true);
            task.get();
        }
        catch (CancellationException e) {
            cancelled = true;
        }

        assertNull("Intent not null", receiver.getIntent());
        assertTrue("Task not cancelled", cancelled);
    }

    @Test
    public void test06DeleteNoBroadcast() throws Exception {
        assertNull("Intent not null", receiver.getIntent());

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .forUri(URI)
                .start();

        task.get();

        Thread.sleep(5);
        assertNull("Intent not null", receiver.getIntent());
    }

    @Test(expected = IllegalStateException.class)
    public void test07NoUriForDelete() throws Exception {

        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .start();

        assertNull("Task not null", task);
    }

    @Test(expected = NullPointerException.class)
    public void test08NoContextForDelete() throws Exception {

        // Suppressing warning for passing null validated by Android inspections.
        @SuppressWarnings("ConstantConditions")
        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(null)
                .forUri(URI)
                .start();

        assertNull("Task not null", task);
    }

    @Test(expected = IllegalStateException.class)
    public void test09BothIdAndSelectionInUpdate() throws Exception {
        BasicCRUDDeleteTask task = new BasicCRUDDeleteTask.Builder(context)
                .whereMatchesId(3)
                .whereMatchesSelection("column1 = ?", "test")
                .start();

        assertNull("Task not null", task);
    }
}
