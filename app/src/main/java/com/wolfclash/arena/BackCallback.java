package com.wolfclash.arena;

import android.window.OnBackInvokedCallback;

/** Android 13+ back gesture / button handler (onBackPressed is not called when targeting API 36). */
public class BackCallback implements OnBackInvokedCallback {
    private final MainActivity activity;

    public BackCallback(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onBackInvoked() {
        activity.handleBack();
    }
}
