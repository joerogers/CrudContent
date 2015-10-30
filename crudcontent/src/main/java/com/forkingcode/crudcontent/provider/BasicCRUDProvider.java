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
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * Basic CRUD (Create, Read, Update, Delete) provider for a single table. Assumes basic
 * matching of either all rows, or by id.
 * <p/>
 * The provider uses transactions for insert, update and deletes to provide optimal performance
 * and the ability to cleanly implement rollbacks. It will use the proper type of transaction
 * depending on if write ahead logging is enabled on the database.
 * <p/>
 * URI for matching rows should be in form: content://{authority}/{table}
 * URI for matching row by id should be in form: content://{authority}/{table}/{id}
 * <p/>
 * By default the getType() method will return the following for a directory:
 * vnd.android.cursor.dir/{authority}/table
 * and the following for an individual row:
 * vnd.android.cursor.item/{authority}/table
 * <p/>
 * The following query parameters are supported on the URI:
 * distinct=true  - informs the query to ensure each row returned is unique.
 * limit={n} - return only the first "n" rows of data
 * <p/>
 * Note: if any errors occur in bulkInsert, update, or delete the provider will return 0
 * to indicate an error occurred. Depending on the conflict method the following will
 * happen:
 *
 * ROLLBACK - 0 or n rows.  Essentially an all or nothing operation.
 * IGNORE - 0 to n rows. 0 is 100% failure, n is 100% success, n/2 = 50% success, etc
 * REPLACE - n rows, should always be 100% successful
 *
 * If logging is enabled, more data on the errors will be recorded.
 */
public abstract class BasicCRUDProvider extends ContentProvider {

    private static final String TAG = "BasicCRUDProvider";

    public static final String DISTINCT_PARAMETER = "distinct";
    public static final String LIMIT_PARAMETER = "limit";

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int ALL_ROWS = 1;
    private static final int ROW_BY_ID = 2;

    private static final int TABLE_SEGMENT = 0;
    private static final int ID_SEGMENT = 1;

    private static final String WHERE_MATCHES_ID = BaseColumns._ID + " = ?";


    private static boolean LOGGING_ENABLED = false;

    private SQLiteOpenHelper dbHelper;
    private final String authority;

    /**
     * CONFLICT_ROLLBACK - all inserts (bulk or single), or updates will be rolled back on any
     * data conflict or unexpected error that occurs with the sql command.
     * <p/>
     * Essentially behaves like CONFLICT_ROLLBACK, but uses CONFLICT_NONE (aka CONFLICT_ABORT) to
     * detect errors, but this implementation of the provider will always rollback the transaction.
     *
     * @see SQLiteDatabase#CONFLICT_ROLLBACK
     * @see SQLiteDatabase#CONFLICT_ABORT
     */
    protected static final int CONFLICT_ROLLBACK = SQLiteDatabase.CONFLICT_NONE;

    /**
     * CONFLICT_IGNORE - all inserts (bulk or single), or updates that result in a data conflict
     * will be ignored leaving the original values in place, but the command will be successful.
     *
     * @see SQLiteDatabase#CONFLICT_IGNORE
     */
    protected static final int CONFLICT_IGNORE = SQLiteDatabase.CONFLICT_IGNORE;

    /**
     * CONFLICT_REPLACE - all inserts (bulk or single), that result in a data conflict
     * will be removed, and the new row will be inserted in its place.
     * <p/>
     * For updates that result in conflict, the data resulting in the conflict will be removed
     * prior to the update for the row completing.
     *
     * @see SQLiteDatabase#CONFLICT_REPLACE
     */
    protected static final int CONFLICT_REPLACE = SQLiteDatabase.CONFLICT_REPLACE;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONFLICT_ROLLBACK, CONFLICT_IGNORE, CONFLICT_REPLACE})
    public @interface ConflictAlgorithm {
    }

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

    /**
     * Enable diagnostic logging. Best if flag is tied to BuildConfig.DEBUG for the application
     *
     * @param enabled true if diagnostic logging should occur
     */
    public static void setLoggingEnabled(boolean enabled) {
        LOGGING_ENABLED = enabled;
    }

    @Override
    public boolean onCreate() {
        dbHelper = getDbHelper();
        return true;
    }

    /**
     * Implemented to support unit testing, not needed for standard content providers
     */
    @Override
    public final void shutdown() {
        dbHelper.close();
        dbHelper = null;
    }

    /**
     * Override to provide the database helper associated with this provider
     *
     * @return The SQLiteOpen helper used to access the database
     */
    @NonNull
    protected abstract SQLiteOpenHelper getDbHelper();

    /**
     * Override to provide a custom null column hack for a given table
     *
     * @param table The table being inserted into based on URI
     * @return A specified null column hack or null if no hack is needed.
     * Default is null
     */
    @SuppressWarnings({"SameReturnValue", "UnusedParameters", "WeakerAccess"})
    @Nullable
    protected String getNullColumnHack(@NonNull String table) {
        return null;
    }

    /**
     * Override to provide a insert conflict algorithm for the specified table.
     * <p/>
     * By default it will use CONFLICT_ROLLBACK which will roll back the insert
     * operation if a conflict occurs leaving the database unchanged.
     *
     * @param table The table to determine the conflict algorithm for.
     * @return SQLiteDatabase conflict algorithm
     * @see SQLiteDatabase
     */
    @SuppressWarnings("UnusedParameters")
    @ConflictAlgorithm
    protected int getInsertConflictAlgorithm(@NonNull String table) {
        return CONFLICT_ROLLBACK;
    }

    /**
     * Override to provide an update conflict algorithm for the specified table.
     * <p/>
     * By default it will use CONFLICT_ROLLBACK which will roll back the update
     * operation if a conflict occurs leaving the database unchanged.
     *
     * @param table The table to determine the conflict algorithm for.
     * @return SQLiteDatabase conflict algorithm
     * @see SQLiteDatabase
     */
    @SuppressWarnings("UnusedParameters")
    @ConflictAlgorithm
    protected int getUpdateConflictAlgorithm(@NonNull String table) {
        return CONFLICT_ROLLBACK;
    }

    @Override
    @Nullable
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
    @Nullable
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
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
            int conflictAlgorithm = translateConflictAlgorithm(getInsertConflictAlgorithm(table));
            startTransaction(db);
            try {
                id = db.insertWithOnConflict(table, getNullColumnHack(table), values, conflictAlgorithm);

                if (id != -1) {
                    db.setTransactionSuccessful();
                }
            }
            catch (SQLiteException e) {
                if (LOGGING_ENABLED) {
                    Log.e(TAG, "Error inserting " + table +
                            " with " + values, e);
                }
                id = -1;
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
                final int conflictAlgorithm = translateConflictAlgorithm(getInsertConflictAlgorithm(table));

                for (ContentValues values : valuesArray) {
                    long id;
                    try {
                        id = db.insertWithOnConflict(table, nullColumnHack, values, conflictAlgorithm);
                    }
                    catch (SQLiteConstraintException e) {
                        if (LOGGING_ENABLED) {
                            Log.e(TAG, "Error inserting " + table +
                                    " with " + values, e);
                        }
                        throw e;
                    }

                    if (id != -1) {
                        ++count;
                    }
                }
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {
                if (LOGGING_ENABLED && !(e instanceof SQLiteConstraintException)) {
                    Log.e(TAG, "Unexpected error bulk inserting " + table, e);
                }
                count = 0;
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
    @Nullable
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection,
                        @Nullable String selection,
                        @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

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
        catch (SQLiteException e) {
            if (LOGGING_ENABLED) {
                Log.e(TAG, "Error querying " + table +
                        " where " + useSelection + " " + Arrays.toString(useSelectionArgs), e);
            }
            return null;
        }
        finally {
            db.releaseReference();
        }
    }

    @Override
    public int update(@NonNull Uri uri,
                      @Nullable ContentValues values,
                      @Nullable String selection,
                      @Nullable String[] selectionArgs) {

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
                final int conflictAlgorithm = translateConflictAlgorithm(getUpdateConflictAlgorithm(table));
                rows = db.updateWithOnConflict(table, values, useSelection, useSelectionArgs, conflictAlgorithm);
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {
                if (LOGGING_ENABLED) {
                    Log.e(TAG, "Error updating " + table +
                            " where " + useSelection + " " + Arrays.toString(useSelectionArgs) +
                            " with " + values, e);
                }
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
    public int delete(@NonNull Uri uri,
                      @Nullable String selection,
                      @Nullable String[] selectionArgs) {

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
            catch (SQLiteException e) {
                if (LOGGING_ENABLED) {
                    Log.e(TAG, "Error deleting " + table +
                            " where " + useSelection + " " + Arrays.toString(useSelectionArgs), e);
                }
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
            // due to explicit reference counting added via this provider implementation.
            // The database will close when all references are released.
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

    private static int translateConflictAlgorithm(int algorithm) {
        switch (algorithm) {
            case CONFLICT_ROLLBACK:
            case CONFLICT_IGNORE:
            case CONFLICT_REPLACE:
                return algorithm;
            default:
                if (LOGGING_ENABLED) {
                    Log.w(TAG, "Unexpected conflict algorithm provided: " + algorithm);
                }
                return CONFLICT_REPLACE;
        }
    }
}
