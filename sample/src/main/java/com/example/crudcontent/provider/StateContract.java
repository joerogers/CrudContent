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
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;

public final class StateContract {

    // Hide constructor to prevent creation.
    private StateContract() {
    }

    public interface Columns extends BaseColumns {
        // _ID provided by base columns
        String NAME = "name";
        String ABBREVIATION = "abbreviation";
    }

    /* package */ static final String TABLE = "state";

    // The URI starts with "content" to indicate it will come
    // from a content provider. Also references the Authority
    // Will have final form of: content://com.example.crudcontent.provider.SampleProvider/state
    public static final Uri URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SampleProvider.AUTHORITY)
            .appendPath(TABLE)
            .build();

    // Database statements
    /* package */ static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " ( " +
                    BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                    Columns.NAME + " TEXT UNIQUE NOT NULL, " +
                    Columns.ABBREVIATION + " TEXT NOT NULL " +
                    " )";

    /**
     * default state values. In real world this should come from a file or the
     * internet. Just a subset of actual states for this example
     *
     * @return state values array
     */
    public static ContentValues[] buildStateValuesArray() {
        return new ContentValues[]{
                buildStateValues("Washington", "WA"),
                buildStateValues("Oregon", "OR"),
                buildStateValues("California", "CA"),
                buildStateValues("Nevada", "NV"),
                buildStateValues("Hawaii", "HI"),
                buildStateValues("Alaska", "AK"),
                buildStateValues("Texas", "TX"),
                buildStateValues("New York", "NY")
        };
    }

    private static ContentValues buildStateValues(String name, String abbreviation) {
        ContentValues values = new ContentValues();
        values.put(StateContract.Columns.NAME, name);
        values.put(StateContract.Columns.ABBREVIATION, abbreviation);
        return values;
    }
}
