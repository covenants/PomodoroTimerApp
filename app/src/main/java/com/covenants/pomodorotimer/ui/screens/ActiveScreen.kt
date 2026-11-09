package com.covenants.pomodorotimer.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.core.domain.TimeFormat
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.ui.AppUiState
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.OrbitRing
import com.covenants.pomodorotimer.ui.components.PrimaryActionButton
import com.covenants.pomodorotimer.ui.components.SecondaryActionButton
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.StatusPill
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors
import java.util.TimeZone

@Composable
fun ActiveScreen(
    state: AppUiState,
    notificationsGranted: Boolean,
    exactAlarmsAllowed: Boolean,
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    onAddFive: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    onStartBreakNow: () -> Unit,
    onDismissBreak: () -> Unit,
    onAcknowledge: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val active = state.active
    var showEndDialog by remember { mutableStateOf(false) }
    val isWaitingBreak = state.isWaitingBreak
    val isWaitingFocus = state.isWaitingFocus
    val isCompleted = state.isCompleted
    val isStopwatch = active?.mode == TimerMode.STOPWATCH
    val isCountdown = active?.mode == TimerMode.COUNTDOWN
    val stage = active?.stage

    val statusLabel = when {
        isCompleted -> "SESSION COMPLETE"
        isWaitingBreak -> "BREAK READY"
        isWaitingFocus -> "FOCUS READY"
        active?.mode == TimerMode.COUNTDOWN -> "COUNTDOWN"
        isStopwatch -> "STOPWATCH"
        stage == FocusStage.SHORT_BREAK -> "SHORT BREAK"
        stage == FocusStage.LONG_BREAK -> "LONG BREAK"
        stage == FocusStage.WORK -> {
            val every = state.routine.longBreakEverySafe
            val current = ((active?.completedWorkCount ?: 0) % every) + 1
            "FOCUSING · $current OF $every"
        }
        else -> "FOCUS"
    }

    val title = when {
        isCompleted -> "Time's up."
        isWaitingBreak -> "Break ready."
        isWaitingFocus -> "Focus ready."
        stage == FocusStage.SHORT_BREAK || stage == FocusStage.LONG_BREAK -> "Break time."
        isCountdown -> "Quick Timer"
        isStopwatch -> "Stopwatch"
        else -> "Stay in your orbit."
    }

    val displayMs = if (isStopwatch) state.stopwatchMs else state.remainingMs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 30.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircleButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Text(
                text = when {
                    isStopwatch -> "STOPWATCH"
                    isCountdown -> "QUICK TIMER"
                    stage == FocusStage.WORK -> "FOCUS / ${state.routine.name.uppercase()}"
                    stage == FocusStage.SHORT_BREAK -> "FOCUS / SHORT BREAK"
                    stage == FocusStage.LONG_BREAK -> "FOCUS / LONG BREAK"
                    else -> "SESSION"
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconCircleButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Close",
                onClick = onBack,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.text,
        )
        Spacer(Modifier.height(16.dp))

        OrbitRing(
            progress = state.progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(286.dp),
            reduceMotion = state.settings.reduceMotion,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel(statusLabel)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isStopwatch) TimeFormat.stopwatch(displayMs) else TimeFormat.countdown(displayMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.text,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = when {
                        isCompleted -> "Saved to your local history"
                        isWaitingBreak -> "Ready when you are"
                        isWaitingFocus -> "Your next focus is ready"
                        state.isPaused -> "Paused · resumes when you're ready"
                        isStopwatch -> "Elapsed active time"
                        else -> "Finish at ${
                            TimeFormat.finishAt(
                                state.nowWallMs + displayMs,
                                TimeZone.getDefault().id,
                            )
                        }"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!isStopwatch && !isCompleted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface.copy(alpha = 0.55f))
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = active?.taskTitle ?: if (isCountdown) "Quick countdown" else "No task attached",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (active?.taskTitle != null) {
                    StatusPill("Task", accent = true)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            isCompleted -> {
                OrbitCard {
                    Text(
                        "Session recorded",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Your completed time is saved on this device. Nothing was uploaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryActionButton(
                        text = "Done",
                        icon = Icons.Rounded.Check,
                        onClick = onAcknowledge,
                    )
                }
            }

            isWaitingBreak || isWaitingFocus -> {
                OrbitCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isWaitingFocus) {
                                    "Next up · Focus session"
                                } else {
                                    "Next up · ${if (stage == FocusStage.LONG_BREAK) "Long break" else "Short break"}"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.text,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isWaitingFocus) {
                                    "${TimeFormat.durationLabel(active?.totalDurationMs ?: 0L)} of focused attention."
                                } else {
                                    "${TimeFormat.durationLabel(active?.totalDurationMs ?: 0L)} to reset your attention."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                            )
                        }
                        StatusPill("Ready", accent = true)
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryActionButton(
                        text = if (isWaitingFocus) "Start focus" else "Start break",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onStartBreakNow,
                    )
                    Spacer(Modifier.height(9.dp))
                    if (isWaitingBreak) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            SecondaryActionButton(
                                text = "Later",
                                onClick = onDismissBreak,
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryActionButton(
                                text = "Skip break",
                                icon = Icons.Rounded.SkipNext,
                                onClick = onSkip,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        SecondaryActionButton(
                            text = "Back to timer",
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            else -> {
                PrimaryActionButton(
                    text = if (state.isPaused) "Resume" else "Pause",
                    icon = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    onClick = onTogglePause,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (!isStopwatch) {
                        SecondaryActionButton(
                            text = "+5 minutes",
                            icon = Icons.Rounded.Add,
                            onClick = onAddFive,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryActionButton(
                            text = if (isCountdown) "Skip" else "Skip stage",
                            icon = Icons.Rounded.SkipNext,
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        SecondaryActionButton(
                            text = "Reset stopwatch",
                            icon = Icons.Rounded.TimerOff,
                            onClick = { showEndDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                SecondaryActionButton(
                    text = "End session",
                    icon = Icons.Rounded.Close,
                    onClick = { showEndDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if ((!notificationsGranted || (!exactAlarmsAllowed && !isStopwatch)) &&
            (state.isRunning || state.isPaused)
        ) {
            Spacer(Modifier.height(14.dp))
            if (!notificationsGranted) {
                PermissionNotice(
                    icon = Icons.Rounded.NotificationsOff,
                    title = "Alerts are off",
                    body = "Your timer still runs accurately, but Android cannot show the end alert.",
                    action = "Allow",
                    onAction = onRequestNotifications,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!exactAlarmsAllowed && !isStopwatch) {
                PermissionNotice(
                    icon = Icons.Rounded.TimerOff,
                    title = "Precise alerts unavailable",
                    body = "Android may delay the completion alert. The countdown itself stays correct.",
                    action = "Settings",
                    onAction = onRequestExactAlarm,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Notification actions use the same command processor as this screen.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            containerColor = colors.surface,
            title = { Text(if (isStopwatch) "Reset stopwatch?" else "End this session?") },
            text = {
                Text(
                    if (isStopwatch) {
                        "The elapsed time will be recorded as a stopped session. You can start a new stopwatch afterwards."
                    } else {
                        "The time you have already focused is recorded as a stopped session. No completed session is counted."
                    },
                    color = colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onStop()
                }) { Text("End") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Keep going") }
            },
        )
    }
}

@Composable
private fun PermissionNotice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    OrbitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.text, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
