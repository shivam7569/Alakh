package com.andy.alakh.presentation.breathing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.theme.AlakhAccent
import com.andy.alakh.shared.model.BreathingTechnique
import com.andy.alakh.shared.rules.BreathingCatalog

private val Muted = Color(0xFF9AA3A0)

/**
 * Technique detail + the safety gate. CAUTION/ADVANCED techniques show their contraindications and
 * (for breath-hold/hyperventilation) the retention warning, and require a one-time "I understand"
 * before [onStart] is allowed. GENERAL techniques start straight away.
 */
@Composable
fun BreathingDetailScreen(technique: BreathingTechnique, onStart: () -> Unit) {
    val gated = technique.requiresAcknowledgement && !BreathingNav.isAcknowledged(technique.id)
    val showRetention = BreathingCatalog.showsRetentionWarning(technique)

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                technique.name,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = AlakhAccent,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${technique.category.displayName} · ${technique.difficulty.name.lowercase()} · ${safetyLabel(technique.safetyLevel)}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = safetyColor(technique.safetyLevel),
            )
            Text(patternLabel(technique), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Muted)

            Spacer(Modifier.height(2.dp))
            Text(technique.summary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)

            if (technique.benefits.isNotEmpty()) {
                SectionLabel("BENEFITS")
                technique.benefits.forEach { Bullet(it) }
            }

            if (technique.nasalNote.isNotBlank()) {
                SectionLabel("HOW")
                Text(technique.nasalNote, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Muted)
            }

            // --- safety ---
            SectionLabel("SAFETY")
            Text(
                technique.safetyNote,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = safetyColor(technique.safetyLevel),
            )
            if (gated) {
                if (showRetention) WarningCard(BreathingCatalog.RETENTION_WARNING)
                if (technique.contraindications.isNotEmpty()) {
                    SectionLabel("AVOID IF")
                    technique.contraindications.forEach { Bullet(it) }
                }
            }

            Spacer(Modifier.height(6.dp))
            if (gated) {
                Button(
                    onClick = { BreathingNav.acknowledge(technique.id); onStart() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("I understand — Start") }
            } else {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 1.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = Muted,
    )
}

@Composable
private fun Bullet(text: String) {
    Text(
        "• $text",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun WarningCard(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1FE0573E))
            .padding(10.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFF0B8AE),
    )
}
