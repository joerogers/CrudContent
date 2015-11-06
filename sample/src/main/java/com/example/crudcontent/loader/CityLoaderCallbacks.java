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

package com.example.crudcontent.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.example.crudcontent.provider.CityContract;

/**
 * City Loader callbacks. This class is part mandatory implementation of the LoaderCallbacks
 * for a loader and part abstraction to allow Activities/Fragments to avoid implementing this code
 * directly and reuse it directly.
 * <p/>
 * As you can see this is a lot of code and almost all of the code would have to be implemented
 * in the activity or fragment. The only savings you would have is a slight ability to avoid creating a
 * listener. However, since this should be a "static" inner class in an activity/fragment you would still
 * need a weak reference to the activity/fragment to receive data changes.
 */
public final class CityLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CityLoaderCB";

    private static final String ARG_URI = "uri";
    private static final String ARG_PROJECTION = "projection";

    private final Context applicationContext;
    //private final WeakReference<CityLoadListener> listenerRef;
    private final CityLoadListener listener;

    public interface CityLoadListener {
        void onCityLoadComplete(Cursor cursor);
    }

    /**
     * Initialize a loader. Call from an Activity's onCreate or a Fragment's onActivityCreated method.
     */
    public static void initLoader(Context context, LoaderManager loaderManager, CityLoadListener listener, String[] projection) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, CityContract.URI);
        args.putStringArray(ARG_PROJECTION, projection);
        loaderManager.initLoader(LoaderIds.CITY_LOADER,
                args, new CityLoaderCallbacks(context, listener));

    }

    /**
     * Initialize a loader to query a specific city using the provided city id.
     */
    public static void initLoader(Context context, LoaderManager loaderManager, CityLoadListener listener, String[] projection, long cityId) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, ContentUris.withAppendedId(CityContract.URI, cityId));
        args.putStringArray(ARG_PROJECTION, projection);
        loaderManager.initLoader(LoaderIds.CITY_LOADER,
                args, new CityLoaderCallbacks(context, listener));
    }

    /**
     * Private constructor to prevent direct instantiation
     *
     * @param context  The context to use for starting the loader
     * @param listener The listener to use to provide city cursor data back to the caller.
     */
    private CityLoaderCallbacks(Context context, CityLoadListener listener) {
        applicationContext = context.getApplicationContext();
        //listenerRef = new WeakReference<>(listener);
        this.listener = listener;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        Log.d(TAG, "onCreateLoader");
        Uri uri = bundle.getParcelable(ARG_URI);

        return new CursorLoader(
                applicationContext,
                uri,
                bundle.getStringArray(ARG_PROJECTION),
                null,
                null,
                CityContract.Columns.DATE_VISITED + " desc");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished result is " + (cursor != null ? "not null" : "null"));

        // Have a result. Note, it may be null, but we want to tell our listener if
        // we still have one with the value regardless.
        // CityLoadListener listener = listenerRef.get();
        if (listener != null) {
            Log.d(TAG, "onLoadFinished. Notifying listener");
            listener.onCityLoadComplete(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG, "onLoadReset");
        onLoadFinished(cursorLoader, null);
    }
}
