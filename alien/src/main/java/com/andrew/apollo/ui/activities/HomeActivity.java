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

package com.andrew.apollo.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ListView;

import com.andrew.apollo.R;
import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import com.andrew.apollo.utils.MusicUtils;

import static com.andrew.apollo.utils.MusicUtils.mService;

/**
 * This class is used to display the {@link android.support.v4.view.ViewPager} used to swipe between the
 * main {@link android.support.v4.app.Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeActivity extends BaseActivity {
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private ListView mDrawerListView;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the music browser fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_base_content,
                            new MusicBrowserPhoneFragment()).commit();
        }
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // transWindows();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                    mToolbar, R.string.drawer_open, R.string.drawer_close);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
        mDrawerListView = (ListView) findViewById(R.id.left_drawer);
        // int padding = getResources().getDimensionPixelSize(
        // R.dimen.actionbar_size);
        // ViewGroup.MarginLayoutParams localMarginLayoutParams =
        // (ViewGroup.MarginLayoutParams) mDrawerListView
        // .getLayoutParams();
        // localMarginLayoutParams.topMargin = padding;
        // mDrawerListView.setLayoutParams(localMarginLayoutParams);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle != null
                && mDrawerToggle.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        startPlayback();
    }

    /**
     * Checks whether the passed intent contains a playback request, and starts
     * playback if that's the case
     */
    private void startPlayback() {
        Intent intent = getIntent();

        if (intent == null || mService == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(this, uri);
            handled = true;
        } else if (Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = intent.getLongExtra("playlistId", -1);
            if (id < 0) {
                String idString = intent.getStringExtra("playlist");
                if (idString != null) {
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
            if (id >= 0) {
                MusicUtils.playPlaylist(this, id);
                handled = true;
            }
        }

        if (handled) {
            // Make sure to process intent only once
            setIntent(new Intent());
            // Refresh the queue
            // ((QueueFragment) mPagerAdapter.getFragment(0)).refreshQueue();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setContentView() {
        return R.layout.activity_base;
    }
}
