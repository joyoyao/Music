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

package com.andrew.apollo.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.MusicHolder;
import com.andrew.apollo.ui.MusicHolder.DataHolder;
import com.andrew.apollo.utils.PreferenceUtils;

/**
 * This {@link android.widget.ArrayAdapter} is used to display all of the songs on a user's
 * device for {@link com.andrew.apollo.ui.fragments.SongFragment}. It is also used to show the queue in
 * {@link com.andrew.apollo.ui.fragments.QueueFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongAdapter extends ArrayAdapter<Song> {

    /**
     * Number of views (TextView)
     */
    private static final int VIEW_TYPE_COUNT = 1;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Used to cache the song info
     */
    private DataHolder[] mData;
    private Context mContext;

    /**
     * Constructor of <code>SongAdapter</code>
     *
     * @param context  The {@link android.content.Context} to use.
     * @param layoutId The resource Id of the view to inflate.
     */
    public SongAdapter(final Context context, final int layoutId) {
        super(context, 0);
        mContext = context;
        // Get the layout Id
        mLayoutId = layoutId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            //ImageView iv = convertView.findViewById(R.id.edit_track_list_item_handle);
            // Hide the third line of text
            holder.mLineThree.get().setVisibility(View.GONE);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        // Retrieve the data holder
        final DataHolder dataHolder = mData[position];

        // Set each song name (line one)
        holder.mLineOne.get().setText(dataHolder.mLineOne);
        // Set the album name (line two)
        holder.mLineTwo.get().setText(dataHolder.mLineTwo);
        ImageView iv = (ImageView) convertView.findViewById(R.id.edit_track_list_item_handle);
        if (iv != null) {
            Drawable d = iv.getDrawable();
            if (d != null)
                d.setColorFilter(PreferenceUtils.getInstance(getContext()).getDefaultThemeColor(getContext()), PorterDuff.Mode.MULTIPLY);
//        	d.setColorFilter(
//    				new PorterDuffColorFilter(PreferenceUtils.getInstance(getContext()).getDefaultThemeColor(getContext()),
//    						PorterDuff.Mode.SRC_ATOP));
        }
        return convertView;
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
     * Method used to cache the data used to populate the list or grid. The idea
     * is to cache everything before {@code #getView(int, View, ViewGroup)} is
     * called.
     */
    public void buildCache() {
        mData = new DataHolder[getCount()];
        for (int i = 0; i < getCount(); i++) {
            // Build the song
            final Song song = getItem(i);

            // Build the data holder
            mData[i] = new DataHolder();
            // Song Id
            mData[i].mItemId = song.mSongId;
            // Song names (line one)
            mData[i].mLineOne = song.mSongName;
            // Album names (line two)
            mData[i].mLineTwo = song.mAlbumName;
        }
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        clear();
        mData = null;
    }

}
