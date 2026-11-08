# Notes for Claude

Owner: Vignesh V (learnnewhere@gmail.com). Android game **Wolf Clash Arena**, package `com.wolfclash.arena`, published under a personal Google Play developer account.

Read first: `PROJECT-HISTORY.md` (state, IDs, what's done, what's next) and the build skill `.claude/skills/android-game-cloud-build/SKILL.md`.

## Key facts
- Game = `app/src/main/assets/index.html` (canvas, WebView). Browser copy in `web/wolf-clash-arena.html`; keep both in sync (the asset version adds a doctype wrapper + safe-area padding).
- JS ↔ Android: `window.WolfBridge` (netState/retry), `window.WolfAds` (rewardedReady, interstitialReady, showRewarded(tag), showInterstitial, privacyRequired, showPrivacy). Android calls back `onAndroidBack`, `onAndroidPause`, `onAdReward(tag, ok)`, `onAdClosed()`.
- AdMob real IDs are in `app/build.gradle` defaultConfig; debug build type uses Google test IDs. Don't change the real IDs.
- Current release: 1.5 / versionCode 6. **Bump versionCode for every upload.**
- EU privacy rules kept in the app: no remote fonts (fonts embedded as data URIs), neutral age screen before ads (WolfAds.needsAge/setBirthYear), no analytics/session replay, no purchases. Keep it that way or re-review.
- targetSdk must follow Play's current rule (36 as of Nov 2026). Check before each release.

## How releases are built
The cloud sandbox can't download Google's SDK/Maven, so ad builds run on GitHub Actions (`build-unsigned.yml`) and are signed afterwards with the upload key (`tools/sign-release.sh` on the Mac, or jarsigner + re-align + v2 sign in the sandbox, see skill section 9).

## Rules
- Never commit or upload the keystore, `keystore.properties`, the base64 key, key zips or the password. Never type secrets into web pages; the user adds GitHub secrets himself.
- Current upload key = **v3**, SHA-256 starts `E9:3B:D8` ends `3D:A5`. Keys v1 and v2 were exposed and must not be used.
- Ask before deleting anything in the user's repos or Play Console, and before any legal declaration.
