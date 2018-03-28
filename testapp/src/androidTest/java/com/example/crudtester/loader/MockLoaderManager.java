package com.example.crudtester.loader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Mock loader manager for validating the BasicCRUDLoader
 */
public class MockLoaderManager extends LoaderManager {

    private boolean isInit = false;
    private boolean isRestart = false;
    private int loaderId = 0;
    private Object loaderCallbacks;

    @NonNull
    @Override
    public <D> Loader<D> initLoader(int id, Bundle args, @NonNull LoaderCallbacks<D> callback) {

        // Simulate an init loader.
        isInit = true;
        loaderCallbacks = callback;
        loaderId = id;

        Loader<D> loader = callback.onCreateLoader(id, args);

        // Need to register listeners to fully "activate" the loader for mock testing.
        loader.registerListener(id, new LoadCompleteListener<D>());
        return loader;
    }

    @NonNull
    @Override
    public <D> Loader<D> restartLoader(int id, Bundle args, @NonNull LoaderCallbacks<D> callback) {

        // Not doing a "true" restart. Just simulating one by going through the create path.
        isRestart = true;
        loaderCallbacks = callback;
        loaderId = id;

        Loader<D> loader = callback.onCreateLoader(id, args);

        // Need to register listeners to fully "activate" the loader for mock testing.
        loader.registerListener(id, new LoadCompleteListener<D>());
        return loader;
    }

    public boolean isInit() {
        return isInit;
    }

    public boolean isRestart() {
        return isRestart;
    }

    public Object getLoaderCallbacks() {
        return loaderCallbacks;
    }

    public int getLoaderId() {
        return loaderId;
    }

    @Override
    public void destroyLoader(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <D> Loader<D> getLoader(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        throw new UnsupportedOperationException();
    }

    private class LoadCompleteListener<D> implements Loader.OnLoadCompleteListener<D> {
        @Override
        public void onLoadComplete(@NonNull Loader<D> loader, D data) {
            // Shouldn't get here so throw an unsupported operation exception just in case
            throw new UnsupportedOperationException();
        }
    }
}
