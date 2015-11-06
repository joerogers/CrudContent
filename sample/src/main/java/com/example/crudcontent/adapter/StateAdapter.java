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

package com.example.crudcontent.adapter;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

import com.example.crudcontent.R;
import com.example.crudcontent.provider.StateContract;

public class StateAdapter extends SimpleCursorAdapter {

    public static final String[] PROJECTION = new String[]{
            StateContract.Columns._ID,
            StateContract.Columns.NAME,
            StateContract.Columns.ABBREVIATION
    };

    private static final int ABBREVIATION_POS = 2;

    private static final String[] FROM = new String[]{
            StateContract.Columns.NAME
    };

    private static final int[] TO = new int[]{
            android.R.id.text1
    };

    public StateAdapter(Context context) {
        // null indicates no cursor at this time.
        // 0 sets flags indicating loader managing cursor. Essentially should never be anything else
        super(context, android.R.layout.simple_spinner_item, null, FROM, TO, 0);
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    }

    public String getAbbreviation(int position) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(position)) {
            return cursor.getString(ABBREVIATION_POS);
        }

        return null;
    }
}
