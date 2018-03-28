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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test the content provider using the REPLACE conflict algorithm
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReplaceBasicCRUDProviderTest extends BaseBasicCRUDProviderTest {

    public ReplaceBasicCRUDProviderTest() {
        super();
    }

    @NonNull
    @Override
    protected String getTable() {
        return ReplaceContract.TABLE;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return ReplaceContract.URI;
    }

    @NonNull
    @Override
    public String getUniqueColumn() {
        return ReplaceContract.Columns.DATA1;
    }

    @NonNull
    @Override
    public String getDistinctColumn() {
        return ReplaceContract.Columns.DATA2;
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
     * Verify insertion conflicts are replaced. Meaning if you insert the exact same data
     * the "id" will be different, but the contents identical.
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
        assertThat(conflictUri, is(notNullValue()));
        assertThat(conflictUri, is(not(uri)));

        long conflictId = ContentUris.parseId(conflictUri);
        assertThat(conflictId, is(not(id)));

        // Verify the data is "unchanged" in the database except for the id. Must use new
        // URI since original insert no longer exists.
        //noinspection ConstantConditions
        try (Cursor cursor = providerTestRule.getResolver().query(conflictUri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Verify after ignore", cursor, insertData, conflictId);
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

        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, getUniqueColumn())) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Bulk Insert", getUniqueColumn(), cursor, insertData);

            // Attempt to insert the data again.

            // Inserting same data again. Rows will be 2 to indicate 100% successful
            rows = providerTestRule.getResolver().bulkInsert(getUri(), insertData);
            assertThat(rows, is(insertData.length));

            // Verify the data is "unchanged" in the database
            try (Cursor conflictCursor = providerTestRule.getResolver().query(getUri(), null, null, null, getUniqueColumn())) {
                assertThat(conflictCursor, is(notNullValue()));
                //noinspection ConstantConditions
                CursorUtilities.ensureCursorsMatchExcludingId("Cursors match", cursor, conflictCursor);
            }
        }
    }

    /**
     * Verify bulk insertion conflicts change the database and 1is returned as the
     * number of rows altered which indicates the insert partially was ignored.
     * <p/>
     * This variation tests bulk inserting identical data, however only part of the data is
     * conflicted. All rows should be inserted and the original row should be replaced.
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

        // Rows will be match rows provided as all data inserted or replaced
        ContentValues[] bulkInsertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), bulkInsertData);
        assertThat(rows, is(bulkInsertData.length));

        // Verify the data is "changed". Ie all expected rows now exist in the db.
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, bulkInsertData);
        }

        // Also verify original row no longer exists, using the original id
        try (Cursor cursor = providerTestRule.getResolver().query(uri, null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(0));
        }
    }

    /**
     * Verify update conflicts do not change the database and 2 is returned as the
     * number of rows altered which indicates the update was successful. However, because
     * the conflicted row was deleted, only one row remains in the database.
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

        // Rows will be 2 to indicate all rows updated or replaced
        ContentValues updateData = DataUtilities.updateDataPossibleConflict();
        rows = providerTestRule.getResolver().update(getUri(), updateData, null, null);
        assertThat(rows, is(2));

        // Verify only the user1 now exists in the database as the conflicting row was replaced (ie
        // deleted to allow update).
        try (Cursor cursor = providerTestRule.getResolver().query(getUri(), null, null, null, null)) {
            assertThat(cursor, is(notNullValue()));
            //noinspection ConstantConditions
            assertThat(cursor.getCount(), is(1));
            CursorUtilities.validateCursor("Verify after conflict", getUniqueColumn(), cursor, new ContentValues[]{updateData});
        }
    }
}
