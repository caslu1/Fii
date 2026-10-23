package br.com.cotista.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.cotista.ui.theme.C

/**
 * Gráfico de barras com toque para selecionar.
 * [header] recebe o índice selecionado e desenha o valor acima do gráfico.
 */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    highlightLast: Boolean = true,
    estimatedFlags: List<Boolean>? = null,
    header: @Composable (Int) -> Unit = {},
) {
    if (values.isEmpty()) return
    var selected by remember(values.size) { mutableIntStateOf(values.lastIndex) }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(values) { anim.snapTo(0f); anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(0.0001)

    Column(modifier.fillMaxWidth()) {
        header(selected.coerceIn(0, values.lastIndex))
        Spacer(Modifier.height(14.dp))
        Canvas(
            Modifier.fillMaxWidth().height(height).pointerInput(values.size) {
                detectTapGestures { off ->
                    val slot = size.width / values.size
                    selected = (off.x / slot).toInt().coerceIn(0, values.lastIndex)
                }
            },
        ) {
            val n = values.size
            val slot = size.width / n
            val barW = slot * 0.56f
            // linhas guia
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
            for (i in 1..3) {
                val y = size.height * (1 - i / 3.5f)
                drawLine(C.Stroke, Offset(0f, y), Offset(size.width, y), 1.5f, pathEffect = dash)
            }
            values.forEachIndexed { i, v ->
                val h = (v / max).toFloat() * (size.height - 6f) * anim.value
                val x = i * slot + (slot - barW) / 2
                val isSel = i == selected
                val est = estimatedFlags?.getOrNull(i) == true
                val color = when {
                    isSel -> C.Mint
                    highlightLast && i == n - 1 -> C.Mint.copy(alpha = 0.55f)
                    else -> C.Mint.copy(alpha = 0.28f)
                }
                // trilho
                drawRoundRect(C.Surface2, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2.6f))
                if (h > 0) {
                    if (est) {
                        drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 2.6f),
                            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                    } else {
                        drawRoundRect(
                            Brush.verticalGradient(listOf(color, color.copy(alpha = color.alpha * 0.55f)), size.height - h, size.height),
                            Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 2.6f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(
                    l, Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) C.Text else C.Text3,
                )
            }
        }
    }
}

@Composable
fun Donut(
    slices: List<Pair<Color, Double>>,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    thickness: Dp = 16.dp,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val total = slices.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
    val anim = remember { Animatable(0f) }
    LaunchedEffect(slices.size) { anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(C.Surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            var start = -90f
            val gap = if (slices.size > 1) 3f else 0f
            slices.forEach { (color, v) ->
                val sweep = (v / total * 360f).toFloat() * anim.value
                if (sweep > gap) {
                    drawArc(color, start + gap / 2, sweep - gap, false, Offset(inset, inset), arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt))
                }
                start += sweep
            }
        }
        center()
    }
}

/** Barra de progresso horizontal segmentada (alocação). */
@Composable
fun SegmentBar(slices: List<Pair<Color, Double>>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        var x = 0f
        slices.forEach { (c, v) ->
            val w = (v / total).toFloat() * size.width
            drawRoundRect(c, Offset(x, 0f), Size((w - 3f).coerceAtLeast(1f), size.height), CornerRadius(size.height / 2))
            x += w
        }
    }
}
