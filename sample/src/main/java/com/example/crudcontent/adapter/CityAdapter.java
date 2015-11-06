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
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.crudcontent.R;
import com.example.crudcontent.provider.CityContract;

import java.util.Date;

public class CityAdapter extends CursorAdapter {

    public static final String[] PROJECTION = new String[]{
            CityContract.Columns._ID,
            CityContract.Columns.NAME,
            CityContract.Columns.STATE_ABBREVIATION,
            CityContract.Columns.DATE_VISITED
    };

    private static final int ID_POS = 0;
    private static final int NAME_POS = 1;
    private static final int STATE_POS = 2;
    private static final int DATE_POS = 3;

    public CityAdapter(Context context) {
        // null indicates no cursor at this time.
        // 0 sets flags indicating loader managing cursor. Essentially should never be anything else
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Unless you are supporting different types of views, just return a view here.
        // This is called from getView when the convert view is null
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_item_city, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // cursor already in position
        ViewHolder holder = (ViewHolder) view.getTag();
        StringBuilder builder = new StringBuilder(cursor.getString(NAME_POS));
        String state = cursor.getString(STATE_POS);
        if (!TextUtils.isEmpty(state)) {
            builder.append(", ").append(state);
        }
        holder.cityName.setText(builder.toString());

        Date date = new Date(cursor.getLong(DATE_POS));
        holder.dateVisited.setText(DateFormat.getMediumDateFormat(context).format(date));
    }

    /* package */ static class ViewHolder {
        final TextView cityName;
        final TextView dateVisited;

        ViewHolder(View view) {
            cityName = (TextView) view.findViewById(R.id.city_name);
            dateVisited = (TextView) view.findViewById(R.id.date_visited);
        }
    }
}
