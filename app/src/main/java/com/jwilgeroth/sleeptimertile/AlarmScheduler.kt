package com.jwilgeroth.sleeptimertile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Schedules (and cancels) the one alarm that ends the sleep timer.
 *
 * WHY AN ALARM AND NOT A TIMER/COROUTINE: for most of the countdown the phone
 * is asleep and dozing. A coroutine delay, Handler.postDelayed, or a foreground
 * service loop all get frozen by Doze and won't fire on time. AlarmManager's
 * setExactAndAllowWhileIdle() is specifically allowed to punch through Doze at
 * an exact moment. So the alarm is the source of truth for firing; anything
 * else (like a countdown display) is just decoration.
 */
object AlarmScheduler {
    private const val TAG = "SleepTimerTile"
    private const val REQUEST_CODE = 1001

    fun schedule(context: Context, triggerAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)

        // On API 31+ exact alarms require permission. We declared USE_EXACT_ALARM
        // (auto-granted at install for sideloaded apps), so canScheduleExactAlarms()
        // should be true. We check anyway and fall back to a less-precise alarm
        // rather than crashing if it isn't.
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            am.canScheduleExactAlarms()

        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            Log.d(TAG, "Exact alarm scheduled for $triggerAtMillis")
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            Log.w(TAG, "Exact alarms not permitted; scheduled inexact alarm")
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pendingIntent(context))
        Log.d(TAG, "Alarm cancelled")
    }

    /**
     * The PendingIntent must be built identically to schedule AND cancel it, so
     * the same REQUEST_CODE + intent are used in both. FLAG_IMMUTABLE is required
     * on modern Android; FLAG_UPDATE_CURRENT reuses the existing one.
     */
    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TimerExpiredReceiver::class.java)
            .setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
