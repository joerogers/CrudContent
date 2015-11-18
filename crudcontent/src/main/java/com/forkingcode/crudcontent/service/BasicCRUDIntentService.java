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

        // In case service is killed, try the last operation again.
        setIntentRedelivery(true);
    }

    /**
     * Handler for the service intent. It processes the intent data and performs
     * the requested database operation. Since database writes are blocking, this
     * also queues the operations in the order requested. Ie if calling code creates
     * and object and then updates it, the intent service will execute the create
     * intent prior to executing the update intent.
     *
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    @Override
    protected final void onHandleIntent(Intent intent) {
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
     * Indicate want to perform an insert operation with the service. Only one "for"
     * operation may be provided per Intent
     *
     * @param context The context used to build the intent
     * @param uri     The URI for the provider/table you wish to insert into
     * @return this intent builder
     */
    public static IntentBuilder performInsert(@NonNull Context context, @NonNull Uri uri) {
        IntentBuilder intentBuilder = new IntentBuilder(context);
        intentBuilder.setActionAndUri(ACTION_INSERT, uri);
        return intentBuilder;
    }

    /**
     * Indicate want to perform a bulk insert operation with the service. Only one "for"
     * operation may be provided per Intent
     *
     * @param context The context used to build the intent
     * @param uri The URI for the provider/table you wish to insert data
     * @return this intent builder
     */
    public static IntentBuilder performBulkInsert(@NonNull Context context, @NonNull Uri uri) {
        IntentBuilder intentBuilder = new IntentBuilder(context);
        intentBuilder.setActionAndUri(ACTION_BULK_INSERT, uri);
        intentBuilder.isBulkOperation = true;
        return intentBuilder;
    }

    /**
     * Indicate want to perform an update operation with the service. Only one "for"
     * operation may be provided per Intent
     *
     * @param context The context used to build the intent
     * @param uri The URI for the provider/table you wish to update
     * @return this intent builder
     */
    public static IntentBuilder performUpdate(@NonNull Context context, @NonNull Uri uri) {
        IntentBuilder intentBuilder = new IntentBuilder(context);
        intentBuilder.setActionAndUri(ACTION_UPDATE, uri);
        return intentBuilder;
    }

    /**
     * Indicate want to perform a delete operation with the service. Only one "for"
     * operation may be provided per Intent
     *
     * @param context The context used to build the intent
     * @param uri The URI for the provider/table you wish to delete from
     * @return this intent builder
     */
    public static IntentBuilder performDelete(@NonNull Context context, @NonNull Uri uri) {
        IntentBuilder intentBuilder = new IntentBuilder(context);
        intentBuilder.setActionAndUri(ACTION_DELETE, uri);
        intentBuilder.isDeleteOperation = true;
        return intentBuilder;
    }

    /**
     * Helper class used to build an intent for starting the BasicCRUDIntentService.
     */
    public static class IntentBuilder {
        private final Intent intent;
        private final Context context;
        private ArrayList<ContentValues> contentValuesList;
        private long id = -1;
        private boolean isBulkOperation = false;
        private boolean isDeleteOperation = false;

        /**
         * Create a new intent builder, called by perform{type} static methods.
         *
         * @param context Context used to build the Intent
         */
        protected IntentBuilder(@NonNull Context context) {
            this.context = context;
            intent = new Intent(context, BasicCRUDIntentService.class);
        }

        /**
         * Performs the operation where matches a specific row of data by id. Must not use
         * with whereSelection.
         *
         * <p>If the Uri used to start the builder already contains the item id, calling this
         * will cause the id to be appended twice.
         *
         * @param id The id associated with the row in the database
         * @return this intent builder
         */
        public IntentBuilder whereMatchesId(long id) {
            if (intent.hasExtra(EXTRA_SELECTION)) {
                throw new IllegalStateException("Only call one of whereMatchesId and whereMatchesSelection");
            }
            this.id = id;
            return this;
        }

        /**
         * Performs the operation where matches a specific selection with optional arguments. Must
         * not use with whereMatchesId
         *
         * <p>If the Uri used to start the builder already contains the item id, calling this
         * method will have no effect on the operation as the selection will be ignored.
         *
         * @param selection     The selection clause for the update or delete
         * @param selectionArgs Optional arguments to bind to the selection. Note, best practice
         *                      is to use binding to avoid sql injection.
         * @return this intent builder
         */
        public IntentBuilder whereMatchesSelection(String selection, String[] selectionArgs) {
            if (id != -1) {
                throw new IllegalStateException("Only call one of whereMatchesId and whereMatchesSelection");
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
         * @throws IllegalStateException if providing values for a delete operation, or providing multiple row
         * for a plain insert/update operation
         */
        public IntentBuilder usingValues(@NonNull ContentValues values) {
            ArrayList<ContentValues> valuesList = new ArrayList<>(1);
            valuesList.add(values);
            return usingValues(valuesList);
        }

        /**
         * Provide multiple rows of values for the bulk insert operation via an array
         *
         * @param values The content values array indicating the columns/value pairs for the operation
         * @return this intent builder
         * @throws IllegalStateException if providing values for a delete operation, or providing multiple row
         * for a plain insert/update operation
         */
        public IntentBuilder usingValues(@NonNull ContentValues[] values) {
            ArrayList<ContentValues> valuesList = new ArrayList<>(values.length);
            Collections.addAll(valuesList, values);
            return usingValues(valuesList);
        }

        /**
         * Provide multiple rows of values for the bulk insert operation via an ArrayList
         *
         * @param values The content values array indicating the columns/value pairs for the operation
         * @return this intent builder
         * @throws IllegalStateException if providing values for a delete operation, or providing multiple row
         * for a plain insert/update operation
         */
        public IntentBuilder usingValues(@NonNull ArrayList<ContentValues> values) {
            if (isDeleteOperation) {
                throw new IllegalStateException("Providing content values for delete operation");
            }
            if (values.size() > 1 && !isBulkOperation) {
                throw new IllegalStateException("Multiple rows of data provided to insert/update operation");
            }
            contentValuesList = new ArrayList<>(values);
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
        public IntentBuilder resultReceiver(@NonNull BasicCrudResultReceiver resultReceiver) {
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            return this;
        }

        /**
         * Build the intent and start the service. The options provided will be validated
         * @throws IllegalStateException If the content values were not provided for
         * insert, bulk insert, or update.
         */
        public void start() {
            context.startService(buildIntent());
        }

        /**
         * Build and validate the intent.
         *
         * @return The new intent to use to invoke the BasicCRUDIntentService
         * @see BasicCRUDIntentService
         * @throws IllegalStateException If the content values were not provided for
         * insert, bulk insert, or update.
         */
        public Intent buildIntent() {
            if (contentValuesList != null && !contentValuesList.isEmpty()) {
                intent.putExtra(EXTRA_VALUES, contentValuesList);
            }
            else if (!isDeleteOperation) {
                throw new IllegalStateException("Must provide ContentValues for insert, bulk insert or update operations");
            }

            if (id > 0) {
                Uri uri = intent.getData();
                intent.setData(ContentUris.withAppendedId(uri, id));
            }

            return intent;
        }

        private void setActionAndUri(String action, Uri uri) {
            intent.setAction(action);
            intent.setData(uri);
        }
    }
}
