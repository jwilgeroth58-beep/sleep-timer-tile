package com.jwilgeroth.sleeptimertile

import android.content.Context

/**
 * The single source of truth for "is a timer running, and when does it end?"
 *
 * We store one number: the wall-clock time (millis since epoch) the timer
 * should fire. 0 / absent means idle. Using SharedPreferences (a tiny key-value
 * file) means the state survives the tile service being destroyed and recreated
 * by the OS, which happens constantly. (Phase 4 migrates settings to DataStore;
 * this transient timer state can stay in prefs.)
 */
object TimerState {
    private const val PREFS = "sleep_timer_state"
    private const val KEY_TARGET = "target_at_millis"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setTarget(context: Context, atMillis: Long) {
        prefs(context).edit().putLong(KEY_TARGET, atMillis).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_TARGET).apply()
    }

    /** The scheduled fire time, or 0 if no timer is set. */
    fun targetAt(context: Context): Long = prefs(context).getLong(KEY_TARGET, 0L)

    /** True only if a timer is set AND still in the future. */
    fun isRunning(context: Context): Boolean = targetAt(context) > System.currentTimeMillis()
}
