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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueryStats
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.covenants.pomodorotimer.ui.components.IconCircleButton
import com.covenants.pomodorotimer.ui.components.OrbitCard
import com.covenants.pomodorotimer.ui.components.PrimaryActionButton
import com.covenants.pomodorotimer.ui.components.ScreenHeader
import com.covenants.pomodorotimer.ui.components.SectionLabel
import com.covenants.pomodorotimer.ui.components.StatusPill
import com.covenants.pomodorotimer.ui.theme.LocalOrbitColors

@Composable
fun PlusScreen(
    onBack: () -> Unit,
) {
    val colors = LocalOrbitColors.current
    var showBillingDialog by remember { mutableStateOf(false) }

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
                text = "OPTIONAL UPGRADE",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
                modifier = Modifier.weight(1f),
            )
            IconCircleButton(
                icon = Icons.Rounded.AutoAwesome,
                contentDescription = "Lodestar Plus",
                onClick = {},
            )
        }
        Spacer(Modifier.height(16.dp))
        ScreenHeader(eyebrow = "Lodestar Plus", title = "More ways to make focus your own.")
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.surfaceElevated,
                            colors.primary.copy(alpha = 0.28f),
                            colors.accent.copy(alpha = 0.14f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(62.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        OrbitCard {
            FeatureRow(Icons.Rounded.QueryStats, "Extended insights & export", "Longer ranges and richer CSV history.")
            Spacer(Modifier.height(12.dp))
            FeatureRow(Icons.Rounded.MusicNote, "Premium ambient soundscapes", "Optional sound packs for focus and breaks.")
            Spacer(Modifier.height(12.dp))
            FeatureRow(Icons.Rounded.Palette, "Custom art & widget themes", "More ways to make the timer feel like yours.")
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Annual · sample offer")
                Spacer(Modifier.weight(1f))
                StatusPill("Illustrative")
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$14.99",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.text,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "/yr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            PrimaryActionButton(
                text = "Continue",
                onClick = { showBillingDialog = true },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Core timers remain free forever. This sample price is a hypothesis, not a launched offer.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "No ads during focus. No required account. No dark patterns.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showBillingDialog) {
        AlertDialog(
            onDismissRequest = { showBillingDialog = false },
            containerColor = colors.surface,
            title = { Text("Billing is not connected") },
            text = {
                Text(
                    "This build does not ship a fake checkout. Google Play Billing must be configured " +
                        "before any subscription can be offered. Every timer, alert and local history feature remains free.",
                    color = colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { showBillingDialog = false }) { Text("Understood") }
            },
        )
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    val colors = LocalOrbitColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.text, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
    }
}
