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

package com.forkingcode.crudcontent.provider;

import android.content.ComponentCallbacks2;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.List;

/**
 * Basic CRUD (Create, Read, Update, Delete) provider for a single table. Assumes basic
 * matching of either all rows, or by id.
 * <p/>
 * URI for matching rows should be in form: content://{authority}/{table}
 * URI for matching row by id should be in form: content://{authority}/{table}/{id}
 * <p/>
 * By default the getType() method will return the following for a directory:
 * vnd.android.cursor.dir/{authority}/table
 * and the following for an individual row:
 * vnd.android.cursor.item/{authority}/table
 */
public abstract class BasicCRUDProvider extends ContentProvider {

    public static final String DISTINCT_PARAMETER = "distinct";
    public static final String LIMIT_PARAMETER = "limit";

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int ALL_ROWS = 1;
    private static final int ROW_BY_ID = 2;

    private static final int TABLE_SEGMENT = 0;
    private static final int ID_SEGMENT = 1;

    private static final String WHERE_MATCHES_ID = BaseColumns._ID + " = ?";

    private SQLiteOpenHelper dbHelper;
    private final String authority;

    public BasicCRUDProvider(String authority) {
        this.authority = authority;
        uriMatcher.addURI(authority, "*", ALL_ROWS);
        uriMatcher.addURI(authority, "*/#", ROW_BY_ID);
    }

    @Override
    public boolean onCreate() {
        dbHelper = getDbHelper();
        return true;
    }

    /**
     * Implemented for tests
     */
    @Override
    public final void shutdown() {
        dbHelper.close();
        dbHelper = null;
    }

    protected abstract SQLiteOpenHelper getDbHelper();

    /**
     * Override to provide a custom null column hack for a given table
     *
     * @param table The table being inserted into based on URI
     * @return A specified null column hack or null if no hack is needed.
     * Default is null
     */
    @SuppressWarnings({"SameReturnValue", "UnusedParameters", "WeakerAccess"})
    protected String getNullColumnHack(@NonNull String table) {
        return null;
    }

    /**
     * Override to provide a conflict algorithm for the specified table.
     *
     * @param table The table to determine the conflict algorithm for.
     * @return SQLiteDatabase conflict algorithm
     */
    protected abstract int getConflictAlgorithm(@NonNull String table);

    @Override
    public String getType(Uri uri) {
        int match = uriMatcher.match(uri);
        String table;
        switch (match) {
            case ALL_ROWS:
                table = uri.getLastPathSegment();
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + authority + "/" + table;
            case ROW_BY_ID:
                table = uri.getPathSegments().get(TABLE_SEGMENT);
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + authority + "/" + table;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = uriMatcher.match(uri);
        String table;
        switch (match) {
            case ALL_ROWS:
                table = uri.getLastPathSegment();
                break;
            case ROW_BY_ID:
                throw new UnsupportedOperationException("Unable to insert by id for uri: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        long id = -1;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.acquireReference();
        try {
            startTransaction(db);
            try {
                id = db.insertWithOnConflict(table, getNullColumnHack(table), values, getConflictAlgorithm(table));
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            db.releaseReference();
        }

        if (id == -1) {
            return null;
        }

        // notify change essentially indicates to any users with active cursors
        // that they need to "reload" the data
        notifyChange(uri);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] valuesArray) {
        int match = uriMatcher.match(uri);
        String table;
        switch (match) {
            case ALL_ROWS:
                table = uri.getLastPathSegment();
                break;
            case ROW_BY_ID:
                throw new UnsupportedOperationException("Unable to insert by id for uri: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count = 0;
        db.acquireReference();
        try {
            startTransaction(db);
            try {
                final String nullColumnHack = getNullColumnHack(table);
                final int conflictAlgorithm = getConflictAlgorithm(table);

                for (ContentValues values : valuesArray) {
                    long id = db.insertWithOnConflict(table, nullColumnHack, values, conflictAlgorithm);
                    if (id != -1) {
                        ++count;
                    }
                }
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            db.releaseReference();
        }

        // notify change essentially indicates to any users with active cursors
        // that they need to "reload" the data
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int match = uriMatcher.match(uri);
        String table;
        String useSelection = selection;
        String[] useSelectionArgs = selectionArgs;
        switch (match) {
            case ALL_ROWS:
                table = uri.getLastPathSegment();
                break;
            case ROW_BY_ID:
                List<String> segments = uri.getPathSegments();
                table = segments.get(TABLE_SEGMENT);
                useSelection = WHERE_MATCHES_ID;
                useSelectionArgs = new String[]{segments.get(ID_SEGMENT)};
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        boolean distinct = false;
        String limit = null;

        // If have query parameters, see if any exist interested in.
        if (!TextUtils.isEmpty(uri.getQuery())) {
            distinct = uri.getBooleanQueryParameter(DISTINCT_PARAMETER, false);
            limit = uri.getQueryParameter(LIMIT_PARAMETER);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.acquireReference();

        try {
            Cursor cursor = db.query(distinct, table, projection, useSelection, useSelectionArgs, null, null, sortOrder, limit);
            // Register the cursor with the requested URI so the caller will receive
            // future database change notifications. Useful for "loaders" which take advantage
            // of this concept.
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        }
        finally {
            db.releaseReference();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        String table;
        String useSelection = selection;
        String[] useSelectionArgs = selectionArgs;
        switch (match) {
            case ALL_ROWS:
                table = uri.getLastPathSegment();
                break;
            case ROW_BY_ID:
                List<String> segments = uri.getPathSegments();
                table = segments.get(TABLE_SEGMENT);
                useSelection = WHERE_MATCHES_ID;
                useSelectionArgs = new String[]{segments.get(ID_SEGMENT)};
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        int rows = 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.acquireReference();

        try {
            startTransaction(db);
            try {
                rows = db.update(table, values, useSelection, useSelectionArgs);
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            db.releaseReference();
        }

        // notify change essentially indicates to any users with active cursors
        // that they need to "reload" the data
        if (rows > 0) {
            notifyChange(uri);
        }
        return rows;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        String table;
        String useSelection = selection;
        String[] useSelectionArgs = selectionArgs;
        switch (match) {
            case ALL_ROWS:
                table = uri.getLastPathSegment();
                break;
            case ROW_BY_ID:
                List<String> segments = uri.getPathSegments();
                table = segments.get(TABLE_SEGMENT);
                useSelection = WHERE_MATCHES_ID;
                useSelectionArgs = new String[]{segments.get(ID_SEGMENT)};
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        int rows = 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.acquireReference();
        try {
            startTransaction(db);
            try {
                rows = db.delete(table, useSelection, useSelectionArgs);
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            db.releaseReference();
        }

        // notify change essentially indicates to any users with active cursors
        // that they need to "reload" the data
        if (rows > 0) {
            notifyChange(uri);
        }
        return rows;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                // In the background lru list. Close the database via
                // the helper.
                dbHelper.close();
                break;
        }
    }

    /**
     * Helper method to notify listeners of the changes to the database. Useful with loaders
     *
     * @param uri the URI for the content that changed.
     */
    protected void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null, false);
    }

    private static void startTransaction(SQLiteDatabase db) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && db.isWriteAheadLoggingEnabled()) {
            db.beginTransactionNonExclusive();
        }
        else {
            db.beginTransaction();
        }
    }
}
