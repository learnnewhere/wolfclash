package com.wolfclash.arena;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Checks for a working internet connection in the background by making a
 * real HTTPS request (not just checking that Wi-Fi is on). The game only
 * runs while {@link #isOnline()} is true.
 */
public class NetworkMonitor implements Runnable {
    public static final int CHECKING = 0;
    public static final int ONLINE = 1;
    public static final int OFFLINE = 2;

    private static final String[] PROBES = {
            "https://connectivitycheck.gstatic.com/generate_204",
            "https://www.google.com/generate_204",
            "https://cloudflare.com/cdn-cgi/trace"
    };

    private volatile int state = CHECKING;
    private volatile boolean running;
    private volatile boolean forceCheck;
    private Thread thread;

    public int getState() { return state; }
    public boolean isOnline() { return state == ONLINE; }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "net-monitor");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) thread.interrupt();
        thread = null;
    }

    /** Ask for an immediate re-check (e.g. the user tapped Retry). */
    public void retry() {
        state = CHECKING;
        forceCheck = true;
        Thread t = thread;
        if (t != null) t.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            boolean ok = probe();
            state = ok ? ONLINE : OFFLINE;
            forceCheck = false;
            long wait = ok ? 6000 : 3000;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                // woken up for a retry or stop
            }
        }
    }

    private boolean probe() {
        for (String p : PROBES) {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(p).openConnection();
                c.setConnectTimeout(4000);
                c.setReadTimeout(4000);
                c.setUseCaches(false);
                c.setInstanceFollowRedirects(false);
                int code = c.getResponseCode();
                if (code >= 200 && code < 400) return true;
            } catch (Exception e) {
                // try next probe
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return false;
    }
}
