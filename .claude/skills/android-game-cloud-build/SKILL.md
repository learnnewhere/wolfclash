---
name: "android-game-cloud-build"
description: "Use when building, signing or updating an Android game APK or Play Store AAB (e.g. Wolf Clash Arena) in a cloud sandbox where Google's SDK/Maven downloads are blocked."
---

# Android game build in the cloud sandbox (APK + AAB)

This covers Vignesh's Android games: Wolf Clash Arena (package `com.wolfclash.arena`, repo layout described in this repo's CLAUDE.md) and Snake Clash Arena (`com.vignesh.snakeclash`).
In this sandbox `dl.google.com`, `maven.google.com` and Maven Central are blocked. npm and PyPI work.
So the Android SDK is assembled from npm/PyPI packages plus hand-written stubs. Everything below is proven to work.

## 0. Before building
- Run `curl -s -o /dev/null -w '%{http_code}' https://dl.google.com` first. If it works, a normal SDK + Gradle build is simpler, so use that instead.
- Check the current Google Play target-API rule (developer.android.com/google/play/requirements/target-sdk).
  As of Nov 2026, new apps and updates must target **API 36**.
- Bump `versionCode` for every Play upload.
- Set `export JAVA_TOOL_OPTIONS=` in every shell to silence the proxy banner.

## 1. Toolchain (rebuild in ~3 min)
```bash
mkdir -p ~/sdkpkgs ~/tools && cd ~/sdkpkgs
npm pack aaptjs3 d8-termux node-bundletool -q
for f in *.tgz; do mkdir -p x/${f%.tgz}; tar xzf $f -C x/${f%.tgz}; done
cp x/aaptjs3-*/package/bin/x64/linux/aapt2 ~/tools/aapt2 && chmod +x ~/tools/aapt2   # aapt2 2.20
cp x/node-bundletool-*/package/bundletool.jar ~/tools/                                 # bundletool 1.18.x
pip install --break-system-packages enjarify-adapter apksigtool androguard
# d8-termux ships D8 as DEX; convert it to JVM bytecode (the input must have an .apk extension):
cp x/d8-termux-*/package/d8.jar d8src.apk && enjarify d8src.apk -o d8-jvm.jar -f -q
python3 ~/tools/fixcp.py d8-jvm.jar ~/tools/d8-fixed.jar
```
Run D8 with:
`java -XX:+UnlockDiagnosticVMOptions -XX:-BytecodeVerificationRemote -Xmx2g -cp ~/tools/d8-fixed.jar com.android.tools.r8.D8 --release --min-api 24 --lib stubs.jar --output out/ *.class`

**fixcp.py** bumps class files to version 52 and changes Methodref to InterfaceMethodref for interface owners (enjarify output fails otherwise):
```python
import zipfile,struct,sys,subprocess
src,dst=sys.argv[1],sys.argv[2]; zi=zipfile.ZipFile(src)
def parse(d):
    n=struct.unpack('>H',d[8:10])[0]; i=1; p=10; cp=[None]*n
    while i<n:
        t=d[p]
        if t==1: l=struct.unpack('>H',d[p+1:p+3])[0]; cp[i]=(t,p,d[p+3:p+3+l].decode('utf8','replace')); p+=3+l
        elif t in(3,4): cp[i]=(t,p,None); p+=5
        elif t in(5,6): cp[i]=(t,p,None); p+=9; i+=1
        elif t in(7,8,16,19,20): cp[i]=(t,p,struct.unpack('>H',d[p+1:p+3])[0]); p+=3
        elif t in(9,10,11,12,17,18): cp[i]=(t,p,struct.unpack('>HH',d[p+1:p+5])); p+=5
        elif t==15: cp[i]=(t,p,None); p+=4
        else: raise Exception(t)
        i+=1
    return cp,p
ifaces=set(); refs=set(); data={}
for inf in zi.infolist():
    d=zi.read(inf); data[inf.filename]=d
    if inf.filename.endswith('.class'):
        cp,p=parse(d); acc,this=struct.unpack('>HH',d[p:p+4])
        if acc&0x200: ifaces.add(cp[cp[this][2]][2])
        for e in cp:
            if e and e[0]==10: refs.add(cp[cp[e[2][0]][2]][2])
ext=[r for r in refs if r.split('/')[0] in('java','javax','sun','jdk')]
open('/tmp/ext.txt','w').write('\n'.join(ext))
open('/tmp/Q.java','w').write('import java.nio.file.*;public class Q{public static void main(String[] a)throws Exception{for(String s:Files.readAllLines(Paths.get("/tmp/ext.txt"))){try{if(Class.forName(s.replace(\'/\',\'.\'),false,null).isInterface())System.out.println(s);}catch(Throwable t){}}}}')
ifaces|=set(subprocess.run(['java','/tmp/Q.java'],capture_output=True,text=True).stdout.split())
zo=zipfile.ZipFile(dst,'w',zipfile.ZIP_DEFLATED)
for fn,d in data.items():
    if fn.endswith('.class'):
        d=bytearray(d); d[4:8]=struct.pack('>HH',0,52); cp,_=parse(bytes(d))
        for e in cp:
            if e and e[0]==10 and cp[cp[e[2][0]][2]][2] in ifaces: d[e[1]]=11
        d=bytes(d)
    zo.writestr(fn,d)
zo.close()
```

## 2. Android API stubs (compile-time only)
- Write minimal `.java` stubs **only for the APIs the app uses** (Activity, Context, SharedPreferences, View, SurfaceView, SurfaceHolder, MotionEvent, Canvas, Paint, Path, RectF, Typeface, Window, WindowManager, webkit.WebView/WebSettings/WebViewClient/ValueCallback/JavascriptInterface, window.OnBackInvokedCallback/Dispatcher, os.Build/Handler/Looper). Compile them with `javac --release 8` into `android-stubs.jar`.
- `Build.VERSION.SDK_INT` must NOT be a compile-time constant, or javac folds the SDK check away. Use `= Integer.parseInt("0")`.
- `@JavascriptInterface` stub must have `@Retention(RUNTIME) @Target(METHOD)`.
- **D8 1.0.35 crashes** (`NullPointerException ... String.length()`) on a class that implements a *generic* interface, such as an anonymous `ValueCallback<String>`. Use top-level classes and raw types (`implements ValueCallback`, `onReceiveValue(Object)`). Lambdas and plain anonymous classes are fine for Gradle builds, but avoid them in this toolchain when unsure.

## 3. Framework resource stub for aapt2 (`-I android-fw-stub.apk`)
You need a `package="android"` manifest plus `res/values/attrs.xml` + `public.xml` declaring these attrs with the real IDs. Then run `aapt2 compile --dir res -o res.zip && aapt2 link --manifest AndroidManifest.xml -o android-fw-stub.apk res.zip`.
theme 0x01010000, label 01010001, icon 01010002, name 01010003, debuggable 0101000f, exported 01010010, launchMode 0101001d (enum standard0/singleTop1/singleTask2/singleInstance3), screenOrientation 0101001e (enum unspecified-1/landscape0/portrait1), configChanges 0101001f (flags keyboard0x10/keyboardHidden0x20/orientation0x80/screenLayout0x100/screenSize0x400/smallestScreenSize0x800), minSdkVersion 0101020c, versionCode 0101021b, versionName 0101021c, targetSdkVersion 01010270, allowBackup 01010280, hardwareAccelerated 010102d3.
If the manifest uses another `android:` attribute or a `@android:` resource, add it here first.

## 4. Signing key
Create a PKCS12 upload key once and keep it with the user:
`keytool -genkeypair -storetype PKCS12 -keystore upload-keystore.p12 -alias upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=..., C=IN"`
Write `keystore.properties` (storeFile, storePassword, keyAlias, keyPassword) for the Gradle `signingConfigs.release`.
If the user already has `upload-key.zip` from an earlier session, **ask for it and reuse it**. Play updates must use the same key.

## 5. Build script (APK v2-signed + AAB)
The Gradle source manifest has no `package` attribute (it uses `namespace`), so inject it for aapt2:
```bash
P=<project>/app/src/main; B=build; T=~/tools; PKG=com.wolfclash.arena
javac --release 8 -cp stubs.jar -d $B/classes $(find $P/java -name '*.java')
java ...D8... --output $B/dex $(find $B/classes -name '*.class')
sed "s|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">|& package=\"$PKG\">|;s|\">  *package|\" package|" ...   # or python: add package="$PKG"
$T/aapt2 compile --dir $P/res -o $B/res.zip
LINK="-I $T/android-fw-stub.apk --manifest $B/AndroidManifest.xml --min-sdk-version 24 --target-sdk-version 36 --version-code N --version-name X"
$T/aapt2 link $LINK -o $B/res.apk $B/res.zip -A $P/assets                 # APK resources
$T/aapt2 link --proto-format $LINK -o $B/proto.apk $B/res.zip -A $P/assets # AAB resources
```
- **APK:** take res.apk, add `classes.dex`, align stored entries to 4 bytes (via zip extra-field padding), then sign with **APK Signature Scheme v2** in Python (`cryptography` package): 1 MB chunk SHA-256 digests over entries/central directory/EOCD; RSA PKCS1v15 SHA-256, algorithm id 0x0103; signing block magic `APK Sig Block 42`, v2 id 0x7109871a; patch the EOCD central-directory offset. Check with `apksigtool verify app.apk` (it should print "v2 verified").
- **AAB:** unzip proto.apk into `base/`, move `AndroidManifest.xml` to `base/manifest/`, copy dex to `base/dex/`, then zip as `base.zip`. Run `java -jar bundletool.jar build-bundle --modules=base.zip --output=app-unsigned.aab`, then `jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -storetype PKCS12 -keystore upload-keystore.p12 -signedjar app.aab app-unsigned.aab upload`.
- **Verify the AAB:** `bundletool validate --bundle=app.aab`, then `bundletool dump manifest`, then `bundletool build-apks --mode=universal --ks=...` and `aapt2 dump badging` on the universal APK (check package, versionCode, targetSdk, INTERNET permission, and that dex/assets are present).

## 6. App architecture notes
- **Wolf Clash Arena:** an HTML5 canvas game in `app/src/main/assets/index.html`, hosted in a full-screen WebView.
  - `window.WolfBridge` (`NetworkMonitor`: a real HTTPS check every few seconds) gates play on internet.
  - `window.WolfAds` is the AdMob bridge.
  - JS hooks: `onAndroidBack`, `onAndroidPause`, `onAdReward(tag, ok)`, `onAdClosed()`.
  - The same HTML (minus the wrapper) is the published online version.
  - When making the asset, wrap the page in a full doctype/head and add safe-area padding.
- **Targeting API 36:** `onBackPressed` isn't called. Register `getOnBackInvokedDispatcher().registerOnBackInvokedCallback(PRIORITY_DEFAULT, new BackCallback(this))` when `SDK_INT >= 33`, and keep `onBackPressed` for older devices.
- **Test the game logic** headlessly with Playwright (`require('playwright')`, Chromium preinstalled), injecting fake `WolfBridge`/`WolfAds` via `addInitScript`. Take one screenshot strip per change and read it.

## 7. AdMob (cannot be compiled in the sandbox; build it on GitHub Actions, see section 9)
The Google Mobile Ads SDK only comes from Google Maven, so the ad-enabled build runs on a GitHub runner (or the user's Android Studio). In the sandbox:
- Write the code against `play-services-ads` + `user-messaging-platform` (verify current versions in the release notes first; Nov 2026: 25.5.0 / UMP 4.0.0, AGP 8.13.0, Gradle 8.13, compileSdk 36, Java 17).
- **Compile-check** against hand-written stubs of the API used: MobileAds, AdRequest.Builder, AdError/LoadAdError, AdLoadCallback<T>, FullScreenContentCallback, OnUserEarnedRewardListener, RewardItem, InterstitialAd(+LoadCallback), RewardedAd(+LoadCallback), the UMP ConsentInformation (PrivacyOptionsRequirementStatus enum, success/failure listeners), ConsentRequestParameters.Builder, ConsentForm.OnConsentFormDismissedListener, UserMessagingPlatform. Use a fake `BuildConfig`.
- **Rules:** run consent (UMP) before `MobileAds.initialize` (background thread), then load and show on the main thread. Reload after show, retry after 30 s on failure, and drop ads older than 55 min.
- **IDs:** release uses the real IDs; debug uses Google test IDs (via `manifestPlaceholders` + `buildConfigField`, `buildFeatures.buildConfig true`).
  - Wolf Clash real IDs: App `ca-app-pub-1765077834611057~7754500301`, Rewarded `ca-app-pub-1765077834611057/1000942582`, Interstitial `ca-app-pub-1765077834611057/8687860914`.
  - Test IDs: App `ca-app-pub-3940256099942544~3347511713`, Rewarded `.../5224354917`, Interstitial `.../1033173712`.
- **Placements:** rewarded revive (once per run, 6-second offer) and rewarded 2× boss score; an interstitial between runs at most every 3rd run, skipped in runs where a reward was watched, never during play.

## 8. Delivery
- Write deliverables to `/mnt/user-data/outputs/` and send them with SendUserFile (`display: attach`). Deliverables: APK, AAB, `<App>-source.zip` (the Gradle project incl. README / ADS-SETUP.md), and `upload-key.zip` only when a new key was created.
- Tell the user plainly what was verified (bundletool validate, signatures, universal APK contents, Playwright run) and what wasn't (no device/emulator run; ad builds need Android Studio).
- Remind them: uninstall builds signed with a different key; never tap their own real ads; declare ads + Data safety (Advertising ID) + a privacy policy in Play Console.

## 9. Ad-enabled release via GitHub Actions (proven Nov 2026)
- Repo root holds the Gradle project and `.github/workflows/build-unsigned.yml` (no secrets) and `build.yml` (signed; needs `UPLOAD_KEYSTORE_BASE64` + `UPLOAD_KEYSTORE_PASSWORD` secrets, which only the user can add; never type secrets into web fields yourself).
- Workflows must live at the repo ROOT `.github/workflows/`, not inside a subfolder, or GitHub ignores them. If the project sits in a subfolder, add `defaults.run.working-directory` and prefix the artifact `path`.
- Flow: user (or you, in their browser) runs "Build unsigned AAB" from the Actions tab, downloads the artifact zip, then sign it:
  - On the user's Mac: `tools/sign-release.sh <artifact.zip> <upload-keystore.p12>` (needs Android Studio build-tools).
  - In the sandbox: AAB with `jarsigner` as in section 5. APK: AGP's unsigned release APK is NOT aligned the way the v2 signer expects; re-align (stored entries 4 bytes, `.so` 16 KB) with `apk_finish.aligned_zip` before `sign_v2`, then verify with apksigtool.
- Always validate with bundletool and check `dump manifest` for `com.google.android.gms.ads.APPLICATION_ID` = the real App ID.

## 10. Key hygiene (lessons learned)
- The upload key was leaked twice by committing key zips to a public repo. Never commit `*.p12`, `*.jks`, `keystore.properties`, `UPLOAD_KEYSTORE_BASE64.txt` or key zips; keep the repo private; run a leak check (`find . -name '*.p12' -o -name '*key*.zip'`) before any upload.
- If a key leaks after an AAB was uploaded: generate a new key, export `upload_certificate.pem` (`keytool -export -rfc`), and have the user request an **upload key reset** in Play Console > App integrity > App signing. Do NOT use "Change app signing key".
