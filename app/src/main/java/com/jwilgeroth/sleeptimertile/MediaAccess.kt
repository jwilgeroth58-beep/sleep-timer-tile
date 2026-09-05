package com.jwilgeroth.sleeptimertile

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Small helpers around the Notification Listener permission, which is what
 * unlocks MediaSessionManager.getActiveSessions() (the precise "pause the app
 * that's actually playing" path).
 */
object MediaAccess {

    /** True if the user has granted us Notification Listener access. */
    fun isListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /**
     * The system settings page where the user turns Notification Listener access
     * on. Apps are NOT allowed to toggle this themselves — we can only send the
     * user here.
     */
    fun listenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
