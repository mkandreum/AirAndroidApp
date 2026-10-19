package com.xyon.airandroid.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat

fun Context.findActivity(): android.app.Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

fun mimeForName(name: String): String {
    val extension = name.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}

fun openFile(context: Context, uriString: String?, name: String): Boolean {
    val uri = uriString?.let(Uri::parse) ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeForName(name))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

fun shareFile(context: Context, uriString: String?, name: String): Boolean {
    val uri = uriString?.let(Uri::parse) ?: return false
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeForName(name)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
