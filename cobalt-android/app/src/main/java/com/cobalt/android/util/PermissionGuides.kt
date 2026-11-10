package com.cobalt.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * The permission "guide" screens VidMate shows before it can do something that
 * needs a special access: draw-over-other-apps (for the floating player) and
 * all-files/storage (for the download folder and the file manager). Android
 * gives no dialog for either, so the app must send the user to the right
 * Settings page itself — this centralises those deep links.
 */
object PermissionGuides {

    /** True when the app may draw the floating player over other apps. */
    fun canDrawOverlays(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    /** Opens the "Display over other apps" screen for this app. */
    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    /**
     * Opens the storage settings the user needs for this Android version:
     * the per-app "All files access" screen on R+, the app-details screen on
     * Q, and the app's storage entry otherwise.
     */
    fun storageSettingsIntent(context: Context): Intent = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"))
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))
        else ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))
    }

    /** True when the app can read/write the whole shared storage. */
    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            android.os.Environment.isExternalStorageManager()
        else true
}
