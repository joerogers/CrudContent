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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.example.crudcontent.adapter.CityAdapter;
import com.example.crudcontent.databinding.CityListFragmentBinding;
import com.example.crudcontent.provider.CityContract;
import com.example.crudcontent.provider.LoaderIds;
import com.forkingcode.crudcontent.loader.BasicCRUDLoader;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class CityListFragment extends Fragment
        implements BasicCRUDLoader.BasicCRUDLoaderCallback {

    private static final int SORT_BY_DATE = 0;
    private static final int SORT_BY_NAME = 1;

    private CityListFragmentListener listener;
    private int animationDuration;
    private String orderByClause;
    private int sortOrder = -1;
    private boolean loaderStarted = false;

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
        CityListFragmentBinding binding = CityListFragmentBinding.inflate(inflater, container, false);
        binding.setListeners(this);
        binding.list.setEmptyView(binding.empty);
        binding.list.setAdapter(new CityAdapter(binding.list.getContext()));
        showProgress(binding, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the city loader after the activity is fully created. Not using restart
        // so that on rotation, the "same" data is provided without having to query the database
        // again.
        BasicCRUDLoader.newInstance(getContext(), getLoaderManager())
                .forUri(CityContract.URI)
                .selectColumns(CityAdapter.PROJECTION)
                .orderBy(orderByClause)
                .callback(this)
                .loaderId(LoaderIds.CITY_LOADER)
                .initLoader();
        loaderStarted = true;
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

    public void setSortOrder(int sortOrder) {
        // Sometimes adapter, updates position more than once. If already using right
        // sort order, then no need to change anything.
        if (this.sortOrder == sortOrder) return;

        this.sortOrder = sortOrder;

        switch (sortOrder) {
            case SORT_BY_DATE:
                orderByClause = CityContract.Columns.DATE_VISITED + " desc";
                break;
            case SORT_BY_NAME:
                orderByClause = CityContract.Columns.NAME;
                break;
            default:
                orderByClause = null;
                break;
        }

        if (loaderStarted) {
            // example of restarting a loader. If the sort order changes, restart is needed
            // to modify the order by clause.
            BasicCRUDLoader.newInstance(getContext(), getLoaderManager())
                    .forUri(CityContract.URI)
                    .selectColumns(CityAdapter.PROJECTION)
                    .orderBy(orderByClause)
                    .callback(this)
                    .loaderId(LoaderIds.CITY_LOADER)
                    .restartLoader();
        }
    }

    @Override
    public void onCursorLoaded(int loaderId, @Nullable Cursor cursor) {
        if (loaderId == LoaderIds.CITY_LOADER) {
            onCityLoadComplete(cursor);
        }
    }

    private void onCityLoadComplete(final Cursor cursor) {

        CityListFragmentBinding binding = DataBindingUtil.getBinding(getView());
        if (binding == null) return;

        CityAdapter adapter = (CityAdapter) binding.list.getAdapter();
        adapter.swapCursor(cursor);

        // Show list now either with contents or via the empty view.
        showList(binding, true);
    }

    private void showList(CityListFragmentBinding binding, boolean showAnimation) {
        // cross fade only if list is not already showing
        if (binding.listContainer.getVisibility() != View.VISIBLE) {
            if (showAnimation) {
                crossFadeViews(binding.listContainer, binding.progress);
            }
            else {
                binding.progress.setVisibility(View.GONE);
                binding.listContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showProgress(CityListFragmentBinding binding, boolean showAnimation) {
        // cross fade only if list is currently showing
        if (binding.progress.getVisibility() != View.VISIBLE) {

            if (showAnimation) {
                crossFadeViews(binding.progress, binding.listContainer);
            }
            else {
                binding.progress.setVisibility(View.VISIBLE);
                binding.listContainer.setVisibility(View.GONE);
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
                .withEndAction(new FadeOutEndAction(fadeOutView));
    }

    /**
     * The end action is not cleared on some older releases, use a weak ref to allow
     * the activity/fragment and views to get destroyed.
     */
    static class FadeOutEndAction implements Runnable {
        private final WeakReference<View> viewRef;
        FadeOutEndAction(View view) {
            viewRef = new WeakReference<>(view);
        }

        @Override
        public void run() {
            View view = viewRef.get();
            if (view != null) {
                view.setVisibility(View.GONE);
                view.animate().setListener(null);
            }
        }
    }
}
