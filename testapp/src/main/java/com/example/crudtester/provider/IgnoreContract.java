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

package com.example.crudtester.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract for ignore testing
 */
public final class IgnoreContract {

    /* package */ static final String TABLE = "ignore_table";

    public static final Uri URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(TestBasicCRUDProvider.AUTHORITY)
            .appendPath(TABLE)
            .build();

    public interface Columns extends BaseColumns {
        String DATA1 = "data1";
        String DATA2 = "data2";
    }

    public static final long NO_ROW_ID = -1;

    /* package */ static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " ( " +
                    BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                    Columns.DATA1 + " TEXT NOT NULL UNIQUE, " +
                    Columns.DATA2 + " TEXT NOT NULL " +
                    ")";
}
