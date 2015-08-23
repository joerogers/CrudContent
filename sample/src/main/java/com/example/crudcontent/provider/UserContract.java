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

package com.example.crudcontent.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.example.crudcontent.BuildConfig;

/**
 * Example contract for a simple user table.
 */
public final class UserContract {

    /* package */ static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.SampleBasicCRUDProvider";

    /* package */ static final String TABLE = "user_info";

    public static final Uri URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath(TABLE)
            .build();

    public interface Columns extends BaseColumns {
        String NAME = "name";
        String EMAIL = "email";
    }

    public static final long NO_USER_ID = -1;

    /* package */ static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " ( " +
                    BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                    Columns.EMAIL + " TEXT NOT NULL UNIQUE, " +
                    Columns.NAME + " TEXT NOT NULL " +
                    ")";
}
