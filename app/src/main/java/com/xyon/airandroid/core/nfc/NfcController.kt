package com.xyon.airandroid.core.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.cardemulation.CardEmulation
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.xyon.airandroid.model.NfcPairing
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class NfcController(context: Context) {

    enum class Mode { READ, WRITE }

    private val appContext = context.applicationContext
    private val adapter: NfcAdapter? = runCatching {
        (appContext.getSystemService(Context.NFC_SERVICE) as? NfcManager)?.defaultAdapter
    }.getOrNull()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val handling = AtomicBoolean(false)

    private var activity: Activity? = null
    private var callback: NfcAdapter.ReaderCallback? = null
    private var mode = Mode.READ
    private var pairingToWrite: NfcPairing? = null
    private var onPairing: ((NfcPairing) -> Unit)? = null
    private var onWritten: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun hasNfc(): Boolean = adapter != null

    fun isEnabled(): Boolean = runCatching {
        adapter?.isEnabled == true
    }.getOrDefault(false)

    fun isHceReady(): Boolean {
        val nfc = adapter ?: return false
        return runCatching {
            val cardEmulation = CardEmulation.getInstance(nfc)
            cardEmulation.isDefaultServiceForAid(
                ComponentName(appContext, AirAndroidHceService::class.java),
                AirAndroidHceService.AID_HEX,
            )
        }.getOrDefault(false)
    }

    fun isReaderActive(): Boolean = activity != null && callback != null

    fun startReader(
        activity: Activity,
        mode: Mode,
        pairingToWrite: NfcPairing? = null,
        onPairing: (NfcPairing) -> Unit,
        onWritten: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val nfc = adapter
        if (nfc == null) {
            onError("Este móvil no tiene NFC")
            return
        }
        if (!isEnabled()) {
            onError("NFC no disponible. Actívalo o reinicia el móvil si el servicio se ha detenido.")
            return
        }
        stopReader()

        this.activity = activity
        this.mode = mode
        this.pairingToWrite = pairingToWrite
        this.onPairing = onPairing
        this.onWritten = onWritten
        this.onError = onError
        handling.set(false)

        val readerCallback = NfcAdapter.ReaderCallback { tag ->
            if (handling.compareAndSet(false, true)) {
                handleTag(tag)
            }
        }
        callback = readerCallback

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val extras = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        runCatching {
            nfc.enableReaderMode(activity, readerCallback, flags, extras)
        }.onFailure {
            clear()
            onError("No se pudo activar el lector NFC")
        }
    }

    fun stopReader() {
        val nfc = adapter
        val act = activity
        if (nfc != null && act != null) {
            runCatching { nfc.disableReaderMode(act) }
        }
        clear()
    }

    private fun clear() {
        activity = null
        callback = null
        pairingToWrite = null
        onPairing = null
        onWritten = null
        onError = null
        handling.set(false)
    }

    private fun handleTag(tag: Tag) {
        val currentMode = mode
        val writePairing = pairingToWrite
        val result = runCatching {
            when (currentMode) {
                Mode.READ -> readPairing(tag)
                Mode.WRITE -> {
                    val pairing = writePairing ?: throw IOException("No hay datos para grabar")
                    writePairing(tag, pairing)
                    null
                }
            }
        }

        mainHandler.post {
            val nfc = adapter
            val act = activity
            if (nfc != null && act != null) {
                runCatching { nfc.disableReaderMode(act) }
            }
            val pairingHandler = onPairing
            val writtenHandler = onWritten
            val errorHandler = onError
            activity = null
            callback = null
            pairingToWrite = null
            onPairing = null
            onWritten = null
            onError = null
            handling.set(false)

            result.onSuccess { pairing ->
                if (currentMode == Mode.WRITE) {
                    writtenHandler?.invoke()
                } else if (pairing != null) {
                    pairingHandler?.invoke(pairing)
                } else {
                    errorHandler?.invoke("Tag NFC no compatible")
                }
            }.onFailure { error ->
                errorHandler?.invoke(error.message ?: "No se pudo usar el tag NFC")
            }
        }
    }

    private fun readPairing(tag: Tag): NfcPairing? {
        val iso = runCatching { IsoDep.get(tag) }.getOrNull()
        if (iso != null) {
            try {
                iso.connect()
                iso.timeout = 3_000
                val selected = iso.transceive(AirAndroidHceService.SELECT_AID)
                if (selected.endsWithBytes(AirAndroidHceService.STATUS_OK)) {
                    val response = iso.transceive(AirAndroidHceService.GET_DATA)
                    if (response.endsWithBytes(AirAndroidHceService.STATUS_OK)) {
                        val payload = response.copyOf(response.size - 2)
                        NfcPairing.fromJson(String(payload, Charsets.UTF_8))?.let { return it }
                    }
                }
            } catch (_: Exception) {
                // The tag may be a regular NDEF tag instead of our HCE service.
            } finally {
                runCatching { iso.close() }
            }
        }

        val ndef = runCatching { Ndef.get(tag) }.getOrNull()
        if (ndef != null) {
            try {
                ndef.connect()
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                message?.records?.forEach { record ->
                    if (record.tnf == NdefRecord.TNF_MIME_MEDIA &&
                        String(record.type, Charsets.US_ASCII) ==
                        AirAndroidHceService.MIME_TYPE
                    ) {
                        val payload = String(record.payload, Charsets.UTF_8)
                        NfcPairing.fromJson(payload)?.let { return it }
                    }
                }
            } catch (_: Exception) {
                // Fall through to the generic error.
            } finally {
                runCatching { ndef.close() }
            }
        }
        return null
    }

    private fun ByteArray.endsWithBytes(suffix: ByteArray): Boolean {
        if (size < suffix.size) return false
        for (index in suffix.indices) {
            if (this[size - suffix.size + index] != suffix[index]) return false
        }
        return true
    }

    private fun writePairing(tag: Tag, pairing: NfcPairing) {
        val payload = pairing.toJson().toByteArray(Charsets.UTF_8)
        val message = NdefMessage(
            arrayOf(
                NdefRecord.createMime(AirAndroidHceService.MIME_TYPE, payload),
                NdefRecord.createApplicationRecord(appContext.packageName),
            ),
        )

        val ndef = runCatching { Ndef.get(tag) }.getOrNull()
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) throw IOException("El tag es de solo lectura")
                if (ndef.maxSize < message.toByteArray().size) {
                    throw IOException("El tag es demasiado pequeño")
                }
                ndef.writeNdefMessage(message)
            } finally {
                runCatching { ndef.close() }
            }
            return
        }

        val formatable = runCatching { NdefFormatable.get(tag) }.getOrNull()
        if (formatable != null) {
            formatable.connect()
            try {
                formatable.format(message)
            } finally {
                runCatching { formatable.close() }
            }
            return
        }

        throw IOException("Tag NFC no compatible")
    }
}
