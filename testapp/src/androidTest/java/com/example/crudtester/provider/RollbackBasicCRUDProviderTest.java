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

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import com.example.crudtester.utils.CursorUtilities;
import com.example.crudtester.utils.DataUtilities;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Test the content provider using the ROLLBACK conflict algorithm
 */
@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RollbackBasicCRUDProviderTest extends BaseBasicCRUDProviderTest {

    /**
     * Constructor.
     */
    public RollbackBasicCRUDProviderTest() {
        super();
    }

    @NonNull
    @Override
    public String getTable() {
        return RollbackContract.TABLE;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return RollbackContract.URI;
    }

    @NonNull
    @Override
    public String getUniqueColumn() {
        return RollbackContract.Columns.DATA1;
    }

    @NonNull
    @Override
    public String getDistinctColumn() {
        return RollbackContract.Columns.DATA2;
    }

    // These tests are the same and shouldn't vary based on the conflict algorithm see base
    // class for implementation. Only reason for override is to ensure run in order

    @Test
    @Override
    public void test01GetType() throws Exception {
        super.test01GetType();
    }

    @Test
    @Override
    public void test02BasicCrudOperations() {
        super.test02BasicCrudOperations();
    }

    @Test
    @Override
    public void test03BasicBulkCrudOperations() {
        super.test03BasicBulkCrudOperations();
    }

    @Test
    @Override
    public void test04QueryParameters() {
        super.test04QueryParameters();
    }

    /**
     * Verify insertion conflicts do not change the database and "null" is returned as the URI
     * which indicates the insert failed and was rolled back.
     */
    @Test
    @Override
    public void test05InsertConflicts() {
        // Prep by inserting data
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = getMockContentResolver().insert(getUri(), insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        Cursor cursor = getMockContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        // Attempt to insert the data again.

        // Inserting same data again. Uri should be null
        Uri conflictUri = getMockContentResolver().insert(getUri(), insertData);
        assertNull(conflictUri);

        // Verify the data is "unchanged" in the database
        cursor = getMockContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Verify after conflict", cursor, insertData, id);
        cursor.close();
    }

    /**
     * Verify bulk insertion conflicts do not change the database and 0 is returned as the
     * number of rows altered which indicates the insert failed and was rolled back.
     * <p/>
     * This variation tests bulk inserting identical data.  100% conflicts
     */
    @Test
    @Override
    public void test06BulkInsertConflicts() {
        // Prep by inserting data
        ContentValues[] insertData = DataUtilities.insertBulkUsers();
        int rows = getMockContentResolver().bulkInsert(getUri(), insertData);
        assertEquals(insertData.length, rows);

        Cursor cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Bulk Insert", getUniqueColumn(), cursor, insertData);
        cursor.close();

        // Attempt to insert the data again.

        // Inserting same data again. Rows will be 0 to indicate an error occurred inserting
        rows = getMockContentResolver().bulkInsert(getUri(), insertData);
        assertEquals(0, rows);

        // Verify the data is "unchanged" in the database
        cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, insertData);
        cursor.close();
    }

    /**
     * Verify bulk insertion conflicts do not change the database and 0 is returned as the
     * number of rows altered which indicates the insert failed and was rolled back.
     * <p/>
     * This variation tests bulk inserting identical data, however only part of the data is
     * conflicted. However, due to rollbacks, no additional data is inserted.
     */
    @Test
    @Override
    public void test07BulkInsertPartialConflicts() {
        // Prep by inserting data. This data wil conflict on 2nd row of bulk update
        ContentValues insertData = DataUtilities.insertUser2();
        Uri uri = getMockContentResolver().insert(getUri(), insertData);
        assertNotNull(uri);

        long id = ContentUris.parseId(uri);

        Cursor cursor = getMockContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        cursor.close();

        // Attempt to bulk insert data where the 2nd row conflicts

        // Rows will be 0 to indicate an error occurred inserting
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = getMockContentResolver().bulkInsert(getUri(), bulkInsertData);
        assertEquals(0, rows);

        // Verify the data is "unchanged" in the database. Ie should only be one row...
        cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, new ContentValues[]{insertData});
        cursor.close();
    }

    /**
     * Verify update conflicts do not change the database and 0 is returned as the
     * number of rows altered which indicates the update failed and was rolled back.
     * <p/>
     * To generate update conflict, attempt to update a "unique" column with data that
     * already exists.
     */
    @Test
    @Override
    public void test08UpdateConflicts() {
        // Prep by inserting data
        ContentValues[] insertData = DataUtilities.insertBulkUsers();
        int rows = getMockContentResolver().bulkInsert(getUri(), insertData);
        assertEquals(insertData.length, rows);

        Cursor cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Bulk Insert", getUniqueColumn(), cursor, insertData);
        cursor.close();

        // Attempt to update data to have same data on all rows.

        // Rows will be 0 to indicate an error occurred updating
        ContentValues updateData = DataUtilities.updateDataPossibleConflict();
        rows = getMockContentResolver().update(getUri(), updateData, null, null);
        assertEquals(0, rows);

        // Verify the data is "unchanged" in the database, both rows still exist as inserted.
        cursor = getMockContentResolver().query(getUri(), null, null, null, null);
        assertNotNull(cursor);
        CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, insertData);
        cursor.close();
    }
}