package com.xyon.airandroid.core.nfc

import com.xyon.airandroid.model.NfcPairing

object NfcPairingStore {
    @Volatile
    var pairing: NfcPairing? = null

    fun update(value: NfcPairing?) {
        pairing = value
    }
}
