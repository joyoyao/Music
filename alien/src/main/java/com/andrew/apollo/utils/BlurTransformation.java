package com.andrew.apollo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

public class BlurTransformation {

    private BlurTransformation() {
    }

    public static Bitmap transform(Context context, Bitmap source) {
        return transform(context, source, 16f);
    }

    public static Bitmap transform(Context context, Bitmap source,
                                   float radius) {
        if (context == null || source == null)
            return null;
        RenderScript mRenderScript = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(mRenderScript,
                source);
        // Use this constructor for best performance, because it uses
        // USAGE_SHARED mode which reuses
        // memory
        final Allocation output = Allocation.createTyped(mRenderScript,
                input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(
                mRenderScript, Element.U8_4(mRenderScript));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(source);
        return source;
    }
}