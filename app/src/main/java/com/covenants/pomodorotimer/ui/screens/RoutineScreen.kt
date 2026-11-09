package com.covenants.pomodorotimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.ui.components.DividerLine
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.PrimaryActionButton
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors

@Composable
fun RoutineScreen(
    routine: RoutineConfig,
    onBack: () -> Unit,
    onSave: (RoutineConfig) -> Unit,
) {
    val colors = LocalOrbitColors.current
    var draft by remember(routine) { mutableStateOf(routine) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 30.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "PERSONALIZE YOUR FLOW",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
                modifier = Modifier.weight(1f),
            )
            IconCircleButton(
                icon = Icons.Rounded.Check,
                contentDescription = "Save routine",
                onClick = { onSave(draft) },
                filled = true,
            )
        }
        Spacer(Modifier.height(16.dp))
        ScreenHeader(eyebrow = "Focus routine", title = "Build your rhythm.")
        Spacer(Modifier.height(18.dp))

        OrbitCard {
            SectionLabel("Routine name")
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it.take(40)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            StepperRow(
                label = "Focus duration",
                value = draft.focusMinutes,
                range = 1..180,
                unit = "min",
                onChange = { draft = draft.copy(focusMinutes = it) },
            )
            DividerLine()
            StepperRow(
                label = "Short break",
                value = draft.shortBreakMinutes,
                range = 1..60,
                unit = "min",
                onChange = { draft = draft.copy(shortBreakMinutes = it) },
            )
            DividerLine()
            StepperRow(
                label = "Long break",
                value = draft.longBreakMinutes,
                range = 1..120,
                unit = "min",
                onChange = { draft = draft.copy(longBreakMinutes = it) },
            )
            DividerLine()
            StepperRow(
                label = "Long break after",
                value = draft.longBreakEvery,
                range = 2..12,
                unit = "sessions",
                onChange = { draft = draft.copy(longBreakEvery = it) },
            )
        }

        Spacer(Modifier.height(12.dp))
        OrbitCard {
            SectionLabel("Automation")
            Spacer(Modifier.height(4.dp))
            ToggleRow(
                title = "Auto-start breaks",
                subtitle = "Begin the break immediately when focus completes.",
                checked = draft.autoStartBreaks,
                onChange = { draft = draft.copy(autoStartBreaks = it) },
            )
            DividerLine()
            ToggleRow(
                title = "Auto-start next focus",
                subtitle = "Begin the next focus stage after a break.",
                checked = draft.autoStartFocus,
                onChange = { draft = draft.copy(autoStartFocus = it) },
            )
            DividerLine()
            ToggleRow(
                title = "Completion haptics",
                subtitle = "One gentle vibration at the end of a stage.",
                checked = draft.haptics,
                onChange = { draft = draft.copy(haptics = it) },
            )
        }

        Spacer(Modifier.height(16.dp))
        PrimaryActionButton(
            text = "Save routine",
            icon = Icons.Rounded.Check,
            onClick = { onSave(draft) },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Changes apply to the next stage. A running session keeps its current duration.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.faint,
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onChange: (Int) -> Unit,
) {
    val colors = LocalOrbitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceElevated.copy(alpha = 0.65f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onChange((value - 1).coerceIn(range)) }) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(17.dp))
            }
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.labelLarge,
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onChange((value + 1).coerceIn(range)) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase $label", modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(
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
