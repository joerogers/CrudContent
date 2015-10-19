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

import android.database.sqlite.SQLiteOpenHelper;

import com.forkingcode.crudcontent.provider.BasicCRUDProvider;


public class SampleBasicCRUDProvider extends BasicCRUDProvider {


    /**
     * Must provide empty constructor in order for Android to instantiate the provider
     */
    public SampleBasicCRUDProvider() {
        super(UserContract.AUTHORITY);
    }

    @Override
    protected SQLiteOpenHelper getDbHelper() {
        return DBHelper.getInstance(getContext());
    }

    // Note the other method you may want to override is getConflictAlgorithm. By default
    // BasicCRUDProvider uses SQLiteDatabase.CONFLICT_NONE, however depending on the use case
    // CONFLICT_ABORT, CONFLICT_IGNORE, CONFLICT_REPLACE, or CONFLICT_FAIL may be more appropriate.
    // Note: CONFLICT_NONE uses the default for SQLite which is CONFLICT_ABORT

    // Here is a commented out variation, using conflict replace for all tables
    /*
    @Override
    protected int getConflictAlgorithm(@NonNull String table) {
        // All tables use replace algorithm, however if you need to have different algorithms
        // per table, the table name parsed from the URI is provided.
        return SQLiteDatabase.CONFLICT_REPLACE;
    }
    */
}
