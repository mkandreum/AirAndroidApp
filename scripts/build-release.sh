#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BUILD_TOOLS="${BUILD_TOOLS:-$SDK/build-tools/36.0.0}"
KEYSTORE="${AIRANDROID_KEYSTORE:-$HOME/.android/airandroid/release.jks}"
STORE_PASS_FILE="${AIRANDROID_STORE_PASS_FILE:-$HOME/.android/airandroid/storepass.txt}"
KEY_PASS_FILE="${AIRANDROID_KEY_PASS_FILE:-$HOME/.android/airandroid/keypass.txt}"
KEY_ALIAS="${AIRANDROID_KEY_ALIAS:-airandroid}"
VERSION="1.0.4"

cd "$ROOT"
mkdir -p dist

if [ ! -f "$KEYSTORE" ]; then
  echo "Falta el keystore: $KEYSTORE" >&2
  exit 1
fi
if [ ! -f "$STORE_PASS_FILE" ] || [ ! -f "$KEY_PASS_FILE" ]; then
  echo "Faltan los archivos de contraseña en $HOME/.android/airandroid" >&2
  exit 1
fi

echo "[1/4] Compilando release con R8..."
./gradlew :app:assembleRelease

UNSIGNED="app/build/outputs/apk/release/app-release-unsigned.apk"
ALIGNED="dist/AirAndroid-$VERSION-aligned.apk"
OUT="dist/AirAndroid-$VERSION-release.apk"

echo "[2/4] Alineando..."
"$BUILD_TOOLS/zipalign" -f -p 4 "$UNSIGNED" "$ALIGNED"

echo "[3/4] Firmando..."
"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "file:$STORE_PASS_FILE" \
  --key-pass "file:$KEY_PASS_FILE" \
  --out "$OUT" \
  "$ALIGNED"

echo "[4/4] Verificando..."
"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$OUT"
if "$BUILD_TOOLS/aapt" dump badging "$OUT" | grep -q "application-debuggable"; then
  echo "ERROR: el APK es debuggable" >&2
  exit 1
fi

echo
echo "APK: $OUT"
sha256sum "$OUT"
