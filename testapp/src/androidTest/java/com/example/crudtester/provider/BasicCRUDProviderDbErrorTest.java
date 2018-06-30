/*
 * Copyright 2018 Joe Rogers
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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.provider.ProviderTestRule;

import com.example.crudtester.utils.DataUtilities;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsNull.notNullValue;

public class BasicCRUDProviderDbErrorTest {

    private final ProviderTestRule providerTestRule = new ProviderTestRule
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

    @NonNull
    private String getTable() {
        return ReplaceContract.TABLE;
    }

    @NonNull
    public Uri getUri() {
        return ReplaceContract.URI;
    }

    @Test
    public void testInsertFailure() {
        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(2);
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);

        assertThat(uri, is(nullValue()));
    }

    @Test
    public void testInsertRetrySuccess() {
        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(1);
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);

        assertThat(uri, is(notNullValue()));
        long id = ContentUris.parseId(uri);
        assertThat(id, is(greaterThan(0L)));
    }

    @Test
    public void testBulkInsertFailure() {
        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(2);
        ContentValues[] insertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), insertData);

        assertThat(rows, is(0));
    }

    @Test
    public void testBulkInsertRetrySuccess() {
        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(1);
        ContentValues[] insertData = DataUtilities.insertBulkUsers();
        int rows = providerTestRule.getResolver().bulkInsert(getUri(), insertData);

        assertThat(rows, is(insertData.length));
    }

    @Test
    public void testUpdateFailure() {
        Uri uri = insertDataSuccessfully();

        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(2);
        ContentValues updateData = DataUtilities.updateUser1();
        int rows = providerTestRule.getResolver().update(uri, updateData, null, null);

        assertThat(rows, is(0));
    }

    @Test
    public void testUpdateRetrySuccess() {
        Uri uri = insertDataSuccessfully();

        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(1);
        ContentValues updateData = DataUtilities.updateUser1();
        int rows = providerTestRule.getResolver().update(uri, updateData, null, null);

        assertThat(rows, is(1));
    }


    @Test
    public void testDeleteFailure() {
        Uri uri = insertDataSuccessfully();

        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(2);
        int rows = providerTestRule.getResolver().delete(uri, null, null);

        assertThat(rows, is(0));
    }

    @Test
    public void testDeleteRetrySuccess() {
        Uri uri = insertDataSuccessfully();

        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(1);
        int rows = providerTestRule.getResolver().delete(uri, null, null);

        assertThat(rows, is(1));
    }

    private Uri insertDataSuccessfully() {
        // Test basic insertion
        DBHelper.getInstance(InstrumentationRegistry.getTargetContext()).setErrorCount(0);
        ContentValues insertData = DataUtilities.insertUser1();
        Uri uri = providerTestRule.getResolver().insert(getUri(), insertData);

        assertThat(uri, is(notNullValue()));
        long id = ContentUris.parseId(uri);
        assertThat(id, is(greaterThan(0L)));
        return uri;
    }
}
