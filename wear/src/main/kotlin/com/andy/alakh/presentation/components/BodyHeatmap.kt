package com.andy.alakh.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import com.andy.alakh.shared.model.MuscleGroup

private val Accent = Color(0xFF34C796)        // primary target
private val AccentDim = Color(0x6634C796)     // secondary target
private val Inactive = Color(0xFF333A38)      // untargeted muscle
private val Frame = Color(0xFF555F5B)         // head / neutral structure

/**
 * A compact front + back body map that lights up the muscles an exercise targets
 * (bright = primary, dim = secondary). Drawn once with simple shapes, so it's cheap on the watch;
 * a one-shot fade-in is the only animation.
 */
@Composable
fun BodyHeatmap(
    primary: Set<MuscleGroup>,
    secondary: Set<MuscleGroup>,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, animationSpec = tween(450), label = "heatmap")
    LaunchedEffect(Unit) { shown = true }

    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val gap = size.width * 0.10f
        val figW = (size.width - gap) / 2f
        drawFigure(Offset(0f, 0f), Size(figW, size.height), isFront = true, primary, secondary)
        drawFigure(Offset(figW + gap, 0f), Size(figW, size.height), isFront = false, primary, secondary)
    }
}

private fun DrawScope.drawFigure(
    origin: Offset,
    box: Size,
    isFront: Boolean,
    primary: Set<MuscleGroup>,
    secondary: Set<MuscleGroup>,
) {
    fun colorFor(group: MuscleGroup): Color = when (group) {
        in primary -> Accent
        in secondary -> AccentDim
        else -> Inactive
    }

    fun part(xf: Float, yf: Float, wf: Float, hf: Float, color: Color) {
        drawRoundRect(
            color = color,
            topLeft = Offset(origin.x + box.width * xf, origin.y + box.height * yf),
            size = Size(box.width * wf, box.height * hf),
            cornerRadius = CornerRadius(box.width * 0.05f),
        )
    }

    // head (neutral)
    drawCircle(Frame, radius = box.width * 0.10f, center = Offset(origin.x + box.width * 0.5f, origin.y + box.height * 0.07f))

    if (isFront) {
        part(0.15f, 0.15f, 0.20f, 0.09f, colorFor(MuscleGroup.SHOULDERS))
        part(0.65f, 0.15f, 0.20f, 0.09f, colorFor(MuscleGroup.SHOULDERS))
        part(0.30f, 0.17f, 0.40f, 0.16f, colorFor(MuscleGroup.CHEST))
        part(0.09f, 0.19f, 0.14f, 0.18f, colorFor(MuscleGroup.BICEPS))
        part(0.77f, 0.19f, 0.14f, 0.18f, colorFor(MuscleGroup.BICEPS))
        part(0.09f, 0.39f, 0.13f, 0.16f, colorFor(MuscleGroup.FOREARMS))
        part(0.78f, 0.39f, 0.13f, 0.16f, colorFor(MuscleGroup.FOREARMS))
        part(0.34f, 0.34f, 0.32f, 0.17f, colorFor(MuscleGroup.CORE))
        part(0.34f, 0.54f, 0.14f, 0.22f, colorFor(MuscleGroup.QUADS))
        part(0.52f, 0.54f, 0.14f, 0.22f, colorFor(MuscleGroup.QUADS))
        part(0.35f, 0.78f, 0.12f, 0.17f, colorFor(MuscleGroup.SHIN))
        part(0.53f, 0.78f, 0.12f, 0.17f, colorFor(MuscleGroup.SHIN))
    } else {
        part(0.15f, 0.15f, 0.20f, 0.09f, colorFor(MuscleGroup.SHOULDERS))
        part(0.65f, 0.15f, 0.20f, 0.09f, colorFor(MuscleGroup.SHOULDERS))
        part(0.34f, 0.15f, 0.32f, 0.09f, colorFor(MuscleGroup.TRAPS))
        part(0.30f, 0.24f, 0.40f, 0.15f, colorFor(MuscleGroup.BACK))
        part(0.09f, 0.19f, 0.14f, 0.18f, colorFor(MuscleGroup.TRICEPS))
        part(0.77f, 0.19f, 0.14f, 0.18f, colorFor(MuscleGroup.TRICEPS))
        part(0.09f, 0.39f, 0.13f, 0.16f, colorFor(MuscleGroup.FOREARMS))
        part(0.78f, 0.39f, 0.13f, 0.16f, colorFor(MuscleGroup.FOREARMS))
        part(0.34f, 0.40f, 0.32f, 0.09f, colorFor(MuscleGroup.LOWER_BACK))
        part(0.34f, 0.50f, 0.32f, 0.10f, colorFor(MuscleGroup.GLUTES))
        part(0.34f, 0.61f, 0.14f, 0.18f, colorFor(MuscleGroup.HAMSTRINGS))
        part(0.52f, 0.61f, 0.14f, 0.18f, colorFor(MuscleGroup.HAMSTRINGS))
        part(0.35f, 0.80f, 0.12f, 0.16f, colorFor(MuscleGroup.CALVES))
        part(0.53f, 0.80f, 0.12f, 0.16f, colorFor(MuscleGroup.CALVES))
    }
}
