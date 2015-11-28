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
import android.support.annotation.NonNull;

import com.example.crudcontent.BuildConfig;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

public class SampleProvider extends BasicCRUDProvider {

    // Using application id, to allow for proper functionality if you have a special debug/release target
    // with an alternate package name.

    /* package */ static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.SampleProvider";

    public SampleProvider() {
        super(AUTHORITY);
    }

    @NonNull
    @Override
    protected SQLiteOpenHelper getDbHelper() {
        // A singleton is handy if the helper is able to be accessed outside of the provider. However
        // if only the provider accesses the helper, you can instantiate a new instance here instead.
        return DBHelper.getInstance(getContext());
    }

    @Override
    protected int getInsertConflictAlgorithm(@NonNull String table) {
        switch (table) {
            case CityContract.TABLE:
                // for cities there is no "unique" key so should never have a conflict. Using
                // default of rollback in case something goes wrong. Others would also work but
                // to no real benefit or consequence
                return CONFLICT_ROLLBACK;

            case StateContract.TABLE:
                // For states, the setup may try to insert duplicates. Just ignore them maintaining
                // the existing FK relationship to City. Rollback would work as well, but does throw
                // an exception which doesn't perform as well knowing app should ignore duplicate rows.
                //
                // Replace is bad because it would constantly cause the city table to loose rows. If
                // city removed the foreign key "dependency", replace would be an option, but not as
                // efficient as ignore since state data doesn't change often.
                return CONFLICT_IGNORE;

            default:

                // Throw exception to remind us to "fix" code if add a 3rd table
                throw new IllegalArgumentException("Unexpected table: " + table);
        }
    }
}
