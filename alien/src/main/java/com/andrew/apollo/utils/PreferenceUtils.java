/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.andrew.apollo.R;

/**
 * A collection of helpers designed to get and set various preferences across
 * Apollo.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class PreferenceUtils {

    /* Default start page (Artist page) */
    public static final int DEFFAULT_PAGE = 2;

    /* Saves the last page the pager was on in {@link MusicBrowserPhoneFragment} */
    public static final String START_PAGE = "start_page";

    // Sort order for the artist list
    public static final String ARTIST_SORT_ORDER = "artist_sort_order";

    // Sort order for the artist song list
    public static final String ARTIST_SONG_SORT_ORDER = "artist_song_sort_order";

    // Sort order for the artist album list
    public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";

    // Sort order for the album list
    public static final String ALBUM_SORT_ORDER = "album_sort_order";

    // Sort order for the album song list
    public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";

    // Sort order for the song list
    public static final String SONG_SORT_ORDER = "song_sort_order";

    // Sets the type of layout to use for the artist list
    public static final String ARTIST_LAYOUT = "artist_layout";

    // Sets the type of layout to use for the album list
    public static final String ALBUM_LAYOUT = "album_layout";

    // Sets the type of layout to use for the recent list
    public static final String RECENT_LAYOUT = "recent_layout";

    // Key used to download images only on Wi-Fi
    public static final String ONLY_ON_WIFI = "only_on_wifi";
    public static final String DOWNLOAD_MISSING_LYRIC_ARTWORK = "download_missing_lyric_artwork";


    // Key used to set the overall theme color
    public static final String DEFAULT_THEME_COLOR = "default_theme_color";

    public static final String CHANGE_THEME_COLOR = "change_theme_color";

    private static PreferenceUtils sInstance;

    private final SharedPreferences mPreferences;

    /**
     * Constructor for <code>PreferenceUtils</code>
     *
     * @param context The {@link android.content.Context} to use.
     */
    public PreferenceUtils(final Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @param context The {@link android.content.Context} to use.
     * @return A singleton of this class
     */
    public static final PreferenceUtils getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Returns the last page the user was on when the app was exited.
     *
     * @return The page to start on when the app is opened.
     */
    public final int getStartPage() {
        return mPreferences.getInt(START_PAGE, DEFFAULT_PAGE);
    }

    /**
     * Saves the current page the user is on when they close the app.
     *
     * @param value The last page the pager was on when the onDestroy is called in
     *              {@link com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment}.
     */
    public void setStartPage(final int value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(START_PAGE, value);
                editor.apply();

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Sets the new theme color.
     *
     * @param value The new theme color to use.
     */
    public void setDefaultThemeColor(final int value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(DEFAULT_THEME_COLOR, value);
                editor.apply();

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Returns the current theme color.
     *
     * @param context The {@link android.content.Context} to use.
     * @return The default theme color.
     */
    public final int getDefaultThemeColor(final Context context) {
        return mPreferences.getInt(DEFAULT_THEME_COLOR, context.getResources()
                .getColor(R.color.material_blue));
    }

    public final void setChangeThemeColor(final boolean isChange) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(CHANGE_THEME_COLOR, isChange);
                editor.apply();

                return null;
            }
        }, (Void[]) null);
    }

    public final boolean getChangeThemeColor(final Context context) {
        return mPreferences.getBoolean(CHANGE_THEME_COLOR, false);
    }

    /**
     * @return True if the user has checked to only download images on Wi-Fi,
     * false otherwise
     */
    public final boolean onlyOnWifi() {
        return mPreferences.getBoolean(ONLY_ON_WIFI, true);
    }

    /**
     * @return True if the user has checked to download missing album covers,
     * false otherwise.
     */
    public final boolean downloadMissingLyricAndArtwork() {
        return mPreferences.getBoolean(DOWNLOAD_MISSING_LYRIC_ARTWORK, true);
    }

    /**
     * Saves the sort order for a list.
     *
     * @param key   Which sort order to change
     * @param value The new sort order
     */
    private void setSortOrder(final String key, final String value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(key, value);
                editor.apply();

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * @return The sort order used for the artist list in {@link com.andrew.apollo.ui.fragments.ArtistFragment}
     */
    public final String getArtistSortOrder() {
        return mPreferences.getString(ARTIST_SORT_ORDER,
                SortOrder.ArtistSortOrder.ARTIST_A_Z);
    }

    /**
     * Sets the sort order for the artist list.
     *
     * @param value The new sort order
     */
    public void setArtistSortOrder(final String value) {
        setSortOrder(ARTIST_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist song list in
     * {@link com.andrew.apollo.ui.fragments.profile.ArtistSongFragment}
     */
    public final String getArtistSongSortOrder() {
        return mPreferences.getString(ARTIST_SONG_SORT_ORDER,
                SortOrder.ArtistSongSortOrder.SONG_A_Z);
    }

    /**
     * Sets the sort order for the artist song list.
     *
     * @param value The new sort order
     */
    public void setArtistSongSortOrder(final String value) {
        setSortOrder(ARTIST_SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist album list in
     * {@link com.andrew.apollo.ui.fragments.profile.ArtistAlbumFragment}
     */
    public final String getArtistAlbumSortOrder() {
        return mPreferences.getString(ARTIST_ALBUM_SORT_ORDER,
                SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
    }

    /**
     * Sets the sort order for the artist album list.
     *
     * @param value The new sort order
     */
    public void setArtistAlbumSortOrder(final String value) {
        setSortOrder(ARTIST_ALBUM_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the album list in {@link com.andrew.apollo.ui.fragments.AlbumFragment}
     */
    public final String getAlbumSortOrder() {
        return mPreferences.getString(ALBUM_SORT_ORDER,
                SortOrder.AlbumSortOrder.ALBUM_A_Z);
    }

    /**
     * Sets the sort order for the album list.
     *
     * @param value The new sort order
     */
    public void setAlbumSortOrder(final String value) {
        setSortOrder(ALBUM_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the album song in
     * {@link com.andrew.apollo.ui.fragments.profile.AlbumSongFragment}
     */
    public final String getAlbumSongSortOrder() {
        return mPreferences.getString(ALBUM_SONG_SORT_ORDER,
                SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
    }

    /**
     * Sets the sort order for the album song list.
     *
     * @param value The new sort order
     */
    public void setAlbumSongSortOrder(final String value) {
        setSortOrder(ALBUM_SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the song list in {@link com.andrew.apollo.ui.fragments.SongFragment}
     */
    public final String getSongSortOrder() {
        return mPreferences.getString(SONG_SORT_ORDER,
                SortOrder.SongSortOrder.SONG_A_Z);
    }

    /**
     * Sets the sort order for the song list.
     *
     * @param value The new sort order
     */
    public void setSongSortOrder(final String value) {
        setSortOrder(SONG_SORT_ORDER, value);
    }

    /**
     * Saves the layout type for a list
     *
     * @param key   Which layout to change
     * @param value The new layout type
     */
    private void setLayoutType(final String key, final String value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(key, value);
                editor.apply();

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Sets the layout type for the artist list
     *
     * @param value The new layout type
     */
    public void setArtistLayout(final String value) {
        setLayoutType(ARTIST_LAYOUT, value);
    }

    /**
     * Sets the layout type for the album list
     *
     * @param value The new layout type
     */
    public void setAlbumLayout(final String value) {
        setLayoutType(ALBUM_LAYOUT, value);
    }

    /**
     * Sets the layout type for the recent list
     *
     * @param value The new layout type
     */
    public void setRecentLayout(final String value) {
        setLayoutType(RECENT_LAYOUT, value);
    }

    /**
     * @param context The {@link android.content.Context} to use.
     * @param which   Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isSimpleLayout(final String which, final Context context) {
        final String simple = "simple";
        final String defaultValue = "grid";
        return mPreferences.getString(which, defaultValue).equals(simple);
    }

    /**
     * @param context The {@link android.content.Context} to use.
     * @param which   Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isDetailedLayout(final String which, final Context context) {
        final String detailed = "detailed";
        final String defaultValue = "grid";
        return mPreferences.getString(which, defaultValue).equals(detailed);
    }

    /**
     * @param context The {@link android.content.Context} to use.
     * @param which   Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isGridLayout(final String which, final Context context) {
        final String grid = "grid";
        final String defaultValue = "simple";
        return mPreferences.getString(which, defaultValue).equals(grid);
    }

}
