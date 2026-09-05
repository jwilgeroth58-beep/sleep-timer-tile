package com.jwilgeroth.sleeptimertile

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * The ongoing "a sleep timer is running" notification.
 *
 * Trick that keeps this cheap: instead of a service updating the text every
 * second, we hand Android the target time and let it render a live countdown
 * chronometer itself (setUsesChronometer + setChronometerCountDown + setWhen).
 * The system UI ticks it down; we post exactly once. The alarm remains the real
 * source of firing — this notification is purely a display companion, exactly as
 * the spec allows.
 */
object NotificationHelper {
    private const val TAG = "SleepTimerTile"
    private const val CHANNEL_ID = "sleep_timer_countdown"
    private const val NOTIF_ID = 1

    fun showCountdown(context: Context, targetAtMillis: Long) {
        if (!hasPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping countdown notification")
            return
        }
        ensureChannel(context)

        val cancelIntent = PendingIntent.getBroadcast(
            context,
            2001,
            Intent(context, CancelTimerReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val tapIntent = PendingIntent.getActivity(
            context,
            2002,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_sleep)
            .setOngoing(true)             // can't be swiped away while running
            .setOnlyAlertOnce(true)       // no repeat buzz on updates
            .setShowWhen(false)           // our custom view shows the countdown
            .setContentIntent(tapIntent)
            // Custom layout, but still decorated with the system header + actions.
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(buildContentView(context, targetAtMillis))
            .setCustomBigContentView(buildContentView(context, targetAtMillis))
            .addAction(R.drawable.ic_tile_sleep, context.getString(R.string.notif_cancel), cancelIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    /**
     * Builds the custom notification body: icon, title/text, and a Chronometer
     * that counts down to the target time on its own.
     *
     * Chronometer works in the SystemClock.elapsedRealtime() timebase, not wall
     * clock, so we convert: base = now(elapsed) + milliseconds remaining. With
     * count-down mode on, it displays (base - now) ticking toward 0.
     */
    private fun buildContentView(context: Context, targetAtMillis: Long): RemoteViews {
        val remaining = targetAtMillis - System.currentTimeMillis()
        val base = SystemClock.elapsedRealtime() + remaining
        return RemoteViews(context.packageName, R.layout.notif_countdown).apply {
            setTextViewText(R.id.notif_title, context.getString(R.string.notif_title))
            setTextViewText(R.id.notif_text, context.getString(R.string.notif_text))
            setChronometerCountDown(R.id.notif_chrono, true)
            setChronometer(R.id.notif_chrono, base, null, true)
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW // silent; it's a status, not an alert
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
