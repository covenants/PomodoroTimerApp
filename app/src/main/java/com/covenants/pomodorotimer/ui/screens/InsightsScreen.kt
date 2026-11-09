package com.covenants.pomodorotimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.core.domain.TimeFormat
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.SessionHistory
import com.covenants.pomodorotimer.core.model.SessionOutcome
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.ui.AppUiState
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.StatusPill
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private enum class InsightRange(val label: String) { WEEK("Week"), MONTH("Month"), YEAR("Year") }

@Composable
fun InsightsScreen(state: AppUiState) {
    val colors = LocalOrbitColors.current
    var range by rememberSaveable { mutableStateOf(InsightRange.WEEK) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(state.nowWallMs) {
        Instant.ofEpochMilli(state.nowWallMs).atZone(zone).toLocalDate()
    }
    val completed = remember(state.history) {
        state.history.filter {
            it.mode == TimerMode.FOCUS &&
                it.stage == FocusStage.WORK &&
                it.outcome == SessionOutcome.COMPLETED
        }
    }

    val rangeStart: LocalDate = when (range) {
        InsightRange.WEEK -> today.minusDays(6)
        InsightRange.MONTH -> today.minusDays(27)
        InsightRange.YEAR -> today.minusMonths(11).withDayOfMonth(1)
    }
    val rangeEnd = today.plusDays(1)
    val sessions = completed.filter { it.localDate(zone) in rangeStart..today }
    val totalMs = sessions.sumOf { it.activeDurationMs }

    val previousStart: LocalDate = when (range) {
        InsightRange.WEEK -> today.minusDays(13)
        InsightRange.MONTH -> today.minusDays(55)
        InsightRange.YEAR -> today.minusMonths(23).withDayOfMonth(1)
    }
    val previousEnd: LocalDate = when (range) {
        InsightRange.WEEK -> today.minusDays(6)
        InsightRange.MONTH -> today.minusDays(27)
        InsightRange.YEAR -> today.minusMonths(11).withDayOfMonth(1)
    }
    val previousMs = completed
        .filter { it.localDate(zone) in previousStart..<previousEnd }
        .sumOf { it.activeDurationMs }
    val delta = if (previousMs > 0) {
        (((totalMs - previousMs).toDouble() / previousMs.toDouble()) * 100).roundToInt()
    } else {
        null
    }

    val (bars, labels) = when (range) {
        InsightRange.WEEK -> {
            val values = (6 downTo 0).map { offset ->
                minutesOn(completed, today.minusDays(offset.toLong()), zone)
            }
            values to (6 downTo 0).map {
                today.minusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            }
        }
        InsightRange.MONTH -> {
            val values = (3 downTo 0).map { week ->
                val start = today.minusDays((week * 7L) + 6)
                (0..6).sumOf { day -> minutesOn(completed, start.plusDays(day.toLong()), zone) }
            }
            values to listOf("W1", "W2", "W3", "W4")
        }
        InsightRange.YEAR -> {
            val months = (11 downTo 0).map { today.minusMonths(it.toLong()).withDayOfMonth(1) }
            val values = months.map { monthStart ->
                val next = monthStart.plusMonths(1)
                (completed.filter {
                    val d = it.localDate(zone)
                    !d.isBefore(monthStart) && d.isBefore(next)
                }.sumOf { it.activeDurationMs } / 60_000L).toInt()
            }
            values to months.map { it.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        ScreenHeader(eyebrow = "Your focus story", title = "Progress looks good.")
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface.copy(alpha = 0.65f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            InsightRange.entries.forEach { option ->
                val active = option == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) colors.surfaceElevated else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { range = option },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) colors.text else colors.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        OrbitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    when (range) {
                        InsightRange.WEEK -> "Focused this week"
                        InsightRange.MONTH -> "Focused · past 4 weeks"
                        InsightRange.YEAR -> "Focused · past 12 months"
                    }
                )
                Spacer(Modifier.weight(1f))
                if (delta != null) {
                    StatusPill(
                        text = "${if (delta >= 0) "↗" else "↘"} ${if (delta >= 0) "+" else ""}$delta%",
                        accent = delta >= 0,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = TimeFormat.durationLabel(totalMs),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.text,
            )
            Spacer(Modifier.height(16.dp))
            FocusBarChart(values = bars, labels = labels)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (delta == null) {
                    "Complete another period to see a comparison."
                } else {
                    "Compared with the previous ${range.label.lowercase()}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.faint,
            )
        }

        Spacer(Modifier.height(12.dp))
        OrbitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Daily consistency")
                Spacer(Modifier.weight(1f))
                StatusPill("✦ ${state.streakDays} day streak", accent = state.streakDays > 0)
            }
            Spacer(Modifier.height(4.dp))
            Text("Past 4 weeks", style = MaterialTheme.typography.bodySmall, color = colors.faint)
            Spacer(Modifier.height(12.dp))
            Heatmap(state.heatmapDays)
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(
                    icon = Icons.Rounded.CheckCircle,
                    value = "${sessions.size}",
                    label = "Completed",
                    modifier = Modifier.weight(1f),
                )
                MiniStat(
                    icon = Icons.Rounded.Schedule,
                    value = state.mostFocusedWindow,
                    label = "Most focused",
                    modifier = Modifier.weight(1.4f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OrbitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(
                        text = "${state.allTimeSessions} completed focus ${TimeFormat.plural(state.allTimeSessions, "session")}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text,
                    )
                    Text(
                        text = "All analytics are calculated on this device. No account, no upload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent.copy(alpha = 0.08f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "Breaks are part of the work. Interrupted sessions are never punished.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun FocusBarChart(values: List<Int>, labels: List<String>) {
    val colors = LocalOrbitColors.current
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEach { value ->
                val fraction = value.toFloat() / max.toFloat()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((6f + fraction * 108f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(
                                if (value == 0) {
                                    Brush.verticalGradient(listOf(colors.border, colors.border.copy(alpha = 0.5f)))
                                } else {
                                    Brush.verticalGradient(listOf(colors.accent, colors.primary.copy(alpha = 0.7f)))
                                }
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.faint,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Heatmap(values: List<Int>) {
    val colors = LocalOrbitColors.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        values.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                week.forEach { minutes ->
                    val alpha = when {
                        minutes <= 0 -> 0.07f
                        minutes < 15 -> 0.22f
                        minutes < 30 -> 0.42f
                        minutes < 60 -> 0.66f
                        else -> 0.95f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.55f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (minutes <= 0) colors.border.copy(alpha = alpha)
                                else colors.accent.copy(alpha = alpha)
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOrbitColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.faint)
        }
    }
}

private fun SessionHistory.localDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(endedWallMs).atZone(zone).toLocalDate()

private fun minutesOn(history: List<SessionHistory>, date: LocalDate, zone: ZoneId): Int =
    (history.filter { it.localDate(zone) == date }.sumOf { it.activeDurationMs } / 60_000L).toInt()
