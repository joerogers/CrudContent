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

import com.example.crudtester.provider.RollbackContract;

/**
 * Helper to build sample data for testing
 */
public class DataUtilities {

    public static ContentValues insertUser1() {
        ContentValues values = new ContentValues();
        values.put(RollbackContract.Columns.DATA1, "john@example.com");
        values.put(RollbackContract.Columns.DATA2, "John Doe");
        return values;
    }

    public static ContentValues insertUser2() {
        ContentValues values = new ContentValues();
        values.put(RollbackContract.Columns.DATA1, "jane@example.com");
        values.put(RollbackContract.Columns.DATA2, "Jane Doe");
        return values;
    }

    public static ContentValues[] insertBulkUsers() {
        ContentValues[] valuesArray = new ContentValues[2];
        valuesArray[0] = insertUser1();
        valuesArray[1] = insertUser2();
        return valuesArray;
    }

    public static ContentValues updateUser1() {
        ContentValues values = new ContentValues();
        values.put(RollbackContract.Columns.DATA1, "jdoe@example.com");
        values.put(RollbackContract.Columns.DATA2, "John Doe");
        return values;
    }

    public static ContentValues updateDataNoConflict() {
        ContentValues values = new ContentValues();
        values.put(RollbackContract.Columns.DATA2, "Tester Doe");
        return values;
    }

    public static ContentValues updateDataPossibleConflict() {
        ContentValues values = new ContentValues();
        values.put(RollbackContract.Columns.DATA1, "john@example.com");
        values.put(RollbackContract.Columns.DATA2, "Tester Doe");
        return values;
    }


    public static ContentValues[] mergeValues(ContentValues[] targetArray, ContentValues source) {
        for (ContentValues target : targetArray) {
            target.putAll(source);
        }
        return targetArray;
    }
}
