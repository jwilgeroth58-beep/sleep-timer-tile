package com.jwilgeroth.sleeptimertile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

/**
 * The Quick Settings tile.
 *
 * Phase 2 behavior:
 *   - Tap while idle  -> schedule an exact alarm, mark the tile ACTIVE, show
 *                        the remaining time in the label.
 *   - Tap while active -> cancel the alarm, mark the tile IDLE again.
 *   - When the alarm fires, TimerExpiredReceiver locks the screen and clears
 *     the state; the tile returns to idle next time it's shown.
 *
 * The tile is only "live" while the shade is open (onStartListening..onStop-
 * Listening), so we recompute its appearance from TimerState whenever we get a
 * chance to draw.
 */
class SleepTimerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening")
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (TimerState.isRunning(this)) {
            Log.d(TAG, "onClick -> cancel timer")
            SleepTimer.cancel(this)
        } else {
            val durationMs = Settings.durationMinutes(this) * 60_000L
            Log.d(TAG, "onClick -> start timer for ${durationMs}ms")
            SleepTimer.start(this, durationMs)
        }
        refreshTile()
    }

    /** Draw the tile to match the current timer state. */
    private fun refreshTile() {
        val tile = qsTile ?: return
        val target = TimerState.targetAt(this)
        val remaining = target - System.currentTimeMillis()

        if (remaining > 0) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.tile_label_active, remainingText(remaining))
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_label)
        }
        tile.updateTile()
    }

    /** "45m" when >= 1 min remaining, otherwise "30s". Rounds up. */
    private fun remainingText(remainingMs: Long): String {
        val totalSeconds = (remainingMs + 999) / 1000
        return if (totalSeconds >= 60) {
            "${(totalSeconds + 59) / 60}m"
        } else {
            "${totalSeconds}s"
        }
    }

    private companion object {
        const val TAG = "SleepTimerTile"
    }
}
