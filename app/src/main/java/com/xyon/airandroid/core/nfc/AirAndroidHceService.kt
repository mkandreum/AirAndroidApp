package com.xyon.airandroid.core.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.xyon.airandroid.model.NfcPairing

class AirAndroidHceService : HostApduService() {

    private var selected = false

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_NOT_FOUND

        if (isSelectAid(commandApdu)) {
            selected = true
            return STATUS_OK
        }

        if (isGetData(commandApdu)) {
            val pairing = NfcPairingStore.pairing
            if (pairing == null) {
                selected = false
                return STATUS_NOT_FOUND
            }
            selected = false
            val payload = pairing.toJson().toByteArray(Charsets.UTF_8)
            return payload + STATUS_OK
        }

        selected = false
        return STATUS_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        selected = false
    }

    private fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 5 + AID.size) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte()) return false
        if (apdu[2] != 0x04.toByte() || apdu[3] != 0x00.toByte()) return false
        val length = apdu[4].toInt() and 0xFF
        if (length < AID.size) return false
        if (apdu.size < 5 + length) return false
        for (index in AID.indices) {
            if (apdu[5 + index] != AID[index]) return false
        }
        return true
    }

    private fun isGetData(apdu: ByteArray): Boolean =
        apdu.size >= 4 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xCA.toByte()

    companion object {
        val AID = byteArrayOf(
            0xF0.toByte(),
            0x41, 0x49, 0x52, 0x41, 0x4E, 0x44, 0x52, 0x4F, 0x49, 0x44,
        )
        val SELECT_AID: ByteArray =
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte()) + AID + byteArrayOf(0x00)
        val GET_DATA: ByteArray = byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00, 0x00)
        val STATUS_OK = byteArrayOf(0x90.toByte(), 0x00)
        val STATUS_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        const val AID_HEX = "F04149524E44524F4944"
        const val MIME_TYPE = "application/vnd.xyon.airandroid.pair"
    }
}
