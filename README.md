# Wolf Clash Arena (Android)

Run, eat and grow in a forest arena. Bigger wolves hunt you; as your wolf grows it sprouts extra heads, up to five.
When you're knocked out, your wolf clashes the Alpha boss and its size becomes bonus score. Needs internet to play. Free with AdMob ads.

- Package: `com.wolfclash.arena` · current version **1.3 (versionCode 4)** · minSdk 24 · targetSdk 36
- Privacy policy: https://learnnewhere.github.io/wolfclash-privacy/

## What's in this repo

| Path | What it is |
|---|---|
| `app/` | Android Gradle project. The game itself is `app/src/main/assets/index.html` (HTML5 canvas) in a full-screen WebView. |
| `app/src/main/java/com/wolfclash/arena/` | WebView host, internet check (`NetworkMonitor`), AdMob + consent (`AdsManager`), JS bridges. |
| `.github/workflows/build-unsigned.yml` | Builds the release AAB + APK on GitHub, unsigned. No secrets needed. |
| `.github/workflows/build.yml` | Builds and signs, if you add the two key secrets (see below). |
| `tools/sign-release.sh` | Signs the unsigned build on your Mac with your upload key. |
| `store/` | Play Store icon, feature graphic, screenshots, listing text, privacy policy page. |
| `web/` | Browser versions of the game (plain, and with a mock ad overlay) for quick testing. |
| `ADS-SETUP.md` | AdMob IDs, ad placements, test vs real IDs. |
| `PROJECT-HISTORY.md` | Everything that happened so far, decisions, Play Console status, next steps. |
| `CLAUDE.md`, `.claude/skills/` | Instructions for Claude so a future session can pick this up immediately. |

**Not in this repo, on purpose:** the upload key (`upload-keystore.p12`), `keystore.properties`, its password, and built AAB/APK files. Keep the key in a safe place (password manager / encrypted drive). Losing it means a Play key reset; leaking it means the same.

## Build a new release

1. Make your change (game code is `app/src/main/assets/index.html`).
2. In `app/build.gradle` bump `versionCode` (next is **5**) and `versionName`. Also update the version in the two workflow files' file names if you like.
3. Push to GitHub, open **Actions → Build unsigned AAB → Run workflow**, wait ~4 minutes, download the artifact zip.
4. Sign it on your Mac:
   ```bash
   tools/sign-release.sh ~/Downloads/WolfClashArena-1.3-ads-unsigned.zip ~/path/to/upload-keystore.p12
   ```
   It asks for the keystore password and writes the signed `.aab` and `.apk` next to the zip.
5. Upload the `.aab` in Play Console.

Alternative: add repository secrets `UPLOAD_KEYSTORE_BASE64` (contents of `UPLOAD_KEYSTORE_BASE64.txt`) and `UPLOAD_KEYSTORE_PASSWORD`, then run **Build signed AAB** to get a ready-to-upload AAB directly.

Or open the folder in Android Studio (JDK 17) and use **Build → Generate Signed App Bundle**.

## Test in a browser
Open `web/wolf-clash-arena.html` (or `web/wolf-ad-preview.html` to see where ads appear). Arrow keys/WASD or drag; Space = dash.
