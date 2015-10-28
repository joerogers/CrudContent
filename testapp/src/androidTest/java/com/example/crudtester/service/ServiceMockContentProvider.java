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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.net.Uri;
import android.test.mock.MockContentProvider;

/**
 * Mock provider "pretending" it talks to a database. Results vary based on input.
 */
public class ServiceMockContentProvider extends MockContentProvider {

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int ALL_ROWS = 1;
    private static final int ROW_BY_ID = 2;


    public ServiceMockContentProvider(String authority) {
        super();
        uriMatcher.addURI(authority, "*", ALL_ROWS);
        uriMatcher.addURI(authority, "*/#", ROW_BY_ID);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return values != null ? values.length : 0;
            default:
                return -1;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return selectionArgs != null ? selectionArgs.length : 0;
            case ROW_BY_ID:
                return (int) ContentUris.parseId(uri);
            default:
                return -1;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return ContentUris.withAppendedId(uri, 1);
            default:
                return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return selectionArgs != null ? selectionArgs.length : 0;
            case ROW_BY_ID:
                return (int) ContentUris.parseId(uri);
            default:
                return -1;
        }
    }
}
