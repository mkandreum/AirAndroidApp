package com.xyon.airandroid.data

import android.content.Context
import com.xyon.airandroid.model.HistoryItem
import com.xyon.airandroid.model.TransferDirection
import com.xyon.airandroid.model.TransferStatus
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("airandroid_history", Context.MODE_PRIVATE)
    private val lock = Any()

    fun load(): List<HistoryItem> = synchronized(lock) {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(item.toHistoryItem() ?: continue)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(item: HistoryItem): List<HistoryItem> = synchronized(lock) {
        val items = (listOf(item) + load()).take(MAX_ITEMS)
        persist(items)
        items
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit().remove(KEY_HISTORY).apply()
        }
    }

    private fun persist(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { item -> array.put(item.toJson()) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun HistoryItem.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", fileName)
        .put("size", size)
        .put("direction", when (direction) {
            TransferDirection.SEND -> "send"
            TransferDirection.RECEIVE -> "receive"
        })
        .put("device", deviceName)
        .put("status", when (status) {
            TransferStatus.CONNECTING -> "connecting"
            TransferStatus.WAITING -> "waiting"
            TransferStatus.TRANSFERRING -> "transferring"
            TransferStatus.COMPLETED -> "completed"
            TransferStatus.FAILED -> "failed"
            TransferStatus.CANCELLED -> "cancelled"
        })
        .put("ts", timestamp)
        .put("uri", uri)

    private fun JSONObject.toHistoryItem(): HistoryItem? {
        val id = optString("id")
        val name = optString("name")
        if (id.isBlank() || name.isBlank()) return null
        return HistoryItem(
            id = id,
            fileName = name,
            size = optLong("size", 0L),
            direction = when (optString("direction")) {
                "send" -> TransferDirection.SEND
                else -> TransferDirection.RECEIVE
            },
            deviceName = optString("device"),
            status = when (optString("status")) {
                "connecting" -> TransferStatus.CONNECTING
                "waiting" -> TransferStatus.WAITING
                "transferring" -> TransferStatus.TRANSFERRING
                "failed" -> TransferStatus.FAILED
                "cancelled" -> TransferStatus.CANCELLED
                else -> TransferStatus.COMPLETED
            },
            timestamp = optLong("ts", 0L),
            uri = optString("uri").takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val KEY_HISTORY = "items"
        const val MAX_ITEMS = 120
    }
}
