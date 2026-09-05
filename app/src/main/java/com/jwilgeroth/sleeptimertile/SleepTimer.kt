package com.jwilgeroth.sleeptimertile

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

/**
 * One place that starts and cancels a sleep timer, so the tile, the "Test now"
 * button, and the notification's Cancel action all behave identically.
 *
 * Starting = schedule the alarm + remember the target + show the countdown
 * notification. Cancelling = undo all three. Either way we nudge the tile to
 * redraw.
 */
object SleepTimer {

    fun start(context: Context, durationMs: Long) {
        val target = System.currentTimeMillis() + durationMs
        AlarmScheduler.schedule(context, target)
        TimerState.setTarget(context, target)
        NotificationHelper.showCountdown(context, target)
        refreshTile(context)
    }

    fun cancel(context: Context) {
        AlarmScheduler.cancel(context)
        TimerState.clear(context)
        NotificationHelper.cancel(context)
        refreshTile(context)
    }

    private fun refreshTile(context: Context) {
        TileService.requestListeningState(
            context,
            ComponentName(context, SleepTimerTileService::class.java)
        )
    }
}
