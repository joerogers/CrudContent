package com.example.crudtester.loader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;

import com.forkingcode.crudcontent.loader.BasicCRUDLoader;
import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test to ensure the BasicCRUDLoader creates the proper cursor loader
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCRUDLoaderTest {

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
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();
        BasicCRUDLoader.enableLogging(true);
        mockLoaderManager = new MockLoaderManager();
        mockBasicCRUDLoaderCallback = new MockBasicCRUDLoaderCallback();
    }

    @After
    public void tearDown() {
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

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(true));
        assertThat(mockLoaderManager.isRestart(), is(false));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(testUri));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));
    }

    /**
     * Basic test to validate a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test02InitLoaderWithParams() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .selectColumns(testProjection)
                .whereMatchesSelection(testSelection, testSelectionArgs)
                .orderBy(testOrderBy)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(true));
        assertThat(mockLoaderManager.isRestart(), is(false));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(testUri));
        assertThat(loader.getProjection(), is(testProjection));
        assertThat(loader.getSelection(), is(testSelection));
        assertThat(loader.getSelectionArgs(), is(testSelectionArgs));
        assertThat(loader.getSortOrder(), is(testOrderBy));
    }

    /**
     * Basic test to validate a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test03InitLoaderWithParamsAndId() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                // Use var args technique
                .selectColumns(testProjection[0], testProjection[1])
                .whereMatchesId(ROW_ID)
                .orderBy(testOrderBy)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(true));
        assertThat(mockLoaderManager.isRestart(), is(false));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(ContentUris.withAppendedId(testUri, ROW_ID)));
        assertThat(loader.getProjection(), is(testProjection));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(testOrderBy));
    }

    /**
     * Basic test to validate a simple cursor loader providing no query parameters. Uri will have
     * the row id already appended. Useful if query a single row given a pre built Uri
     */
    @Test
    @UiThreadTest
    public void test04InitLoaderUriWithRowId() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(ContentUris.withAppendedId(testUri, ROW_ID))
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(true));
        assertThat(mockLoaderManager.isRestart(), is(false));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(ContentUris.withAppendedId(testUri, ROW_ID)));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));
    }

    /**
     * Basic test to validate restarting a simple cursor loader providing no query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test05RestartLoaderNoParams() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .restartLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(false));
        assertThat(mockLoaderManager.isRestart(), is(true));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(testUri));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));
    }

    /**
     * Basic test to validate restarting a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test06RestartLoaderWithParams() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .selectColumns(testProjection)
                // using var args
                .whereMatchesSelection(testSelection, testSelectionArgs[0])
                .orderBy(testOrderBy)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .restartLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(false));
        assertThat(mockLoaderManager.isRestart(), is(true));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(testUri));
        assertThat(loader.getProjection(), is(testProjection));
        assertThat(loader.getSelection(), is(testSelection));
        assertThat(loader.getSelectionArgs(), is(testSelectionArgs));
        assertThat(loader.getSortOrder(), is(testOrderBy));
    }

    /**
     * Basic test to validate restarting a simple cursor loader providing with query parameters. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test07RestartLoaderWithParamsAndId() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .selectColumns(testProjection)
                .whereMatchesId(ROW_ID)
                .orderBy(testOrderBy)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .restartLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(false));
        assertThat(mockLoaderManager.isRestart(), is(true));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getUri(), is(ContentUris.withAppendedId(testUri, ROW_ID)));
        assertThat(loader.getProjection(), is(testProjection));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(testOrderBy));
    }

    @Test
    @UiThreadTest
    public void test08TestLoadFinishedCallback() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        // Ensure the callback has not yet been called
        assertThat(mockBasicCRUDLoaderCallback.isCalled(), is(false));

        @SuppressWarnings("unchecked")
        LoaderManager.LoaderCallbacks<Cursor> callbacks = (LoaderManager.LoaderCallbacks<Cursor>) mockLoaderManager.getLoaderCallbacks();
        assertThat(callbacks, is(notNullValue()));

        MatrixCursor cursor = new MatrixCursor(new String[]{"Column1"});

        // Simulate a loader call. Loader id should be set and the cursor should be the same
        // cursor that was just built.
        callbacks.onLoadFinished(loader, cursor);

        assertThat(mockBasicCRUDLoaderCallback.isCalled(), is(true));
        assertThat(mockBasicCRUDLoaderCallback.getLoaderId(), is(LOADER_ID));
        assertThat(mockBasicCRUDLoaderCallback.getCursor(), is((Cursor) cursor));
    }

    @Test
    @UiThreadTest
    public void test09TestLoadResetCallback() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        // Ensure the callback has not yet been called
        assertThat(mockBasicCRUDLoaderCallback.isCalled(), is(false));

        @SuppressWarnings("unchecked")
        LoaderManager.LoaderCallbacks<Cursor> callbacks = (LoaderManager.LoaderCallbacks<Cursor>) mockLoaderManager.getLoaderCallbacks();
        assertThat(callbacks, is(notNullValue()));

        // Simulate a loader reset. The loader id should be set and cursor is null
        callbacks.onLoaderReset(loader);

        assertThat(mockBasicCRUDLoaderCallback.isCalled(), is(true));
        assertThat(mockBasicCRUDLoaderCallback.getLoaderId(), is(LOADER_ID));
        assertThat(mockBasicCRUDLoaderCallback.getCursor(), is(nullValue()));
    }

    /**
     * Basic test to validate a loader with a distinct query parameter. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test10LoaderDistinctQuery() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .distinct()
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(true));
        assertThat(mockLoaderManager.isRestart(), is(false));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));

        // Validate the URI
        Uri uri = loader.getUri();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.getScheme(), is(ContentResolver.SCHEME_CONTENT));
        assertThat(uri.getAuthority(), is(AUTHORITY));
        assertThat(uri.getLastPathSegment(), is(TABLE));
        assertThat(uri.getQueryParameterNames(), is(notNullValue()));
        assertThat(uri.getQueryParameterNames().size(), is(1));
        assertThat(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false), is(true));
        assertThat(uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER), is(nullValue()));
    }

    /**
     * Basic test to validate restart loader with a limit query parameter. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test11LoaderLimitQuery() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .limit(testLimit)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .restartLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(false));
        assertThat(mockLoaderManager.isRestart(), is(true));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));

        // Validate the URI
        Uri uri = loader.getUri();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.getScheme(), is(ContentResolver.SCHEME_CONTENT));
        assertThat(uri.getAuthority(), is(AUTHORITY));
        assertThat(uri.getLastPathSegment(), is(TABLE));
        assertThat(uri.getQueryParameterNames(), is(notNullValue()));
        assertThat(uri.getQueryParameterNames().size(), is(1));
        assertThat(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false), is(false));
        assertThat(uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER), is(testLimit));
    }

    /**
     * Basic test to validate restart loader with a distinct and limit query parameter. Must
     * run on the UI thread due to the fact the AsyncLoader requires the UI thread to create a
     * handler internally.
     */
    @Test
    @UiThreadTest
    public void test12LoaderDistinctAndLimitQuery() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .distinct()
                .limit(testLimit)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .restartLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(false));
        assertThat(mockLoaderManager.isRestart(), is(true));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));

        // Validate the URI
        Uri uri = loader.getUri();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.getScheme(), is(ContentResolver.SCHEME_CONTENT));
        assertThat(uri.getAuthority(), is(AUTHORITY));
        assertThat(uri.getLastPathSegment(), is(TABLE));
        assertThat(uri.getQueryParameterNames(), is(notNullValue()));
        assertThat(uri.getQueryParameterNames().size(), is(2));
        assertThat(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false), is(true));
        assertThat(uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER), is(testLimit));
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

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(creationUri)
                .distinct()
                .limit(testLimit)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .restartLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(false));
        assertThat(mockLoaderManager.isRestart(), is(true));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(LOADER_ID));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));

        // Validate the URI
        Uri uri = loader.getUri();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.getScheme(), is(ContentResolver.SCHEME_CONTENT));
        assertThat(uri.getAuthority(), is(AUTHORITY));
        assertThat(uri.getLastPathSegment(), is(TABLE));
        assertThat(uri.getQueryParameterNames(), is(notNullValue()));
        assertThat(uri.getQueryParameterNames().size(), is(2));
        assertThat(uri.getBooleanQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, false), is(true));
        assertThat(uri.getQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER), is(testLimit));

        // Junk should be removed...
        assertThat(uri.getQueryParameter("Junk"), is(nullValue()));
    }

    /**
     * Validate exception thrown if user provides both whereMatchesId and whereMatchesSelection
     */
    @Test
    @UiThreadTest
    public void test14WhereMatchesSelectionConflict() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .whereMatchesId(ROW_ID)

                // exception should be thrown here
                .whereMatchesSelection(testSelection, testSelectionArgs);
    }

    /**
     * Validate exception thrown if user provides both whereMatchesId and whereMatchesSelection
     */
    @Test
    @UiThreadTest
    public void test15WhereMatchesIdConflict() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .whereMatchesSelection(testSelection, testSelectionArgs)

                // exception should be thrown here
                .whereMatchesId(ROW_ID);
    }

    /**
     * Validate exception thrown no uri is provided
     */
    @Test
    @UiThreadTest
    public void test16EnsureUriProvided() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID)
                .initLoader();
    }

    /**
     * Validate exception thrown no uri is provided
     */
    @Test
    @UiThreadTest
    public void test17EnsureOnlyInitOrRestartCalled() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDLoader.RequestBuilder builder = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .callback(mockBasicCRUDLoaderCallback)
                .loaderId(LOADER_ID);

        builder.initLoader();

        // should throw exception
        builder.restartLoader();
    }

    /**
     * Validate exception thrown no uri is provided
     */
    @Test
    @UiThreadTest
    public void test18EnsureCallbackCalled() {
        thrown.expect(IllegalStateException.class);

        BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .loaderId(LOADER_ID)

                // should throw exception because callback not called
                .initLoader();
    }

    /**
     * Basic test to validate a simple cursor loader providing no query parameters and
     * the default loader id. Must run on the UI thread due to the fact the AsyncLoader
     * requires the UI thread to create a handler internally.
     */
    @Test
    @UiThreadTest
    public void test19DefaultLoaderId() {

        CursorLoader loader = BasicCRUDLoader.newInstance(context, mockLoaderManager)
                .forUri(testUri)
                .callback(mockBasicCRUDLoaderCallback)
                .initLoader();

        assertThat(loader, is(notNullValue()));

        // Validate expected loader manager methods called
        assertThat(mockLoaderManager.getLoaderId(), is(BasicCRUDLoader.DEFAULT_LOADER_ID));
        assertThat(mockLoaderManager.isInit(), is(true));
        assertThat(mockLoaderManager.isRestart(), is(false));
        assertThat(mockLoaderManager.getLoaderCallbacks(), is(notNullValue()));

        // Validate loader contains expected data
        assertThat(loader.getId(), is(BasicCRUDLoader.DEFAULT_LOADER_ID));
        assertThat(loader.getUri(), is(testUri));
        assertThat(loader.getProjection(), is(nullValue()));
        assertThat(loader.getSelection(), is(nullValue()));
        assertThat(loader.getSelectionArgs(), is(nullValue()));
        assertThat(loader.getSortOrder(), is(nullValue()));
    }
}

