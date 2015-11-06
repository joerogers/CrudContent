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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Utilities to help validate a cursor against expected content values
 */
public class CursorUtilities {

    public static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues, long id) {
        assertNotNull("Null cursor returned." + error, valueCursor);
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateId(error, valueCursor, id);
        validateCurrentRecord(error, valueCursor, expectedValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned in cursor. " + error, valueCursor.moveToNext());
    }

    public static void validateCursor(String error, String column, Cursor valueCursor, ContentValues[] expectedValues) {
        assertNotNull("Null cursor returned." + error, valueCursor);
        assertEquals("Row count mismatched." + error, expectedValues.length, valueCursor.getCount());
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());

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
            assertTrue("Row not found", rowFound);
        } while (valueCursor.moveToNext());
    }

    public static void ensureCursorsMatchExcludingId(String error,
                                                     @NonNull Cursor cursor1,
                                                     @NonNull Cursor cursor2) {
        assertEquals("Row count mismatched." + error, cursor1.getCount(), cursor2.getCount());
        assertEquals("Column count mismatched" + error, cursor1.getColumnCount(), cursor2.getColumnCount());
        assertTrue("Empty cursor returned. " + error, cursor1.moveToFirst());
        assertTrue("Empty cursor returned. " + error, cursor2.moveToFirst());

        int count = cursor1.getColumnCount();
        for (int i = 0; i < count; ++i) {
            String columnName = cursor1.getColumnName(i);
            String value1 = cursor1.getString(i);
            int index = cursor2.getColumnIndexOrThrow(columnName);
            String value2 = cursor2.getString(index);

            if (BaseColumns._ID.equals(columnName)) {
                assertFalse(value1.equals(value2));
            }
            else {
                assertEquals(value1, value2);
            }
        }
    }

    private static void validateId(String error, Cursor valueCursor, long id) {
        int idx = valueCursor.getColumnIndex(BaseColumns._ID);
        assertFalse("Column '" + BaseColumns._ID + "' not found. " + error, idx == -1);
        assertEquals("Id does not match " + error, id, valueCursor.getLong(idx));
    }

    private static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            if (valueCursor.getType(idx) != Cursor.FIELD_TYPE_FLOAT) {
                assertEquals("Value '" + entry.getValue().toString() +
                        "' did not match the expected value '" +
                        expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
            }
            else {
                assertEquals("Value '" + entry.getValue().toString() +
                        "' did not match the expected value '" +
                        expectedValue + "'. " + error, expectedValue, Double.toString(valueCursor.getDouble(idx)));
            }
        }
    }
}