package com.andrew.apollo.widgets.theme;

import android.content.Context;
import android.util.AttributeSet;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;
import com.viewpagerindicator.CirclePageIndicator;

public class ThemeableCirclePageIndicator extends CirclePageIndicator {

    public ThemeableCirclePageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFillColor(PreferenceUtils.getInstance(context).getDefaultThemeColor(context));
        setStrokeColor(getResources().getColor(R.color.action_bar_title));
//		setRadius(2.0f);
    }

}
