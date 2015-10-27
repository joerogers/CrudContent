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

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class BasicCrudResultReceiver extends ResultReceiver {

    /* package */ static final int ACTION_INSERT_COMPLETE = 1;
    /* package */ static final int ACTION_BULK_INSERT_COMPLETE = 2;
    /* package */ static final int ACTION_UPDATE_COMPLETE = 3;
    /* package */ static final int ACTION_DELETE_COMPLETE = 4;

    private static final String EXTRA_DATA = "BasicCrudResultReceiver.extra.data";

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
    static final void sendRowCount(ResultReceiver resultReceiver, int resultCode, int rowCount) {
        Bundle resultData = new Bundle();
        resultData.putInt(EXTRA_DATA, rowCount);
        resultReceiver.send(resultCode, resultData);
    }

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

    protected void onInsertComplete(Uri uri) {

    }

    protected void onBulkInsertComplete(int rows) {

    }

    protected void onUpdateComplete(int rows) {

    }

    protected void onDeleteComplete(int rows) {

    }
}
