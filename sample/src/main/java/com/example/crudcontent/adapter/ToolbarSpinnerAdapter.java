package com.example.crudcontent.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.crudcontent.R;

/**
 * Adapter to ensure look and feel of spinner is as expected in app bar
 */
public class ToolbarSpinnerAdapter extends ArrayAdapter<String> implements ThemedSpinnerAdapter {

    private final ThemedSpinnerAdapter.Helper dropDownHelper;

    public ToolbarSpinnerAdapter(Context context) {
        super(context, 0, context.getResources().getStringArray(R.array.sort_types));
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.simple_spinner_item, parent, false);
        }
        ((TextView) view).setText(getItem(position));
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = dropDownHelper.getDropDownViewInflater().inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) view).setText(getItem(position));

        return view;
    }

    @Nullable
    @Override
    public Resources.Theme getDropDownViewTheme() {
        return dropDownHelper.getDropDownViewTheme();
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme) {
        dropDownHelper.setDropDownViewTheme(theme);
    }
}
