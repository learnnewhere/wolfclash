#!/bin/bash
# Signs the unsigned AAB/APK produced by the "Build unsigned AAB" GitHub workflow.
# Runs on macOS/Linux with the Android SDK installed (Android Studio installs it).
#
# Usage:
#   tools/sign-release.sh <unsigned-artifact-folder-or-zip> <path/to/upload-keystore.p12>
# Output: WolfClashArena-<version>-ads.aab and .apk next to the input, signed with alias "upload".
# The keystore password is asked for interactively (never stored).
set -euo pipefail

IN="${1:?give the downloaded artifact zip or folder}"
KS="${2:?give the path to upload-keystore.p12}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
[ -x "$BT/apksigner" ] || { echo "apksigner not found under $SDK/build-tools (install Build-Tools in Android Studio's SDK Manager)"; exit 1; }

WORK=$(mktemp -d)
if [ -f "$IN" ]; then unzip -q "$IN" -d "$WORK"; else cp "$IN"/* "$WORK"/; fi
AAB=$(ls "$WORK"/*.aab | head -1); APK=$(ls "$WORK"/*.apk | head -1)
OUT=$(dirname "$IN"); BASE=$(basename "$AAB" | sed 's/-unsigned//; s/\.aab$//')

read -r -s -p "Upload keystore password: " PW; echo

# AAB: JAR signature (what Play Console expects for the upload key)
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -storetype PKCS12 -keystore "$KS" -storepass "$PW" \
  -signedjar "$OUT/$BASE.aab" "$AAB" upload >/dev/null
jarsigner -verify "$OUT/$BASE.aab" | grep -q "jar verified" && echo "AAB signed: $OUT/$BASE.aab"

# APK: align, then sign (v1+v2+v3)
"$BT/zipalign" -f -p 4 "$APK" "$WORK/aligned.apk"
"$BT/apksigner" sign --ks "$KS" --ks-type PKCS12 --ks-key-alias upload --ks-pass "pass:$PW" \
  --out "$OUT/$BASE.apk" "$WORK/aligned.apk"
"$BT/apksigner" verify "$OUT/$BASE.apk" && echo "APK signed: $OUT/$BASE.apk"
rm -rf "$WORK"
