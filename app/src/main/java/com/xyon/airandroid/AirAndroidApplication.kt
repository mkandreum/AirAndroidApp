package com.xyon.airandroid

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AirAndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = File(filesDir, "last_crash.txt")
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())
                file.writeText(
                    buildString {
                        appendLine("AirAndroid crash $stamp")
                        appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                        appendLine("Android: ${android.os.Build.VERSION.RELEASE} (${android.os.Build.VERSION.SDK_INT})")
                        appendLine()
                        appendLine(throwable.stackTraceToString())
                    },
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
