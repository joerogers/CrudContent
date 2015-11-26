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
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.example.crudcontent.databinding.CityListItemBinding;
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
        CityListItemBinding binding = CityListItemBinding.inflate(LayoutInflater.from(context), parent, false);
        binding.setCityItem(new ItemData());
        return binding.getRoot();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // cursor already in position
        CityListItemBinding binding = DataBindingUtil.getBinding(view);
        if (binding == null) return;

        ItemData data = binding.getCityItem();
        data.updateData(cursor, view.getContext());
        // bind immediately vs waiting until next frame to avoid extra layout pass
        binding.executePendingBindings();
    }

    public static class ItemData {
        public final ObservableField<String> city = new ObservableField<>();
        public final ObservableField<String> state = new ObservableField<>();
        public final ObservableField<String> date = new ObservableField<>();

        /* package */ void updateData(Cursor cursor, Context context) {
            city.set(cursor.getString(NAME_POS));
            state.set(cursor.getString(STATE_POS));
            Date dateObj = new Date(cursor.getLong(DATE_POS));
            date.set(DateFormat.getMediumDateFormat(context).format(dateObj));
        }
    }
}
