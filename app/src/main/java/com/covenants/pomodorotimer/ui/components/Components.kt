package com.covenants.pomodorotimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalOrbitColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.background, colors.backgroundEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glow = Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.08f),
                radius = size.width * 0.85f,
            )
            drawRect(brush = glow)
            // Restrained, deterministic star field.
            val stars = listOf(
                0.08f to 0.10f, 0.20f to 0.05f, 0.33f to 0.14f, 0.48f to 0.06f,
                0.67f to 0.12f, 0.79f to 0.04f, 0.92f to 0.16f, 0.13f to 0.26f,
                0.88f to 0.30f, 0.72f to 0.23f, 0.42f to 0.20f, 0.57f to 0.28f,
            )
            stars.forEachIndexed { index, (x, y) ->
                drawCircle(
                    color = colors.star.copy(alpha = if (index % 3 == 0) 0.28f else 0.14f),
                    radius = if (index % 4 == 0) 1.9f else 1.1f,
                    center = Offset(size.width * x, size.height * y),
                )
            }
        }
        content()
    }
}

@Composable
fun OrbitRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 7.dp,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) tween(0) else tween(420),
        label = "orbitProgress",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2f + 3.dp.toPx()
            val diameter = minOf(size.width, size.height) - inset * 2f
            val radius = diameter / 2f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            drawCircle(
                color = colors.border.copy(alpha = if (colors.isDark) 0.75f else 0.9f),
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawCircle(
                color = colors.primary.copy(alpha = 0.045f),
                radius = radius - stroke,
            )

            if (animated > 0.001f) {
                val sweep = 360f * animated
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(colors.primary, colors.accent, colors.primary)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 2.4f, cap = StrokeCap.Round),
                    alpha = 0.12f,
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(colors.primary, colors.accent, colors.primary)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val angle = (-90f + sweep) * PI.toFloat() / 180f
                val dot = Offset(
                    x = center.x + radius * cos(angle),
                    y = center.y + radius * sin(angle),
                )
                drawCircle(color = colors.accent.copy(alpha = 0.22f), radius = stroke * 2.1f, center = dot)
                drawCircle(color = colors.accent, radius = stroke * 0.85f, center = dot)
            }
        }
        content()
    }
}

@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalOrbitColors.current
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .background(colors.surface.copy(alpha = if (colors.isDark) 0.88f else 1f))
        .border(1.dp, colors.border.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
    Column(modifier = base.padding(18.dp), content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LocalOrbitColors.current.muted,
        modifier = modifier,
    )
}

@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LocalOrbitColors.current.muted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = LocalOrbitColors.current.text,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun SegmentedModes(
    selected: TimerMode,
    onSelect: (TimerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOrbitColors.current
    val options = listOf(
        Triple(TimerMode.FOCUS, "Focus", Icons.Rounded.RadioButtonChecked),
        Triple(TimerMode.COUNTDOWN, "Timer", Icons.Rounded.Timer),
        Triple(TimerMode.STOPWATCH, "Stopwatch", Icons.Rounded.AvTimer),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface.copy(alpha = if (colors.isDark) 0.7f else 1f))
            .border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (mode, label, icon) ->
            val active = mode == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.primary else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .semantics { contentDescription = "$label mode${if (active) ", selected" else ""}" }
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) colors.onPrimary else colors.muted,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) colors.onPrimary else colors.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LocalOrbitColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceElevated,
            disabledContentColor = colors.faint,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalOrbitColors.current
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated.copy(alpha = 0.65f))
            .border(1.dp, colors.border.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOrbitColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) colors.surfaceElevated else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) colors.primary.copy(alpha = 0.75f) else colors.border.copy(alpha = 0.8f),
                shape = RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.text else colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val colors = LocalOrbitColors.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (filled) colors.primary else colors.surfaceElevated.copy(alpha = 0.7f))
            .border(1.dp, colors.border.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) colors.onPrimary else colors.text,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = LocalOrbitColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (accent) colors.accent.copy(alpha = 0.14f) else colors.surfaceElevated.copy(alpha = 0.7f))
            .border(
                1.dp,
                if (accent) colors.accent.copy(alpha = 0.35f) else colors.border.copy(alpha = 0.7f),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) colors.accent else colors.muted,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    val colors = LocalOrbitColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border.copy(alpha = 0.6f))
    )
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalOrbitColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
        }
        trailing()
    }
}

@Composable
fun BottomNavBar(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOrbitColors.current
    val items = listOf(
        Triple("home", "Timer", Icons.Rounded.Timer),
        Triple("tasks", "Tasks", Icons.Rounded.Checklist),
        Triple("insights", "Insights", Icons.Rounded.BarChart),
        Triple("you", "You", Icons.Rounded.Person),
    )
    Surface(
        color = colors.background.copy(alpha = 0.96f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            DividerLine()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                items.forEach { (route, label, icon) ->
                    val active = route == selected
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(route) }
                            .padding(horizontal = 18.dp, vertical = 3.dp)
                            .semantics { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (active) colors.accent else colors.faint,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) colors.accent else colors.faint,
                        )
                    }
                }
            }
        }
    }
}
