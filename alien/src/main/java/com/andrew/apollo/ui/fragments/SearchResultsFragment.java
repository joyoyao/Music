package com.andrew.apollo.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.format.PrefixHighlighter;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.ui.MusicHolder;
import com.andrew.apollo.ui.activities.BaseActivity;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import java.util.Locale;

public class SearchResultsFragment extends Fragment implements
        LoaderCallbacks<Cursor>, OnScrollListener, OnItemClickListener {
    /**
     * Grid view column count. ONE - list, TWO - normal grid
     */
    private static final int ONE = 1, TWO = 2;
    /**
     * The query
     */
    private String mFilterString;

    /**
     * Grid view
     */
    private GridView mGridView;

    /**
     * List view adapter
     */
    private SearchAdapter mAdapter;
    private SearchView mSearchView;
    private View mRootView;

    public void setSearchView(SearchView searchView) {
        mSearchView = searchView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.grid_base, container, false);
        mRootView.setBackgroundResource(android.R.color.white);
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize the adapter
        mAdapter = new SearchAdapter(getActivity());
        // Set the prefix
        mAdapter.setPrefix(mFilterString);
        // Initialze the list
        mGridView = (GridView) view.findViewById(R.id.grid_base);
        // Bind the data
        mGridView.setAdapter(mAdapter);
        // Recycle the data
        mGridView.setRecyclerListener(new RecycleHolder());
        // Seepd up scrolling
        mGridView.setOnScrollListener(this);
        mGridView.setOnItemClickListener(this);
        if (ApolloUtils.isLandscape(getActivity())) {
            mGridView.setNumColumns(TWO);
        } else {
            mGridView.setNumColumns(ONE);
        }
        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return;
        }
        // Get the MIME type
        final String mimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

        // If it's an artist, open the artist profile
        if ("artist".equals(mimeType)) {
            NavUtils.openArtistProfile(getActivity(),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)));
        } else if ("album".equals(mimeType)) {
            // If it's an album, open the album profile
            NavUtils.openAlbumProfile(getActivity(),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)));
        } else if (position >= 0 && id >= 0) {
            // If it's a song, play it and leave
            final long[] list = new long[]{
                    id
            };
            MusicUtils.playAll(getActivity(), list, 0, false);
        }

        // Close it up
        cursor.close();
        cursor = null;
        // All done
        ((BaseActivity) getActivity()).revertToInitialFragment();
        //finish();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING
                || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            mAdapter.setPauseDiskCache(true);
        } else {
            mAdapter.setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final Uri uri = Uri.parse("content://media/external/audio/search/fancy/"
                + Uri.encode(mFilterString));
        final String[] projection = new String[]{
                BaseColumns._ID, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Media.TITLE, "data1", "data2"
        };
        return new CursorLoader(getActivity(), uri, projection, null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        if (data == null || data.isClosed() || data.getCount() <= 0) {
            // Set the empty text
            final TextView empty = (TextView) mRootView.findViewById(R.id.empty);
            empty.setText(getString(R.string.empty_search));
            mGridView.setEmptyView(empty);
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    public boolean onQueryTextSubmit(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        // When the search is "committed" by the user, then hide the keyboard so
        // the user can
        // more easily browse the list of results.
        if (mSearchView != null) {
            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();
        }
        //mFilterString = !TextUtils.isEmpty(query) ? query : null;
        //getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    public boolean onQueryTextChange(String newText) {
        mFilterString = !TextUtils.isEmpty(newText) ? newText : null;
        mAdapter.setPrefix(mFilterString);
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    /**
     * Used to populate the list view with the search results.
     */
    private static final class SearchAdapter extends CursorAdapter {

        /**
         * Number of views (ImageView and TextView)
         */
        private static final int VIEW_TYPE_COUNT = 2;

        /**
         * Image cache and image fetcher
         */
        private final ImageFetcher mImageFetcher;

        /**
         * Highlights the query
         */
        private final PrefixHighlighter mHighlighter;

        /**
         * The prefix that's highlighted
         */
        private char[] mPrefix;

        /**
         * Constructor for <code>SearchAdapter</code>
         *
         * @param context The {@link android.content.Context} to use.
         */
        public SearchAdapter(final Activity context) {
            super(context, null, false);
            // Initialize the cache & image fetcher
            mImageFetcher = ApolloUtils.getImageFetcher(context);
            // Create the prefix highlighter
            mHighlighter = new PrefixHighlighter(context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bindView(final View convertView, final Context context,
                             final Cursor cursor) {
            /* Recycle ViewHolder's items */
            MusicHolder holder = (MusicHolder) convertView.getTag();
            if (holder == null) {
                holder = new MusicHolder(convertView);
                convertView.setTag(holder);
            }

            // Get the MIME type
            final String mimetype = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

            if (mimetype.equals("artist")) {
                holder.mImage.get().setScaleType(ScaleType.CENTER_CROP);

                // Get the artist name
                final String artist = cursor
                        .getString(cursor
                                .getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                holder.mLineOne.get().setText(artist);

                // Get the album count
                final int albumCount = cursor.getInt(cursor
                        .getColumnIndexOrThrow("data1"));
                holder.mLineTwo.get().setText(
                        MusicUtils.makeLabel(context, R.plurals.Nalbums,
                                albumCount));

                // Get the song count
                final int songCount = cursor.getInt(cursor
                        .getColumnIndexOrThrow("data2"));
                holder.mLineThree.get().setText(
                        MusicUtils.makeLabel(context, R.plurals.Nsongs,
                                songCount));

                // Asynchronously load the artist image into the adapter
                mImageFetcher.loadArtistImage(artist, holder.mImage.get());

                // Highlght the query
                mHighlighter.setText(holder.mLineOne.get(), artist, mPrefix);
            } else if (mimetype.equals("album")) {
                holder.mImage.get().setScaleType(ScaleType.FIT_XY);

                // Get the Id of the album
                final long id = cursor.getLong(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));

                // Get the album name
                final String album = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
                holder.mLineOne.get().setText(album);

                // Get the artist name
                final String artist = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
                holder.mLineTwo.get().setText(artist);

                // Asynchronously load the album images into the adapter
                mImageFetcher.loadAlbumImage(artist, album, id,
                        holder.mImage.get());
                // Asynchronously load the artist image into the adapter
                // mImageFetcher.loadArtistImage(artist,
                // holder.mBackground.get());
                // mImageFetcher.loadArtistImage(artist, holder.mImage.get());

                // Highlght the query
                mHighlighter.setText(holder.mLineOne.get(), album, mPrefix);

            } else if (mimetype.startsWith("audio/")
                    || mimetype.equals("application/ogg")
                    || mimetype.equals("application/x-ogg")) {
                holder.mImage.get().setScaleType(ScaleType.FIT_XY);
                holder.mImage.get().setImageResource(R.drawable.header_temp);

                // Get the track name
                final String track = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                holder.mLineOne.get().setText(track);

                // Get the album name
                final String album = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                holder.mLineTwo.get().setText(album);

                final String artist = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                // Asynchronously load the artist image into the adapter
                // mImageFetcher.loadArtistImage(artist,
                // holder.mBackground.get());
                mImageFetcher.loadArtistImage(artist, holder.mImage.get());
                holder.mLineThree.get().setText(artist);

                // Highlght the query
                mHighlighter.setText(holder.mLineOne.get(), track, mPrefix);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View newView(final Context context, final Cursor cursor,
                            final ViewGroup parent) {
            return ((Activity) context).getLayoutInflater().inflate(
                    R.layout.list_item_detailed, parent, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasStableIds() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        /**
         * @param pause True to temporarily pause the disk cache, false otherwise.
         */
        public void setPauseDiskCache(final boolean pause) {
            if (mImageFetcher != null) {
                mImageFetcher.setPauseDiskCache(pause);
            }
        }

        /**
         * @param prefix The query to filter.
         */
        public void setPrefix(final CharSequence prefix) {
            if (!TextUtils.isEmpty(prefix)) {
                mPrefix = prefix.toString().toUpperCase(Locale.getDefault())
                        .toCharArray();
            } else {
                mPrefix = null;
            }
        }
    }

}
