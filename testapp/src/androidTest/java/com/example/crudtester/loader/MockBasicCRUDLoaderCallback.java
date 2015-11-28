package com.example.crudtester.loader;

import android.database.Cursor;
import android.support.annotation.Nullable;

import com.forkingcode.crudcontent.loader.BasicCRUDLoader;

/**
 * Mock loader callback to ensure that data was properly passed to the callback by the loader
 * infrastructure.
 */
public class MockBasicCRUDLoaderCallback implements BasicCRUDLoader.Callback {

    private boolean called = false;
    private int loaderId = 0;
    private Cursor cursor = null;

    @Override
    public void onCursorLoaded(int loaderId, @Nullable Cursor cursor) {
        called = true;
        this.loaderId = loaderId;
        this.cursor = cursor;
    }

    public boolean isCalled() {
        return called;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public int getLoaderId() {
        return loaderId;
    }
}
