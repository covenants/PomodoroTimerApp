package com.covenants.pomodorotimer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.ui.AppUiState
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.PrimaryActionButton
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.StatusPill
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors

@Composable
fun ConstellationsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSeeHistory: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    val lit = if (state.allTimeSessions == 0) 0 else (state.allTimeSessions % 8).coerceAtLeast(1)
    val icons = listOf(
        Icons.Rounded.Star,
        Icons.Rounded.AutoAwesome,
        Icons.Rounded.Flare,
        Icons.Rounded.Stars,
        Icons.Rounded.Brightness7,
        Icons.Rounded.Star,
        Icons.Rounded.Flare,
        Icons.Rounded.AutoAwesome,
    )

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
                text = "MILESTONES",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
                modifier = Modifier.weight(1f),
            )
            IconCircleButton(
                icon = Icons.Rounded.AutoAwesome,
                contentDescription = "Milestones",
                onClick = onBack,
            )
        }
        Spacer(Modifier.height(16.dp))
        ScreenHeader(eyebrow = "Gentle progress", title = "Your orbit, your pace.")
        Spacer(Modifier.height(16.dp))

        MoonHero(completedSessions = state.allTimeSessions)
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Constellations")
            Spacer(Modifier.weight(1f))
            StatusPill("${state.allTimeSessions} ✦", accent = state.allTimeSessions > 0)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Little moments add up.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(13.dp))

        OrbitCard {
            icons.chunked(4).forEachIndexed { rowIndex, rowIcons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    rowIcons.forEachIndexed { columnIndex, icon ->
                        val index = rowIndex * 4 + columnIndex
                        val isLit = index < lit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.25f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (isLit) colors.accent.copy(alpha = 0.14f)
                                    else colors.surfaceElevated.copy(alpha = 0.55f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = if (isLit) "Earned constellation" else "Future constellation",
                                tint = if (isLit) colors.accent else colors.faint.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                if (rowIndex == 0) Spacer(Modifier.height(9.dp))
            }
            Spacer(Modifier.height(15.dp))
            Text(
                text = "Every finished focus session brightens your sky. There are no penalties for taking a break.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            Spacer(Modifier.height(13.dp))
            PrimaryActionButton(
                text = "See session history",
                onClick = onSeeHistory,
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "This visual is decorative. It never gates timing, history or accessibility features.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MoonHero(completedSessions: Int) {
    val colors = LocalOrbitColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.surfaceElevated,
                        colors.primary.copy(alpha = 0.22f),
                        colors.surface,
                    )
                )
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.72f, size.height * 0.30f),
                    radius = size.width * 0.46f,
                ),
                radius = size.width * 0.46f,
                center = Offset(size.width * 0.72f, size.height * 0.30f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, colors.primary.copy(alpha = 0.92f)),
                    center = Offset(size.width * 0.66f, size.height * 0.28f),
                    radius = size.width * 0.19f,
                ),
                radius = size.width * 0.19f,
                center = Offset(size.width * 0.72f, size.height * 0.30f),
            )
            val stars = listOf(
                0.08f to 0.22f, 0.18f to 0.64f, 0.30f to 0.34f, 0.44f to 0.72f,
                0.55f to 0.16f, 0.62f to 0.58f, 0.84f to 0.72f, 0.92f to 0.24f,
                0.38f to 0.12f, 0.74f to 0.86f,
            )
            stars.forEachIndexed { index, (x, y) ->
                drawCircle(
                    color = Color.White.copy(alpha = if (index % 2 == 0) 0.85f else 0.45f),
                    radius = if (index % 3 == 0) 2.4f else 1.4f,
                    center = Offset(size.width * x, size.height * y),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            SectionLabel("Your sky")
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (completedSessions == 0) "Your first star is waiting." else "$completedSessions sessions of calm.",
                style = MaterialTheme.typography.titleLarge,
                color = colors.text,
            )
        }
    }
}
