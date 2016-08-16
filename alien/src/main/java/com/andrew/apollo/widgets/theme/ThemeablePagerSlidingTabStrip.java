package com.andrew.apollo.widgets.theme;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.astuetz.PagerSlidingTabStrip;

public class ThemeablePagerSlidingTabStrip extends PagerSlidingTabStrip {
    /**
     * Resource name used to theme the background
     */
    private static final String BACKGROUND = "tpi_background";

    /**
     * Resource name used to theme the selected text color
     */
    private static final String SELECTED_TEXT = "tpi_selected_text_color";

    /**
     * Resource name used to theme the unselected text color
     */
    private static final String TEXT = "tpi_unselected_text_color";

    /**
     * Resource name used to theme the footer color
     */
    private static final String FOOTER = "tpi_footer_color";

    /**
     * @param context The {@link android.content.Context} to use
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public ThemeablePagerSlidingTabStrip(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        // Initialze the theme resources
        final ThemeUtils resources = new ThemeUtils(context);
        // Theme the background
        //setBackgroundDrawable(resources.getDrawable(BACKGROUND));
        //setBackgroundColor(0xff5677fc);
        // Theme the selected text color
        //setIndicatorColor(resources.getColor(SELECTED_TEXT));
        setIndicatorColor(PreferenceUtils.getInstance(context).getDefaultThemeColor(context));
        setDividerColor(Color.TRANSPARENT);
        // Theme the unselected text color
        //setTextColor(resources.getColor(TEXT));
        // Theme the footer
        //setFooterColor(resources.getColor(FOOTER));
    }
}
