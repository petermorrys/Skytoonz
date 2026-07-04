package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashReport(throwable)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save crash report", e)
        } finally {
            // Pass the exception to the original system handler so the system behaves normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReport(throwable: Throwable) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        val stackTrace = stringWriter.toString()

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val report = buildString {
            appendLine("=========================================")
            appendLine("CRASH REPORT")
            appendLine("Time: $timeStamp")
            appendLine("App ID: ${context.packageName}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Exception Message: ${throwable.localizedMessage ?: throwable.message}")
            appendLine("=========================================")
            appendLine("STACK TRACE:")
            appendLine(stackTrace)
            appendLine("=========================================\n")
        }

        // 1. Write to internal storage (always accessible)
        try {
            val internalFile = File(context.filesDir, "crash_log.txt")
            FileWriter(internalFile, true).use { writer ->
                writer.write(report)
            }
            Log.d("CrashHandler", "Crash logged to internal storage: ${internalFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write internal crash log", e)
        }

        // 2. Write to external storage (visible via file managers under Android/data/...)
        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val externalFile = File(externalDir, "crash_log.txt")
                FileWriter(externalFile, true).use { writer ->
                    writer.write(report)
                }
                Log.d("CrashHandler", "Crash logged to external storage: ${externalFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write external crash log", e)
        }
    }

    companion object {
        fun initialize(context: Context): CrashHandler {
            return CrashHandler(context.applicationContext)
        }
    }
}
