package com.andrew.apollo.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.DiskLruCache;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.ui.activities.SettingsActivity;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.andrew.apollo.widgets.ColorSchemeDialog;
import com.way.music.lrc.manager.LyricFetcher;

import java.io.IOException;

public class SettingsFragment extends PreferenceFragment {
    /**
     * Image cache
     */
    private ImageCache mImageCache;

    private PreferenceUtils mPreferences;
    private SettingsActivity mActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        mActivity = (SettingsActivity) getActivity();
        ThemeUtils mThemeUtils = new ThemeUtils(mActivity);
        int color = PreferenceUtils.getInstance(mActivity)
                .getDefaultThemeColor(mActivity);
        mThemeUtils.transWindows(mActivity, color);
        mActivity.getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(color));
        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(mActivity);

        // Initialze the image cache
        mImageCache = ImageCache.getInstance(mActivity);

        // UP
        // mActivity.getActionBar().setDisplayHomeAsUpEnabled(true);

        // Color scheme picker
        updateColorScheme();
        // Removes the cache entries
        deleteCache();
        // Update the version number
        try {
            final PackageInfo packageInfo = mActivity.getPackageManager()
                    .getPackageInfo(mActivity.getPackageName(), 0);
            findPreference("version").setSummary(packageInfo.versionName);
        } catch (final NameNotFoundException e) {
            findPreference("version").setSummary("?");
        }
    }

    /**
     * Shows the {@link ColorSchemeDialog} and then saves the changes.
     */
    private void updateColorScheme() {
        final Preference colorScheme = findPreference("color_scheme");
        colorScheme
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        ApolloUtils.showColorPicker(mActivity);
                        return true;
                    }
                });
    }

    /**
     * Removes all of the cache entries.
     */
    private void deleteCache() {
        final Preference deleteCache = findPreference("delete_cache");
        deleteCache
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        new AlertDialog.Builder(mActivity)
                                .setMessage(R.string.delete_warning)
                                .setPositiveButton(android.R.string.ok,
                                        new OnClickListener() {

                                            @Override
                                            public void onClick(
                                                    final DialogInterface dialog,
                                                    final int which) {
                                                mImageCache.clearCaches();
                                                deleteLyricCaches();
                                                PreferenceUtils.getInstance(
                                                        mActivity)
                                                        .setChangeThemeColor(
                                                                true);
                                            }

                                        })
                                .setNegativeButton(R.string.cancel, null)
                                .create().show();
                        return true;
                    }
                });
    }

    private void deleteLyricCaches() {
        try {
            DiskLruCache.deleteContents(LyricFetcher.getLyricDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
