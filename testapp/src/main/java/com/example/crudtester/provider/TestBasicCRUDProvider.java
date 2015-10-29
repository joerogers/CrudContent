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

package com.example.crudtester.provider;

import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.example.crudtester.BuildConfig;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;


public class TestBasicCRUDProvider extends BasicCRUDProvider {

    /* package */ static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.TestBasicCRUDProvider";

    /**
     * Must provide empty constructor in order for Android to instantiate the provider
     */
    public TestBasicCRUDProvider() {
        super(AUTHORITY);

        // activate logging for testing
        setLoggingEnabled(BuildConfig.DEBUG);
    }

    @Override
    @NonNull
    protected SQLiteOpenHelper getDbHelper() {
        return DBHelper.getInstance(getContext());
    }


    /**
     * For testing, return the appropriate conflict algorithm based on the contract used for testing
     *
     * @param table The table to determine the conflict algorithm for.
     * @return the conflict algorithm
     */
    @Override
    protected int getInsertConflictAlgorithm(@NonNull String table) {
        switch (table) {
            case IgnoreContract.TABLE:
                return CONFLICT_IGNORE;
            case ReplaceContract.TABLE:
                return CONFLICT_REPLACE;
            case RollbackContract.TABLE:
                return CONFLICT_ROLLBACK;
            default:
                throw new IllegalArgumentException("Unexpected table");
        }
    }
}
