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

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.forkingcode.crudcontent.provider.BasicCRUDProvider;


public class SampleBasicCRUDProvider extends BasicCRUDProvider {


    public SampleBasicCRUDProvider() {
        super(UserContract.AUTHORITY);
    }

    @Override
    protected int getConflictAlgorithm(@NonNull String table) {
        // All tables use replace algorithm, however if you need to have different algorithms
        // per table, the table name parsed from the URI is provided.
        return SQLiteDatabase.CONFLICT_REPLACE;
    }

    @Override
    protected SQLiteOpenHelper getDbHelper() {
        return DBHelper.getInstance(getContext());
    }
}
