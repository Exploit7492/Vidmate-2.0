package com.cobalt.android.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded APK to the platform installer (Phase I2).
 *
 * Installing outside a store always requires the user's explicit confirmation —
 * [Intent.ACTION_VIEW] with the APK MIME type opens the system install dialog,
 * which is the consent step. This class never attempts a silent install (that
 * would need root or a device-owner profile, and is exactly the behaviour an
 * updater should not have). On Android 8+ the user must also have granted
 * "install unknown apps" for this app; [canRequestInstalls] reports that so the
 * caller can route to the settings page instead of failing confusingly.
 */
object ApkInstaller {

    fun canRequestInstalls(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** Opens the system "install unknown apps" page for this app (API 26+). */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Launches the install dialog for [apk]. The file is shared through the
     * app's FileProvider (never a `file://` URI), which is what the platform
     * requires and keeps the APK out of world-readable storage.
     */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
