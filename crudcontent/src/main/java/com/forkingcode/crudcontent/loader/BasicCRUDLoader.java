package com.forkingcode.crudcontent.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;

import com.forkingcode.crudcontent.provider.BasicCRUDProvider;

/**
 * A simple cursor loader and loaderCallback wrapper that provides query support for the BasicCRUDProvider.
 * Its goal is to eliminate the boiler plate and repetitive code needed for different cursor loaders. It also
 * works in tandem with the provider to provide automatic updated when the underlying data is modified to provide
 * a new cursor with the updated data.
 *
 * <p>To create and start this loader, use the static newInstance method to start to form queries including
 * properly encoding the distinct and limit query parameters for the loader.
 *
 * <p>You must provide a BasicCRUDLoaderCallback instance which has a single method onCursorLoaded which
 * provides the loaderId used for this loader and the cursor result of the query. At that time, you should release
 * any prior cursors provided during onCursorLoaded as they will be closed upon completion of the method. When
 * the loader is destroyed the method may be called passing "null" as the cursor to indicate the prior cursor
 * should be released.
 *
 * @see android.support.v4.app.LoaderManager
 * @see android.support.v4.content.CursorLoader
 * @see android.support.v4.app.LoaderManager.LoaderCallbacks
 */
public class BasicCRUDLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface Callback {

        /**
         * Called when a loader query has finished its load.  Note
         * that normally an application is <em>not</em> allowed to commit fragment
         * transactions while in this call, since it can happen after an
         * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()}
         * for further discussion on this.
         *
         * <p>This function is guaranteed to be called prior to the release of
         * the last cursor that was supplied for this Loader.  At this point
         * you should remove all use of the old cursor (since it will be released
         * soon), but should not do your own release of the cursor since its Loader
         * owns it and will take care of that.  The Loader will take care of
         * management of its data so you don't have to.  In particular:
         *
         * <ul>
         * <li> <p>The Loader will monitor for changes to the cursor, and report
         * them to you through new calls here.  You should not monitor the
         * cursor yourself.  For example, if the data is a {@link android.database.Cursor}
         * and you place it in a {@link android.widget.CursorAdapter}, use
         * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
         * android.database.Cursor, int)} constructor <em>without</em> passing
         * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
         * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
         * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
         * from doing its own observing of the Cursor, which is not needed since
         * when a change happens you will get a new Cursor throw another call
         * here.
         * <li> The Loader will release the cursor once it knows the application
         * is no longer using it.  For example, if the data is
         * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
         * you should not call close() on it yourself.  If the Cursor is being placed in a
         * {@link android.widget.CursorAdapter}, you should use the
         * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
         * method so that the old Cursor is not closed.
         * </ul>
         *
         * @param loaderId The loader id of the loader used to generate the cursor
         * @param cursor   The cursor that was generated by the query performed by the loader. A null
         *                 value will be passed if the loader is in process of resetting. Use this
         *                 as an indicator that references to a prior cursor should be removed as the loader
         *                 will destroy the cursor once this call is complete.
         */
        void onCursorLoaded(int loaderId, @Nullable Cursor cursor);
    }

    /**
     * Default loader id if none is provided. Using the default will cause issues if you create more
     * than one loader fetching different data from the same activity or fragment. If using init only the
     * first loader will return results. If using restart, only the last loader is guaranteed to return results.
     */
    public static final int DEFAULT_LOADER_ID = 1;

    private static final String ARG_URI = "uri";
    private static final String ARG_PROJECTION = "projection";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selectionArgs";
    private static final String ARG_SORT_ORDER = "sortOrder";

    private static final String TAG = "BasicCRUDLocader";

    /* package */ static boolean DEBUG = false;
    private final Context context;
    private final Callback loaderCallback;

    /* package */ BasicCRUDLoader(Context context, Callback loaderCallback) {
        this.context = context.getApplicationContext();
        this.loaderCallback = loaderCallback;
    }

    /**
     * Enable log tracing via of the loader callback methods.
     *
     * @param enable true to enable logging and false otherwise. By default logging is disabled.
     */
    public static void enableLogging(boolean enable) {
        DEBUG = enable;
    }

    /**
     * Instantiate and return a new Loader for the given ID. In this case a new
     * cursor loader is created to query the database based on the information provided
     * by the builder class.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     * @see RequestBuilder to see how the args are built
     */
    @NonNull
    @Override
    public final Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (DEBUG) {
            Log.d(TAG, "onCreateLoader: id: " + id);
        }

        Uri uri = args != null ? (Uri) args.getParcelable(ARG_URI) : null;
        if (args == null || uri == null) {
            throw new IllegalStateException("Failed to resolve uri");
        }

        return new CursorLoader(context,
                uri,
                args.getStringArray(ARG_PROJECTION),
                args.getString(ARG_SELECTION),
                args.getStringArray(ARG_SELECTION_ARGS),
                args.getString(ARG_SORT_ORDER));
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()}
     * for further discussion on this.
     *
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     *
     * @param loader The Loader that has finished.
     * @param cursor The data generated by the Loader.
     */
    @Override
    public final void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor cursor) {
        if (DEBUG) {
            Log.d(TAG, "onLoadFinished: id: " + loader.getId() +
                    "  result: " + (cursor != null ? "not null" : "null"));
        }

        loaderCallback.onCursorLoaded(loader.getId(), cursor);
    }

    /**
     * Called when the previously created loader is being reset, and thus
     * making its data unavailable. The application should at this point
     * remove any references it has to the Loader's data.
     *
     * <p>This implementation just calls onLoadFinished with a null cursor, to
     * indicate the system should remove references to the cursor.d
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public final void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (DEBUG) {
            Log.d(TAG, "onLoadReset: id: " + loader.getId());
        }
        onLoadFinished(loader, null);
    }


    /**
     * Start creating a new instance of the loader. This follows a builder pattern with the last
     * call being initLoader or restartLoader
     *
     * @param context       The context used to create the loader. Internally an application context is stored.
     * @param loaderManager The loader manager to use to create the loader
     * @return A new loader request builder allowing you to form the query the cursor loader will perform.
     */
    @NonNull
    public static RequestBuilder newInstance(@NonNull Context context, @NonNull LoaderManager loaderManager) {
        return new RequestBuilder(context, loaderManager);
    }

    /**
     * Helper class used to initialize or restart the loader. This builder pattern allows you
     * to add information as required for the query. Note: forUri must be called however, all other
     * methods are optional.
     *
     * <p>To initialize or restart the loader, you should call either the initLoader or restartLoader
     * method on this builder. Once either method is called, a new Builder should be created to initialize
     * a new loader, or restart the current one. When these methods are called the URI is updated with any
     * optional parameters and the loader is started with the appropriate argument bundle.
     */
    public static class RequestBuilder {
        private final Context context;
        private final LoaderManager loaderManager;
        private final Bundle args = new Bundle();
        private Callback loaderCallback;
        private BasicCRUDLoader basicCRUDLoader;
        private boolean distinct = false;
        private String limit = null;
        private long rowId = -1;
        private int loaderId = DEFAULT_LOADER_ID;
        private boolean loaderStarted = false;

        /**
         * Initialize a new builder for invoking the BasicCRUDLoader.
         *
         * @param context       The context used to create the loader. Internally an application context is stored.
         * @param loaderManager The loader manager to use to create the loader
         */
        /* package */ RequestBuilder(@NonNull Context context, @NonNull LoaderManager loaderManager) {
            this.context = context.getApplicationContext();
            this.loaderManager = loaderManager;
        }

        /**
         * Provide the Uri to query. You must call this method on the builder. Failure to do so
         * will result in an IllegalStateException once initLoader/restartLoader is called.
         *
         * @param uri The uri associated with the provider to query.
         * @return This builder object
         */
        @NonNull
        public RequestBuilder forUri(@NonNull Uri uri) {
            args.putParcelable(ARG_URI, uri);
            return this;
        }

        /**
         * Provide the callback used to listen for results of the loader query
         *
         * @param loaderCallback The loader callback to use for this loader. This is a required
         *                       parameter
         * @return This builder object
         */
        @NonNull
        public RequestBuilder callback(@NonNull Callback loaderCallback) {
            this.loaderCallback = loaderCallback;
            return this;
        }

        /**
         * Optionally provide the loader id to use for this loader. Default loader id will be 1.
         * This parameter is required if creating more than one loader in an activity/fragment
         *
         * @param loaderId The loader id of the loader
         * @return This builder object
         */
        @NonNull
        public RequestBuilder loaderId(int loaderId) {
            this.loaderId = loaderId;
            return this;
        }

        /**
         * Optionally provide a the set of columns to query from the table. If this is not
         * called, then all columns will be returned
         *
         * @param projection The list of columns to put into the cursor. Either pass a String[] or
         *                   a variable list of Strings representing column names.
         * @return This builder object
         */
        @NonNull
        public RequestBuilder selectColumns(@NonNull String... projection) {
            args.putStringArray(ARG_PROJECTION, projection);
            return this;
        }

        /**
         * Optionally indicate that this is a distinct query. Ie rows where all columns match
         * another row will be removed from the result set. By default all rows are returned.
         *
         * @return This builder object
         */
        @NonNull
        public RequestBuilder distinct() {
            this.distinct = true;
            return this;
        }

        /**
         * Optionally indicate you wish to query a specific row by id. Note: if you provided a Uri
         * with the rowId already appended, then you should avoid calling this method as will will
         * append the rowId to the end of the Uri provided. If this is not provided
         * all rows will be returned unless whereMatchesSelection was called instead, or you already
         * appended the rowId to the Uri.
         *
         * @param rowId The id of the row to select from the database
         * @return This builder object
         * @throws IllegalStateException If you already called whereMatchesSelection as only the rowId
         *                               or a specific selection should be provided.
         */
        @NonNull
        public RequestBuilder whereMatchesId(long rowId) {
            if (rowId > 0 && !TextUtils.isEmpty(args.getString(ARG_SELECTION))) {
                throw new IllegalStateException("Do not provide both a row id and a selection");
            }
            this.rowId = rowId;
            return this;
        }

        /**
         * Optionally provide a selection and selection arguments to the query. If this is not provided
         * all rows will be returned unless whereMatchesRowId was called instead.
         *
         * @param selection     A selection criteria to apply when filtering rows.
         * @param selectionArgs You may include ?s in selection, which will be replaced by
         *                      the values from selectionArgs, in order that they appear in the selection.
         *                      The values will be bound as Strings. May pass a String[] or comma separated
         *                      strings for each argument. Passing null means nothing in the selection needs
         *                      to be replaced.
         * @return This builder object
         * @throws IllegalStateException If you already called whereMatchesRowId as only the rowId
         *                               or a specific selection should be provided.
         */
        @NonNull
        public RequestBuilder whereMatchesSelection(@NonNull String selection, @Nullable String... selectionArgs) {
            if (rowId > 0) {
                throw new IllegalStateException("Do not provide both a row id and a selection");
            }
            args.putString(ARG_SELECTION, selection);
            args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
            return this;
        }

        /**
         * Optionally provide an order by clause for the query.
         *
         * @param orderBy How the rows in the cursor should be sorted.
         *                If {@code null} then the provider is free to define the sort order.
         * @return This builder object
         */
        @NonNull
        public RequestBuilder orderBy(@Nullable String orderBy) {
            args.putString(ARG_SORT_ORDER, orderBy);
            return this;
        }

        /**
         * Optionally provide a limit clause for the query
         *
         * @param limit Limits the number of rows returned by the query,
         *              formatted as LIMIT clause. Passing null denotes no LIMIT clause.
         * @return This builder object
         */
        @NonNull
        public RequestBuilder limit(@Nullable String limit) {
            this.limit = TextUtils.isEmpty(limit) ? null : limit;
            return this;
        }

        /**
         * Ensures a loader is initialized and active.  If the loader doesn't
         * already exist, one is created and (if the activity/fragment is currently
         * started) starts the loader.  Otherwise the last created
         * loader is re-used.
         *
         * <p>In either case, the given callback is associated with the loader, and
         * will be called as the loader state changes.  If at the point of call
         * the caller is in its started state, and the requested loader
         * already exists and has generated its data, then the callback
         * {@link Callback#onCursorLoaded(int, Cursor)}
         * will be called immediately (inside of this function), so you must be prepared
         * for this to happen.
         *
         * @return The created {@link android.support.v4.content.CursorLoader}
         * @throws IllegalStateException If the Uri is not provided or is null.
         */
        @NonNull
        public CursorLoader initLoader() {
            validateAndPrepareLoaderValues();
            return (CursorLoader) loaderManager.initLoader(loaderId, args, basicCRUDLoader);
        }

        /**
         * Starts a new or restarts an existing {@link android.support.v4.content.CursorLoader} in
         * this manager, registers the callbacks to it,
         * and (if the activity/fragment is currently started) starts loading it.
         * If a loader with the same id has previously been
         * started it will automatically be destroyed when the new loader completes
         * its work. The callback will be delivered before the old loader
         * is destroyed.
         *
         * <p>This is more expensive if not reusing the same loader across rotation, as the existing
         * loader with valid data will be destroyed only to query the same information again.
         *
         * @return The created {@link android.support.v4.content.CursorLoader}
         * @throws IllegalStateException If the Uri is not provided or is null.
         */
        @NonNull
        public CursorLoader restartLoader() {
            validateAndPrepareLoaderValues();
            return (CursorLoader) loaderManager.restartLoader(loaderId, args, basicCRUDLoader);
        }

        /**
         * Helper to validate required parameters are provided and prepare user input to
         * initialize or restart the laoder.
         */
        private void validateAndPrepareLoaderValues() {
            if (loaderStarted) {
                throw new IllegalStateException("Loader already started. Use a new builder");
            }
            loaderStarted = true;

            // validate and prepare the Uri
            finalizeUri();

            if (DEBUG && loaderId == 1) {
                Log.w(TAG, "Using default loader id. May cause issues with multiple loaders");
            }

            if (loaderCallback == null) {
                throw new IllegalStateException("Must provide a BasicCRUDLoaderCallback");
            }

            basicCRUDLoader = new BasicCRUDLoader(context, loaderCallback);
        }

        /**
         * Helper to append the rowId to the URI and add the distinct and limit query parameters
         * to the query if provided.
         *
         * @throws IllegalStateException If the Uri is not provided or is null.
         */
        private void finalizeUri() {
            Uri uri = args.getParcelable(ARG_URI);

            boolean modified = false;

            if (uri == null) {
                throw new IllegalStateException("Uri not provided");
            }

            if (rowId > 0) {
                uri = ContentUris.withAppendedId(uri, rowId);
                modified = true;
            }

            if (distinct || limit != null) {
                Uri.Builder uriBuilder = uri.buildUpon();
                // clear any current query. If using builder, shouldn't have parameters already
                uriBuilder.clearQuery();
                modified = true;

                if (distinct) {
                    uriBuilder.appendQueryParameter(BasicCRUDProvider.DISTINCT_PARAMETER, Boolean.TRUE.toString());
                }
                if (limit != null) {
                    uriBuilder.appendQueryParameter(BasicCRUDProvider.LIMIT_PARAMETER, limit);
                }

                uri = uriBuilder.build();
            }

            if (modified) {
                args.putParcelable(ARG_URI, uri);
            }
        }
    }
}
