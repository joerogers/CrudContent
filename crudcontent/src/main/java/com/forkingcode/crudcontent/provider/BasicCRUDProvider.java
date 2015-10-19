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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.List;

/**
 * Basic CRUD (Create, Read, Update, Delete) provider for a single table. Assumes basic
 * matching of either all rows, or by id.
 *
 * URI for matching rows should be in form: content://{authority}/{table}
 * URI for matching row by id should be in form: content://{authority}/{table}/{id}
 *
 * By default the getType() method will return the following for a directory:
 * vnd.android.cursor.dir/{authority}/table
 * and the following for an individual row:
 * vnd.android.cursor.item/{authority}/table
 *
 * The following query parameters are supported on the URI:
 * distinct=true  - informs the query to ensure each row returned is unique.
 * limit={n} - return only the first "n" rows of data
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

    /**
     * Creates a BasicCRUDProvider.  Invoked by your subclass's constructor.
     *
     * @param authority Name of the authority associated with your provider
     */
    public BasicCRUDProvider(String authority) {
        this.authority = authority;
        uriMatcher.addURI(authority, "*", ALL_ROWS);
        uriMatcher.addURI(authority, "*/#", ROW_BY_ID);
    }

    @Override
    public boolean onCreate() {
        dbHelper = getDbHelper();
        Context context = getContext();
        if (context != null) {
            context.registerComponentCallbacks(this);
        }
        return true;
    }

    /**
     * Implemented to support unit testing, not needed for standard content providers
     */
    @Override
    public final void shutdown() {
        Context context = getContext();
        if (context != null) {
            context.unregisterComponentCallbacks(this);
        }
        dbHelper.close();
        dbHelper = null;
    }

    /**
     * Override to provide the database helper associated with this provider
     *
     * @return The SQLiteOpen helper used to access the database
     */
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
     * Override to provide a insert conflict algorithm for the specified table.
     *
     * By default it will use SQLiteDatabase.CONFLICT_NONE which is equivalent to
     * CONFLICT_ABORT per SQLite specification
     *
     * @param table The table to determine the conflict algorithm for.
     * @return SQLiteDatabase conflict algorithm
     * @see SQLiteDatabase
     */
    @SuppressWarnings("UnusedParameters")
    protected int getConflictAlgorithm(@NonNull String table) {
        return SQLiteDatabase.CONFLICT_NONE;
    }

    @Override
    public String getType(@NonNull Uri uri) {
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
    public Uri insert(@NonNull Uri uri, ContentValues values) {
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
        notifyChange(getContext(), uri);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] valuesArray) {
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
            notifyChange(getContext(), uri);
        }
        return count;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
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
            Context context = getContext();
            if (context != null) {
                cursor.setNotificationUri(context.getContentResolver(), uri);
            }
            return cursor;
        }
        finally {
            db.releaseReference();
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
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
            notifyChange(getContext(), uri);
        }
        return rows;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
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
            notifyChange(getContext(), uri);
        }
        return rows;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // In the background. Close the database via the helper.
            // If there is an active connection, it will continue to process
            // due to reference counting, and close when all references are released.
            dbHelper.close();
        }
    }

    /**
     * Helper method to notify listeners of the changes to the database. Useful with loaders
     *
     * @param uri the URI for the content that changed.
     */
    private static void notifyChange(@Nullable Context context, @NonNull Uri uri) {
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null, false);
        }
    }

    private static void startTransaction(SQLiteDatabase db) {
        // If db configured to use write ahead logging, use a non exclusive transaction to permit
        // both read/write operations at same time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && db.isWriteAheadLoggingEnabled()) {
            db.beginTransactionNonExclusive();
        }
        else {
            db.beginTransaction();
        }
    }
}
