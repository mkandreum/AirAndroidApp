# AirAndroid

**Envía archivos y fotos entre móviles Android por Wi-Fi Direct, sin internet y con cifrado de extremo a extremo.**

*Un producto de [Xyon Platforms](https://github.com/mkandreum)*

![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%20a%2016-3DDC84?style=flat&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Wi-Fi Direct](https://img.shields.io/badge/Wi--Fi%20Direct-P2P-0A84FF?style=flat)
![NFC](https://img.shields.io/badge/NFC-HCE%20%2B%20NDEF-FF6B00?style=flat)

---

## Qué es

AirAndroid es una app nativa para enviar y recibir archivos directamente entre dos Android. No necesita router, cuenta, nube ni conexión a internet: crea una red Wi-Fi Direct privada entre los dos dispositivos y transfiere los datos por un canal cifrado.

El emparejamiento se hace con un **PIN de 6 dígitos** que muestra el receptor. Opcionalmente, si ambos móviles tienen NFC, se pueden **tocar** para leer el PIN automáticamente, o grabar un tag NFC.

Esta versión reescribe la app original (`com.aistudio.airandroid.knyuzw`) corrigiendo sus fallos de seguridad y de diseño. El análisis del APK original está en [mkandreum/AirAndroid](https://github.com/mkandreum/AirAndroid).

## Funcionalidades

- **Wi-Fi Direct** para descubrir y conectar los dos móviles sin infraestructura.
- **PIN de 6 dígitos** para autenticar la sesión.
- **Cifrado AES-256-GCM** por bloques, con clave derivada de ECDH efímero + PBKDF2 + PIN.
- **NFC**:
  - HCE: el receptor emula un tag; el emisor lo lee y rellena el PIN.
  - NDEF: el receptor puede grabar un tag físico con el PIN y los datos de emparejamiento.
- **Selección múltiple** de fotos, vídeos y archivos con Photo Picker y SAF.
- **Guardado automático**:
  - Fotos en `Pictures/AirAndroid`.
  - Vídeos en `Movies/AirAndroid`.
  - Otros archivos en `Downloads/AirAndroid`.
  - En Android 8-9 se guardan en el almacenamiento privado de la app y se pueden abrir o compartir.
- **Historial persistente** de envíos y recepciones.
- **Abrir y compartir** archivos recibidos.
- **Progreso en vivo**: porcentaje, bytes, velocidad y archivo actual.
- **Cancelación** de transferencias.
- **Ajustes**: nombre del dispositivo, estado de NFC y Wi-Fi.
- **UI oscura** con el tema negro/verde de la app original, barra inferior flotante, animaciones y respuesta háptica.

## Seguridad

- Wi-Fi Direct crea una red punto a punto; los datos no pasan por ningún servidor.
- **ECDH P-256 efímero** por sesión: clave de sesión distinta en cada transferencia.
- **PBKDF2-HMAC-SHA256** (210.000 iteraciones) para derivar la clave a partir del PIN.
- **AES-256-GCM** con nonce aleatorio por bloque: confidencialidad e integridad.
- El PIN **nunca se envía por la red**.
- El servidor solo acepta conexiones desde la subred de Wi-Fi Direct (`192.168.49.0/24`).
- El HCE solo responde mientras la recepción está activa y la pantalla desbloqueada.
- Nombres de archivo saneados; límite de tamaño, número de archivos y espacio libre.
- Release firmada, `debuggable=false`, `allowBackup=false`.

Detalle completo en [docs/PROTOCOLO_SEGURO.md](docs/PROTOCOLO_SEGURO.md) y [docs/SEGURIDAD.md](docs/SEGURIDAD.md).

## Stack

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 2.1.21 |
| UI | Jetpack Compose + Material 3 |
| Estado | MVVM, `AndroidViewModel`, `StateFlow` |
| Red | `ServerSocket` / `Socket` sobre Wi-Fi Direct |
| Descubrimiento | `WifiP2pManager` |
| NFC | `HostApduService` (HCE) + `NfcAdapter` reader mode + NDEF |
| Criptografía | JCA: ECDH, PBKDF2, HKDF, AES/GCM |
| Persistencia | SharedPreferences (ajustes e historial) |
| Guardado | MediaStore (API 29+) y FileProvider (API 26-28) |
| Build | AGP 8.13.2, Gradle 8.13, compileSdk 36, minSdk 26, targetSdk 36 |

## Estructura

```
AirAndroidApp/
├── app/src/main/java/com/xyon/airandroid/
│   ├── core/crypto/        SecureChannel, Crypto (ECDH, PBKDF2, HKDF, AES-GCM)
│   ├── core/net/           WifiDirectManager, FileTransferServer/Client, protocolo
│   ├── core/nfc/           NfcController, HCE service, pairing store
│   ├── data/               Settings, historial y guardado de archivos
│   ├── model/              Modelos de UI, transferencia, NFC e historial
│   ├── ui/                 ViewModel, pantallas, componentes y tema
│   └── MainActivity.kt
├── app/src/test/           Tests JVM de criptografía, framing, NFC y nombres
├── docs/                   Arquitectura, protocolo, seguridad, diseño y pruebas
├── scripts/                Build y firma de release
├── gradle/                 Wrapper de Gradle
├── build.gradle.kts
└── settings.gradle.kts
```

## Compilar

Requisitos: JDK 17, Android SDK con compileSdk 36 y Build-Tools 36.

```bash
./gradlew :app:assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para la release firmada:

```bash
# 1. Crea el keystore (solo la primera vez)
mkdir -p ~/.android/airandroid
keytool -genkeypair -v -keystore ~/.android/airandroid/release.jks \
  -storetype PKCS12 -alias airandroid -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=AirAndroid, OU=Xyon Platforms, O=Xyon Platforms, L=Madrid, ST=Madrid, C=ES"

# 2. Guarda las contraseñas en archivos (no en el repo)
printf '%s' 'TU_PASSWORD' > ~/.android/airandroid/storepass.txt
printf '%s' 'TU_PASSWORD' > ~/.android/airandroid/keypass.txt
chmod 600 ~/.android/airandroid/*

# 3. Compila y firma
./scripts/build-release.sh
```

El script deja el APK en `dist/AirAndroid-1.0.4-release.apk`. Más detalle en [docs/COMPILAR_Y_FIRMAR.md](docs/COMPILAR_Y_FIRMAR.md).

## Pruebas

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
```

Los tests cubren ECDH, derivación de clave, AES-GCM, framing del canal seguro, PIN, HKDF, JSON NFC y saneado de nombres. Ver [docs/PRUEBAS.md](docs/PRUEBAS.md).

## Limitaciones conocidas

- Wi-Fi Direct y NFC requieren **hardware real**; el emulador no los implementa.
- En Android 8-9 los archivos se guardan en el almacenamiento privado de la app (no aparecen en la Galería del sistema).
- La app exige Wi-Fi Direct (`uses-feature required=true`); no está pensada para equipos sin esa capacidad.
- Para recibir en segundo plano con la pantalla apagada haría falta un foreground service; esta versión mantiene la recepción mientras la app está viva.
- La UI está en español y no está internacionalizada todavía.

## Aviso

Proyecto de ejemplo y auditoría. AirAndroid y su código original pertenecen a sus autores. Esta reescritura se distribuye como APK de prueba firmado localmente.
