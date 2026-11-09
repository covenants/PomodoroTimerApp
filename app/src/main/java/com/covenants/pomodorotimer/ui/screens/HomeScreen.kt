package com.covenants.pomodorotimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.core.domain.TimeFormat
import com.covenants.pomodorotimer.core.model.FocusTask
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.ui.AppUiState
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.OrbitRing
import com.covenants.pomodorotimer.ui.components.PresetChip
import com.covenants.pomodorotimer.ui.components.PrimaryActionButton
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SecondaryActionButton
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.SegmentedModes
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors
import java.util.TimeZone

@Composable
fun HomeScreen(
    state: AppUiState,
    tasks: List<FocusTask>,
    onSelectMode: (TimerMode) -> Unit,
    onStartFocus: (FocusTask?) -> Unit,
    onStartCountdown: (Long) -> Unit,
    onStartStopwatch: () -> Unit,
    onOpenActive: () -> Unit,
    onSaveRoutine: (RoutineConfig) -> Unit,
    onNavigateRoutine: () -> Unit,
    onNavigateConstellations: () -> Unit,
    onAddTask: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val mode = state.active?.mode ?: state.settings.lastMode
    val reduceMotion = state.settings.reduceMotion
    var countdownMs by rememberSaveable { mutableLongStateOf(15 * 60_000L) }
    var showDurationEditor by rememberSaveable { mutableStateOf(false) }

    val title = when (mode) {
        TimerMode.FOCUS -> "Make time count."
        TimerMode.COUNTDOWN -> "Quick Timer"
        TimerMode.STOPWATCH -> "Stopwatch"
    }
    val eyebrow = when (mode) {
        TimerMode.FOCUS -> TimeFormat.eyebrowDate(TimeZone.getDefault().id)
        else -> "EVERYDAY TOOLS"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            eyebrow = eyebrow,
            title = title,
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconCircleButton(
                        icon = Icons.Rounded.AutoAwesome,
                        contentDescription = "Milestones",
                        onClick = onNavigateConstellations,
                    )
                    IconCircleButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add task",
                        onClick = onAddTask,
                        filled = true,
                    )
                }
            },
        )
        Spacer(Modifier.height(20.dp))
        SegmentedModes(
            selected = mode,
            onSelect = { selected ->
                if (state.active == null) onSelectMode(selected)
            },
        )
        Spacer(Modifier.height(26.dp))

        when (mode) {
            TimerMode.FOCUS -> FocusHome(
                state = state,
                reduceMotion = reduceMotion,
                onStartFocus = { onStartFocus(null) },
                onOpenActive = onOpenActive,
                onSaveRoutine = onSaveRoutine,
                onNavigateRoutine = onNavigateRoutine,
            )

            TimerMode.COUNTDOWN -> CountdownHome(
                state = state,
                countdownMs = countdownMs,
                onCountdownChange = { countdownMs = it },
                onEditTime = { showDurationEditor = true },
                onStart = { onStartCountdown(countdownMs) },
                onOpenActive = onOpenActive,
            )

            TimerMode.STOPWATCH -> StopwatchHome(
                state = state,
                onStart = onStartStopwatch,
                onOpenActive = onOpenActive,
            )
        }
    }

    if (showDurationEditor) {
        DurationEditDialog(
            initialMs = countdownMs,
            onDismiss = { showDurationEditor = false },
            onConfirm = {
                countdownMs = it
                showDurationEditor = false
            },
        )
    }
}

@Composable
private fun FocusHome(
    state: AppUiState,
    reduceMotion: Boolean,
    onStartFocus: () -> Unit,
    onOpenActive: () -> Unit,
    onSaveRoutine: (RoutineConfig) -> Unit,
    onNavigateRoutine: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val routine = state.routine
    val active = state.active
    val hasActive = active != null
    val displayMs = if (hasActive) state.remainingMs else routine.focusMs
    val progress = if (hasActive) state.progress else 0f

    OrbitRing(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(282.dp),
        reduceMotion = reduceMotion,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel("Focus session")
            Spacer(Modifier.height(8.dp))
            Text(
                text = TimeFormat.countdown(displayMs),
                style = MaterialTheme.typography.displayLarge,
                color = colors.text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    state.isPaused -> "Paused"
                    state.isRunning -> "Focusing now"
                    else -> "Ready when you are"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Today's focus",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${TimeFormat.durationLabel(state.todayFocusMs)} · ${state.todaySessions} sessions",
            style = MaterialTheme.typography.bodySmall,
            color = colors.text,
            fontWeight = FontWeight.Medium,
        )
    }

    Spacer(Modifier.height(16.dp))
    PrimaryActionButton(
        text = when {
            hasActive && state.isPaused -> "Resume session"
            hasActive -> "Open session"
            else -> "Start focus"
        },
        icon = Icons.Rounded.PlayArrow,
        onClick = if (hasActive) onOpenActive else onStartFocus,
    )

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetChip(
            label = "25 / 5",
            selected = routine.focusMinutes == 25 && routine.shortBreakMinutes == 5,
            onClick = { onSaveRoutine(routine.copy(focusMinutes = 25, shortBreakMinutes = 5)) },
            modifier = Modifier.weight(1f),
        )
        PresetChip(
            label = "50 / 10",
            selected = routine.focusMinutes == 50 && routine.shortBreakMinutes == 10,
            onClick = { onSaveRoutine(routine.copy(focusMinutes = 50, shortBreakMinutes = 10)) },
            modifier = Modifier.weight(1f),
        )
        PresetChip(
            label = "5 min",
            selected = routine.focusMinutes == 5,
            onClick = { onSaveRoutine(routine.copy(focusMinutes = 5)) },
            modifier = Modifier.weight(1f),
        )
        PresetChip(
            label = "Custom",
            selected = false,
            onClick = onNavigateRoutine,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "One tap to start. Your time stays yours — every session is stored on this device.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.faint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CountdownHome(
    state: AppUiState,
    countdownMs: Long,
    onCountdownChange: (Long) -> Unit,
    onEditTime: () -> Unit,
    onStart: () -> Unit,
    onOpenActive: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val active = state.active?.takeIf { it.mode == TimerMode.COUNTDOWN }
    val displayMs = active?.let { state.remainingMs } ?: countdownMs
    val progress = active?.let { state.progress } ?: 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(276.dp)
            .clickable(onClick = onEditTime),
        contentAlignment = Alignment.Center,
    ) {
        OrbitRing(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            reduceMotion = state.settings.reduceMotion,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel("Countdown")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = TimeFormat.countdown(displayMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (active != null) "Tap to return" else "Tap time to edit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    SectionLabel("Quick start")
    Spacer(Modifier.height(9.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
            val ms = minutes * 60_000L
            PresetChip(
                label = "$minutes",
                selected = active == null && countdownMs == ms,
                onClick = { onCountdownChange(ms) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    PrimaryActionButton(
        text = if (active != null) "Open countdown" else "Start countdown",
        icon = Icons.Rounded.PlayArrow,
        onClick = if (active != null) onOpenActive else onStart,
    )
    Spacer(Modifier.height(11.dp))
    OrbitCard(
        onClick = { onCountdownChange(8 * 60_000L) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Saved timer", style = MaterialTheme.typography.bodyMedium, color = colors.text)
                Text(
                    "Stretching · 08:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            Text(
                "Use",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun StopwatchHome(
    state: AppUiState,
    onStart: () -> Unit,
    onOpenActive: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val active = state.active?.takeIf { it.mode == TimerMode.STOPWATCH }
    val displayMs = active?.let { state.stopwatchMs } ?: 0L

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(276.dp),
        contentAlignment = Alignment.Center,
    ) {
        OrbitRing(
            progress = if (active != null) state.progress else 0f,
            modifier = Modifier.fillMaxSize(),
            reduceMotion = state.settings.reduceMotion,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel("Stopwatch")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = TimeFormat.stopwatch(displayMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (active != null) "Tap to return" else "Counts up, keeps running",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryActionButton(
        text = if (active != null) "Open stopwatch" else "Start stopwatch",
        icon = Icons.Rounded.PlayArrow,
        onClick = if (active != null) onOpenActive else onStart,
    )
    Spacer(Modifier.height(12.dp))
    SecondaryActionButton(
        text = "No lap tracking in this release",
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun DurationEditDialog(
    initialMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val colors = LocalOrbitColors.current
    val initialSeconds = initialMs / 1000L
    var hours by remember { mutableStateOf((initialSeconds / 3600).toString()) }
    var minutes by remember { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }
    var seconds by remember { mutableStateOf((initialSeconds % 60).toString()) }
    val total = (hours.toLongOrNull() ?: 0L) * 3600L +
        (minutes.toLongOrNull() ?: 0L) * 60L +
        (seconds.toLongOrNull() ?: 0L)
    val valid = total in 1..(12 * 3600L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Set a countdown", color = colors.text) },
        text = {
            Column {
                Text(
                    "Enter any duration up to 12 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationField("HH", hours, { hours = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f))
                    DurationField("MM", minutes, { minutes = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f))
                    DurationField("SS", seconds, { seconds = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (valid) TimeFormat.countdown(total * 1000L) else "Enter a positive duration",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (valid) colors.accent else colors.danger,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onConfirm(total * 1000L) }, enabled = valid) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DurationField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
