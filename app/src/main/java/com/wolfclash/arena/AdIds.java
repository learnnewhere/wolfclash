package com.wolfclash.arena;

/**
 * AdMob ad unit IDs, set per build type in app/build.gradle.
 *
 * Release (Play Store) builds:
 *   Rewarded     ca-app-pub-1765077834611057/1000942582  (revive + 2x boss score)
 *   Interstitial ca-app-pub-1765077834611057/8687860914  (between runs, every 3rd run at most)
 * Debug builds use Google's official test IDs.
 *
 * Never tap real ads in your own app; AdMob can suspend the account for that.
 */
public final class AdIds {
    private AdIds() {}

    public static final String REWARDED = BuildConfig.ADMOB_REWARDED;
    public static final String INTERSTITIAL = BuildConfig.ADMOB_INTERSTITIAL;
}
