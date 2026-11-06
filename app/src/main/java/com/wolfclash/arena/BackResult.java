package com.wolfclash.arena;

import android.app.Activity;
import android.webkit.ValueCallback;

/** Closes the app when the game did not handle the back press itself. */
@SuppressWarnings("rawtypes")
public class BackResult implements ValueCallback {
    private final Activity activity;

    public BackResult(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceiveValue(Object handled) {
        if (handled == null || !String.valueOf(handled).contains("yes")) activity.finish();
    }
}
