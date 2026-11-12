package br.com.cotista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cotista.ui.theme.C
import br.com.cotista.util.pct

val CardShape = RoundedCornerShape(26.dp)

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    brush: Brush? = null,
    glow: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier
        .fillMaxWidth()
        .clip(CardShape)
    m = if (brush != null) m.background(brush) else m.background(C.Surface)
    if (glow != null) {
        m = m.drawBehind {
            drawCircle(
                Brush.radialGradient(listOf(glow.copy(alpha = 0.32f), Color.Transparent), center = Offset(size.width * 0.92f, 0f), radius = size.width * 0.75f),
                radius = size.width * 0.75f, center = Offset(size.width * 0.92f, 0f),
            )
        }
    }
    m = m.border(1.dp, C.borderBrush, CardShape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = C.Text, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing, style = MaterialTheme.typography.labelLarge, color = C.Mint,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = onTrailing != null) { onTrailing?.invoke() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun TickerBadge(ticker: String, size: Dp = 44.dp) {
    val color = C.forKey(ticker.take(4))
    val shape = RoundedCornerShape(size * 0.34f)
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.34f), color.copy(alpha = 0.10f))))
            .border(1.dp, Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.08f))), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ticker.take(4), color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.255f).sp, letterSpacing = (-0.2).sp, maxLines = 1,
        )
    }
}

@Composable
fun ChangePill(change: Double?, modifier: Modifier = Modifier) {
    if (change == null) return
    val color = C.signed(change)
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (change != 0.0) {
            Icon(
                if (change > 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                null, tint = color, modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
        }
        Text(change.pct(2, signed = true), color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Tag(text: String, color: Color = C.Text2, modifier: Modifier = Modifier) {
    Text(
        text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = C.Text, hint: String? = null) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1)
    }
}

@Composable
fun StatGrid(items: List<Triple<String, String, Color>>, columns: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (l, v, c) -> Stat(l, v, Modifier.weight(1f), c) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(C.MintDeep), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.Mint, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = C.Text)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = C.Text2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(18.dp)); action() }
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(C.Stroke))
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

fun masked(hide: Boolean, s: String) = if (hide) "R$ •••••" else s

// ------------------------------------------------------------------ componentes do visual 2

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = C.Text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = C.Text3)
        }
        action?.invoke()
    }
}

@Composable
fun PillButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(50)).background(C.mintBrush).clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = C.Bg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = C.Bg)
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface)
            .border(1.dp, C.Stroke, RoundedCornerShape(14.dp)).padding(4.dp),
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) C.Surface3 else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(o, style = MaterialTheme.typography.labelLarge, color = if (sel) C.Text else C.Text3, maxLines = 1)
            }
        }
    }
}

/** Abas sublinhadas (detalhe do fundo). */
@Composable
fun UnderlineTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, o ->
                val sel = i == selected
                Column(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSelect(i) }.padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        o, style = MaterialTheme.typography.labelLarge, color = if (sel) C.Text else C.Text3,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(Modifier.height(2.5.dp).width(if (sel) 28.dp else 0.dp).clip(RoundedCornerShape(2.dp)).background(C.Mint))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Stroke))
    }
}

/** Linha de lista: selo, título, subtítulo e dois valores à direita. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRow(
    ticker: String,
    title: String,
    subtitle: String,
    value: String,
    valueSub: String? = null,
    valueColor: Color = C.Text,
    valueSubColor: Color = C.Text2,
    tag: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickerBadge(ticker, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = C.Text, maxLines = 1)
                if (tag != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tag, style = MaterialTheme.typography.labelSmall, color = C.Mint, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(C.MintDeep).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, maxLines = 1)
            if (valueSub != null) Text(valueSub, style = MaterialTheme.typography.labelMedium, color = valueSubColor, maxLines = 1)
        }
        trailing?.invoke()
    }
}

/** Painel de lista com divisórias entre as linhas. */
@Composable
fun <T> ListPanel(items: List<T>, row: @Composable (T) -> Unit) {
    Panel(padding = 4.dp) {
        items.forEachIndexed { i, it ->
            if (i > 0) Divider(Modifier.padding(horizontal = 14.dp))
            row(it)
        }
    }
}

/** Bloco de número sobre fundo verde (herói). */
@Composable
fun GlassTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = C.Text, sub: String? = null, subColor: Color = C.Text2) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.Text2, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor, maxLines = 1)
    }
}

/** Cartão de número (grade 2×2). */
@Composable
fun MetricCard(label: String, value: String, sub: String?, modifier: Modifier = Modifier, valueColor: Color = C.Text, subColor: Color = C.Text3, brush: Brush? = null) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).let { if (brush != null) it.background(brush) else it.background(C.Surface) }
            .border(1.dp, C.borderBrush, RoundedCornerShape(18.dp)).padding(14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, maxLines = 1)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelMedium, color = subColor, maxLines = 1)
    }
}

@Composable
fun DateBox(weekday: String, day: Int, color: Color = C.Amber) {
    Column(
        Modifier.width(48.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(weekday.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
        Text("$day", style = MaterialTheme.typography.titleLarge, color = color)
    }
}
