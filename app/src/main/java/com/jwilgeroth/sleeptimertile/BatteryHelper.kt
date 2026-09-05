package com.jwilgeroth.sleeptimertile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for the "please don't kill my alarm" problem.
 *
 * One UI (and stock Doze) can defer or drop alarms from apps it considers
 * background-idle. Being on the battery-optimization *exemption* list is the
 * programmatic half of the fix. The other half — One UI's "Sleeping apps" /
 * "Background usage limits" — has no public API, so we can only send the user
 * to the right settings page and tell them what to toggle.
 */
object BatteryHelper {

    fun isIgnoringOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** System dialog: "Allow app to ignore battery optimizations?" */
    fun requestIgnoreIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )

    /** This app's App info page — the path to One UI's per-app Battery settings. */
    fun appDetailsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
}
