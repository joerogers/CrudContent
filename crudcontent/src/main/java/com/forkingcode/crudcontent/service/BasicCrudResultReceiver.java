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

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Wrapper around a standard ResultReceiver. It provides friendly methods to know
 * when an operation is complete.
 *
 * <p>This is called asynchronously. It is recommended that you do not implicitly
 * reference activities, views, etc via the receiver. If you need to access the activity,
 * it is recommended use a {@link java.lang.ref.WeakReference} to hold the activity or some
 * other mechanism such as a LocalBroadcastReceiver such that the receiver
 * doesn't have direct access to the activity.
 * The weak reference allows the activity to be destroyed even if the database operation is
 * ongoing. The code should no-op if the activity is null when the result arrives.
 * See the sample CityActivity for an example of how to do this.
 *
 * @see ResultReceiver
 * @see java.lang.ref.WeakReference
 * @see <a href="https://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager.html">LocalBroadcastReceiver</a>
 */
@SuppressLint("ParcelCreator")
public class BasicCrudResultReceiver extends ResultReceiver {

    /* package */ static final int ACTION_INSERT_COMPLETE = 1;
    /* package */ static final int ACTION_BULK_INSERT_COMPLETE = 2;
    /* package */ static final int ACTION_UPDATE_COMPLETE = 3;
    /* package */ static final int ACTION_DELETE_COMPLETE = 4;

    private static final String EXTRA_DATA = "BasicCrudResultReceiver.extra.data";

    /**
     * Constructor
     *
     * @param handler {@link #onInsertComplete(Uri)}, {@link #onBulkInsertComplete(int)},
     *                {@link #onUpdateComplete(int)}, or {@link #onDeleteComplete(int)}
     *                will be called from the thread running
     *                <var>handler</var> if given, or from an arbitrary thread if null.
     */
    public BasicCrudResultReceiver(Handler handler) {
        super(handler);
    }

    /* package */
    static void sendInsertComplete(ResultReceiver resultReceiver, Uri uri) {
        Bundle resultData = new Bundle();
        resultData.putParcelable(EXTRA_DATA, uri);
        resultReceiver.send(ACTION_INSERT_COMPLETE, resultData);
    }

    /* package */
    static void sendRowCount(ResultReceiver resultReceiver, int resultCode, int rowCount) {
        Bundle resultData = new Bundle();
        resultData.putInt(EXTRA_DATA, rowCount);
        resultReceiver.send(resultCode, resultData);
    }

    /**
     * Parses the result and calls the appropriate callback.
     *
     * @param resultCode code indicating type of result being sent
     * @param resultData bundle containing the values of the result.
     */
    @Override
    protected final void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ACTION_INSERT_COMPLETE:
                Uri uri = resultData.getParcelable(EXTRA_DATA);
                onInsertComplete(uri);
                break;
            case ACTION_BULK_INSERT_COMPLETE:
                onBulkInsertComplete(resultData.getInt(EXTRA_DATA, 0));
                break;
            case ACTION_UPDATE_COMPLETE:
                onUpdateComplete(resultData.getInt(EXTRA_DATA, 0));
                break;
            case ACTION_DELETE_COMPLETE:
                onDeleteComplete(resultData.getInt(EXTRA_DATA, 0));
                break;
        }
    }

    /**
     * Called for insert operations.
     *
     * @param uri The URI of the specific row that was inserted, null if insertion failed
     */
    @SuppressWarnings("UnusedParameters")
    protected void onInsertComplete(Uri uri) {

    }

    /**
     * Called for bulk insert operations.
     *
     * @param rows The number of rows successfully inserted.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onBulkInsertComplete(int rows) {

    }

    /**
     * Called for bulk insert operations.
     *
     * @param rows The number of rows successfully updated.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onUpdateComplete(int rows) {

    }

    /**
     * Called for bulk insert operations.
     *
     * @param rows The number of rows successfully deleted.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onDeleteComplete(int rows) {

    }
}
