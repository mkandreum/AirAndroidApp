# Pruebas

## Automáticas

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
```

### Tests JVM

`app/src/test/java/com/xyon/airandroid/core/CryptoTest.kt` cubre:

- ECDH: ambas partes derivan el mismo secreto.
- Derivación de clave de sesión con PIN, nonces y PBKDF2.
- Clave distinta con PIN incorrecto.
- AES-256-GCM: cifrar/descifrar y verificar que el ciphertext difiere.
- Framing de `SecureChannel`.
- PIN de 6 dígitos.
- HKDF determinista y longitud correcta.
- JSON de `NfcPairing` (válido e inválido).
- Saneado de nombres (`../../evil.txt`, `/etc/passwd`, `a:b.txt`).

### Lint

`lintDebug` debe terminar sin errores. Se revisaron permisos de Wi-Fi Direct, APIs de MediaStore y atributos de tema.

## Verificación del APK

```bash
apksigner verify --verbose --print-certs dist/AirAndroid-1.0.6-release.apk
aapt dump badging dist/AirAndroid-1.0.6-release.apk
```

Comprobar:

- `Verified using v2 scheme: true`
- `Verified using v3 scheme: true`
- `package: com.xyon.airandroid`
- `sdkVersion: 26`, `targetSdkVersion: 36`
- Sin `application-debuggable`
- Firma con `CN=AirAndroid`

## Pruebas manuales necesarias

Wi-Fi Direct y NFC no existen en el emulador. Para validar de verdad hacen falta **dos móviles Android físicos**:

1. Instalar el APK en ambos.
2. Receptor: Recibir → Activar recepción → comprobar PIN.
3. Emisor: Enviar → elegir una foto → Buscar → seleccionar receptor → PIN → Enviar.
4. Verificar que la foto aparece en `Pictures/AirAndroid` y en el Historial.
5. Probar PIN incorrecto 3 veces y comprobar que cambia.
6. Probar NFC: tocar el móvil del receptor con el emisor (HCE) y comprobar que el PIN se rellena.
7. Probar tag NFC físico con Grabar tag.
8. Probar cancelación a mitad de transferencia.
9. Probar archivos grandes (>1 GB), muchos archivos y nombres con caracteres raros.
10. Probar en Android 13+ y en Android 10/11.

## Emulador

El emulador permite comprobar arranque, tema, navegación y permisos, pero no Wi-Fi Direct ni NFC. La app muestra un aviso de “Wi-Fi Direct no disponible” si el hardware no existe.

## Estado de la entrega

- Compilación debug: correcta.
- Compilación release con R8: correcta.
- Tests JVM: correctos.
- Lint: 0 errores.
- Firma v2/v3: correcta.
- `debuggable`: false.
- Pruebas en hardware real: pendientes por parte del usuario.
