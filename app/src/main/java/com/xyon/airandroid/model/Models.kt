package com.xyon.airandroid.model

import android.net.Uri

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
)

data class PeerDevice(
    val name: String,
    val address: String,
    val status: Int,
    val isGroupOwner: Boolean = false,
) {
    val displayName: String
        get() = name.ifBlank { address }
}

data class TransferProgress(
    val fileName: String,
    val bytes: Long,
    val total: Long,
    val speedBytesPerSecond: Long,
    val fileIndex: Int,
    val fileCount: Int,
) {
    val fraction: Float
        get() = if (total > 0L) (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class TransferDirection { SEND, RECEIVE }

enum class TransferStatus { CONNECTING, WAITING, TRANSFERRING, COMPLETED, FAILED, CANCELLED }

data class HistoryItem(
    val id: String,
    val fileName: String,
    val size: Long,
    val direction: TransferDirection,
    val deviceName: String,
    val status: TransferStatus,
    val timestamp: Long,
    val uri: String? = null,
)

data class AppSettings(
    val deviceName: String,
)

data class NfcPairing(
    val ssid: String,
    val pin: String,
    val deviceName: String,
    val port: Int = 8988,
    val version: Int = 1,
) {
    fun toJson(): String = org.json.JSONObject()
        .put("v", version)
        .put("t", "airandroid-pair")
        .put("ssid", ssid.take(64))
        .put("pin", pin)
        .put("name", deviceName.take(48))
        .put("port", port)
        .toString()

    companion object {
        fun fromJson(raw: String): NfcPairing? = runCatching {
            val json = org.json.JSONObject(raw)
            if (json.optString("t") != "airandroid-pair") return null
            val pin = json.optString("pin")
            if (!pin.matches(Regex("\\d{6}"))) return null
            val port = json.optInt("port", 8988).let {
                if (it in 1..65535) it else 8988
            }
            NfcPairing(
                ssid = json.optString("ssid").take(64),
                pin = pin,
                deviceName = json.optString("name").ifBlank { "Android" }.take(48),
                port = port,
                version = json.optInt("v", 1),
            )
        }.getOrNull()
    }
}

enum class Role { IDLE, SENDER, RECEIVER }

data class ConnectionState(
    val supported: Boolean = true,
    val wifiP2pEnabled: Boolean = false,
    val role: Role = Role.IDLE,
    val groupReady: Boolean = false,
    val connectedPeerName: String? = null,
    val message: String? = null,
    val nfcAvailable: Boolean = false,
    val nfcEnabled: Boolean = false,
    val nfcReaderActive: Boolean = false,
)

sealed interface TransferUiState {
    data object Idle : TransferUiState
    data class Connecting(val message: String) : TransferUiState
    data class Sending(val progress: TransferProgress) : TransferUiState
    data class Receiving(val progress: TransferProgress) : TransferUiState
    data class Completed(
        val direction: TransferDirection,
        val fileCount: Int,
        val totalBytes: Long,
    ) : TransferUiState

    data class Failed(val message: String) : TransferUiState
    data object Cancelled : TransferUiState
}

data class ReceiveUiState(
    val active: Boolean = false,
    val pin: String? = null,
    val peerName: String? = null,
    val waiting: Boolean = false,
    val receivedFiles: List<HistoryItem> = emptyList(),
    val badPinAttempts: Int = 0,
)

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data object TransferCompleted : UiEvent
    data object Error : UiEvent
    data object NeedPermissions : UiEvent
    data object PinRegenerated : UiEvent
}

data class TransferResult(
    val success: Boolean,
    val message: String?,
    val filesSent: Int,
    val bytesSent: Long,
)
