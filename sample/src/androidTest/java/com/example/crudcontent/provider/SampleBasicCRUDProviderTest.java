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

package com.example.crudcontent.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.example.crudcontent.utils.CursorUtilities;
import com.example.crudcontent.utils.DataUtilities;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
@RunWith(AndroidJUnit4.class)
public class SampleBasicCRUDProviderTest extends ProviderTestCase2<SampleBasicCRUDProvider> {

    /**
     * Constructor.
     */
    public SampleBasicCRUDProviderTest() {
        super(SampleBasicCRUDProvider.class, UserContract.AUTHORITY);
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
        db.delete(UserContract.TABLE, null, null);
        db.close();
    }

    @After
    public void compatibleTearDown() throws Exception {
        tearDown();
    }

    @Test
    public void testGetType() throws Exception {
        // Verify the right type is returned using the standard URI
        String type = getMockContentResolver().getType(UserContract.URI);
        assertEquals(ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + UserContract.AUTHORITY + "/" + UserContract.TABLE, type);

        // Verify the right type is returned using a URI with an _ID attached.
        type = getMockContentResolver().getType(ContentUris.withAppendedId(UserContract.URI, 1));
        assertEquals(ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + UserContract.AUTHORITY + "/" + UserContract.TABLE, type);

        // Verify bad uri bad id
        type = getMockContentResolver().getType(ContentUris.withAppendedId(ContentUris.withAppendedId(UserContract.URI, 1), 2));
        assertNull(type);

        // Verify bad uri missing table
        Uri noTableUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(UserContract.AUTHORITY).build();
        type = getMockContentResolver().getType(noTableUri);
        assertNull(type);

        // Verify a bad uri with extra path
        type = getMockContentResolver().getType(UserContract.URI.buildUpon().appendPath("error").build());
        assertNull(type);
    }

    @Test
    public void testBasicCrudOperations() {

        // Test basic insertion
        ContentValues insertData = DataUtilities.createUser1();
        Uri uri = getMockContentResolver().insert(UserContract.URI, insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        Cursor cursor = getMockContentResolver().query(uri, null, null, null, null);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        ContentValues updateData = DataUtilities.editUser1();
        int rows = getMockContentResolver().update(uri, updateData, null, null);
        assertEquals(1, rows);

        cursor = getMockContentResolver().query(uri, null, null, null, null);
        CursorUtilities.validateCursor("Update", cursor, updateData, id);
        cursor.close();

        rows = getMockContentResolver().delete(uri, null, null);
        assertEquals(1, rows);

        // No rows should remain...
        cursor = getMockContentResolver().query(UserContract.URI, null, null, null, null);
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testBasicBulkCrudOperations() {
        // Test bulk insertion
        ContentValues[] bulkInsertData = DataUtilities.createBulkUsers();
        int rows = getMockContentResolver().bulkInsert(UserContract.URI, bulkInsertData);
        assertEquals(2, rows);

        Cursor cursor = getMockContentResolver().query(UserContract.URI, null, null, null, null);
        CursorUtilities.validateCursor("BulkInsert", UserContract.Columns.EMAIL, cursor, bulkInsertData);
        cursor.close();

        // Test basic insertion which should replace one row.
        ContentValues insertData = DataUtilities.createUser1();
        Uri uri = getMockContentResolver().insert(UserContract.URI, insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        cursor = getMockContentResolver().query(uri, null, null, null, null);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        ContentValues updateData = DataUtilities.editUser1();
        rows = getMockContentResolver().update(uri, updateData, null, null);
        assertEquals(1, rows);

        cursor = getMockContentResolver().query(uri, null, null, null, null);
        CursorUtilities.validateCursor("Update", cursor, updateData, id);
        cursor.close();

        rows = getMockContentResolver().delete(UserContract.URI, null, null);
        assertEquals(2, rows);

        cursor = getMockContentResolver().query(UserContract.URI, null, null, null, null);
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testQueryParameters() {
        // Test bulk insertion
        ContentValues[] bulkInsertData = DataUtilities.createBulkUsers();
        int rows = getMockContentResolver().bulkInsert(UserContract.URI, bulkInsertData);
        assertEquals(2, rows);

        Cursor cursor = getMockContentResolver().query(UserContract.URI, null, null, null, null);
        CursorUtilities.validateCursor("BulkInsert", UserContract.Columns.EMAIL, cursor, bulkInsertData);
        cursor.close();

        // Test basic insertion which should add an additional row.
        ContentValues insertData = DataUtilities.editUser1();
        Uri uri = getMockContentResolver().insert(UserContract.URI, insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        cursor = getMockContentResolver().query(uri, null, null, null, null);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        // Also should be 3 rows total
        cursor = getMockContentResolver().query(UserContract.URI, null, null, null, null);
        assertEquals(3, cursor.getCount());
        cursor.close();

        // Test limit parameter
        Uri limitUri = UserContract.URI.buildUpon().appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, "1").build();
        cursor = getMockContentResolver().query(limitUri, null, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // Test distinct, first without...
        String[] projection = new String[]{UserContract.Columns.NAME};
        cursor = getMockContentResolver().query(UserContract.URI, projection, null, null, null);
        assertEquals(3, cursor.getCount());

        // Now with...
        Uri distinctUri = UserContract.URI.buildUpon().appendQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, "true").build();
        cursor = getMockContentResolver().query(distinctUri, projection, null, null, null);
        assertEquals(2, cursor.getCount());

        // Test both limit/distinct...
        Uri bothUri = distinctUri.buildUpon().appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, "1").build();
        cursor = getMockContentResolver().query(bothUri, projection, null, null, null);
        assertEquals(1, cursor.getCount());
    }
}