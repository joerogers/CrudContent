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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

/**
 * An {@link android.os.AsyncTask} for inserting data in the background. The task uses the
 * {@link android.os.AsyncTask#THREAD_POOL_EXECUTOR} allowing multiple tasks to operate concurrently.
 *
 * You must create the task via the {@link Builder}
 */
public class BasicCRUDInsertTask extends AsyncTask<BasicCRUDInsertTask.Builder, Void, Void> {

    /**
     * The {@link android.content.Intent} action sent via {@link android.support.v4.content.LocalBroadcastManager} when
     * the insert operation is complete and requested
     */
    public static final String INSERT_COMPLETE_ACTION = "com.forkingcode.crudcontent.action.insert_complete";

    /**
     * The extra indicating the number uri of the inserted row when a single item is inserted
     */
    public static final String EXTRA_URI = "com.forkingcode.crudcontent.extra.uri";

    /**
     * The extra indicating the number of rows inserted or bulk inserted
     */
    public static final String EXTRA_ROWS = "com.forkingcode.crudcontent.extra.rows";

    /**
     * Builder used to create a new insert task
     */
    public static class Builder {
        private final Context applicationContext;
        private Uri uri;
        private ContentValues[] valuesArray;
        private boolean resultBroadcastRequested = false;

        /**
         * Create a new insert task builder
         *
         * @param context A context used in the creation of the task. The application context
         *                will be retrieved via this context, to avoid holding direct
         *                references to any activities, views, etc.
         * @see android.content.Context
         */
        public Builder(@NonNull Context context) {
            this.applicationContext = context.getApplicationContext();
        }

        /**
         * Provide the Uri for insertion. You must call this method on the builder. Failure to do so
         * will result in an IllegalStateException when {@link #start()} is called.
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
         * Provide a single row of values for the insert operation.
         *
         * @param values The content values indicating the columns/value pairs for the operation
         * @return this intent builder
         */
        @NonNull
        public Builder usingValues(@NonNull ContentValues values) {
            valuesArray = new ContentValues[]{values};
            return this;
        }

        /**
         * Provide multiple rows of values for the bulk insert operation via an array
         *
         * @param values The content values array indicating the columns/value pairs for the operation
         * @return this intent builder
         */
        public Builder usingValues(@NonNull ContentValues[] values) {
            valuesArray = values;
            return this;
        }

        /**
         * Optionally request the task to send a local broadcast intent with the result of the insert
         * operation using the {@link #INSERT_COMPLETE_ACTION}, otherwise no broadcast is sent.
         * If one row was provided, the uri representing the inserted row will be provided via {@link #EXTRA_URI}.
         * The number of rows inserted will be provided via {@link #EXTRA_ROWS}.
         *
         * @return this intent builder
         * @see android.support.v4.content.LocalBroadcastManager
         */
        public Builder requestResultBroadcast() {
            resultBroadcastRequested = true;
            return this;
        }

        /**
         * Start the insert task.
         *
         * @return An instance of the BasicCRUDInsertTask that could be used for cancellation
         * @throws IllegalStateException if uri or on or more ContentValues was not provided
         */
        public BasicCRUDInsertTask start() {
            if (uri == null) {
                throw new IllegalStateException("Must provide URI");
            }
            if (valuesArray == null) {
                throw new IllegalStateException("Must provide content values");
            }

            BasicCRUDInsertTask task = new BasicCRUDInsertTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
            return task;
        }
    }

    private BasicCRUDInsertTask() {
    }

    @Override
    protected Void doInBackground(Builder... builders) {
        Builder builder = builders[0];

        Uri uri = null;
        int rows;

        if (isCancelled()) {
            return null;
        }

        if (builder.valuesArray.length == 1) {
            uri = builder.applicationContext.getContentResolver()
                    .insert(builder.uri, builder.valuesArray[0]);
            rows = uri != null ? 1 : 0;
        }
        else {
            rows = builder.applicationContext.getContentResolver()
                    .bulkInsert(builder.uri, builder.valuesArray);
        }

        if (builder.resultBroadcastRequested && !isCancelled()) {
            Intent resultIntent = new Intent(INSERT_COMPLETE_ACTION);
            resultIntent.putExtra(EXTRA_ROWS, rows);
            if (uri != null) {
                resultIntent.putExtra(EXTRA_URI, uri);
            }

            LocalBroadcastManager.getInstance(builder.applicationContext)
                    .sendBroadcast(resultIntent);
        }

        return null;
    }
}
