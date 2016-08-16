package com.way.longplay;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*View view = new LongPlayViewEx(this);
        view.setBackgroundColor(0x80000000);
        setContentView(view);*/

        setContentView(R.layout.activity_main);

    }
}
