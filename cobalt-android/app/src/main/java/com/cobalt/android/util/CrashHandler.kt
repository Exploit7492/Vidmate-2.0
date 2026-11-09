package com.cobalt.android.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.cobalt.android.MainActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Process-wide uncaught-exception handler (Phase I1).
 *
 * Two jobs, both real rather than cosmetic:
 *
 *  1. **Persist the crash** to `filesDir/crashes/last_crash.txt` (stack trace +
 *     device/build header) so it survives the process dying and can be shown or
 *     attached to a bug report. The previous behaviour was Android's default:
 *     the trace goes to logcat, which a normal user never sees and which is
 *     wiped on reboot.
 *  2. **Restart the app** — the foreground Activity is relaunched on a fresh
 *     process so the user is not left staring at a "keeps stopping" dialog.
 *     This is what VidMate does with its own crash guard; without it a single
 *     crash in a background worker leaves the app dead until the user manually
 *     reopens it.
 *
 * The original handler is still invoked first, so logcat/Play Console crash
 * reporting keeps working.
 */
class CrashHandler private constructor(
    private val context: Context,
    private val delegate: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching { writeCrash(throwable) }
        runCatching { delegate?.uncaughtException(thread, throwable) }
        runCatching { restart() }
        runCatching { Process.killProcess(Process.myPid()) }
        runCatching { kotlin.system.exitProcess(1) }
    }

    private fun writeCrash(throwable: Throwable) {
        val dir = File(context.filesDir, "crashes").apply { mkdirs() }
        val sw = StringWriter()
        PrintWriter(sw).use { throwable.printStackTrace(it) }
        val header = buildString {
            append("time: ").append(timestamp()).append('\n')
            append("version: ").append(appVersion()).append('\n')
            append("device: ").append(Build.MANUFACTURER).append(' ')
                .append(Build.MODEL).append('\n')
            append("android: ").append(Build.VERSION.RELEASE)
                .append(" (sdk ").append(Build.VERSION.SDK_INT).append(")\n")
            append("thread: ").append(Thread.currentThread().name).append("\n\n")
        }
        File(dir, "last_crash.txt").writeText(header + sw.toString())
        // Keep the last few crashes for triage rather than only the newest.
        File(dir, "crash_${System.currentTimeMillis()}.txt").writeText(header + sw.toString())
        prune(dir)
    }

    private fun prune(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_KEPT).forEach { it.delete() }
    }

    private fun restart() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun appVersion(): String = runCatching {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pkg.versionName} (${pkg.longVersionCode})"
    }.getOrDefault("unknown")

    companion object {
        private const val MAX_KEPT = 5

        /** Installs the handler. Safe to call more than once. */
        fun install(context: Context) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context.applicationContext, previous)
            )
        }

        /** The most recent crash text, if any — for an in-app "last crash" view. */
        fun lastCrash(context: Context): String? =
            File(File(context.filesDir, "crashes"), "last_crash.txt")
                .takeIf { it.exists() }?.readText()
    }
}
