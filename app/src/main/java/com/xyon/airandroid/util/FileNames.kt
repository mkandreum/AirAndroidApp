package com.xyon.airandroid.util

import com.xyon.airandroid.core.net.TransferProtocol
import java.io.File

object FileNames {
    private val invalid = Regex("[\\u0000-\\u001F<>:\"/\\\\|?*]")

    fun sanitize(raw: String): String {
        val base = runCatching { File(raw).name }.getOrDefault(raw)
        val cleaned = base
            .replace(invalid, "_")
            .trim()
            .trim('.')
            .take(TransferProtocol.MAX_NAME_CHARS)
        return cleaned.ifBlank { "archivo_${System.currentTimeMillis()}" }
    }

    fun withSuffix(name: String, index: Int): String {
        if (index <= 0) return name
        val dot = name.lastIndexOf('.')
        return if (dot > 0 && dot < name.length - 1) {
            name.substring(0, dot) + " ($index)" + name.substring(dot)
        } else {
            "$name ($index)"
        }
    }
}
