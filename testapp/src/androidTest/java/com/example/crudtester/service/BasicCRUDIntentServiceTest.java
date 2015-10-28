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

package example.com.crudtester.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ServiceTestCase;

import com.forkingcode.crudcontent.service.BasicCRUDIntentService;
import com.forkingcode.crudcontent.service.BasicCrudResultReceiver;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Basic testcase to test the BasicCRUDIntentService features without a real provider. Essentially
 * verifies the service runs and the callback occurs.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDIntentServiceTest extends ServiceTestCase<BasicCRUDIntentService> {

    private static final String AUTHORITY = "test";
    private static final String TABLE = "my_table";

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
        success = false;
        // Initialize system context
        setContext(InstrumentationRegistry.getTargetContext());
        setUp();
        // Setup the mock context
        setContext(new ServiceMockContext(AUTHORITY, InstrumentationRegistry.getTargetContext()));
    }

    @Test
    public void test01Insert() throws Exception {
        // Should return a valid uri
        Intent serviceIntent = new BasicCRUDIntentService.IntentBuilder(getContext())
                .forInsert(uri)
                .usingValues(new ContentValues())
                .setReceiver(new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onInsertComplete(Uri uri) {
                        assertNotNull("Row failed to insert", uri);
                        long id = ContentUris.parseId(uri);
                        assertTrue("Invalid id", id > 0);
                        success = true;
                    }
                })
                .build();

        startService(serviceIntent);
        // delay to give intent service chance to run
        Thread.sleep(100);
        assertTrue(success);
    }

    @Test
    public void test02BulkInsert() throws Exception {

        ContentValues[] valuesArray = new ContentValues[2];
        valuesArray[0] = new ContentValues();
        valuesArray[1] = new ContentValues();

        // Returns number of content values.
        Intent serviceIntent = new BasicCRUDIntentService.IntentBuilder(getContext())
                .forBulkInsert(uri)
                .usingValues(valuesArray)
                .setReceiver(new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onBulkInsertComplete(int rows) {
                        assertEquals(2, rows);
                        success = true;
                    }
                })
                .build();

        startService(serviceIntent);
        // delay to give intent service chance to run
        Thread.sleep(100);
        assertTrue(success);
    }

    @Test
    public void test03UpdateById() throws Exception {

        // Returns id as number of rows

        Intent serviceIntent = new BasicCRUDIntentService.IntentBuilder(getContext())
                .forUpdate(uri)
                .whereMatchesId(23)
                .usingValues(new ContentValues())
                .setReceiver(new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onUpdateComplete(int rows) {
                        assertEquals(23, rows);
                        success = true;
                    }
                })
                .build();

        startService(serviceIntent);
        Thread.sleep(100);
        assertTrue(success);
    }

    @Test
    public void test04UpdateBySelection() throws Exception {

        // Returns number of args...

        Intent serviceIntent = new BasicCRUDIntentService.IntentBuilder(getContext())
                .forUpdate(uri)
                .whereSelection("selection", new String[]{"arg1", "arg2"})
                .usingValues(new ContentValues())
                .setReceiver(new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onUpdateComplete(int rows) {
                        assertEquals(2, rows);
                        success = true;
                    }
                })
                .build();

        startService(serviceIntent);
        Thread.sleep(100);
        assertTrue(success);
    }

    @Test
    public void test05DeleteById() throws Exception {

        // Will return the id as the number of rows.

        Intent serviceIntent = new BasicCRUDIntentService.IntentBuilder(InstrumentationRegistry.getTargetContext())
                .forDelete(uri)
                .whereMatchesId(5)
                .setReceiver(new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onDeleteComplete(int rows) {
                        assertEquals(5, rows);
                        success = true;
                    }
                })
                .build();

        startService(serviceIntent);
        Thread.sleep(100);
        assertTrue(success);
    }

    @Test
    public void test06DeleteBySelection() throws Exception {

        // Returns number of args...

        Intent serviceIntent = new BasicCRUDIntentService.IntentBuilder(InstrumentationRegistry.getTargetContext())
                .forDelete(uri)
                .whereSelection("selection", new String[]{"arg1", "arg2"})
                .setReceiver(new BasicCrudResultReceiver(null) {
                    @Override
                    protected void onDeleteComplete(int rows) {
                        assertEquals(2, rows);
                        success = true;
                    }
                })
                .build();

        startService(serviceIntent);
        Thread.sleep(100);
        assertTrue(success);
    }

    @Test
    public void test07MissingAction() throws TimeoutException {

        try {
            new BasicCRUDIntentService.IntentBuilder(InstrumentationRegistry.getTargetContext())
                    .whereMatchesId(5)
                    .build();
        }
        catch (IllegalStateException e) {
            success = true;
        }
        assertTrue(success);
    }

    @Test
    public void test08NullValues() throws TimeoutException {

        try {
            new BasicCRUDIntentService.IntentBuilder(InstrumentationRegistry.getTargetContext())
                    .forInsert(uri)
                    .build();
        }
        catch (IllegalStateException e) {
            success = true;
        }
        assertTrue(success);
    }

    @Test
    public void test09EmptyValuesArray() throws TimeoutException {

        try {
            new BasicCRUDIntentService.IntentBuilder(InstrumentationRegistry.getTargetContext())
                    .forInsert(uri)
                    .usingValues(new ContentValues[0])
                    .build();
        }
        catch (IllegalStateException e) {
            success = true;
        }
        assertTrue(success);
    }

    @Test
    public void test10EmptyValuesArrayList() throws TimeoutException {

        try {
            new BasicCRUDIntentService.IntentBuilder(InstrumentationRegistry.getTargetContext())
                    .forInsert(uri)
                    .usingValues(new ArrayList<ContentValues>(0))
                    .build();
        }
        catch (IllegalStateException e) {
            success = true;
        }
        assertTrue(success);
    }
}
