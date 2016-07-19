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

package com.example.crudtester.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ServiceTestCase;

import com.example.crudtester.task.TaskMockContentProvider;
import com.example.crudtester.task.TaskMockContext;
import com.forkingcode.crudcontent.service.BasicCRUDIntentService;
import com.forkingcode.crudcontent.service.BasicCrudResultReceiver;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Basic testcase to test the BasicCRUDIntentService features without a real provider. Essentially
 * verifies the service runs and the callback occurs.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDIntentServiceTest extends ServiceTestCase<BasicCRUDIntentService> {

    // Copy of constants from the BasicCRUDIntentService to avoid exposing publicly
    private static final String ACTION_INSERT = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.insert";
    private static final String ACTION_BULK_INSERT = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.bulkInsert";
    private static final String ACTION_UPDATE = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.update";
    private static final String ACTION_DELETE = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.delete";

    private static final String EXTRA_VALUES = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.values";
    private static final String EXTRA_SELECTION = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.selection";
    private static final String EXTRA_SELECTION_ARGS = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.selectionArgs";
    private static final String EXTRA_RESULT_RECEIVER = com.forkingcode.crudcontent.BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.resultReceiver";


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String AUTHORITY = "test";
    private static final String TABLE = "my_table";
    private static final int REQUEST_ID = 4;

    private final Uri uri;

    private boolean success = false;

    public BasicCRUDIntentServiceTest() {
        super(BasicCRUDIntentService.class);
        uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(TABLE)
                .build();
    }

    @Before
    public void compatibleSetUp() throws Exception {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build());
        success = false;
        // Initialize system context
        setContext(InstrumentationRegistry.getTargetContext());
        setUp();
        // Setup the mock context
        setContext(new TaskMockContext(AUTHORITY, InstrumentationRegistry.getTargetContext()));
    }

    @After
    public void compatibleTearDown() throws Exception {
        shutdownService();
    }

    @Test
    public void test01Insert() throws Exception {

        // Should return a valid uri in form of: uri/{id}. Mock provider always set id to 1.
        Intent serviceIntent = BasicCRUDIntentService
                .performInsert(getContext(), uri)
                .usingValues(new ContentValues())

                        // Receiver is called by intent service to indicate status of the insert
                .resultReceiver(REQUEST_ID, new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onInsertComplete(int requestId, Uri insertResultUri) {
                        assertNotNull("Row failed to insert", insertResultUri);
                        assertEquals(uri.getAuthority(), insertResultUri.getAuthority());
                        long id = ContentUris.parseId(insertResultUri);
                        assertEquals("Invalid id", TaskMockContentProvider.INSERT_ID_RESULT, id);
                        assertEquals("Request id", REQUEST_ID, requestId);
                        // indicate receiver was called and successful
                        success = true;
                    }
                })
                .buildIntent();

        // Validate intent
        assertNotNull(serviceIntent);
        assertEquals(ACTION_INSERT, serviceIntent.getAction());
        assertEquals(uri, serviceIntent.getData());
        assertTrue(serviceIntent.hasExtra(EXTRA_VALUES));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION_ARGS));
        assertTrue(serviceIntent.hasExtra(EXTRA_RESULT_RECEIVER));

        // Start intent service and delay to allow to run
        startService(serviceIntent);
        Thread.sleep(100);

        // Ensure the result receiver was called
        assertTrue(success);
    }

    @Test
    public void test02BulkInsert() throws Exception {

        ContentValues[] valuesArray = new ContentValues[2];
        valuesArray[0] = new ContentValues();
        valuesArray[1] = new ContentValues();

        // Returns number of content values.
        Intent serviceIntent = BasicCRUDIntentService
                .performBulkInsert(getContext(), uri)
                .usingValues(valuesArray)
                .resultReceiver(REQUEST_ID, new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onBulkInsertComplete(int requestId, int rows) {
                        assertEquals(TaskMockContentProvider.BULK_INSERT_RESULT, rows);
                        assertEquals("Request id", REQUEST_ID, requestId);
                        // indicate receiver was called and successful
                        success = true;
                    }
                })
                .buildIntent();

        // Validate intent
        assertNotNull(serviceIntent);
        assertEquals(ACTION_BULK_INSERT, serviceIntent.getAction());
        assertEquals(uri, serviceIntent.getData());
        assertTrue(serviceIntent.hasExtra(EXTRA_VALUES));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION_ARGS));
        assertTrue(serviceIntent.hasExtra(EXTRA_RESULT_RECEIVER));

        // Start intent service and delay to allow to run
        startService(serviceIntent);
        Thread.sleep(100);

        // Ensure the result receiver was called
        assertTrue(success);
    }

    @Test
    public void test03UpdateById() throws Exception {

        long id = 2;

        Intent serviceIntent = BasicCRUDIntentService
                .performUpdate(getContext(), uri)
                .whereMatchesId(id)
                .usingValues(new ContentValues())
                .resultReceiver(REQUEST_ID, new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onUpdateComplete(int requestId, int rows) {
                        assertEquals(TaskMockContentProvider.UPDATE_RESULT, rows);
                        assertEquals("Request id", REQUEST_ID, requestId);
                        // indicate receiver was called and successful
                        success = true;
                    }
                })
                .buildIntent();

        // Validate intent
        assertNotNull(serviceIntent);
        assertEquals(ACTION_UPDATE, serviceIntent.getAction());
        assertEquals(ContentUris.withAppendedId(uri, id), serviceIntent.getData());
        assertTrue(serviceIntent.hasExtra(EXTRA_VALUES));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION_ARGS));
        assertTrue(serviceIntent.hasExtra(EXTRA_RESULT_RECEIVER));

        // Start intent service and delay to allow to run
        startService(serviceIntent);
        Thread.sleep(100);

        // Ensure the result receiver was called
        assertTrue(success);
    }

    @Test
    public void test04UpdateBySelection() throws Exception {

        final String selection = "column1 = ? and column2 = ?";
        final String[] selectionArgs = new String[]{"arg1", "arg2"};

        Intent serviceIntent = BasicCRUDIntentService
                .performUpdate(getContext(), uri)
                // passing as a String[]
                .whereMatchesSelection(selection, selectionArgs)
                .usingValues(new ContentValues())
                .resultReceiver(REQUEST_ID, new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onUpdateComplete(int requestId, int rows) {
                        assertEquals(TaskMockContentProvider.UPDATE_RESULT, rows);
                        // If request id not provided, 0 i
                        assertEquals("Request id", REQUEST_ID, requestId);
                        // indicate receiver was called and successful
                        success = true;
                    }
                })
                .buildIntent();

        // Validate intent
        assertNotNull(serviceIntent);
        assertEquals(ACTION_UPDATE, serviceIntent.getAction());
        assertEquals(uri, serviceIntent.getData());
        assertTrue(serviceIntent.hasExtra(EXTRA_VALUES));
        assertEquals(selection, serviceIntent.getStringExtra(EXTRA_SELECTION));
        assertTrue(Arrays.equals(selectionArgs, serviceIntent.getStringArrayExtra(EXTRA_SELECTION_ARGS)));
        assertTrue(serviceIntent.hasExtra(EXTRA_RESULT_RECEIVER));

        // Start intent service and delay to allow to run
        startService(serviceIntent);
        Thread.sleep(100);

        // Ensure the result receiver was called
        assertTrue(success);
    }

    @Test
    public void test05DeleteById() throws Exception {

        final long id = 4;

        Intent serviceIntent = BasicCRUDIntentService
                .performDelete(getContext(), uri)
                .whereMatchesId(id)
                .resultReceiver(REQUEST_ID, new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onDeleteComplete(int requestId, int rows) {
                        assertEquals(TaskMockContentProvider.DELETE_RESULT, rows);
                        assertEquals("Request id", REQUEST_ID, requestId);
                        // indicate receiver was called and successful
                        success = true;
                    }
                })
                .buildIntent();

        // Validate intent
        assertNotNull(serviceIntent);
        assertEquals(ACTION_DELETE, serviceIntent.getAction());
        assertEquals(ContentUris.withAppendedId(uri, id), serviceIntent.getData());
        assertFalse(serviceIntent.hasExtra(EXTRA_VALUES));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION));
        assertFalse(serviceIntent.hasExtra(EXTRA_SELECTION_ARGS));
        assertTrue(serviceIntent.hasExtra(EXTRA_RESULT_RECEIVER));

        // Start intent service and delay to allow to run
        startService(serviceIntent);
        Thread.sleep(100);

        // Ensure the result receiver was called
        assertTrue(success);
    }

    @Test
    public void test06DeleteBySelection() throws Exception {

        final String selection = "column1 = ? and column2 = ?";
        final String[] selectionArgs = new String[]{"arg1", "arg2"};

        Intent serviceIntent = BasicCRUDIntentService
                .performDelete(getContext(), uri)
                // passing individually, using var args to build string array.
                .whereMatchesSelection(selection, selectionArgs[0], selectionArgs[1])
                .resultReceiver(REQUEST_ID, new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onDeleteComplete(int requestId, int rows) {
                        assertEquals(TaskMockContentProvider.DELETE_RESULT, rows);
                        assertEquals("Request id", REQUEST_ID, requestId);
                        // indicate receiver was called and successful
                        success = true;
                    }
                })
                .buildIntent();

        // Validate intent
        assertNotNull(serviceIntent);
        assertEquals(ACTION_DELETE, serviceIntent.getAction());
        assertEquals(uri, serviceIntent.getData());
        assertFalse(serviceIntent.hasExtra(EXTRA_VALUES));
        assertEquals(selection, serviceIntent.getStringExtra(EXTRA_SELECTION));
        assertTrue(Arrays.equals(selectionArgs, serviceIntent.getStringArrayExtra(EXTRA_SELECTION_ARGS)));
        assertTrue(serviceIntent.hasExtra(EXTRA_RESULT_RECEIVER));

        // Start intent service and delay to allow to run
        startService(serviceIntent);
        Thread.sleep(100);

        // Ensure the result receiver was called
        assertTrue(success);
    }

    @Test
    public void test07NoContentValuesForInsert() throws Exception {
        thrown.expect(IllegalStateException.class);

        BasicCRUDIntentService
                .performInsert(getContext(), uri)
                .start();
    }

    @Test
    public void test08BothIdAndSelectionProvided() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDIntentService
                .performUpdate(getContext(), uri)
                .whereMatchesId(1)
                .whereMatchesSelection("test", (String) null)
                .start();
    }

    @Test
    public void test09EmptyValuesArray() throws Exception {
        thrown.expect(IllegalStateException.class);

        BasicCRUDIntentService
                .performInsert(getContext(), uri)
                .usingValues(new ContentValues[0])
                .start();
    }

    @Test
    public void test10EmptyValuesArrayList() throws Exception {
        thrown.expect(IllegalStateException.class);

        BasicCRUDIntentService
                .performInsert(getContext(), uri)
                .usingValues(new ArrayList<ContentValues>(0))
                .start();
    }

    @Test
    public void test11UsingMultipleRowsWithoutBulkInsert() throws Exception {
        thrown.expect(IllegalStateException.class);

        BasicCRUDIntentService
                .performInsert(getContext(), uri)
                .usingValues(new ArrayList<ContentValues>(2))
                .start();
    }

    @Test
    public void test12UsingContentValuesWithDelete() throws Exception {
        thrown.expect(IllegalStateException.class);

        BasicCRUDIntentService
                .performDelete(getContext(), uri)
                .usingValues(new ContentValues())
                .start();
    }
}
