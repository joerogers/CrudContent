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

public final class CityContract {
    // Hide constructor to prevent creation.
    private CityContract() {
    }

    public interface Columns extends BaseColumns {
        // _ID provided by base columns
        String STATE_ID = "state_id";
        String STATE_ABBREVIATION = "state_abbreviation";
        String NAME = "name";
        String IS_CAPITAL = "is_capital";
        String DATE_VISITED = "date_visited";
        String NOTES = "notes";
    }

    public static final long NO_CITY_ID = -1;

    /* package */ static final String TABLE = "city";

    // The URI starts with "content" to indicate it will come
    // from a content provider. Also references the Authority
    // Will have final form of: content://com.example.crudcontent.provider.SampleProvider/city
    public static final Uri URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SampleProvider.AUTHORITY)
            .appendPath(TABLE)
            .build();


    // Database statements
    /* package */ static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " ( " +
                    BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                    Columns.STATE_ID + " INTEGER NOT NULL, " +
                    Columns.STATE_ABBREVIATION + " TEXT NOT NULL, " +
                    Columns.NAME + " TEXT NOT NULL, " +
                    Columns.IS_CAPITAL + " INTEGER NOT NULL DEFAULT 0, " +
                    Columns.DATE_VISITED + " INTEGER NOT NULL, " +
                    Columns.NOTES + " TEXT NULL, " +

                    // Foreign key to state table. Will purge all city entries if state deleted. However, that is not likely in this app.
                    "FOREIGN KEY(" + Columns.STATE_ID + ") REFERENCES " + StateContract.TABLE + "(" + BaseColumns._ID + ") ON DELETE CASCADE " +
                    " )";

    // Adding an index for the FK. This helps performance and prevent global table locking.
    /* package */ static final String STATE_ID_FK_INDEX = "STATE_ID_FK_IDX";

    /* package */ static final String CREATE_STATE_ID_FK_INDEX =
            "CREATE INDEX IF NOT EXISTS " + STATE_ID_FK_INDEX + " ON " + TABLE +
                    " (" + Columns.STATE_ID + ")";

}
