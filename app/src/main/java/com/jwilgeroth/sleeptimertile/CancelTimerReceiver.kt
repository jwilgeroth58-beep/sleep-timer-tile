package com.jwilgeroth.sleeptimertile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles the "Cancel" button on the countdown notification. Same effect as
 * tapping the active tile.
 */
class CancelTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Cancel requested from notification")
        SleepTimer.cancel(context)
    }

    private companion object {
        const val TAG = "SleepTimerTile"
    }
}
