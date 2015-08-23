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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Intent service for running common DB operations in the background. The service
 * will broadcast complete actions using the LocalBroadcastManager after each operation.
 */
@SuppressWarnings("unused")
public class BasicCRUDIntentService extends IntentService {


    public static final String ACTION_INSERT_COMPLETE = BasicCRUDIntentService.class.getName() + ".action.insertComplete";
    public static final String ACTION_BULK_INSERT_COMPLETE = BasicCRUDIntentService.class.getName() + ".action.bulkInsertComplete";
    public static final String ACTION_UPDATE_COMPLETE = BasicCRUDIntentService.class.getName() + ".action.updateComplete";
    public static final String ACTION_DELETE_COMPLETE = BasicCRUDIntentService.class.getName() + ".action.deleteComplete";

    public static final String EXTRA_URI = BasicCRUDIntentService.class.getName() + ".extra.uri";
    public static final String EXTRA_ROWS = BasicCRUDIntentService.class.getName() + ".extra.rows";

    private static final String ACTION_INSERT = BasicCRUDIntentService.class.getName() + ".action.insert";
    private static final String ACTION_BULK_INSERT = BasicCRUDIntentService.class.getName() + ".action.bulkInsert";
    private static final String ACTION_UPDATE = BasicCRUDIntentService.class.getName() + ".action.update";
    private static final String ACTION_DELETE = BasicCRUDIntentService.class.getName() + ".action.delete";

    private static final String EXTRA_VALUES = BasicCRUDIntentService.class.getName() + ".values";
    private static final String EXTRA_SELECTION = BasicCRUDIntentService.class.getName() + ".selection";
    private static final String EXTRA_SELECTION_ARGS = BasicCRUDIntentService.class.getName() + ".selectionArgs";


    public static void startServiceForInsert(@NonNull Context context, @NonNull Uri uri, @NonNull ContentValues values) {
        startService(context, ACTION_INSERT, uri, values, null, null);
    }

    public static void startServiceForBulkInsert(@NonNull Context context, @NonNull Uri uri, @NonNull ContentValues[] values) {
        ArrayList<ContentValues> list = new ArrayList<>(values.length);
        Collections.addAll(list, values);
        startServiceForBulkInsert(context, uri, list);
    }

    public static void startServiceForBulkInsert(@NonNull Context context, @NonNull Uri uri, @NonNull ArrayList<ContentValues> values) {
        Intent intent = new Intent(context, BasicCRUDIntentService.class);
        intent.setAction(ACTION_BULK_INSERT);
        intent.setData(uri);
        intent.putExtra(EXTRA_VALUES, values);
        context.startService(intent);
    }

    public static void startServiceForUpdate(@NonNull Context context, @NonNull Uri uri, long id, @NonNull ContentValues values) {
        startServiceForUpdate(context, ContentUris.withAppendedId(uri, id), values, null, null);
    }

    public static void startServiceForUpdate(@NonNull Context context, @NonNull Uri uri, @NonNull ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        startService(context, ACTION_UPDATE, uri, values, selection, selectionArgs);
    }

    public static void startServiceForDelete(@NonNull Context context, @NonNull Uri uri, long id) {
        startServiceForDelete(context, ContentUris.withAppendedId(uri, id), null, null);
    }

    public static void startServiceForDelete(@NonNull Context context, @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        startService(context, ACTION_DELETE, uri, null, selection, selectionArgs);
    }

    private static void startService(Context context, String action, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Intent intent = new Intent(context, BasicCRUDIntentService.class);
        intent.setAction(action);
        intent.setData(uri);
        intent.putExtra(EXTRA_VALUES, values);
        intent.putExtra(EXTRA_SELECTION, selection);
        intent.putExtra(EXTRA_SELECTION_ARGS, selectionArgs);
        context.startService(intent);
    }

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
        final ContentValues values = intent.getParcelableExtra(EXTRA_VALUES);
        Uri insertedUri = getContentResolver().insert(uri, values);
        sendInsertBroadcast(insertedUri);
    }

    private void handleActionBulkInsert(Intent intent) {
        final Uri uri = intent.getData();
        final ArrayList<ContentValues> valuesList = intent.getParcelableArrayListExtra(EXTRA_VALUES);
        ContentValues[] values = new ContentValues[valuesList.size()];
        valuesList.toArray(values);
        int rows = getContentResolver().bulkInsert(uri, values);
        sendRowsBroadcast(ACTION_BULK_INSERT_COMPLETE, rows);
    }

    private void handleActionUpdate(Intent intent) {
        final Uri uri = intent.getData();
        final ContentValues values = intent.getParcelableExtra(EXTRA_VALUES);
        final String selection = intent.getStringExtra(EXTRA_SELECTION);
        final String[] selectionArgs = intent.getStringArrayExtra(EXTRA_SELECTION_ARGS);
        int rows = getContentResolver().update(uri, values, selection, selectionArgs);
        sendRowsBroadcast(ACTION_UPDATE_COMPLETE, rows);
    }

    private void handleActionDelete(Intent intent) {
        final Uri uri = intent.getData();
        final String selection = intent.getStringExtra(EXTRA_SELECTION);
        final String[] selectionArgs = intent.getStringArrayExtra(EXTRA_SELECTION_ARGS);
        int rows = getContentResolver().delete(uri, selection, selectionArgs);
        sendRowsBroadcast(ACTION_DELETE_COMPLETE, rows);
    }

    private void sendInsertBroadcast(Uri uri) {
        Intent intent = new Intent(ACTION_INSERT_COMPLETE);
        intent.putExtra(EXTRA_URI, uri);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendRowsBroadcast(String action, int rows) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ROWS, rows);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
