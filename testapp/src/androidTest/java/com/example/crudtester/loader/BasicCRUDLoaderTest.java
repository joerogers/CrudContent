package com.example.crudtester.loader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;

import com.forkingcode.crudcontent.loader.BasicCRUDLoader;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Test to ensure the BasicCRUDLoader creates the proper cursor loader
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDLoaderTest extends Assert {

    @Rule
    public final UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Context context;
    private MockLoaderManager mockLoaderManager;
    private MockBasicCRUDLoaderCallback mockBasicCRUDLoaderCallback;
    private static final int LOADER_ID = 100;
    private static final long ROW_ID = 5;

    private static final String AUTHORITY = "test";
    private static final String TABLE = "table";

    private static final Uri testUri;

    private static final String[] testProjection = new String[]{
            "Column1", "Column2"
    };

    private static final String testSelection = "Column1 = ?";
    private static final String[] testSelectionArgs = new String[]{"5"};
    private static final String testOrderBy = "Column2 desc";
    private static final String testLimit = "2";

    static {
        testUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(TABLE)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        mockLoaderManager = new MockLoaderManager();
        mockBasicCRUDLoaderCallback = new MockBasicCRUDLoaderCallback();
    }

    @After
    public void tearDown() throws Exception {
        context = null;
        mockLoaderManager = null;
        mockBasicCRUDLoaderCallback = null;
    }

    /**
     * Basic test to validate a simple cursor loader providing no query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test01InitLoaderNoParams() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .initLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertTrue(mockLoaderManager.isInit());
        assertFalse(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(testUri, loader.getUri());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());
    }

    /**
     * Basic test to validate a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test02InitLoaderWithParams() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .queryProjection(testProjection)
                .whereMatchesSelection(testSelection, testSelectionArgs)
                .orderBy(testOrderBy)
                .initLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertTrue(mockLoaderManager.isInit());
        assertFalse(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(testUri, loader.getUri());
        assertEquals(testProjection, loader.getProjection());
        assertEquals(testSelection, loader.getSelection());
        assertEquals(testSelectionArgs, loader.getSelectionArgs());
        assertEquals(testOrderBy, loader.getSortOrder());
    }

    /**
     * Basic test to validate a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test03InitLoaderWithParamsAndId() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .queryProjection(testProjection)
                .whereMatchesId(ROW_ID)
                .orderBy(testOrderBy)
                .initLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertTrue(mockLoaderManager.isInit());
        assertFalse(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(ContentUris.withAppendedId(testUri, ROW_ID), loader.getUri());
        assertEquals(testProjection, loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertEquals(testOrderBy, loader.getSortOrder());
    }

    /**
     * Basic test to validate a simple cursor loader providing no query parameters. Uri will have
     * the row id already appended. Useful if query a single row given a pre built Uri
     */
    @Test
    @UiThreadTest
    public void test04InitLoaderUriWithRowId() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(ContentUris.withAppendedId(testUri, ROW_ID))
                .initLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertTrue(mockLoaderManager.isInit());
        assertFalse(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(ContentUris.withAppendedId(testUri, ROW_ID), loader.getUri());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());
    }

    /**
     * Basic test to validate restarting a simple cursor loader providing no query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test05RestartLoaderNoParams() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .restartLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertFalse(mockLoaderManager.isInit());
        assertTrue(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(testUri, loader.getUri());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());
    }

    /**
     * Basic test to validate restarting a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test06RestartLoaderWithParams() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .queryProjection(testProjection)
                .whereMatchesSelection(testSelection, testSelectionArgs)
                .orderBy(testOrderBy)
                .restartLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertFalse(mockLoaderManager.isInit());
        assertTrue(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(testUri, loader.getUri());
        assertEquals(testProjection, loader.getProjection());
        assertEquals(testSelection, loader.getSelection());
        assertEquals(testSelectionArgs, loader.getSelectionArgs());
        assertEquals(testOrderBy, loader.getSortOrder());
    }

    /**
     * Basic test to validate restarting a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test07RestartLoaderWithParamsAndId() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .queryProjection(testProjection)
                .whereMatchesId(ROW_ID)
                .orderBy(testOrderBy)
                .initLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertTrue(mockLoaderManager.isInit());
        assertFalse(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertEquals(ContentUris.withAppendedId(testUri, ROW_ID), loader.getUri());
        assertEquals(testProjection, loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertEquals(testOrderBy, loader.getSortOrder());
    }

    @Test
    @UiThreadTest
    public void test08TestLoadFinishedCallback() throws Exception {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .initLoader(mockLoaderManager, LOADER_ID);

        // Ensure the callback has not yet been called
        assertFalse(mockBasicCRUDLoaderCallback.isCalled());

        @SuppressWarnings("unchecked")
        LoaderManager.LoaderCallbacks<Cursor> callbacks = (LoaderManager.LoaderCallbacks<Cursor>) mockLoaderManager.getLoaderCallbacks();
        assertNotNull(callbacks);

        MatrixCursor cursor = new MatrixCursor(new String[]{"Column1"});

        // Simulate a loader call. Loader id should be set and the cursor should be the same
        // cursor that was just built.
        callbacks.onLoadFinished(loader, cursor);

        assertTrue(mockBasicCRUDLoaderCallback.isCalled());
        assertEquals(LOADER_ID, mockBasicCRUDLoaderCallback.getLoaderId());
        assertEquals(cursor, mockBasicCRUDLoaderCallback.getCursor());
    }

    @Test
    @UiThreadTest
    public void test09TestLoadResetCallback() throws Exception {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .initLoader(mockLoaderManager, LOADER_ID);

        // Ensure the callback has not yet been called
        assertFalse(mockBasicCRUDLoaderCallback.isCalled());

        @SuppressWarnings("unchecked")
        LoaderManager.LoaderCallbacks<Cursor> callbacks = (LoaderManager.LoaderCallbacks<Cursor>) mockLoaderManager.getLoaderCallbacks();
        assertNotNull(callbacks);

        // Simulate a loader reset. The loader id should be set and cursor is null
        callbacks.onLoaderReset(loader);

        assertTrue(mockBasicCRUDLoaderCallback.isCalled());
        assertEquals(LOADER_ID, mockBasicCRUDLoaderCallback.getLoaderId());
        assertNull(mockBasicCRUDLoaderCallback.getCursor());
    }

    /**
     * Basic test to validate a loader with a distinct query parameter. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test10LoaderDistinctQuery() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .distinct()
                .initLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertTrue(mockLoaderManager.isInit());
        assertFalse(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());

        // Validate the URI
        Uri uri = loader.getUri();
        assertNotNull(uri);
        assertEquals(ContentResolver.SCHEME_CONTENT, uri.getScheme());
        assertEquals(AUTHORITY, uri.getAuthority());
        assertEquals(TABLE, uri.getLastPathSegment());
        assertNotNull(uri.getQueryParameterNames());
        assertEquals(1, uri.getQueryParameterNames().size());
        assertTrue(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false));
    }

    /**
     * Basic test to validate restart loader with a limit query parameter. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test11LoaderLimitQuery() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .withLimit(testLimit)
                .restartLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertFalse(mockLoaderManager.isInit());
        assertTrue(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());

        // Validate the URI
        Uri uri = loader.getUri();
        assertNotNull(uri);
        assertEquals(ContentResolver.SCHEME_CONTENT, uri.getScheme());
        assertEquals(AUTHORITY, uri.getAuthority());
        assertEquals(TABLE, uri.getLastPathSegment());
        assertNotNull(uri.getQueryParameterNames());
        assertEquals(1, uri.getQueryParameterNames().size());
        assertEquals(testLimit, uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER));
    }

    /**
     * Basic test to validate restart loader with a distinct and limit query parameter. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test12LoaderDistinctAndLimitQuery() {

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .distinct()
                .withLimit(testLimit)
                .restartLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertFalse(mockLoaderManager.isInit());
        assertTrue(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());

        // Validate the URI
        Uri uri = loader.getUri();
        assertNotNull(uri);
        assertEquals(ContentResolver.SCHEME_CONTENT, uri.getScheme());
        assertEquals(AUTHORITY, uri.getAuthority());
        assertEquals(TABLE, uri.getLastPathSegment());
        assertNotNull(uri.getQueryParameterNames());
        assertEquals(2, uri.getQueryParameterNames().size());
        assertTrue(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false));
        assertEquals(testLimit, uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER));
    }

    /**
     * Basic test to validate loader with a distinct and limit query parameter. Uri had "extra"
     * parameter that is removed. Must run on the UI thread due to the fact the AsyncLoader
     * requires the UI thread to create a handler internally.
     */
    @Test
    @UiThreadTest
    public void test13LoaderDistinctAndLimitQueryWithUriExtraParameter() {

        Uri creationUri = testUri.buildUpon().appendQueryParameter("Junk", "Garbage").build();

        CursorLoader loader = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(creationUri)
                .distinct()
                .withLimit(testLimit)
                .restartLoader(mockLoaderManager, LOADER_ID);

        assertNotNull(loader);

        // Validate expected loader manager methods called
        assertEquals(LOADER_ID, mockLoaderManager.getLoaderId());
        assertFalse(mockLoaderManager.isInit());
        assertTrue(mockLoaderManager.isRestart());
        assertNotNull(mockLoaderManager.getLoaderCallbacks());

        // Validate loader contains expected data
        assertEquals(LOADER_ID, loader.getId());
        assertNull(loader.getProjection());
        assertNull(loader.getSelection());
        assertNull(loader.getSelectionArgs());
        assertNull(loader.getSortOrder());

        // Validate the URI
        Uri uri = loader.getUri();
        assertNotNull(uri);
        assertEquals(ContentResolver.SCHEME_CONTENT, uri.getScheme());
        assertEquals(AUTHORITY, uri.getAuthority());
        assertEquals(TABLE, uri.getLastPathSegment());
        assertNotNull(uri.getQueryParameterNames());
        assertEquals(2, uri.getQueryParameterNames().size());
        assertTrue(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false));
        assertEquals(testLimit, uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER));

        // Junk should be removed...
        assertNull(uri.getQueryParameter("Junk"));
    }

    /**
     * Validate exception thrown if user provides both whereMatchesId and whereMatchesSelection
     */
    @Test
    @UiThreadTest
    public void test14WhereMatchesSelectionConflict() {
        thrown.expect(IllegalStateException.class);

        new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .whereMatchesId(ROW_ID)

                        // exception should be thrown here
                .whereMatchesSelection(testSelection, testSelectionArgs)
                .initLoader(mockLoaderManager, LOADER_ID);
    }

    /**
     * Validate exception thrown if user provides both whereMatchesId and whereMatchesSelection
     */
    @Test
    @UiThreadTest
    public void test15WhereMatchesIdConflict() {
        thrown.expect(IllegalStateException.class);

        new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri)
                .whereMatchesSelection(testSelection, testSelectionArgs)

                        // exception should be thrown here
                .whereMatchesId(ROW_ID)
                .initLoader(mockLoaderManager, LOADER_ID);
    }

    /**
     * Validate exception thrown no uri is provided
     */
    @Test
    @UiThreadTest
    public void test16EnsureUriProvided() {
        thrown.expect(IllegalStateException.class);

        new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .initLoader(mockLoaderManager, LOADER_ID);
    }

    /**
     * Validate exception thrown no uri is provided
     */
    @Test
    @UiThreadTest
    public void test17EnsureOnlyInitOrRestartCalled() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDLoader.Builder builder = new BasicCRUDLoader.Builder(context, mockBasicCRUDLoaderCallback)
                .forUri(testUri);

        builder.initLoader(mockLoaderManager, LOADER_ID);

        // should throw exception
        builder.restartLoader(mockLoaderManager, LOADER_ID);
    }

}
