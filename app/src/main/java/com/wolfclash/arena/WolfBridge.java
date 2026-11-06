package com.wolfclash.arena;

import android.webkit.JavascriptInterface;

/** Exposed to the game page as window.WolfBridge. */
public class WolfBridge {
    private final NetworkMonitor net;

    public WolfBridge(NetworkMonitor net) {
        this.net = net;
    }

    /** 0 = checking, 1 = online, 2 = offline. */
    @JavascriptInterface
    public int netState() {
        return net.getState();
    }

    @JavascriptInterface
    public void retry() {
        net.retry();
    }
}
