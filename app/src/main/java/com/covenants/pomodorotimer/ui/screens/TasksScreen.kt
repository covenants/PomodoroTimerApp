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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.FocusTask
import com.covenants.pomodorotimer.ui.AppUiState
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.PrimaryActionButton
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SecondaryActionButton
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.StatusPill
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors

@Composable
fun TasksScreen(
    state: AppUiState,
    tasks: List<FocusTask>,
    onToggleTask: (FocusTask) -> Unit,
    onDeleteTask: (FocusTask) -> Unit,
    onAddTask: (String, String?, Int) -> Unit,
    onStartFocus: (FocusTask) -> Unit,
    onOpenActive: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    var showAddDialog by remember { mutableStateOf(false) }
    val activeTaskId = state.active?.taskId
    val currentTask = tasks.firstOrNull { it.id == activeTaskId }
    val nextTask = tasks.firstOrNull { !it.done }
    val openTasks = tasks.filter { !it.done }
    val doneTasks = tasks.filter { it.done }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            eyebrow = "Today's priorities",
            title = "One thing at a time.",
            trailing = {
                IconCircleButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Add task",
                    onClick = { showAddDialog = true },
                    filled = true,
                )
            },
        )
        Spacer(Modifier.height(20.dp))

        val focusTask = currentTask ?: nextTask
        OrbitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(if (currentTask != null) "Current focus" else "Up next")
                Spacer(Modifier.weight(1f))
                if (focusTask?.tag != null) StatusPill(focusTask.tag)
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = focusTask?.title ?: "Choose one thing to focus on.",
                style = MaterialTheme.typography.titleLarge,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            val completedInCycle = state.active?.completedWorkCount ?: 0
            val every = state.routine.longBreakEverySafe
            Text(
                text = if (state.active?.stage == FocusStage.WORK) {
                    "${(completedInCycle % every) + 1} of $every focus sessions · this cycle"
                } else {
                    "Each session counts only when it completes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(12.dp))
            val fraction = ((completedInCycle % every) + if (state.active?.stage == FocusStage.WORK) 1 else 0)
                .toFloat() / every.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.border.copy(alpha = 0.7f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.accent),
                )
            }
            Spacer(Modifier.height(13.dp))
            if (state.active != null) {
                SecondaryActionButton(
                    text = "Open session",
                    icon = Icons.Rounded.SelfImprovement,
                    onClick = onOpenActive,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PrimaryActionButton(
                    text = if (focusTask != null) "Focus on task" else "Start a focus session",
                    icon = Icons.Rounded.SelfImprovement,
                    onClick = { if (focusTask != null) onStartFocus(focusTask) else showAddDialog = true },
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("To-do · ${openTasks.size} open")
            Spacer(Modifier.weight(1f))
            Text(
                text = "${doneTasks.size} done",
                style = MaterialTheme.typography.bodySmall,
                color = colors.faint,
            )
        }
        Spacer(Modifier.height(9.dp))

        if (tasks.isEmpty()) {
            OrbitCard {
                Text("No tasks yet", style = MaterialTheme.typography.titleLarge, color = colors.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tasks are optional. A title is enough; tags and session estimates stay local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }

        openTasks.forEach { task ->
            TaskRow(
                task = task,
                onToggle = { onToggleTask(task) },
                onDelete = { onDeleteTask(task) },
            )
            Spacer(Modifier.height(8.dp))
        }

        if (doneTasks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionLabel("Done")
            Spacer(Modifier.height(8.dp))
            doneTasks.forEach { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggleTask(task) },
                    onDelete = { onDeleteTask(task) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        SecondaryActionButton(
            text = "Add a task",
            icon = Icons.Rounded.Add,
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, tag, estimate ->
                onAddTask(title, tag, estimate)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun TaskRow(
    task: FocusTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.82f))
            .padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (task.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (task.done) "Mark ${task.title} not done" else "Mark ${task.title} done",
                tint = if (task.done) colors.accent else colors.faint,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.done) colors.muted else colors.text,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.tag != null) {
                    Text(
                        text = task.tag,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.faint,
                    )
                    Text(" · ", style = MaterialTheme.typography.bodySmall, color = colors.faint)
                }
                Text(
                    text = "${task.estimateSessions} × ${task.completedSessions} done",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.faint,
                )
            }
        }
        Text(
            text = "${task.estimateSessions} × 25m",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete ${task.title}",
                tint = colors.faint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Int) -> Unit,
) {
    val colors = LocalOrbitColors.current
    var title by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var estimate by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Add a task", color = colors.text) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("What needs your attention?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it.take(24) },
                    label = { Text("Tag (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Estimated sessions", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { estimate = (estimate - 1).coerceAtLeast(1) }) { Text("−") }
                    Text(
                        text = estimate.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.text,
                    )
                    TextButton(onClick = { estimate = (estimate + 1).coerceAtMost(8) }) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), tag.trim().takeIf { it.isNotBlank() }, estimate) },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
