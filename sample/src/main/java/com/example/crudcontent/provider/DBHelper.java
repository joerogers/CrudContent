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


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.example.crudcontent.BuildConfig;


public class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    /* package */ static final String DATABASE_NAME = "Test.db";

    private static DBHelper dbHelper;

    public static synchronized DBHelper getInstance(Context context) {
        // If under test, just return a new helper using the provided context.
        if (BuildConfig.DEBUG && context.getClass().getName().contains("android.test")) {
            return new DBHelper(context);
        }

        if (dbHelper == null) {
            // Use application context because this is a singleton and do
            // not want it to reference a "specific" context"
            dbHelper = new DBHelper(context.getApplicationContext());
        }

        return dbHelper;
    }

    private DBHelper(Context context) {
        // Using application context since if in a singleton, the helper likely will
        // outlive the activity, asyncTask, provider etc that starts it.

        // the "null" indicates we want to use the default cursor factory
        super(context, DATABASE_NAME, null, DATABASE_VERSION);


        // Write ahead logging allows multiple threads access the database
        // at same time which improves performance. Essentially a writer will no
        // longer block a reader accessing the database at the same time.
        // This method configures it for the active or next instance.
        // The method can also be used to toggle it off. Only supported via method call on
        // api 16 or higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(UserContract.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No implementation for sample. However, if updating your database version, you
        // should implement the schema changes here.
    }
}
