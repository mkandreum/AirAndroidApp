# Protocolo seguro

## Puertos y constantes

| Constante | Valor |
|-----------|-------|
| Puerto de transferencia | `8988` |
| Subred P2P aceptada | `192.168.49.0/24` |
| Bloque de datos | 64 KB |
| Frame máximo | 256 KB |
| Tamaño máximo por archivo | 8 GB |
| Máximo de archivos por sesión | 100 |
| Espacio libre mínimo | 100 MB |
| PIN | 6 dígitos |
| Iteraciones PBKDF2 | 210.000 |

## Handshake

El cliente (emisor) conecta al servidor (receptor) y ambos intercambian un saludo JSON mediante `DataInputStream.writeUTF/readUTF`:

```json
{
  "v": 1,
  "t": "hello",
  "role": "sender",
  "name": "Pixel 7",
  "pub": "<clave pública EC en Base64>",
  "nonce": "<16 bytes en Base64>"
}
```

El servidor responde con su propio saludo y una clave pública EC efímera.

### Derivación de clave

1. `shared = ECDH(privada_propia, pública_peer)` sobre `secp256r1`.
2. `salt = SHA-256(clientNonce || serverNonce)`.
3. `pinKey = PBKDF2-HMAC-SHA256(PIN, salt, 210000, 256 bits)`.
4. `key = HKDF-SHA256(ikm = shared, salt = pinKey, info = "AirAndroid/v1", 32 bytes)`.

El PIN no viaja por la red. Un atacante pasivo no puede derivar la clave sin el secreto ECDH. Un MITM activo necesitaría además adivinar el PIN y pagar el coste de PBKDF2.

### Autenticación

El cliente envía un frame cifrado:

```json
{"t": "auth", "name": "Pixel 7"}
```

El servidor intenta descifrarlo. Si el tag GCM falla, cierra la conexión y cuenta el intento. Al tercer fallo se regenera el PIN. Si todo va bien, responde con:

```json
{"t": "ready"}
```

## Canal seguro

Cada frame:

```
+----------------+----------------+---------------------------+
| int length     | 12 bytes IV    | ciphertext + 16 bytes tag |
+----------------+----------------+---------------------------+
```

- `length` es el tamaño del ciphertext (incluido el tag GCM).
- El IV es aleatorio por frame.
- AES-256-GCM autentica el frame completo.
- Un frame manipulado lanza `AEADBadTagException` y se aborta la sesión.

## Transferencia

Después del handshake, el cliente envía por cada archivo:

```json
{
  "t": "file",
  "name": "foto.jpg",
  "size": 3456789,
  "mime": "image/jpeg"
}
```

Después envía frames binarios con el contenido, en bloques de hasta 64 KB, hasta completar `size`. El receptor no escribe más allá del tamaño declarado.

Al terminar todos los archivos:

```json
{"t": "done", "files": 3}
```

El receptor responde:

```json
{"t": "ok"}
```

## Reglas de validación

- Solo se aceptan conexiones desde `192.168.49.*`.
- El nombre se sanea con `File(name).name`, se eliminan caracteres inválidos y se limita a 180 caracteres.
- El tamaño debe ser `>= 0` y `<= 8 GB`.
- Se comprueba espacio libre antes de empezar.
- Se rechazan más de 100 archivos.
- Si el cliente envía más bytes de los declarados, se aborta.
- Si el socket deja de responder, salta el timeout de 30 s.
- El receptor solo escribe en `Pictures/AirAndroid`, `Movies/AirAndroid`, `Downloads/AirAndroid` o almacenamiento privado de la app.

## NFC

### HCE

- AID: `F04149524E44524F4944`.
- Secuencia APDU:
  1. `SELECT AID` (`00 A4 04 00 0B ...`).
  2. `GET DATA` (`00 CA 00 00 00`).
- Respuesta: JSON UTF-8 + `90 00`.
- Si no hay sesión de recepción activa: `6A 82`.
- `requireDeviceUnlock=true`.

### NDEF

- MIME: `application/vnd.xyon.airandroid.pair`.
- Payload: JSON con `ssid`, `pin`, `name`, `port` y `v`.
- Incluye un Android Application Record con el package name.

## Modelo de amenazas

| Amenaza | Mitigación |
|---------|------------|
| Sniffing en la red P2P | AES-256-GCM |
| Suplantación de receptor | PIN + autenticación GCM |
| Replay de una sesión | ECDH efímero + nonces aleatorios |
| Fuerza bruta del PIN | 3 intentos + regeneración + PBKDF2 |
| Path traversal | `File(name).name` + saneado |
| Disco lleno | límite de tamaño + espacio libre |
| Acceso desde la LAN | solo subred `192.168.49.*` |
| Tag NFC robado | el PIN se regenera al iniciar/fallar; HCE solo activo en recepción |

## Limitaciones

- La identidad del receptor no está vinculada a una cuenta; el PIN es el único autenticador.
- El HCE depende del fabricante y puede requerir desbloqueo.
- La escritura de tags NFC compite con el modo lector en algunos dispositivos.
