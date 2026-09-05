package com.jwilgeroth.sleeptimertile

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import android.view.KeyEvent

/**
 * Pauses whatever is currently playing when the sleep timer ends.
 *
 * Two mechanisms, most-precise first:
 *
 *   1. Media sessions (needs Notification Listener access): ask the system for
 *      the active MediaControllers and tell the ones that are actually PLAYING
 *      to pause. Precise, and won't accidentally toggle something.
 *
 *   2. Media key event (needs NOTHING): simulate pressing the hardware "pause"
 *      key. Works for most apps and requires no permission, so it's our always-
 *      on fallback / backstop.
 *
 * We run the key event regardless. KEYCODE_MEDIA_PAUSE only ever pauses (unlike
 * PLAY_PAUSE, it never starts playback), so firing it after a session pause is
 * harmless.
 */
object MediaPauser {
    private const val TAG = "SleepTimerTile"

    fun pauseAll(context: Context) {
        if (MediaAccess.isListenerEnabled(context)) {
            val paused = pauseViaSessions(context)
            Log.d(TAG, "pauseViaSessions paused something = $paused")
        } else {
            Log.d(TAG, "Notification Listener off; relying on media key only")
        }
        dispatchPauseKey(context)
    }

    private fun pauseViaSessions(context: Context): Boolean {
        val msm = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, MediaNotificationListener::class.java)
        return try {
            var pausedAny = false
            for (controller in msm.getActiveSessions(listener)) {
                val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                if (isPlaying) {
                    controller.transportControls.pause()
                    pausedAny = true
                }
            }
            pausedAny
        } catch (e: SecurityException) {
            // Listener flipped off between our check and this call.
            Log.w(TAG, "getActiveSessions denied: ${e.message}")
            false
        }
    }

    private fun dispatchPauseKey(context: Context) {
        val am = context.getSystemService(AudioManager::class.java)
        val code = KeyEvent.KEYCODE_MEDIA_PAUSE
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        Log.d(TAG, "Dispatched KEYCODE_MEDIA_PAUSE")
    }
}
