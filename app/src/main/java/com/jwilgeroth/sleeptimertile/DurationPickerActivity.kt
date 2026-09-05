package com.jwilgeroth.sleeptimertile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jwilgeroth.sleeptimertile.ui.theme.SleepTimerTileTheme

/**
 * Shown when the user LONG-PRESSES the Quick Settings tile. Android routes the
 * long-press to whichever activity registers for the QS_TILE_PREFERENCES action
 * (declared in the manifest). We present it as a floating dialog of preset
 * lengths; picking one starts a timer of that length right away.
 *
 * (Short-tapping the tile is unchanged: start-default / cancel.)
 */
class DurationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val running = TimerState.isRunning(this)

        setContent {
            SleepTimerTileTheme {
                DurationPickerDialog(
                    running = running,
                    onPick = { minutes ->
                        SleepTimer.start(this, minutes * 60_000L)
                        finish()
                    },
                    onCancelTimer = {
                        SleepTimer.cancel(this)
                        finish()
                    },
                    onMore = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun DurationPickerDialog(
    running: Boolean,
    onPick: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Start a sleep timer", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                listOf(15, 30, 45, 60).forEach { minutes ->
                    Button(
                        onClick = { onPick(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes minutes")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (running) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onCancelTimer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel running timer")
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onMore, modifier = Modifier.fillMaxWidth()) {
                    Text("More options…")
                }
            }
        }
    }
}
