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

import java.util.ArrayList;
import java.util.Collections;


/**
 * Intent service for running common DB operations in the background. The service
 * will broadcast complete actions using the LocalBroadcastManager after each operation.
 */
@SuppressWarnings("unused")
public class BasicCRUDIntentService extends IntentService {

    private static final String ACTION_INSERT = BasicCRUDIntentService.class.getName() + ".action.insert";
    private static final String ACTION_BULK_INSERT = BasicCRUDIntentService.class.getName() + ".action.bulkInsert";
    private static final String ACTION_UPDATE = BasicCRUDIntentService.class.getName() + ".action.update";
    private static final String ACTION_DELETE = BasicCRUDIntentService.class.getName() + ".action.delete";

    private static final String EXTRA_VALUES = BasicCRUDIntentService.class.getName() + ".values";
    private static final String EXTRA_SELECTION = BasicCRUDIntentService.class.getName() + ".selection";
    private static final String EXTRA_SELECTION_ARGS = BasicCRUDIntentService.class.getName() + ".selectionArgs";
    private static final String EXTRA_RESULT_RECEIVER = BasicCRUDIntentService.class.getName() + ".resultReceiver";


    public BasicCRUDIntentService() {
        super("BasicCRUDIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        if (ACTION_INSERT.equals(action)) {
            handleActionInsert(intent);
        }
        else if (ACTION_UPDATE.equals(action)) {
            handleActionUpdate(intent);
        }
        else if (ACTION_DELETE.equals(action)) {
            handleActionDelete(intent);
        }
        else if (ACTION_BULK_INSERT.equals(action)) {
            handleActionBulkInsert(intent);
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

    public static class IntentBuilder {
        private final Intent intent;
        private ArrayList<ContentValues> valuesList;
        private long id = -1;

        public IntentBuilder(@NonNull Context context) {
            intent = new Intent(context, BasicCRUDIntentService.class);
        }

        public IntentBuilder forInsert(@NonNull Uri uri) {
            setActionAndUri(ACTION_INSERT, uri);
            return this;
        }

        public IntentBuilder forBulkInsert(@NonNull Uri uri) {
            setActionAndUri(ACTION_BULK_INSERT, uri);
            return this;
        }

        public IntentBuilder forUpdate(@NonNull Uri uri) {
            setActionAndUri(ACTION_UPDATE, uri);
            return this;
        }

        public IntentBuilder forDelete(@NonNull Uri uri) {
            setActionAndUri(ACTION_DELETE, uri);
            return this;
        }

        public IntentBuilder whereMatchesId(long id) {
            this.id = id;
            return this;
        }

        public IntentBuilder whereSelection(String selection, String[] selectionArgs) {
            intent.putExtra(EXTRA_SELECTION, selection);
            intent.putExtra(EXTRA_SELECTION_ARGS, selectionArgs);
            return this;
        }

        public IntentBuilder usingValues(@NonNull ContentValues values) {
            valuesList = new ArrayList<>(1);
            valuesList.add(values);
            return this;
        }

        public IntentBuilder usingValues(@NonNull ContentValues[] values) {
            valuesList = new ArrayList<>(values.length);
            Collections.addAll(valuesList, values);
            return this;
        }

        public IntentBuilder usingValues(@NonNull ArrayList<ContentValues> values) {
            valuesList = new ArrayList<>(values);
            return this;
        }

        public IntentBuilder setReceiver(@NonNull BasicCrudResultReceiver resultReceiver) {
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            return this;
        }

        public Intent build() {
            String action = intent.getAction();

            if (action == null) {
                throw new IllegalStateException("Must call one of forInsert(), forBulkInsert(), forUpdate(), or forDelete()");
            }

            if (valuesList != null && !valuesList.isEmpty()) {
                intent.putExtra(EXTRA_VALUES, valuesList);
            }
            else if (!ACTION_DELETE.equals(action)) {
                throw new IllegalStateException("Must provide ContentValues for insert, bulk insert or update");
            }

            if (id > 0) {
                Uri uri = intent.getData();
                intent.setData(ContentUris.withAppendedId(uri, id));
                intent.removeExtra(EXTRA_SELECTION);
                intent.removeExtra(EXTRA_SELECTION_ARGS);
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
