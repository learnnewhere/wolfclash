# Wolf Clash Arena: project history and status

Last updated: 6 November 2026. Built with Claude (Cowork) in a cloud sandbox.

## Timeline
1. **Snake Clash Arena prototype.** Snake game: bigger snakes chase you, survive and grow, boss clash on knockout uses your length for score. App only works with internet (real HTTPS check, not just "network connected").
2. **Re-themed to wolves.** Wolf eats meat/rabbits/smaller wolves, grows, and gains heads at size thresholds (1 → 5 heads). Each head = stronger bite, extra heart, a bit more speed. Knockout → clash with the Alpha boss; boss level rises after each win.
3. **AAB added** alongside the APK for Play Store.
4. **AdMob ads added** (only to Wolf Clash Arena):
   - Rewarded **revive**: once per run, offered for 6 s on knockout.
   - Rewarded **2× boss score** after the clash.
   - **Interstitial** between runs, at most every 3rd run, skipped if a reward was watched that run, never during play.
   - UMP consent form (EEA/UK/CH) + "Privacy options" button on the menu.
5. **Package renamed** `com.wolfoo.wolfclash` → **`com.wolfclash.arena`** (final, can never change once published).
6. **Fixes:** boss screen clipped the player on narrow phones; touch-aware hints; Android 13+ back gesture (OnBackInvokedCallback); targetSdk 36.
7. **Play Store prep:** icon, feature graphic, 4 screenshots, listing text, privacy policy hosted on GitHub Pages.
8. **Ad-enabled AAB 1.3 (versionCode 4)** built on GitHub Actions (unsigned) and signed with upload key v3.
9. **1.4 (versionCode 5)**: first AAB accepted by Play (registered v3 as upload key). Closed test "Closed testing - Alpha" approved and live; testers via Google Groups testers-community@ and roughtough@googlegroups.com (Testers Community).
10. **1.5 (versionCode 6), EU privacy hardening**: fonts bundled in the app (no Google Fonts request); neutral birth-year screen on first launch (stored on device only) that tags ad/consent requests: under 13 child-directed + G rating, under 16 under-age-of-consent; all ads capped at T. Privacy policy updated (8 Nov 2026) and its page no longer loads Google Fonts. No session replay/analytics SDKs; no purchases/subscriptions.

## IDs and settings
| Item | Value |
|---|---|
| Package | `com.wolfclash.arena` |
| Version | 1.5, versionCode 6 (next upload: 7) |
| SDK | minSdk 24, targetSdk/compileSdk 36, Java 17, AGP 8.13.0, Gradle 8.13 |
| AdMob App ID | `ca-app-pub-1765077834611057~7754500301` |
| Rewarded unit | `ca-app-pub-1765077834611057/1000942582` |
| Interstitial unit | `ca-app-pub-1765077834611057/8687860914` |
| Ads SDK | play-services-ads 25.5.0, user-messaging-platform 4.0.0 |
| Privacy policy | https://learnnewhere.github.io/wolfclash-privacy/ (source: `store/privacy-policy.html`) |
| Developer / contact | Vignesh V · learnnewhere@gmail.com |
| Internet check hosts | connectivitycheck.gstatic.com, google.com/generate_204, cloudflare.com/cdn-cgi/trace |

## Signing key history
| Key | Status |
|---|---|
| v1 (`upload-key_1.zip`) | **Exposed** (was in the public `wolfoo` repo). Don't use. |
| v2 (`upload-key-NEW.zip`) | **Exposed** (also uploaded to the repo). An AAB signed with v2 was uploaded to Play. Don't use. |
| v3 (`upload-key-v3.zip`) | **Current.** PKCS12, alias `upload`, SHA-256 `E9:3B:D8…3D:A5`. Kept offline by Vignesh, never in Git. |

Because Play saw v2 first, Play needs an **upload key reset** to accept v3: Play Console → Test and release → App integrity → App signing → *Request upload key reset*, attach `upload_certificate.pem` from the v3 zip. Do **not** use "Change app signing key".

## Play Console status (personal account)
Done: app created, privacy policy, app access (no sign-in), ads = yes, content rating (ESRB E10+, PEGI 7), target audience 13+, Data safety (approx. location, app interactions, diagnostics, device IDs; collected + shared for ads/analytics/fraud prevention; encrypted in transit), government/financial/health = no, category Action, contact email, store listing text.

Still to do:
- [ ] Upload store graphics from `store/graphics/` (icon 512, feature graphic 1024×500, screenshots).
- [ ] Request the upload key reset (above) and wait for approval (usually ~2 days).
- [ ] Trader status: finish the declaration (advised: Trader, because the app earns ad revenue).
- [ ] AI asset declaration on the listing.
- [ ] Closed test: upload the AAB, add **12+ testers**, keep them opted in for **14 days**.
- [ ] Apply for production access, then release.

## Repo history
The old repo `learnnewhere/wolfoo` (now private) had the project in a subfolder `wolfclash-repo-upload/` and contains the exposed v1/v2 key zips in its history. This clean repo replaces it: project at the root, no keys, no builds.

## How Claude built things (for future sessions)
See `.claude/skills/android-game-cloud-build/SKILL.md`. Short version: the sandbox can't reach Google's SDK, so the no-ads build used a hand-assembled toolchain (npm aapt2 + converted D8 + stubs + Python v2 signer); the ads build runs on GitHub Actions and is signed afterwards.
