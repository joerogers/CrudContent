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

package com.example.crudtester.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Utilities to help validate a cursor against expected content values
 */
public class CursorUtilities {

    public static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues, long id) {
        assertThat(valueCursor, is(notNullValue()));
        assertThat("Empty cursor returned. " + error, valueCursor.moveToFirst(), is(true));
        validateId(error, valueCursor, id);
        validateCurrentRecord(error, valueCursor, expectedValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertThat("Error: More than one record returned in cursor. " + error, valueCursor.moveToNext(), is(false));
    }

    public static void validateCursor(String error, String column, Cursor valueCursor, ContentValues[] expectedValues) {
        assertThat(valueCursor, is(notNullValue()));
        assertThat("Row count mismatched." + error, valueCursor.getCount(), is(expectedValues.length));
        assertThat("Empty cursor returned. " + error, valueCursor.moveToFirst(), is(true));

        int primaryIndex = valueCursor.getColumnIndex(column);

        do {
            boolean rowFound = false;
            for (ContentValues values : expectedValues) {
                if (values.getAsString(column).equals(valueCursor.getString(primaryIndex))) {
                    validateCurrentRecord(error, valueCursor, values);
                    rowFound = true;
                    break;
                }
            }
            assertThat("Row not found", rowFound, is(true));
        } while (valueCursor.moveToNext());
    }

    public static void ensureCursorsMatchExcludingId(String error,
                                                     @NonNull Cursor cursor1,
                                                     @NonNull Cursor cursor2) {
        assertThat("Row count mismatched." + error, cursor1.getCount(), is(cursor2.getCount()));
        assertThat("Column count mismatched" + error, cursor1.getColumnCount(), is(cursor2.getColumnCount()));
        assertThat("Empty cursor returned. " + error, cursor1.moveToFirst(), is(true));
        assertThat("Empty cursor returned. " + error, cursor2.moveToFirst(), is(true));

        int count = cursor1.getColumnCount();
        for (int i = 0; i < count; ++i) {
            String columnName = cursor1.getColumnName(i);
            String value1 = cursor1.getString(i);
            int index = cursor2.getColumnIndexOrThrow(columnName);
            String value2 = cursor2.getString(index);

            if (BaseColumns._ID.equals(columnName)) {
                assertThat(value1, is(not(value2)));
            }
            else {
                assertThat(value1, is(value2));
            }
        }
    }

    private static void validateId(String error, Cursor valueCursor, long id) {
        int idx = valueCursor.getColumnIndex(BaseColumns._ID);
        assertThat("Column '" + BaseColumns._ID + "' not found. " + error, idx, is(not(-1)));
        assertThat("Id does not match " + error, valueCursor.getLong(idx), is(id));
    }

    private static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertThat("Column '" + columnName + "' not found. " + error, idx, is(not(-1)));
            String expectedValue = entry.getValue().toString();
            if (valueCursor.getType(idx) != Cursor.FIELD_TYPE_FLOAT) {
                assertThat("Value '" + entry.getValue().toString() +
                        "' did not match the expected value '" +
                        expectedValue + "'. " + error, valueCursor.getString(idx), is(expectedValue));
            }
            else {
                assertThat("Value '" + entry.getValue().toString() +
                        "' did not match the expected value '" +
                        expectedValue + "'. " + error, Double.toString(valueCursor.getDouble(idx)), is(expectedValue));
            }
        }
    }
}
