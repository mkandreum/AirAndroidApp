# Seguridad

## Resumen

La versión reescrita elimina los problemas principales del APK original: servidores abiertos sin autenticación, transferencia en texto plano, build debug y falta de saneado. El diseño actual usa Wi-Fi Direct, PIN, ECDH efímero y AES-GCM.

## Controles implementados

- **Confidencialidad**: AES-256-GCM por frame.
- **Integridad**: tag GCM de 128 bits en cada frame.
- **Autenticación**: PIN de 6 dígitos + verificación del primer frame cifrado.
- **Secreto forward**: par ECDH nuevo por sesión.
- **Anti-replay**: nonces aleatorios de 16 bytes y claves efímeras.
- **Anti-fuerza bruta**: 3 intentos por PIN, luego regeneración.
- **Aislamiento de red**: solo se aceptan IPs `192.168.49.*`.
- **Saneado de nombres**: sin rutas, sin separadores, sin caracteres de control.
- **Límites**: 8 GB por archivo, 100 archivos, 100 MB libres mínimos.
- **Almacenamiento**: MediaStore/FileProvider, sin permisos amplios de almacenamiento.
- **Build**: release firmada, `debuggable=false`, `allowBackup=false`, R8 activo.

## Superficie expuesta

| Componente | Exportado | Protección |
|------------|-----------|------------|
| `MainActivity` | sí | launcher |
| `AirAndroidHceService` | sí | `BIND_NFC_SERVICE` |
| `FileProvider` | no | grant por URI |
| Servidor 8988 | red P2P | PIN + AES-GCM + subred |
| NFC HCE | proximidad | desbloqueo + recepción activa |

## Riesgos residuales

1. **PIN de 6 dígitos**: un atacante con capacidad de MITM activo y captura del handshake podría intentar fuerza bruta offline. PBKDF2 con 210.000 iteraciones y la regeneración tras 3 fallos lo dificultan, pero no es una autenticación de hardware.
2. **Sin identidad persistente**: el receptor no tiene clave pública estable; el PIN es el único vínculo. Para uso corporativo convendría un par de claves persistente y verificación por huella.
3. **NFC físico**: un tag grabado contiene el PIN mientras la recepción esté activa. El PIN cambia al reiniciar o regenerar; el tag queda obsoleto.
4. **HCE del fabricante**: algunos dispositivos limitan la emulación o exigen el selector de servicio por defecto.
5. **Android 8-9**: los archivos se guardan en almacenamiento privado de la app; no se publican en la Galería.
6. **Recepción en segundo plano**: no hay foreground service; si el proceso muere, la recepción termina.
7. **Red P2P**: la subred `192.168.49.0/24` es el filtro; un atacante en esa subred con el PIN podría conectar.
8. **Contenido recibido**: los archivos no se analizan con antivirus; el usuario decide qué acepta.

## Recomendaciones para producción

- Añadir un par de claves persistente por dispositivo y mostrar una huella verificable.
- Firmar el payload NFC con la clave del emisor.
- Añadir foreground service para recepción en segundo plano.
- Añadir límites de tasa por IP y cierre de conexiones lentas.
- Integrar un escaneo opcional de malware.
- Publicar en Play Store con Play Integrity y política de privacidad.
- Rotar el keystore de release y custodiarlo fuera del equipo de desarrollo.

## Comparación con el APK original

| Problema original | Estado actual |
|-------------------|---------------|
| Servidor 8888 abierto a cualquier host | Solo subred P2P + PIN + AES-GCM |
| Control 8889 secuestrable | Eliminado; la IP se obtiene de Wi-Fi Direct |
| Transferencia en texto plano | AES-256-GCM |
| Build debug con `debuggable=true` | Release firmada, `debuggable=false` |
| Falta `ACCESS_FINE_LOCATION` en Android 7-12 | Declarado con `maxSdkVersion=32` |
| Nombre de archivo sin sanear | Saneado y limitado |
| Sin límites de tamaño/tiempo | Límites y timeouts |
| NFC sin autenticidad | PIN + ECDH, HCE solo en recepción |
| UI compleja | 3 pestañas, feedback claro |
