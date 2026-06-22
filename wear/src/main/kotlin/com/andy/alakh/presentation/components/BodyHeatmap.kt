package com.andy.alakh.presentation.components

import android.content.Context
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import com.andy.alakh.shared.model.MuscleGroup
import org.json.JSONArray
import org.json.JSONObject

/*
 * Muscle path data adapted from react-native-body-highlighter (Hicham EL BSI), MIT License.
 * See docs/THIRD_PARTY_LICENSES.md. We map its muscle slugs onto our MuscleGroup axes at build time
 * (see the muscle_map.json asset) and render the SVG paths here, tinted by what an exercise targets.
 */

private val Primary = Color(0xFF34C796)    // bright: primary target
private val Secondary = Color(0xFF1E6E59)  // dim: secondary target
private val BaseMuscle = Color(0xFF3A413D) // untargeted muscle
private val Structure = Color(0xFF565F5B)  // head / hands / feet (neutral)

private class Region(val group: String, val path: Path)
private class BodyView(val regions: List<Region>, val bounds: Rect)

/** Parses muscle_map.json once and caches the Compose paths for the whole process. */
private object MuscleMap {
    @Volatile private var front: BodyView? = null
    @Volatile private var back: BodyView? = null

    fun front(context: Context): BodyView { ensure(context); return front!! }
    fun back(context: Context): BodyView { ensure(context); return back!! }

    private fun ensure(context: Context) {
        if (front != null) return
        synchronized(this) {
            if (front != null) return
            val text = context.applicationContext.assets
                .open("muscle_map.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            front = parseView(root.getJSONArray("front"))
            back = parseView(root.getJSONArray("back"))
        }
    }

    private fun parseView(arr: JSONArray): BodyView {
        val regions = ArrayList<Region>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val combined = Path()
            for (d in pathStrings(o.opt("d"))) {
                runCatching { combined.addPath(PathParser().parsePathString(d).toPath()) }
            }
            regions.add(Region(o.getString("g"), combined))
        }
        return BodyView(regions, unionBounds(regions))
    }

    private fun pathStrings(value: Any?): List<String> = when (value) {
        is JSONArray -> (0 until value.length()).map { value.getString(it) }
        is String -> listOf(value)
        else -> emptyList()
    }

    private fun unionBounds(regions: List<Region>): Rect {
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (reg in regions) {
            val bounds = reg.path.getBounds()
            if (bounds.isEmpty) continue
            l = minOf(l, bounds.left); t = minOf(t, bounds.top)
            r = maxOf(r, bounds.right); b = maxOf(b, bounds.bottom)
        }
        return if (r <= l) Rect(0f, 0f, 1f, 1f) else Rect(l, t, r, b)
    }
}

/**
 * A front + back anatomical muscle map that highlights the muscles an exercise targets
 * (bright = primary, dim = secondary). Paths are parsed once and cached; a one-shot fade-in
 * is the only animation, so it's cheap to draw on the watch.
 */
@Composable
fun BodyHeatmap(
    primary: Set<MuscleGroup>,
    secondary: Set<MuscleGroup>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val front = remember { MuscleMap.front(context) }
    val back = remember { MuscleMap.back(context) }
    val primaryNames = remember(primary) { primary.mapTo(HashSet()) { it.name } }
    val secondaryNames = remember(secondary) { secondary.mapTo(HashSet()) { it.name } }

    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, animationSpec = tween(450), label = "heatmap")
    LaunchedEffect(Unit) { shown = true }

    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val gap = size.width * 0.08f
        val halfW = (size.width - gap) / 2f
        drawView(front, Rect(0f, 0f, halfW, size.height), primaryNames, secondaryNames)
        drawView(back, Rect(halfW + gap, 0f, size.width, size.height), primaryNames, secondaryNames)
    }
}

private fun DrawScope.drawView(
    view: BodyView,
    box: Rect,
    primary: Set<String>,
    secondary: Set<String>,
) {
    val bw = view.bounds.width
    val bh = view.bounds.height
    if (bw <= 0f || bh <= 0f) return
    val s = minOf(box.width / bw, box.height / bh)
    val tx = box.left + (box.width - bw * s) / 2f - view.bounds.left * s
    val ty = box.top + (box.height - bh * s) / 2f - view.bounds.top * s

    withTransform({
        translate(tx, ty)
        scale(s, s, pivot = Offset.Zero)
    }) {
        for (reg in view.regions) {
            val color = when {
                reg.group in primary -> Primary
                reg.group in secondary -> Secondary
                reg.group == "NEUTRAL" -> Structure
                else -> BaseMuscle
            }
            drawPath(reg.path, color)
        }
    }
}
