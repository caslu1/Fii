package br.com.cotista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

val CardShape = RoundedCornerShape(22.dp)

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier
        .fillMaxWidth()
        .clip(CardShape)
    m = if (brush != null) m.background(brush) else m.background(C.Surface)
    m = m.border(1.dp, C.Stroke, CardShape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = C.Text, modifier = Modifier.weight(1f))
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
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ticker.take(4), color = color, fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.27f).sp, letterSpacing = 0.sp, maxLines = 1,
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
