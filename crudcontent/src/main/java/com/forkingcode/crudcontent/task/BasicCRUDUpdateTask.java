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

package com.forkingcode.crudcontent.task;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

/**
 * An async task for updating data asynchronously. The task uses the AsyncTask tread pool allowing
 * multiple tasks to operate concurrently.
 */
public class BasicCRUDUpdateTask extends AsyncTask<BasicCRUDUpdateTask.Builder, Void, Void> {

    public static final String UPDATE_COMPLETE_ACTION = "com.forkingcode.crudcontent.action.update_complete";
    public static final String EXTRA_ROWS = "com.forkingcode.crudcontent.extra.rows";

    /**
     * Builder uses to create a new update task
     */
    public static class Builder {
        private final Context applicationContext;
        private Uri uri;
        private long rowId = 0;
        private String selection = null;
        private String[] selectionArgs = null;
        private ContentValues values;
        private boolean resultBroadcastRequested = false;

        /**
         * Create a new update task builder
         *
         * @param context A context used in the creation of the task. The application context
         *                will be retrieved via this context, to avoid holding direct
         *                references to activities, etc.
         */
        public Builder(@NonNull Context context) {
            this.applicationContext = context.getApplicationContext();
        }

        /**
         * Provide the Uri for updates. You must call this method on the builder. Failure to do so
         * will result in an IllegalStateException when start() is called.
         *
         * @param uri The uri associated with the provider to query.
         * @return This builder object
         */
        @NonNull
        public Builder forUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Provide a single row of values for the update operation. Failure to do so
         * will result in an IllegalStateException when start() is called.
         *
         * @param values The content values indicating the columns/value pairs for the operation
         * @return this intent builder
         */
        @NonNull
        public Builder usingValues(@NonNull ContentValues values) {
            this.values = values;
            return this;
        }

        /**
         * Optionally indicate you wish to query a specific row by id. If this is not provided
         * all rows will be returned unless whereMatchesSelection was called instead, or you already
         * appended the rowId to the Uri.
         *
         * Note: if you provided a Uri with the rowId already appended, then you should avoid calling
         * this method as it will append the rowId to the end of the Uri provided.
         *
         * An IllegalStateException will be thrown if both a rowId and a selection are provided when
         * start() is called.
         * @param rowId The id of the row to select from the database
         * @return This builder object
         */
        public Builder whereMatchesId(long rowId) {
            this.rowId = rowId;
            return this;
        }

        /**
         * Optionally provide a selection and selection arguments for the update. If this is not provided
         * all rows will be updated unless whereMatchesRowId was called instead, or you alreadly appended
         * the rowId to the the Uri.
         *
         * An IllegalStateException will be thrown if both a rowId and a selection are provided when
         * start() is called.
         *
         * @param selection     A selection criteria to apply when filtering rows (ie where clause).
         * @param selectionArgs You may include ?s in selection, which will be replaced by
         *                      the values from selectionArgs, in order that they appear in the selection.
         *                      The values will be bound as Strings. May pass a String[] or comma separated
         *                      strings for each argument. Passing null means nothing in the selection needs
         *                      to be replaced.
         * @return This builder object
         */
        public Builder whereMatchesSelection(@NonNull String selection, @Nullable String... selectionArgs) {
            this.selection = selection;
            this.selectionArgs = selectionArgs;
            return this;
        }

        /**
         * Optionally request the task to send an local broadcast with the result of the update
         * operation using the UPDATE_COMPLETE_ACTION, otherwise no broadcast is sent.
         * The number of rows updated will be provided via EXTRA_ROWS.
         *
         * @return this intent builder
         * @see LocalBroadcastManager
         */
        public Builder requestResultBroadcast() {
            resultBroadcastRequested = true;
            return this;
        }

        /**
         * Start the update task.
         *
         * @return An instance of the BasicCRUDUpdateTask that could be used for cancellation
         * @throws IllegalStateException If conditions for uri, values, rowId and/or selection are
         * not met per individual method descriptions
         */
        public BasicCRUDUpdateTask start() {
            if (uri == null) {
                throw new IllegalStateException("Must provide URI");
            }
            if (values == null) {
                throw new IllegalStateException("Must provide content values");
            }
            if (rowId != 0 && selection != null) {
                throw new IllegalStateException("Do not provide both a row id and a selection");
            }

            BasicCRUDUpdateTask task = new BasicCRUDUpdateTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
            return task;
        }
    }

    private BasicCRUDUpdateTask() {
    }

    @Override
    protected Void doInBackground(Builder... builders) {
        Builder builder = builders[0];

        if (isCancelled()) {
            return null;
        }

        Uri uri = builder.uri;
        if (builder.rowId != 0) {
            uri = ContentUris.withAppendedId(uri, builder.rowId);
            builder.selection = null;
            builder.selectionArgs = null;
        }

        int rows = builder.applicationContext.getContentResolver()
                    .update(uri, builder.values, builder.selection, builder.selectionArgs);

        if (builder.resultBroadcastRequested && !isCancelled()) {
            Intent resultIntent = new Intent(UPDATE_COMPLETE_ACTION);
            resultIntent.putExtra(EXTRA_ROWS, rows);

            LocalBroadcastManager.getInstance(builder.applicationContext)
                    .sendBroadcast(resultIntent);
        }

        return null;
    }
}