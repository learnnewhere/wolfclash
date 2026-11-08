package com.wolfclash.arena;

import android.webkit.JavascriptInterface;

/** Exposed to the game page as window.WolfAds. */
public class WolfAds {
    private final AdsManager ads;

    public WolfAds(AdsManager ads) {
        this.ads = ads;
    }

    @JavascriptInterface
    public boolean needsAge() {
        return ads.needsAge();
    }

    @JavascriptInterface
    public void setBirthYear(int year) {
        ads.setBirthYear(year);
    }

    @JavascriptInterface
    public boolean rewardedReady() {
        return ads.rewardedReady();
    }

    @JavascriptInterface
    public boolean interstitialReady() {
        return ads.interstitialReady();
    }

    @JavascriptInterface
    public void showRewarded(String tag) {
        ads.showRewarded(tag);
    }

    @JavascriptInterface
    public void showInterstitial() {
        ads.showInterstitial();
    }

    @JavascriptInterface
    public boolean privacyRequired() {
        return ads.privacyOptionsRequired();
    }

    @JavascriptInterface
    public void showPrivacy() {
        ads.showPrivacyOptions();
    }
}
