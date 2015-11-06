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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.example.crudcontent.provider.StateContract;

import java.lang.ref.WeakReference;

/**
 * State Loader callbacks. This class is part mandatory implementation of the LoaderCallbacks
 * for a loader and part abstraction to allow Activities/Fragments to avoid implementing this code
 * directly and reuse it directly.
 * <p/>
 * As you can see this is a lot of code and almost all of the code would have to be implemented
 * in the activity or fragment. The only savings you would have is a slight ability to avoid creating a
 * listener. However, since this should be a "static" inner class in an activity/fragment you would still
 * need a weak reference to the activity/fragment to receive data changes.
 */
public final class StateLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "StateLoaderCB";

    private static final String ARG_PROJECTION = "projection";

    private final Context applicationContext;
    private final WeakReference<StateLoadListener> listenerRef;

    public interface StateLoadListener {
        void onStateLoadComplete(Cursor cursor);
    }

    /**
     * Initialize a loader. Call from an Activity's onCreate or a Fragment's onActivityCreated method.
     */
    public static void initLoader(Context context, LoaderManager loaderManager, StateLoadListener listener, String[] projection) {
        Bundle args = new Bundle();
        args.putStringArray(ARG_PROJECTION, projection);

        // Every loader needs a unique id per the current activity/fragment.
        loaderManager.initLoader(LoaderIds.STATE_LOADER, args, new StateLoaderCallbacks(context, listener));
    }

    /**
     * Private constructor to prevent direct instantiation
     *
     * @param context  The context to use for starting the loader
     * @param listener The listener to use to provide state cursor data back to the caller.
     */
    private StateLoaderCallbacks(Context context, StateLoadListener listener) {
        applicationContext = context.getApplicationContext();
        listenerRef = new WeakReference<>(listener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        Log.d(TAG, "onCreateLoader");

        // In this case, we are starting a cursor loader. Notice how this syncs nicely with
        // the content resolver's query call.
        return new CursorLoader(
                applicationContext,
                StateContract.URI,
                bundle.getStringArray(ARG_PROJECTION),
                null,
                null,
                StateContract.Columns.NAME + " asc");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        Log.d(TAG, "onLoadFinished result is " + (cursor != null ? "not null" : "null"));

        // Have a result. Note, it may be null, but we want to tell our listener if
        // we still have one with the value regardless.
        StateLoadListener listener = listenerRef.get();
        if (listener != null) {
            Log.d(TAG, "onLoadFinished. Notifying listener");
            listener.onStateLoadComplete(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG, "onLoadReset");

        // Since we just let the listener know the value is null, call into onLoadFinished
        // to avoid duplicating listener logic.
        onLoadFinished(cursorLoader, null);
    }
}
