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

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.test.mock.MockContentProvider;

/**
 * Mock provider "pretending" it talks to a database with hardcoded results
 */
@SuppressLint("Registered")
public class TaskMockContentProvider extends MockContentProvider {

    public static final long INSERT_ID_RESULT = 1;
    public static final int BULK_INSERT_RESULT = 2;
    public static final int UPDATE_RESULT = 3;
    public static final int DELETE_RESULT = 5;

    public TaskMockContentProvider() {
        super();
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return BULK_INSERT_RESULT;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return DELETE_RESULT;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return ContentUris.withAppendedId(uri, INSERT_ID_RESULT);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return UPDATE_RESULT;
    }
}
