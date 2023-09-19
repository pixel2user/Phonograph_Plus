/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeUnit
import player.phonograph.model.time.displayText
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.util.SleepTimer
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.BridgeDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.WheelPicker
import player.phonograph.util.debug
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class SleepTimerDialog2 : BridgeDialogFragment() {

    private lateinit var timer: SleepTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = MusicPlayerRemote.musicService
        if (service != null) {
            timer = SleepTimer.instance(service)
        } else {
            Toast.makeText(requireContext(), "Music Service not available!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        if (hasTimer()) {
            val current = Setting.instance.nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime()
            timerUpdater = TimerUpdater(current.coerceAtLeast(0)).also { it.start() }
            _remainingTimeText.value = resources.getString(R.string.cancel_current_timer)
        } else {
            _remainingTimeText.value = resources.getString(android.R.string.cancel)
        }
    }



    private var timerUpdater: TimerUpdater? = null
    private val _remainingTimeText: MutableStateFlow<String> = MutableStateFlow("")
    private val remainingTimeText get() = _remainingTimeText.asStateFlow()


    @Composable
    override fun Content() {
        PhonographTheme {
            val dialogState = rememberMaterialDialogState(true)


            var duration: Duration by remember { mutableStateOf(currentTimer()) }
            var shouldFinishLastSong: Boolean by remember { mutableStateOf(Setting.instance.sleepTimerFinishMusic) }

            val resources = LocalContext.current.resources
            var previewText by remember { mutableStateOf("") }
            val flow = snapshotFlow { duration }
            LaunchedEffect(dialogState) {
                flow.collect {
                    previewText = it.displayText(resources, "")
                }
            }


            val cancelButtonText: String by remainingTimeText.collectAsState()

            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    positiveButton(res = android.R.string.ok) {
                        val success = applyTimer(duration, shouldFinishLastSong)
                        Toast.makeText(
                            requireActivity(),
                            if (success) {
                                getString(R.string.sleep_timer_set, duration.toSeconds() / 60)
                            } else {
                                getString(R.string.failed)
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                    negativeButton(cancelButtonText) {
                        val success = cancelTimer()
                        Toast.makeText(
                            requireActivity(),
                            if (success) {
                                getString(R.string.sleep_timer_canceled)
                            } else {
                                getString(R.string.failed)
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }
            ) {
                title(res = R.string.action_sleep_timer)
                Column(Modifier.padding(12.dp)) {
                    TimeIntervalPicker(duration) {
                        duration = it
                        debug { Log.v(TAG, it.toString()) }
                    }
                    Text(
                        previewText,
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                shouldFinishLastSong = !shouldFinishLastSong
                            }
                    ) {
                        Checkbox(
                            shouldFinishLastSong, { shouldFinishLastSong = !shouldFinishLastSong },
                            Modifier.padding(4.dp),
                        )
                        Text(
                            stringResource(R.string.finish_current_music_sleep_timer),
                            Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }

    private fun currentTimer(): Duration {
        val time =
            (Setting.instance.nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime()).coerceAtLeast(0) / (1000 * 60)
        return if (time >= 60) Duration.Hour(time / 60) else Duration.Minute(time)
    }

    private fun hasTimer(): Boolean = timer.hasTimer()

    private fun applyTimer(duration: Duration, shouldFinishLastSong: Boolean): Boolean =
        timer.setTimer(duration.toSeconds() / 60, shouldFinishLastSong).also { success ->
            Toast.makeText(
                requireActivity(),
                if (success) {
                    getString(R.string.sleep_timer_set, duration.toSeconds() / 60)
                } else {
                    getString(R.string.failed)
                },
                Toast.LENGTH_SHORT
            ).show()
        }

    private fun cancelTimer() = timer.cancelTimer()


    override fun onDestroy() {
        super.onDestroy()
        timerUpdater?.cancel()
    }

    private inner class TimerUpdater(time: Long) : CountDownTimer(time, 1000) {

        private val defaultText = resources.getString(R.string.cancel_current_timer)

        override fun onTick(millisUntilFinished: Long) {
            _remainingTimeText.value = text(millisUntilFinished)
        }

        override fun onFinish() {
            _remainingTimeText.value = text(0)
        }

        private fun text(time: Long): String = buildString {
            append(defaultText)
            if (time > 0) append("(${getReadableDurationString(time)})")
        }

    }

    companion object {
        private const val TAG = "SleepTimer"
    }
}

@Composable
private fun TimeIntervalPicker(
    selected: Duration,
    modifier: Modifier = Modifier,
    onChangeDuration: (Duration) -> Unit,
) {
    val resources = LocalContext.current.resources
    val units = remember {
        listOf(TimeUnit.Minute, TimeUnit.Hour)
    }
    val numbers = remember { (0..60).toList() }

    var currentNumber by remember { mutableStateOf(selected.value) }
    var currentUnit by remember { mutableStateOf(selected.unit) }

    Row(
        modifier,
        Arrangement.SpaceBetween
    ) {
        WheelPicker(
            items = numbers.map { it.toString() },
            initialIndex = selected.value.coerceAtMost(60).toInt(),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentNumber = numbers[it].toLong()
            onChangeDuration(Duration.of(numbers[it].toLong(), currentUnit))
        }

        WheelPicker(
            items = units.map { it.displayText(resources) },
            initialIndex = units.indexOf(selected.unit),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentUnit = units[it]
            onChangeDuration(Duration.of(currentNumber, units[it]))
        }
    }
}
