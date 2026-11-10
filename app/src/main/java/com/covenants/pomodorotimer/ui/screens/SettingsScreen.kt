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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ChimeChoice
import com.covenants.pomodorotimer.core.model.ThemeChoice
import com.covenants.pomodorotimer.ui.components.DividerLine
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.SettingsRow
import com.covenants.pomodorotimer.ui.components.StatusPill
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors

@Composable
fun SettingsScreen(
    settings: AppSettings,
    notificationsGranted: Boolean,
    exactAlarmsAllowed: Boolean,
    onSetTheme: (ThemeChoice) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetChime: (ChimeChoice) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetFullScreenAlerts: (Boolean) -> Unit,
    fullScreenAlertsAllowed: Boolean,
    onRequestFullScreen: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onExport: () -> Unit,
    onDeleteHistory: () -> Unit,
    onNavigateRoutine: () -> Unit,
    onNavigateConstellations: () -> Unit,
    onNavigatePlus: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            eyebrow = "Make it your own",
            title = "Preferences",
            trailing = {
                IconCircleButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Preferences",
                    onClick = {},
                )
            },
        )
        Spacer(Modifier.height(18.dp))

        SectionLabel("Appearance")
        Spacer(Modifier.height(8.dp))
        OrbitCard {
            DropdownSettingRow(
                title = "Theme",
                value = settings.theme.label(),
                options = ThemeChoice.entries.map { it.label() to it },
                onSelect = { onSetTheme(it) },
            )
            DividerLine()
            ToggleSettingRow(
                title = "Reduce motion",
                subtitle = "Disable nonessential transitions.",
                checked = settings.reduceMotion,
                onChange = onSetReduceMotion,
            )
            DividerLine()
            ToggleSettingRow(
                title = "High contrast",
                subtitle = "Stronger borders and text contrast.",
                checked = settings.highContrast,
                onChange = onSetHighContrast,
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Notifications & sound")
        Spacer(Modifier.height(8.dp))
        OrbitCard {
            ToggleSettingRow(
                title = "Session complete alert",
                subtitle = if (notificationsGranted) "Android alerts are allowed." else "Android has not granted alert permission.",
                checked = settings.notificationsEnabled,
                onChange = onSetNotifications,
            )
            if (!notificationsGranted) {
                TextButton(onClick = onRequestNotifications) { Text("Allow notifications") }
                DividerLine()
            }
            DividerLine()
            ToggleSettingRow(
                title = "Gentle sound",
                subtitle = "A soft chime at the end of a stage.",
                checked = settings.soundEnabled,
                onChange = onSetSound,
            )
            DividerLine()
            DropdownSettingRow(
                title = "Chime",
                value = settings.chime.label(),
                options = ChimeChoice.entries.map { it.label() to it },
                onSelect = { onSetChime(it) },
            )
            DividerLine()
            ToggleSettingRow(
                title = "Haptic feedback",
                subtitle = "One gentle vibration on start and completion.",
                checked = settings.hapticsEnabled,
                onChange = onSetHaptics,
            )
            DividerLine()
            ToggleSettingRow(
                title = "Full-screen alert",
                subtitle = if (fullScreenAlertsAllowed) {
                    "Show the completion alert over the lock screen."
                } else {
                    "Android has not granted full-screen alert access."
                },
                checked = settings.fullScreenAlerts,
                onChange = onSetFullScreenAlerts,
            )
            if (!fullScreenAlertsAllowed) {
                TextButton(onClick = onRequestFullScreen) { Text("Allow full-screen alerts") }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Alarm precision")
        Spacer(Modifier.height(8.dp))
        OrbitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = if (exactAlarmsAllowed) colors.accent else colors.danger,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Precise alarms",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text,
                    )
                    Text(
                        text = if (exactAlarmsAllowed) {
                            "Exact alarms are allowed. Completion alerts can be timely."
                        } else {
                            "Android may delay alerts. The countdown itself stays accurate."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
                StatusPill(
                    text = if (exactAlarmsAllowed) "Allowed" else "Limited",
                    accent = exactAlarmsAllowed,
                )
            }
            if (!exactAlarmsAllowed) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onRequestExactAlarm) { Text("Open Android alarm settings") }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Your data")
        Spacer(Modifier.height(8.dp))
        OrbitCard {
            SettingsRow(
                title = "Export local sessions",
                subtitle = "Versioned CSV via Android's file picker.",
                onClick = onExport,
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            DividerLine()
            SettingsRow(
                title = "Delete all history",
                subtitle = "Removes completed and stopped sessions. The active session is kept.",
                onClick = { showDeleteDialog = true },
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Task titles, focus history and preferences stay on this device. No account or upload.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.faint,
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("More")
        Spacer(Modifier.height(8.dp))
        OrbitCard {
            SettingsRow(
                title = "Focus routine",
                subtitle = "Durations, long-break cadence and automation.",
                onClick = onNavigateRoutine,
                trailing = { Chevron() },
            )
            DividerLine()
            SettingsRow(
                title = "Milestones",
                subtitle = "A calm constellation that grows with completed sessions.",
                onClick = onNavigateConstellations,
                trailing = { Chevron() },
            )
            DividerLine()
            SettingsRow(
                title = "Lodestar Plus",
                subtitle = "Optional upgrades. Core timers stay free.",
                onClick = onNavigatePlus,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Chevron()
                    }
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Lodestar ${com.covenants.pomodorotimer.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = colors.muted)
            Text("Offline-first · no account", style = MaterialTheme.typography.bodySmall, color = colors.faint)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = colors.surface,
            title = { Text("Delete all local history?") },
            text = {
                Text(
                    "Completed and stopped sessions will be permanently removed from this device. " +
                        "The active timer, tasks and preferences are kept. This cannot be undone.",
                    color = colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteHistory()
                }) { Text("Delete history") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = LocalOrbitColors.current.faint,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = LocalOrbitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> DropdownSettingRow(
    title: String,
    value: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    val colors = LocalOrbitColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.faint,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surfaceElevated,
        ) {
            options.forEach { (label, option) ->
                DropdownMenuItem(
                    text = { Text(label, color = colors.text) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = {
                        if (label == value) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent)
                        }
                    },
                )
            }
        }
    }
}

private fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.MIDNIGHT -> "Midnight Orbit"
    ThemeChoice.WARM_PAPER -> "Warm Paper"
    ThemeChoice.AURORA -> "Quiet Aurora"
}

private fun ChimeChoice.label(): String = when (this) {
    ChimeChoice.GENTLE -> "Gentle"
    ChimeChoice.CHIME -> "Chime"
    ChimeChoice.BELL -> "Bell"
}
