package com.andy.alakh.presentation.breathing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.theme.AlakhAccent
import com.andy.alakh.shared.model.BreathCategory
import com.andy.alakh.shared.model.BreathPatternType
import com.andy.alakh.shared.model.BreathSafetyLevel
import com.andy.alakh.shared.model.BreathingTechnique
import com.andy.alakh.shared.rules.BreathingCatalog

/** Generous top/bottom padding so the round screen never clips the first/last row. */
internal val BreathingPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 30.dp, bottom = 48.dp)

private val Muted = Color(0xFF9AA3A0)
internal val CautionColor = Color(0xFFE0A030)
internal val AdvancedColor = Color(0xFFE0573E)

/** Color used for a safety tier (font color only — never a component background, per the theme rule). */
internal fun safetyColor(level: BreathSafetyLevel): Color = when (level) {
    BreathSafetyLevel.GENERAL -> AlakhAccent
    BreathSafetyLevel.CAUTION -> CautionColor
    BreathSafetyLevel.ADVANCED -> AdvancedColor
}

internal fun safetyLabel(level: BreathSafetyLevel): String = when (level) {
    BreathSafetyLevel.GENERAL -> "Safe"
    BreathSafetyLevel.CAUTION -> "Caution"
    BreathSafetyLevel.ADVANCED -> "Advanced"
}

private fun fmt(x: Double): String = if (x == x.toLong().toDouble()) x.toLong().toString() else x.toString()

/** Compact one-line description of a technique's rhythm, for list/detail subtitles. */
internal fun patternLabel(t: BreathingTechnique): String = when (t.patternType) {
    BreathPatternType.ROUNDS -> "${t.defaultRounds.coerceAtLeast(1)} rounds + holds"
    BreathPatternType.FREEFORM -> if (t.cycleSec <= 0.0) "Free pace" else "Rapid breaths"
    BreathPatternType.PACED ->
        if (t.holdSec == 0.0 && t.holdAfterExhaleSec == 0.0) {
            "${fmt(t.inhaleSec)}s in · ${fmt(t.exhaleSec)}s out"
        } else {
            val base = "${fmt(t.inhaleSec)}-${fmt(t.holdSec)}-${fmt(t.exhaleSec)}"
            if (t.holdAfterExhaleSec > 0.0) "$base-${fmt(t.holdAfterExhaleSec)}" else base
        }
}

/**
 * First breathing page: the segments (categories). Tapping one opens its [BreathingCategoryScreen].
 */
@Composable
fun BreathingCategoriesScreen(onSelectCategory: (BreathCategory) -> Unit) {
    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = BreathingPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "title", contentType = "title") {
                Text(
                    "BREATHING",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlakhAccent,
                )
            }
            items(BreathingCatalog.categories(), key = { it.name }, contentType = { "category" }) { category ->
                CategoryCard(category, BreathingCatalog.byCategory(category).size) { onSelectCategory(category) }
            }
            item(key = "footer", contentType = "footer") {
                Text(
                    "Not medical advice. Stop if you feel dizzy.",
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

/**
 * One segment's techniques, grouped Safe → Caution → Advanced. Tapping a technique opens its detail.
 */
@Composable
fun BreathingCategoryScreen(category: BreathCategory, onSelectTechnique: (BreathingTechnique) -> Unit) {
    val techniques = BreathingCatalog.byCategoryRanked(category)
    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = BreathingPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "title", contentType = "title") {
                Text(
                    category.displayName,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlakhAccent,
                )
            }
            BreathSafetyLevel.entries.forEach { tier ->
                val inTier = techniques.filter { it.safetyLevel == tier }
                if (inTier.isNotEmpty()) {
                    item(key = "h_${tier.name}", contentType = "tier") {
                        Text(
                            safetyLabel(tier).uppercase(),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = safetyColor(tier),
                        )
                    }
                    items(inTier, key = { it.id }, contentType = { "technique" }) { technique ->
                        TechniqueRow(technique) { onSelectTechnique(technique) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: BreathCategory, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(category.displayName, style = MaterialTheme.typography.titleSmall, color = AlakhAccent, modifier = Modifier.weight(1f))
        Text("$count", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text("›", color = AlakhAccent, fontSize = 18.sp)
    }
}

@Composable
private fun TechniqueRow(technique: BreathingTechnique, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(technique.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(patternLabel(technique), style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
            Text(
                safetyLabel(technique.safetyLevel),
                color = safetyColor(technique.safetyLevel),
                fontSize = 11.sp,
            )
        }
    }
}
