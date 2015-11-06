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

package com.example.crudcontent.fragment;


import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.example.crudcontent.adapter.CityAdapter;
import com.example.crudcontent.databinding.CityListFragmentBindings;
import com.example.crudcontent.loader.CityLoaderCallbacks;

/**
 * A simple {@link Fragment} subclass.
 */
public class CityListFragment extends Fragment
        implements CityLoaderCallbacks.CityLoadListener {

    private CityListFragmentListener listener;
    private int animationDuration;

    public interface CityListFragmentListener {

        void editCity(long cityId);
    }

    public CityListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parent = getParentFragment();
        Object objectToCast = parent != null ? parent : context;
        try {
            listener = (CityListFragmentListener) objectToCast;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(objectToCast.getClass().getSimpleName()
                    + " must implement CityListFragmentListener");
        }

        animationDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        CityListFragmentBindings bindings = CityListFragmentBindings.inflate(inflater, container, false);
        bindings.setListeners(this);
        bindings.list.setEmptyView(bindings.empty);
        bindings.list.setAdapter(new CityAdapter(bindings.list.getContext()));
        showProgress(bindings, false);
        return bindings.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the loader after the activity is fully created
        CityLoaderCallbacks.initLoader(getContext(), getLoaderManager(), this, CityAdapter.PROJECTION);
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @SuppressWarnings("unused")
    public void onCityClick(AdapterView<?> parent, View view, int position, long id) {
        listener.editCity(id);
    }

    @Override
    public void onCityLoadComplete(final Cursor cursor) {

        CityListFragmentBindings bindings = DataBindingUtil.getBinding(getView());
        if (bindings == null) return;

        CityAdapter adapter = (CityAdapter) bindings.list.getAdapter();
        adapter.swapCursor(cursor);

        // Show list now either with contents or via the empty view.
        showList(bindings, true);
    }

    private void showList(CityListFragmentBindings bindings, boolean showAnimation) {
        // cross fade only if list is not already showing
        if (bindings.listContainer.getVisibility() != View.VISIBLE) {
            if (showAnimation) {
                crossFadeViews(bindings.listContainer, bindings.progress);
            }
            else {
                bindings.progress.setVisibility(View.GONE);
                bindings.listContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showProgress(CityListFragmentBindings bindings, boolean showAnimation) {
        // cross fade only if list is currently showing
        if (bindings.progress.getVisibility() != View.VISIBLE) {

            if (showAnimation) {
                crossFadeViews(bindings.progress, bindings.listContainer);
            }
            else {
                bindings.progress.setVisibility(View.VISIBLE);
                bindings.listContainer.setVisibility(View.GONE);
            }
        }
    }

    private void crossFadeViews(final View fadeInView, final View fadeOutView) {
        fadeInView.clearAnimation();
        fadeOutView.clearAnimation();

        fadeInView.setAlpha(0f);
        fadeInView.setVisibility(View.VISIBLE);

        ViewCompat.animate(fadeInView)
                .alpha(1f)
                .setDuration(animationDuration)
                .setListener(null)
                .withLayer();

        ViewCompat.animate(fadeOutView)
                .alpha(0f)
                .setDuration(animationDuration)
                .setListener(null)
                .withLayer()
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        fadeOutView.setVisibility(View.GONE);
                    }
                });
    }
}
