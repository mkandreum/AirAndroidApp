# Compilar y firmar

## Requisitos

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0
- Gradle 8.13 (el wrapper lo descarga)

## Debug

```bash
./gradlew :app:assembleDebug
```

Salida:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Release

El proyecto no guarda claves en el repositorio. El script `scripts/build-release.sh` usa por defecto:

```
~/.android/airandroid/release.jks
~/.android/airandroid/storepass.txt
~/.android/airandroid/keypass.txt
```

### Crear el keystore (solo una vez)

```bash
mkdir -p ~/.android/airandroid
keytool -genkeypair -v \
  -keystore ~/.android/airandroid/release.jks \
  -storetype PKCS12 \
  -alias airandroid \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=AirAndroid, OU=Xyon Platforms, O=Xyon Platforms, L=Madrid, ST=Madrid, C=ES"
```

Guarda las contraseñas en archivos con permisos 600:

```bash
printf '%s' 'TU_PASSWORD' > ~/.android/airandroid/storepass.txt
printf '%s' 'TU_PASSWORD' > ~/.android/airandroid/keypass.txt
chmod 600 ~/.android/airandroid/release.jks \
  ~/.android/airandroid/storepass.txt \
  ~/.android/airandroid/keypass.txt
```

### Compilar y firmar

```bash
./scripts/build-release.sh
```

El script:

1. Ejecuta `assembleRelease` con R8.
2. Alinea con `zipalign -p 4`.
3. Firma con `apksigner` (v2 + v3).
4. Verifica la firma.
5. Comprueba que no sea `debuggable`.
6. Imprime el SHA-256.

Salida:

```
dist/AirAndroid-1.0.4-release.apk
```

### Variables de entorno opcionales

```bash
AIRANDROID_KEYSTORE=/ruta/release.jks
AIRANDROID_STORE_PASS_FILE=/ruta/storepass.txt
AIRANDROID_KEY_PASS_FILE=/ruta/keypass.txt
AIRANDROID_KEY_ALIAS=airandroid
BUILD_TOOLS=$HOME/Android/Sdk/build-tools/36.0.0
```

## Verificación manual

```bash
apksigner verify --verbose --print-certs \
  dist/AirAndroid-1.0.4-release.apk

aapt dump badging dist/AirAndroid-1.0.4-release.apk | grep -E \
  "package:|sdkVersion|targetSdkVersion|application-debuggable"
```

No debe aparecer `application-debuggable`.

## Firma para futuras versiones

Usa el mismo keystore y sube `versionCode`/`versionName` en `app/build.gradle.kts`. Sin la misma clave, Android no permite actualizar la app instalada.
