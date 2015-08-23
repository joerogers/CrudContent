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

package com.example.crudcontent.utils;

import android.content.ContentValues;

import com.example.crudcontent.provider.UserContract;

/**
 * Helper to build sample data for testing
 */
public class DataUtilities {

    public static ContentValues createUser1() {
        ContentValues values = new ContentValues();
        values.put(UserContract.Columns.NAME, "John Doe");
        values.put(UserContract.Columns.EMAIL, "john@example.com");
        return values;
    }

    public static ContentValues createUser2() {
        ContentValues values = new ContentValues();
        values.put(UserContract.Columns.NAME, "Jane Doe");
        values.put(UserContract.Columns.EMAIL, "jane@example.com");
        return values;
    }

    public static ContentValues editUser1() {
        ContentValues values = new ContentValues();
        values.put(UserContract.Columns.NAME, "John Doe");
        values.put(UserContract.Columns.EMAIL, "jdoe@example.com");
        return values;
    }

    public static ContentValues[] createBulkUsers() {
        ContentValues[] valuesArray = new ContentValues[2];
        valuesArray[0] = createUser1();
        valuesArray[1] = createUser2();
        return valuesArray;
    }

    public static ContentValues[] mergeValues(ContentValues[] targetArray, ContentValues source) {
        for (ContentValues target : targetArray) {
            mergeValues(target, source);
        }
        return targetArray;
    }

    public static ContentValues mergeValues(ContentValues target, ContentValues source) {
        target.putAll(source);
        return target;
    }
}
