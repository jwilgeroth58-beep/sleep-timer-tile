package com.jwilgeroth.sleeptimertile

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log

/**
 * Fires when the alarm goes off (the timer expired). This is what actually
 * ends the sleep session.
 *
 * Phase 2 does two things here:
 *   1. Clear the running-timer state.
 *   2. Lock the screen via Device Admin (turns the display off).
 *
 * Phase 3 will add "pause media" alongside the lock.
 *
 * The OS may have destroyed our whole process by the time this fires — that's
 * fine. A BroadcastReceiver is spun up fresh just to run onReceive().
 */
class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_EXPIRED) return
        Log.d(TAG, "Timer expired -> locking screen")

        TimerState.clear(context)
        NotificationHelper.cancel(context)

        // Honor the user's toggles. Pause playback first (a clean stop), then
        // turn the screen off.
        if (Settings.pauseMedia(context)) {
            MediaPauser.pauseAll(context)
        }
        if (Settings.lockScreen(context)) {
            lockScreen(context)
        }

        // Nudge the tile to redraw itself back to idle (best-effort; only works
        // if/when the shade is open).
        TileService.requestListeningState(
            context,
            ComponentName(context, SleepTimerTileService::class.java)
        )
    }

    private fun lockScreen(context: Context) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, SleepAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()   // requires the force-lock policy we declared
            Log.d(TAG, "lockNow() called")
        } else {
            // Graceful degradation: without Device Admin we simply can't turn
            // the screen off. The timer still "ended", it just can't lock.
            Log.w(TAG, "Device Admin not active; cannot lock screen")
        }
    }

    companion object {
        const val TAG = "SleepTimerTile"
        const val ACTION_TIMER_EXPIRED = "com.jwilgeroth.sleeptimertile.action.TIMER_EXPIRED"
    }
}
