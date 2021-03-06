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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test the content provider using the IGNORE conflict algorithm
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IgnoreBasicCRUDProviderTest extends BaseBasicCRUDProviderTest {

    public IgnoreBasicCRUDProviderTest() {
        super();
    }

    @NonNull
    @Override
    protected String getTable() {
        return IgnoreContract.TABLE;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return IgnoreContract.URI;
    }

    @NonNull
    @Override
    public String getUniqueColumn() {
        return IgnoreContract.Columns.DATA1;
    }

    @NonNull
    @Override
    public String getDistinctColumn() {
        return IgnoreContract.Columns.DATA2;
    }

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
     * Verify insertion conflicts are ignored and do not change the database.
     */
    @Test
    @Override
    public void test05InsertConflicts() {
        // Prep by inserting data
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);
        assertThat(uri, is(notNullValue()));

        long id = ContentUris.parseId(uri);

        //noinspection ConstantConditions
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        }

        // Attempt to insert the data again, ignore very similar to rollback. Only
        // difference is whether or not an exception is thrown to indicate the conflict
        // for a single insert. In both cases null is returned for the uri.

        // Inserting same data again. Uri should be identical to original insert
        Uri conflictUri = providerTestRule.getResolver().insert(getUri(), insertData);
        assertThat(conflictUri, is(nullValue()));

        // Verify the data is "unchanged" in the database
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Verify after ignore", cursor, insertData, id);
        }
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
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), insertData);
        assertThat(rows, is(insertData.length));

        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Bulk Insert", getUniqueColumn(), cursor, insertData);
        }

        // Attempt to insert the data again.

        // Inserting same data again. Rows will be 0 to indicate an error occurred inserting
        rows = providerTestRule.getResolver().bulkInsert(getUri(), insertData);
        assertThat(rows, is(0));

        // Verify the data is "unchanged" in the database
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, insertData);
        }
    }

    /**
     * Verify bulk insertion conflicts change the database and 1is returned as the
     * number of rows altered which indicates the insert partially was ignored.
     * <p/>
     * This variation tests bulk inserting identical data, however only part of the data is
     * conflicted. The non-conflicting row should be added to the database
     */
    @Test
    @Override
    public void test07BulkInsertPartialConflicts() {
        // Prep by inserting data. This data wil conflict on 2nd row of bulk update
        ContentValues insertData = DataUtilities.insertUser2();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);
        assertThat(uri, is(notNullValue()));

        long id = ContentUris.parseId(uri);

        //noinspection ConstantConditions
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        }

        // Attempt to bulk insert data where the 2nd row conflicts

        // Rows will be 1 to indicate some data was inserted
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), bulkInsertData);
        assertThat(rows, is(1));

        // Verify the data is "changed". Ie two rows now exist in the db.
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, bulkInsertData);
        }

        // Also verify original row is identical
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Insert", cursor, insertData, id);
        }
    }

    /**
     * Verify update conflicts do not change the database and 1 is returned as the
     * number of rows altered which indicates the update was ignored for one row.
     * <p/>
     * To generate update conflict, attempt to update a "unique" column with data that
     * already exists.
     */
    @Test
    @Override
    public void test08UpdateConflicts() {
        // Prep by inserting data
        ContentValues[] insertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), insertData);
        assertThat(rows, is(insertData.length));

        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Bulk Insert", getUniqueColumn(), cursor, insertData);
        }

        // Attempt to update data to have same data on all rows.

        // Rows will be 1 to indicate an error occurred updating one user, but that user was
        // unmodified. The other use was modified.
        ContentValues updateData = DataUtilities.updateDataPossibleConflict();
        rows = providerTestRule.getResolver().update(getUri(), updateData, null, null);
        assertThat(rows, is(1));

        // Verify the data in the database matches expected state
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));

            // In this case "user1" was modified to the values in update data. "user2" is unchanged from
            // the original insert.
            ContentValues[] expectedResult = new ContentValues[2];
            expectedResult[0] = updateData;
            expectedResult[1] = DataUtilities.insertUser2();
            CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, expectedResult);
        }
    }
}
