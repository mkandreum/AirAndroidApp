# Arquitectura

## Visión general

AirAndroid es una app de una sola Activity. La UI es Jetpack Compose y el estado vive en un `AndroidViewModel`. La lógica de red, criptografía y NFC está separada en clases sin dependencias de Compose, lo que permite probarla en JVM.

```
MainActivity
  └── AirAndroidApp (Compose)
        ├── SendScreen
        ├── ReceiveScreen
        ├── HistoryScreen
        ├── SettingsSheet
        └── GlassBottomBar
              │
              ▼
        AirAndroidViewModel
        ├── WifiDirectManager
        ├── FileTransferServer
        ├── FileTransferClient
        ├── NfcController
        ├── ReceivedFileStore
        ├── SettingsRepository
        └── HistoryRepository
```

## Capas

### UI

- `ui/AirAndroidApp.kt`: raíz de Compose, permisos, snackbar, navegación por pestañas y ciclo de vida de NFC.
- `ui/screens/SendScreen.kt`: selección de archivos, búsqueda de pares, PIN y envío.
- `ui/screens/ReceiveScreen.kt`: activación de recepción, PIN, NFC, progreso y archivos recibidos.
- `ui/screens/HistoryScreen.kt`: historial persistente y acciones de abrir/compartir.
- `ui/components/`: tarjetas, botones, barra inferior, PIN, radar, progreso y ajustes.
- `ui/theme/`: paleta negra/verde, tipografía y barras del sistema.

La UI no hace I/O directamente. Solo llama al ViewModel y reacciona a `StateFlow`.

### Estado

`AirAndroidViewModel` expone flujos independientes para limitar recomposiciones:

- `settings`, `history`
- `connection`, `peers`, `selectedPeer`, `scanning`
- `files`, `sendPin`
- `transfer`, `receive`
- `events` (`SharedFlow` para snackbars y hápticas)

Los flujos se recogen con `collectAsStateWithLifecycle()`.

### Red

- `WifiDirectManager`: registra el `BroadcastReceiver`, inicializa `WifiP2pManager`, descubre pares, crea grupo y conecta.
- `FileTransferServer`: abre el puerto 8988, valida la subred P2P, hace el handshake y recibe archivos.
- `FileTransferClient`: conecta al group owner, hace el handshake y envía los archivos.
- `TransferProtocol`: constantes de puerto, límites y tipos de mensaje.

### Criptografía

- `Crypto`: ECDH P-256, HKDF-SHA256, PBKDF2-HMAC-SHA256, AES-256-GCM y utilidades.
- `SecureChannel`: framing autenticado (longitud + IV + ciphertext).
- `SecureHandshake`: intercambio de claves y autenticación mutua por PIN.

### NFC

- `NfcController`: reader mode para leer HCE o tags NDEF y para grabar tags.
- `AirAndroidHceService`: emulación de tag ISO-DEP con AID propio.
- `NfcPairingStore`: datos de emparejamiento compartidos entre el ViewModel y el servicio HCE.

### Datos

- `SettingsRepository`: nombre del dispositivo.
- `HistoryRepository`: JSON en SharedPreferences, máximo 120 registros.
- `ReceivedFileStore`: MediaStore en Android 10+ y FileProvider en Android 8-9.

## Flujo de envío

1. El usuario elige archivos y pulsa **Buscar**.
2. El ViewModel arranca `WifiDirectManager.discoverPeers()`.
3. El receptor ya está en modo **Recibir**, es group owner y muestra un PIN.
4. El emisor selecciona el par, escribe el PIN y pulsa **Enviar**.
5. `WifiP2pManager.connect()` forma el grupo. El emisor recibe la IP del owner.
6. `FileTransferClient` conecta a `owner:8988` y ejecuta el handshake.
7. Se envían cabeceras y bloques cifrados.
8. El receptor guarda cada archivo y lo añade al historial.
9. Ambos lados muestran el resultado y disparan háptica de confirmación.

## Flujo de recepción

1. El usuario pulsa **Activar recepción**.
2. Se genera un PIN y se abre el servidor en 8988.
3. Se crea el grupo Wi-Fi Direct como owner.
4. El PIN se publica en `NfcPairingStore` para HCE.
5. Al conectar un emisor, se hace el handshake y se valida el PIN.
6. Los archivos se guardan en Galería/Descargas.
7. Si el PIN falla, se cuentan los intentos y al tercero se regenera.

## NFC

### HCE (móvil a móvil)

- El receptor emula un tag ISO-DEP con AID `F04149524E44524F4944`.
- El emisor usa `NfcAdapter.enableReaderMode`, selecciona el AID y pide los datos.
- El servicio HCE solo responde si `NfcPairingStore.pairing != null` y el dispositivo está desbloqueado.
- El payload es el mismo JSON que se graba en un tag NDEF.

### NDEF (tag físico)

- El receptor puede grabar un `NdefMessage` con un registro MIME `application/vnd.xyon.airandroid.pair` y un AAR.
- El emisor lee el tag con reader mode y rellena el PIN.

## Decisiones de diseño

- **Sin internet**: Wi-Fi Direct es la única vía de red.
- **Sin cuentas**: no hay identidad de usuario ni servidores.
- **PIN + ECDH**: el PIN aporta autenticación; ECDH aporta confidencialidad y secreto forward.
- **Un archivo, un frame**: los bloques de 64 KB permiten progreso y cancelación.
- **MediaStore**: las fotos y vídeos recibidos aparecen en la Galería sin permisos de almacenamiento.
- **R8**: release minificada y con recursos reducidos.
