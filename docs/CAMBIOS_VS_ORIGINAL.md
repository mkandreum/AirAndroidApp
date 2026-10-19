# Cambios frente al APK original

El APK original (`com.aistudio.airandroid.knyuzw`, versión 3.0) era una build debug generada con un paquete de AI Studio. Esta versión es una reescritura nativa.

## Seguridad

| Original | Nuevo |
|----------|-------|
| `debuggable=true` y certificado debug | Release firmada, `debuggable=false` |
| Servidor 8888 abierto a cualquier host | Solo subred `192.168.49.*` + PIN + AES-GCM |
| Control 8889 sin autenticación | Eliminado; la IP viene de Wi-Fi Direct |
| Transferencia en texto plano | AES-256-GCM con clave ECDH + PBKDF2 |
| Sin integridad | Tag GCM por frame |
| Sin límites | 8 GB/archivo, 100 archivos, 100 MB libres |
| Nombre sin sanear | Saneado con `File(name).name` y límite |
| NFC sin autenticidad | PIN + ECDH; HCE solo en recepción |
| `allowBackup=true` | `allowBackup=false` |
| Actividades de preview exportadas | Eliminadas |

## Funcionalidad

- Recepción en **Galería/Descargas** con MediaStore en Android 10+.
- **Historial persistente** con abrir/compartir.
- **Selección múltiple** con Photo Picker y SAF.
- **Cancelación** de transferencias.
- **Progreso** con velocidad y archivo actual.
- **PIN de 6 dígitos** en lugar de depender de NFC.
- **NFC HCE** para tocar móviles, además de tags NDEF.
- **Nombre de dispositivo** configurable.
- **Ajustes de Wi-Fi/NFC** desde la app.

## UI/UX

- De una pantalla densa a tres pestañas claras: Enviar, Recibir e Historial.
- Barra inferior flotante con selección animada.
- Tema negro/verde conservado.
- Tarjetas, radar, progreso y estados de error/éxito.
- Animaciones cortas y háptica.
- Pantalla de bienvenida y splash.
- Icono nuevo.

## Compatibilidad

- `minSdk` sube de 24 a 26.
- `targetSdk` 36.
- Se declara `ACCESS_FINE_LOCATION` con `maxSdkVersion=32`, corrigiendo el fallo del original.
- Se usa `NEARBY_WIFI_DEVICES` en Android 13+.
- NFC es opcional (`required=false`); Wi-Fi Direct sigue siendo obligatorio.

## Lo que no se incluye todavía

- Recepción en segundo plano con foreground service.
- Código QR.
- Internacionalización.
- Firma persistente de identidad por dispositivo.
- Escaneo de malware de archivos recibidos.
