package com.wolfclash.arena;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads and shows AdMob ads, and asks for privacy consent first (required for players in the
 * EEA, UK and Switzerland). Results are reported to the game page through
 * window.onAdReward(tag, earned) and window.onAdClosed().
 *
 * Before any ad or consent request the player is asked for their birth year (a neutral age
 * screen in the game page). The year is stored only on the device and is used to tag requests:
 * under 13 = child-directed treatment, under 16 = under the age of consent (no personalised ads,
 * no consent form), and every request is capped at a Teen ad content rating.
 */
public class AdsManager {
    private static final long RETRY_MS = 30000;
    /** Loaded ads expire after one hour; refresh a little before that. */
    private static final long MAX_AGE_MS = 55 * 60 * 1000L;

    private final Activity activity;
    private final WebView web;
    private final ConsentInformation consent;
    private final SharedPreferences prefs;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean sdkStarted = new AtomicBoolean(false);

    private volatile InterstitialAd interstitial;
    private volatile RewardedAd rewarded;
    private boolean loadingInterstitial, loadingRewarded;
    private volatile long interstitialLoadedAt, rewardedLoadedAt;

    public AdsManager(Activity activity, WebView web) {
        this.activity = activity;
        this.web = web;
        this.consent = UserMessagingPlatform.getConsentInformation(activity);
        this.prefs = activity.getSharedPreferences("wolf", Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- age
    /** True until the player has answered the age screen. */
    public boolean needsAge() {
        return prefs.getInt("birthYear", 0) == 0;
    }

    /** Called from the age screen. Stores the year on the device and starts consent + ads. */
    public void setBirthYear(int year) {
        int now = Calendar.getInstance().get(Calendar.YEAR);
        if (year < now - 120 || year > now) return;
        prefs.edit().putInt("birthYear", year).apply();
        main.post(this::start);
    }

    /** Age in whole years, rounded down to the younger possibility (year only is known). */
    private int age() {
        int year = prefs.getInt("birthYear", 0);
        return Calendar.getInstance().get(Calendar.YEAR) - year - 1;
    }

    /** Call from onCreate (does nothing until the age screen was answered). */
    public void start() {
        if (needsAge() || started.getAndSet(true)) return;
        int age = age();
        boolean child = age < 13, underConsentAge = age < 16;
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(child
                        ? RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                        : RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
                .setTagForUnderAgeOfConsent(underConsentAge
                        ? RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                        : RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
                .setMaxAdContentRating(child
                        ? RequestConfiguration.MAX_AD_CONTENT_RATING_G
                        : RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build());
        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(underConsentAge)
                .build();
        consent.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (consent.canRequestAds()) startSdk();
                }),
                requestError -> {
                    if (consent.canRequestAds()) startSdk();
                });
        // Consent given in an earlier session lets ads start right away.
        if (consent.canRequestAds()) startSdk();
    }

    private void startSdk() {
        if (sdkStarted.getAndSet(true)) return;
        new Thread(() -> MobileAds.initialize(activity, status -> main.post(() -> {
            loadInterstitial();
            loadRewarded();
        }))).start();
    }

    // ---------------------------------------------------------------- loading
    private void loadInterstitial() {
        if (!sdkStarted.get() || interstitial != null || loadingInterstitial) return;
        loadingInterstitial = true;
        InterstitialAd.load(activity, AdIds.INTERSTITIAL, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitial = ad;
                        interstitialLoadedAt = System.currentTimeMillis();
                        loadingInterstitial = false;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        loadingInterstitial = false;
                        main.postDelayed(AdsManager.this::loadInterstitial, RETRY_MS);
                    }
                });
    }

    private void loadRewarded() {
        if (!sdkStarted.get() || rewarded != null || loadingRewarded) return;
        loadingRewarded = true;
        RewardedAd.load(activity, AdIds.REWARDED, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewarded = ad;
                        rewardedLoadedAt = System.currentTimeMillis();
                        loadingRewarded = false;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        loadingRewarded = false;
                        main.postDelayed(AdsManager.this::loadRewarded, RETRY_MS);
                    }
                });
    }

    // ---------------------------------------------------------------- state
    public boolean rewardedReady() {
        if (rewarded != null && System.currentTimeMillis() - rewardedLoadedAt > MAX_AGE_MS) {
            rewarded = null;
            main.post(this::loadRewarded);
        }
        return rewarded != null;
    }

    public boolean interstitialReady() {
        if (interstitial != null && System.currentTimeMillis() - interstitialLoadedAt > MAX_AGE_MS) {
            interstitial = null;
            main.post(this::loadInterstitial);
        }
        return interstitial != null;
    }

    public boolean privacyOptionsRequired() {
        return consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    // ---------------------------------------------------------------- showing
    public void showInterstitial() {
        main.post(() -> {
            InterstitialAd ad = interstitial;
            if (ad == null) {
                js("window.onAdClosed && window.onAdClosed()");
                return;
            }
            interstitial = null;
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    js("window.onAdClosed && window.onAdClosed()");
                    loadInterstitial();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError error) {
                    js("window.onAdClosed && window.onAdClosed()");
                    loadInterstitial();
                }
            });
            ad.show(activity);
        });
    }

    /** @param tag "revive" or "double"; echoed back to the game with the result. */
    public void showRewarded(String tag) {
        final String safeTag = "double".equals(tag) ? "double" : "revive";
        main.post(() -> {
            RewardedAd ad = rewarded;
            if (ad == null) {
                reward(safeTag, false);
                return;
            }
            rewarded = null;
            final boolean[] earned = {false};
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    reward(safeTag, earned[0]);
                    loadRewarded();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError error) {
                    reward(safeTag, false);
                    loadRewarded();
                }
            });
            ad.show(activity, rewardItem -> earned[0] = true);
        });
    }

    public void showPrivacyOptions() {
        main.post(() -> UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> { }));
    }

    private void reward(String tag, boolean earned) {
        js("window.onAdReward && window.onAdReward('" + tag + "', " + earned + ")");
    }

    private void js(String script) {
        main.post(() -> web.evaluateJavascript(script, null));
    }
}
