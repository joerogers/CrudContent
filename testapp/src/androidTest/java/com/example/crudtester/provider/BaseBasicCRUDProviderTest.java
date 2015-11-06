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

package com.example.crudtester.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.test.ProviderTestCase2;

import com.example.crudtester.utils.CursorUtilities;
import com.example.crudtester.utils.DataUtilities;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Base test case, testing each insert conflict type in isolation. However for the tests implemented
 * here, the results should be identical regardless of the conflict algorithm used.
 */
@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseBasicCRUDProviderTest extends ProviderTestCase2<TestBasicCRUDProvider> {

    /**
     * Constructor.
     */
    protected BaseBasicCRUDProviderTest() {
        super(TestBasicCRUDProvider.class, TestBasicCRUDProvider.AUTHORITY);
    }

    @Before
    public void compatibleSetUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        setUp();
        cleanupDB();
    }

    private void cleanupDB() {
        // Use the provider's context so everything is using the same context
        DBHelper helper = DBHelper.getInstance(getProvider().getContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(getTable(), null, null);
        db.close();
    }

    @After
    public void compatibleTearDown() throws Exception {
        tearDown();
    }

    @NonNull
    protected abstract String getTable();

    @NonNull
    protected abstract Uri getUri();

    @NonNull
    public abstract String getUniqueColumn();

    /**
     * The column that has identical data across multiple rows.
     *
     * @return the appropriate column name
     */
    @NonNull
    protected abstract String getDistinctColumn();

    public void test01GetType() throws Exception {
        // Verify the right type is returned using the standard URI
        String type = getMockContentResolver().getType(getUri());
        assertEquals(ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + TestBasicCRUDProvider.AUTHORITY + "/" + getTable(), type);

        // Verify the right type is returned using a URI with an _ID attached.
        type = getMockContentResolver().getType(ContentUris.withAppendedId(getUri(), 1));
        assertEquals(ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + TestBasicCRUDProvider.AUTHORITY + "/" + getTable(), type);

        // Verify bad uri id scheme by appending two ids
        type = getMockContentResolver().getType(ContentUris.withAppendedId(ContentUris.withAppendedId(getUri(), 1), 2));
        assertNull(type);

        // Verify bad uri missing table
        Uri noTableUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(TestBasicCRUDProvider.AUTHORITY).build();
        type = getMockContentResolver().getType(noTableUri);
        assertNull(type);

        // Verify a bad uri with extra path
        type = getMockContentResolver().getType(getUri().buildUpon().appendPath("error").build());
        assertNull(type);
    }

    /**
     * Validates successful operations, insert, update, delete
     */
    public void test02BasicCrudOperations() {

        // Test basic insertion
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = getMockContentResolver().insert(getUri(), insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        Cursor cursor = getMockContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        ContentValues updateData = DataUtilities.updateUser1();
        int rows = getMockContentResolver().update(uri, updateData, null, null);
        assertEquals(1, rows);

        cursor = getMockContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Update", cursor, updateData, id);
        cursor.close();

        rows = getMockContentResolver().delete(uri, null, null);
        assertEquals(1, rows);

        // No rows should remain...
        cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    /**
     * Validates successful bulk insert, update and delete
     */
    public void test03BasicBulkCrudOperations() {
        // Test bulk insertion
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = getMockContentResolver().bulkInsert(getUri(), bulkInsertData);
        assertEquals(2, rows);

        // Verify data matches inserted data.
        Cursor cursor = getMockContentResolver().query(getUri(), null, null, null, BaseColumns._ID);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("BulkInsert", getUniqueColumn(), cursor, bulkInsertData);
        cursor.close();

        // Update data for all rows
        ContentValues updateData = DataUtilities.updateDataNoConflict();
        rows = getMockContentResolver().update(getUri(), updateData, null, null);
        assertEquals(2, rows);

        // Validate database now updated to new contents
        cursor = getMockContentResolver().query(getUri(), null, null, null, BaseColumns._ID);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Update", getUniqueColumn(), cursor, DataUtilities.mergeValues(bulkInsertData, updateData));
        cursor.close();

        // Should delete all rows
        rows = getMockContentResolver().delete(getUri(), null, null);
        assertEquals(2, rows);

        // Should be no more data
        cursor = getMockContentResolver().query(getUri(), null, null, null, BaseColumns._ID);
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void test04QueryParameters() {
        // Test bulk insertion
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = getMockContentResolver().bulkInsert(getUri(), bulkInsertData);
        assertEquals(2, rows);

        Cursor cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("BulkInsert", getUniqueColumn(), cursor, bulkInsertData);
        cursor.close();

        // Test basic insertion which should add an additional row. Data2 shouldn't be
        ContentValues insertData = DataUtilities.updateUser1();
        Uri uri = getMockContentResolver().insert(getUri(), insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        cursor = getMockContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        // Also should be 3 rows total
        cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        assertEquals(3, cursor.getCount());
        cursor.close();

        // Test limit parameter
        Uri limitUri = getUri().buildUpon().appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, "1").build();
        cursor = getMockContentResolver().query(limitUri, null, null, null, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // Test distinct, first without the distinct parameter
        String[] projection = new String[]{getDistinctColumn()};
        cursor = getMockContentResolver().query(getUri(), projection, null, null, null);
        assertNotNull(cursor);
        assertEquals(3, cursor.getCount());

        // Now with distinct parameter
        Uri distinctUri = getUri().buildUpon().appendQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, "true").build();
        cursor = getMockContentResolver().query(distinctUri, projection, null, null, null);
        assertNotNull(cursor);
        assertEquals(2, cursor.getCount());

        // Test both limit/distinct...
        Uri bothUri = distinctUri.buildUpon().appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, "1").build();
        cursor = getMockContentResolver().query(bothUri, projection, null, null, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
    }
}