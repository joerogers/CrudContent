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
import android.os.StrictMode;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.provider.ProviderTestRule;

import com.example.crudtester.utils.CursorUtilities;
import com.example.crudtester.utils.DataUtilities;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Base test case, testing each insert conflict type in isolation. However for the tests implemented
 * here, the results should be identical regardless of the conflict algorithm used.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseBasicCRUDProviderTest {

    final ProviderTestRule providerTestRule = new ProviderTestRule
            .Builder(TestBasicCRUDProvider.class, TestBasicCRUDProvider.AUTHORITY).build();

    @Before
    public void setUp() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build());
        cleanupDB();
    }


    private void cleanupDB() {
        // Use the provider's context so everything is using the same context
        DBHelper helper = DBHelper.getInstance(InstrumentationRegistry.getTargetContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(getTable(), null, null);
        helper.close();
    }

    @After
    public void tearDown() {
        cleanupDB();
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

    protected void test01GetType() throws Exception {
        // Verify the right type is returned using the standard URI
        String type = providerTestRule.getResolver().getType(getUri());

        assertThat(type, is(ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + TestBasicCRUDProvider.AUTHORITY + "/" + getTable()));

        // Verify the right type is returned using a URI with an _ID attached.
        type = providerTestRule.getResolver().getType(ContentUris.withAppendedId(getUri(), 1));
        assertThat(type, is(ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + TestBasicCRUDProvider.AUTHORITY + "/" + getTable()));

        // Verify bad uri id scheme by appending two ids
        type = providerTestRule.getResolver().getType(ContentUris.withAppendedId(ContentUris.withAppendedId(getUri(), 1), 2));
        assertThat(type, is(nullValue()));

        // Verify bad uri missing table
        Uri noTableUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(TestBasicCRUDProvider.AUTHORITY).build();
        type = providerTestRule.getResolver().getType(noTableUri);
        assertThat(type, is(nullValue()));

        // Verify a bad uri with extra path
        type = providerTestRule.getResolver().getType(getUri().buildUpon().appendPath("error").build());
        assertThat(type, is(nullValue()));
    }

    /**
     * Validates successful operations, insert, update, delete
     */
    protected void test02BasicCrudOperations() {

        // Test basic insertion
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);
        assertThat(uri, is(notNullValue()));

        long id = ContentUris.parseId(uri);

        //noinspection ConstantConditions
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        }

        ContentValues updateData = DataUtilities.updateUser1();
        int rows = providerTestRule.getResolver().update(uri, updateData, null, null);
        assertThat(rows, is(1));

        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Update", cursor, updateData, id);
        }

        rows = providerTestRule.getResolver().delete(uri, null, null);
        assertThat(rows, is(1));

        // No rows should remain...
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(0));
        }
    }

    /**
     * Validates successful bulk insert, update and delete
     */
    protected void test03BasicBulkCrudOperations() {
        // Test bulk insertion
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), bulkInsertData);
        assertThat(rows, is(2));

        // Verify data matches inserted data.
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, BaseColumns._ID)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("BulkInsert", getUniqueColumn(), cursor, bulkInsertData);
        }

        // Update data for all rows
        ContentValues updateData = DataUtilities.updateDataNoConflict();
        rows = providerTestRule.getResolver().update(getUri(), updateData, null, null);
        assertThat(rows, is(2));

        // Validate database now updated to new contents
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, BaseColumns._ID)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Update", getUniqueColumn(), cursor, DataUtilities.mergeValues(bulkInsertData, updateData));
        }

        // Should delete all rows
        rows = providerTestRule.getResolver().delete(getUri(), null, null);
        assertThat(rows, is(2));

        // Should be no more data
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, BaseColumns._ID)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(0));
        }
    }

    protected void test04QueryParameters() {
        // Test bulk insertion
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), bulkInsertData);
        assertThat(rows, is(2));

        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("BulkInsert", getUniqueColumn(), cursor, bulkInsertData);
        }

        // Test basic insertion which should add an additional row. Data2 shouldn't be
        ContentValues insertData = DataUtilities.updateUser1();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);
        assertThat(uri, is(notNullValue()));

        long id = ContentUris.parseId(uri);

        //noinspection ConstantConditions
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        }

        // Also should be 3 rows total
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(3));
        }

        // Test limit parameter
        Uri limitUri = getUri().buildUpon().appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, "1").build();
        try (Cursor cursor = providerTestRule.getResolver().query(limitUri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(1));
        }

        // Test distinct, first without the distinct parameter
        String[] projection = new String[]{getDistinctColumn()};
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), projection, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(3));
        }

        // Now with distinct parameter
        Uri distinctUri = getUri().buildUpon().appendQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, "true").build();
        try (Cursor cursor = providerTestRule.getResolver().query(distinctUri, projection, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(2));
        }

        // Test both limit/distinct...
        Uri bothUri = distinctUri.buildUpon().appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, "1").build();
        try (Cursor cursor = providerTestRule.getResolver().query(bothUri, projection, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(1));
        }
    }

    @SuppressWarnings("unused")
    public abstract void test05InsertConflicts();

    @SuppressWarnings("unused")
    public abstract void test06BulkInsertConflicts();

    @SuppressWarnings("unused")
    public abstract void test07BulkInsertPartialConflicts();

    @SuppressWarnings("unused")
    public abstract void test08UpdateConflicts();
}