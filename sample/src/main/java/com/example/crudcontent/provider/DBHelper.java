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

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.example.crudcontent.BuildConfig;

public class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "CrudSample.db";


    // If you are not using a ContentProvider to access your database or use
    // multiple ContentProviders, you should use a singleton to because
    // database is thread safe. The singleton for the helper allows the
    // app to have better throughput especially if there are multiple tables
    // or independent reads/writes
    private static DBHelper helper;

    public synchronized static DBHelper getInstance(Context context) {
        // Only include for debug builds, see if the context is an "android.test" context.
        // if so return a new instance every time.
        if (BuildConfig.DEBUG && context.getClass().getName().contains("android.test")) {
            // do not use application context for "tests" as the context only lives for the
            // duration of the test.
            return new DBHelper(context);
        }

        if (helper == null) {
            // Using application context since if in a singleton, the helper likely will
            // outlive the activity, asyncTask etc that starts it.
            helper = new DBHelper(context.getApplicationContext());
        }
        return helper;
    }

    // If you where using a singleton, this constructor should be made private
    // However, planning to use in a ContentProvider next week so it is public
    private DBHelper(Context context) {
        // the "null" indicates we want to use the default cursor factory
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // Write ahead logging allows multiple threads access the database
        // at same time which improves performance. Essentially a writer will no
        // longer block a reader accessing the database at the same time.
        // This method configures it for the active or next instance.
        // It can also be used to toggle it off.
        //
        // The BasicCRUDProvider will create non-exclusive mode transactions automatically
        // as required by
        // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#enableWriteAheadLogging()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // By default foreign key constraints are NOT enforced. Therefore you should turn
        // them on if you actually want them to help you debug schema problems.
        //
        // Note: if you do add a foreign key, you should also create an index for
        // each one as it should help with performance.
        //
        // If you don't use foreign keys, you do not need this method.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // This is where you create the tables for the database. Should create in
        // dependency order. Since city has a FK to the state table, create state first.
        db.execSQL(StateContract.CREATE_TABLE);
        db.execSQL(CityContract.CREATE_TABLE);
        db.execSQL(CityContract.CREATE_STATE_ID_FK_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Not really needed for a sample app, however, in a production app, this is where
        // you should add logic to "upgrade" the database as needed.
    }
}
