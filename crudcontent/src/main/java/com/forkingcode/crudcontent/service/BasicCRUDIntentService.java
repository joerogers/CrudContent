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

package com.forkingcode.crudcontent.service;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;

import com.forkingcode.crudcontent.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Intent service for running common DB operations in the background. The service
 * will broadcast complete actions using the LocalBroadcastManager after each operation.
 *
 * @see com.forkingcode.crudcontent.service.BasicCRUDIntentService.IntentBuilder
 */
@SuppressWarnings("unused")
public class BasicCRUDIntentService extends IntentService {

    private static final String ACTION_INSERT = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.insert";
    private static final String ACTION_BULK_INSERT = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.bulkInsert";
    private static final String ACTION_UPDATE = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.update";
    private static final String ACTION_DELETE = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.action.delete";

    private static final String EXTRA_VALUES = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.values";
    private static final String EXTRA_SELECTION = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.selection";
    private static final String EXTRA_SELECTION_ARGS = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.selectionArgs";
    private static final String EXTRA_RESULT_RECEIVER = BuildConfig.APPLICATION_ID + ".BasicCRUDIntentService.extra.resultReceiver";


    public BasicCRUDIntentService() {
        super("BasicCRUDIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();

        switch (action) {
            case ACTION_INSERT:
                handleActionInsert(intent);
                break;
            case ACTION_BULK_INSERT:
                handleActionBulkInsert(intent);
                break;
            case ACTION_UPDATE:
                handleActionUpdate(intent);
                break;
            case ACTION_DELETE:
                handleActionDelete(intent);
                break;
        }
    }

    private void handleActionInsert(Intent intent) {
        final Uri uri = intent.getData();
        final ArrayList<ContentValues> values = intent.getParcelableArrayListExtra(EXTRA_VALUES);
        Uri insertedUri = getContentResolver().insert(uri, values.get(0));
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (resultReceiver != null) {
            BasicCrudResultReceiver.sendInsertComplete(resultReceiver, insertedUri);
        }
    }

    private void handleActionBulkInsert(Intent intent) {
        final Uri uri = intent.getData();
        final ArrayList<ContentValues> valuesList = intent.getParcelableArrayListExtra(EXTRA_VALUES);
        ContentValues[] values = new ContentValues[valuesList.size()];
        valuesList.toArray(values);
        int rows = getContentResolver().bulkInsert(uri, values);
        sendRowCountResult(intent, BasicCrudResultReceiver.ACTION_BULK_INSERT_COMPLETE, rows);
    }

    private void handleActionUpdate(Intent intent) {
        final Uri uri = intent.getData();
        final ArrayList<ContentValues> values = intent.getParcelableArrayListExtra(EXTRA_VALUES);
        final String selection = intent.getStringExtra(EXTRA_SELECTION);
        final String[] selectionArgs = intent.getStringArrayExtra(EXTRA_SELECTION_ARGS);
        int rows = getContentResolver().update(uri, values.get(0), selection, selectionArgs);
        sendRowCountResult(intent, BasicCrudResultReceiver.ACTION_UPDATE_COMPLETE, rows);
    }

    private void handleActionDelete(Intent intent) {
        final Uri uri = intent.getData();
        final String selection = intent.getStringExtra(EXTRA_SELECTION);
        final String[] selectionArgs = intent.getStringArrayExtra(EXTRA_SELECTION_ARGS);
        int rows = getContentResolver().delete(uri, selection, selectionArgs);
        sendRowCountResult(intent, BasicCrudResultReceiver.ACTION_DELETE_COMPLETE, rows);
    }

    private static void sendRowCountResult(Intent intent, int action, int rowCount) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (resultReceiver != null) {
            BasicCrudResultReceiver.sendRowCount(resultReceiver, action, rowCount);
        }
    }

    /**
     * Helper class used to build an intent for starting the BasicCRUDIntentService.
     */
    public static class IntentBuilder {
        private final Intent intent;
        private ArrayList<ContentValues> valuesList;
        private long id = -1;

        /**
         * Create a new intent builder
         *
         * @param context Context used to build the Intent
         */
        public IntentBuilder(@NonNull Context context) {
            intent = new Intent(context, BasicCRUDIntentService.class);
        }

        /**
         * Indicate want to perform an insert operation with the service. Only one "for"
         * operation may be provided per Intent
         *
         * @param uri The URI for the provider/table you wish to insert into
         * @return this intent builder
         */
        public IntentBuilder forInsert(@NonNull Uri uri) {
            setActionAndUri(ACTION_INSERT, uri);
            return this;
        }

        /**
         * Indicate want to perform a bulk insert operation with the service. Only one "for"
         * operation may be provided per Intent
         *
         * @param uri The URI for the provider/table you wish to insert into
         * @return this intent builder
         */
        public IntentBuilder forBulkInsert(@NonNull Uri uri) {
            setActionAndUri(ACTION_BULK_INSERT, uri);
            return this;
        }

        /**
         * Indicate want to perform an update operation with the service. Only one "for"
         * operation may be provided per Intent
         *
         * @param uri The URI for the provider/table you wish to update
         * @return this intent builder
         */
        public IntentBuilder forUpdate(@NonNull Uri uri) {
            setActionAndUri(ACTION_UPDATE, uri);
            return this;
        }

        /**
         * Indicate want to perform a delete operation with the service. Only one "for"
         * operation may be provided per Intent
         *
         * @param uri The URI for the provider/table you wish to delete from
         * @return this intent builder
         */
        public IntentBuilder forDelete(@NonNull Uri uri) {
            setActionAndUri(ACTION_DELETE, uri);
            return this;
        }

        /**
         * Performs the operation where matches a specific row of data by id. Must not use
         * with whereSelection
         *
         * @param id The id associated with the row in the database
         * @return this intent builder
         */
        public IntentBuilder whereMatchesId(long id) {
            if (intent.hasExtra(EXTRA_SELECTION)) {
                throw new IllegalStateException("Only call one of whereMatchesId and whereSelection");
            }
            this.id = id;
            return this;
        }

        /**
         * Performs the operation where matches a specific selection with optional arguments. Must
         * not use with whereMatchesId
         *
         * @param selection     The selection clause for the update or delete
         * @param selectionArgs Optional arguments to bind to the selection. Note, best practice
         *                      is to use binding to avoid sql injection.
         * @return this intent builder
         */
        public IntentBuilder whereSelection(String selection, String[] selectionArgs) {
            if (id != -1) {
                throw new IllegalStateException("Only call one of whereMatchesId and whereSelection");
            }
            intent.putExtra(EXTRA_SELECTION, selection);
            intent.putExtra(EXTRA_SELECTION_ARGS, selectionArgs);
            return this;
        }

        /**
         * Provide a single row of values for the insert, or update operation
         *
         * @param values The content values indicating the columns/value pairs for the operation
         * @return this intent builder
         */
        public IntentBuilder usingValues(@NonNull ContentValues values) {
            valuesList = new ArrayList<>(1);
            valuesList.add(values);
            return this;
        }

        /**
         * Provide multiple rows of values for the bulk insert operation via an array
         *
         * @param values The content values array indicating the columns/value pairs for the operation
         * @return this intent builder
         */
        public IntentBuilder usingValues(@NonNull ContentValues[] values) {
            valuesList = new ArrayList<>(values.length);
            Collections.addAll(valuesList, values);
            return this;
        }

        /**
         * Provide multiple rows of values for the bulk insert operation via an ArrayList
         *
         * @param values The content values array indicating the columns/value pairs for the operation
         * @return this intent builder
         */
        public IntentBuilder usingValues(@NonNull ArrayList<ContentValues> values) {
            valuesList = new ArrayList<>(values);
            return this;
        }

        /**
         * Provide the result receiver for the operation. This will serve as the callback mechanism
         * for the result of the operation.
         *
         * @param resultReceiver The result receiver to use for callbacks
         * @return this intent builder
         * @see BasicCrudResultReceiver
         */
        public IntentBuilder setResultReceiver(@NonNull BasicCrudResultReceiver resultReceiver) {
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            return this;
        }

        /**
         * Build and validate the intent.
         *
         * @return The new intent to use to invoke the BasicCRUDIntentService
         * @see BasicCRUDIntentService
         */
        public Intent build() {
            String action = intent.getAction();

            if (action == null) {
                throw new IllegalStateException("Must call one of forInsert(), forBulkInsert(), forUpdate(), or forDelete()");
            }

            if (valuesList != null && !valuesList.isEmpty()) {
                intent.putExtra(EXTRA_VALUES, valuesList);

                if (valuesList.size() > 1 && !ACTION_BULK_INSERT.equals(action)) {
                    throw new IllegalStateException("Multiple rows of data provided to insert/update operation");
                }
                else if (ACTION_DELETE.equals(action)) {
                    throw new IllegalStateException("Providing content values for delete operation");
                }
            }
            else if (!ACTION_DELETE.equals(action)) {
                throw new IllegalStateException("Must provide ContentValues for insert, bulk insert or update");
            }

            if (id > 0) {
                Uri uri = intent.getData();
                intent.setData(ContentUris.withAppendedId(uri, id));
            }

            return intent;
        }

        private void setActionAndUri(String action, Uri uri) {
            if (intent.getAction() != null) {
                throw new IllegalStateException("Only call one of forInsert(), forBulkInsert(), forUpdate(), or forDelete()");
            }
            intent.setAction(action);
            intent.setData(uri);
        }
    }
}
