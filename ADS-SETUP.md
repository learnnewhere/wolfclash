# Ads in Wolf Clash Arena (Google AdMob)

## What players see
| Ad | When | Rule |
|---|---|---|
| Rewarded: **Revive** | When you lose your last heart | Optional, once per run, 6-second offer. Watching gives full hearts and chases the nearby hunters away. |
| Rewarded: **2× boss score** | Results screen | Optional, once per run. Doubles the boss part of the score. |
| Interstitial | After tapping Play Again or Menu on the results screen | At most every 3rd run, and never in a run where the player already watched a rewarded ad. Never during play. |

Before any ad loads, Google's consent form appears for players in the EEA, UK and Switzerland.
A **Privacy options** link appears on the menu when the law requires it.

## The code
- `app/build.gradle`: **your AdMob IDs** for release builds and Google's test IDs for debug builds.
- `AdIds.java` / `AndroidManifest.xml`: read those IDs for the current build type.
- `AdsManager.java`: consent, loading, showing and reloading ads.
- `WolfAds.java`: lets the game page ask for ads (`window.WolfAds`).
- `assets/index.html`: the revive screen, the 2× button and the every-3rd-run rule.

## Your AdMob IDs (already set)
| | ID | Used in |
|---|---|---|
| App ID | `ca-app-pub-1765077834611057~7754500301` | release builds |
| Rewarded | `ca-app-pub-1765077834611057/1000942582` | revive and 2× boss score |
| Interstitial | `ca-app-pub-1765077834611057/8687860914` | between runs |

**Run ▶ (debug)** shows Google's "Test Ad" samples. **Generate Signed App Bundle (release)** uses the real IDs above.
New ad units can take an hour or more to start serving real ads, and the app needs AdMob review before ads fill normally.

## Create your AdMob account and IDs (reference)
1. Go to https://admob.google.com and sign in with a Google account. Click **Get started**,
   then choose your country, time zone and payment currency and accept the terms.
2. **Apps → Add app → Android.** When asked if the app is on a store, choose **No** for now.
   Name it "Wolf Clash Arena". AdMob shows an **App ID** like `ca-app-pub-1234567890123456~1234567890`.
3. In the app, open **Ad units → Add ad unit** and create two units:
   - **Rewarded**, named "Revive and double". Copy its ID (`ca-app-pub-…/…`).
   - **Interstitial**, named "Between runs". Copy its ID.
4. Put the IDs in `app/build.gradle`, under `defaultConfig` (done for this app).
5. **Privacy & messaging → European regulations → Create message**, pick the app and **Publish**.
   Do the same for **US state regulations**. Without a published message, no consent form appears.
6. **Payments:** add your name, address and bank details. Google mails a PIN to verify your address
   once you reach the verification threshold. Payment arrives after earnings pass the payment
   threshold for your currency.
7. After the game is live on Google Play, go to **App settings → Add app store details** in AdMob to link it.
   Also publish an `app-ads.txt` file on the website listed as your developer website in Play Console.
   AdMob shows the exact line to put in it.

**Never tap your own real ads.** Keep the test IDs while you develop, or add your phone as a test
device (AdMob → Settings → Test devices). Invalid clicks can get the AdMob account suspended.

## Play Console checklist for apps with ads
- **App content → Ads:** answer "Yes, my app contains ads".
- **Data safety:** declare that the app collects the **Advertising ID** and app interactions, and shares them with AdMob for advertising.
  The AdMob SDK adds the `AD_ID` permission automatically.
- **Privacy policy:** a public URL is required for apps with ads.
- **Target audience:** if you include children under 13, the Families policy applies and needs extra ad settings.
  Choosing 13+ keeps setup simple.

## Build
This version needs Google's AdMob library, which Android Studio downloads automatically:
1. Open this folder in Android Studio (recent version) and let Gradle sync.
   The project uses Android Gradle Plugin 8.13 and Gradle 8.13.
2. **Run ▶** on your phone. You'll see Google's sample "Test Ad" screens.
3. **Build → Generate Signed App Bundle / APK** with the upload key (see README) to get the `.aab` for Play.
