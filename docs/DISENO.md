# Diseño de producto y UI

## Objetivo

Que enviar una foto o un archivo sea tan simple como: abrir, elegir, buscar, poner el código y enviar. Sin cuentas, sin anuncios y sin internet.

## Principios

1. **Una acción principal por pantalla**: elegir archivos, buscar receptor, introducir PIN.
2. **Feedback inmediato**: háptica al tocar, progreso en vivo, confirmación al terminar.
3. **Confianza visible**: PIN grande, aviso de cifrado y estado de la conexión.
4. **Estética coherente**: negro, verde Android y tarjetas elevadas, manteniendo el tema de la app original.
5. **Rendimiento primero**: animaciones cortas, sin blur costoso, lecturas animadas en fase de dibujo.

## Pantallas

### Enviar

- Cabecera con estado Wi-Fi Direct.
- Hero de envío directo.
- Paso 01: fotos/vídeos o archivos.
- Paso 02: búsqueda y selección de receptor.
- Paso 03: PIN de 6 dígitos + botón NFC.
- Botón de envío grande y progreso en vivo.

### Recibir

- Activación con explicación de 3 pasos.
- PIN grande y botones Copiar/Nuevo código.
- NFC: toque o grabación de tag.
- Radar mientras espera.
- Progreso y archivos recibidos con Abrir/Compartir.
- Botón Detener recepción.

### Historial

- Lista de transferencias con dirección, dispositivo, tamaño, tiempo y estado.
- Abrir/compartir en recibidos.
- Vaciar con confirmación.

### Ajustes

- Nombre del dispositivo.
- Estado de NFC y Wi-Fi.
- Recordatorio de seguridad.
- Versión.

## Barra inferior

- Flotante, con esquinas de 33 dp y fondo translúcido.
- Tres pestañas: Enviar, Recibir, Historial.
- Píldora verde animada en la pestaña activa.
- Icono con escala spring.
- Háptica ligera al cambiar.

## Animaciones

- `AnimatedContent` entre pestañas con fade + slide corto.
- `AnimatedVisibility` para listas de archivos.
- `animateFloatAsState` para progreso y escala.
- `rememberInfiniteTransition` solo en el radar visible.
- Lecturas animadas dentro de `Canvas`/`graphicsLayer` para evitar recomposición por frame.
- Sin `Modifier.blur`; el efecto glass se consigue con color translúcido y borde.

## Háptica

- Tick en pestañas, selección de peer, elegir archivos y copiar PIN.
- Confirmación en envío/recepción completados.
- Rechazo en errores.
- `HapticFeedbackConstants.CONFIRM/REJECT` en Android 11+; fallback en versiones anteriores.

## Rendimiento

- `collectAsStateWithLifecycle()` en todos los flujos.
- Listas con claves estables en historial.
- ViewModel con flujos separados para evitar recomposiciones globales.
- Progreso limitado a ~10 actualizaciones por segundo.
- Canal seguro con buffers de 64 KB.
- R8 y reducción de recursos en release.
- Sin servicios en segundo plano ni animaciones permanentes fuera de pantalla.

## Accesibilidad

- Descripciones en iconos interactivos.
- Roles de pestaña y estado seleccionado.
- Botones de al menos 48 dp.
- Textos con contraste alto sobre negro.
- PIN accesible mediante campo numérico.

## Próximos pasos de diseño

- Internacionalización (es/en).
- Modo claro opcional.
- Onboarding de primera vez.
- Código QR como alternativa al PIN.
- Recepción en segundo plano con notificación.
- Historial con filtros y búsqueda.
