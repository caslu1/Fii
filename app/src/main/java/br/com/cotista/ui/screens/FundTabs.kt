package br.com.cotista.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Balance
import br.com.cotista.data.Fnet
import br.com.cotista.ui.components.Divider
import br.com.cotista.ui.components.Dot
import br.com.cotista.ui.components.LineChart
import br.com.cotista.ui.components.MetricCard
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.SegmentBar
import br.com.cotista.ui.theme.C
import br.com.cotista.util.PT_BR
import br.com.cotista.util.brl
import br.com.cotista.util.brlCompact
import br.com.cotista.util.monthShort
import br.com.cotista.util.monthYear
import br.com.cotista.util.pct
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ------------------------------------------------------------------ aba Balanço

fun LazyListScope.balanceItems(
    balances: List<Balance>,
    sel: Int,
    onSelect: (Int) -> Unit,
    reports: List<Fnet.DocRef>?,
    onOpenReports: () -> Unit,
) {
    if (balances.isEmpty()) {
        item {
            Text("Este fundo ainda não tem informe mensal na CVM.", style = MaterialTheme.typography.bodyMedium, color = C.Text3, modifier = Modifier.padding(vertical = 24.dp))
        }
        return
    }
    val idx = sel.coerceIn(0, balances.lastIndex)
    val b = balances[idx]
    val prev = balances.getOrNull(idx - 1)

    item {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Informe mensal CVM", style = MaterialTheme.typography.labelMedium, color = C.Text3, modifier = Modifier.weight(1f))
            MonthDropdown(balances, idx, onSelect)
        }
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Patrimônio", b.netWorth?.brlCompact() ?: "—", delta(b.netWorth, prev?.netWorth)?.first, Modifier.weight(1f),
                    subColor = delta(b.netWorth, prev?.netWorth)?.second ?: C.Text3, brush = C.heroBrush)
                MetricCard("VP por cota", b.vpCota?.brl() ?: "—", delta(b.vpCota, prev?.vpCota)?.first, Modifier.weight(1f),
                    subColor = delta(b.vpCota, prev?.vpCota)?.second ?: C.Text3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Rentab. efetiva", b.returnEffective?.pct(2, true) ?: "—", "cota + rendimentos", Modifier.weight(1f),
                    valueColor = C.signed(b.returnEffective))
                val lev = b.leverage
                val levColor = when { lev == null -> C.Text; lev < 0.10 -> C.Mint; lev < 0.30 -> C.Amber; else -> C.Coral }
                MetricCard(
                    "Alavancagem", lev?.pct(1) ?: "—",
                    "passivo ${b.liabilities.brlCompact()} · " + when { lev == null -> "—"; lev < 0.10 -> "baixa"; lev < 0.30 -> "moderada"; else -> "alta" },
                    Modifier.weight(1f), valueColor = levColor,
                )
            }
        }
    }

    item {
        SectionTitle("Onde o dinheiro está")
        Panel {
            val comp = b.composition
            val total = b.compositionTotal
            SegmentBar(comp.mapIndexed { i, s -> C.palette[i % C.palette.size] to s.value })
            comp.forEachIndexed { i, s -> ValueRow(s.label, s.value, total, C.palette[i % C.palette.size]) }
        }
    }

    item {
        Spacer(Modifier.height(10.dp))
        Panel {
            Row {
                SmallStat("Dividend yield", b.dy?.pct() ?: "—", Modifier.weight(1f), C.Mint)
                SmallStat("Rentab. patrimonial", b.returnPatrimonial?.pct(2, true) ?: "—", Modifier.weight(1f), C.signed(b.returnPatrimonial))
            }
            Spacer(Modifier.height(14.dp))
            Row {
                SmallStat("Taxa de adm. no mês", b.adminFee?.pct(3) ?: "—", Modifier.weight(1f))
                val h1 = b.holders
                val h0 = prev?.holders
                SmallStat(
                    "Cotistas", h1?.let { NumberFormat.getIntegerInstance(PT_BR).format(it) } ?: "—", Modifier.weight(1f),
                    sub = if (h1 != null && h0 != null) (if (h1 >= h0) "+" else "") + NumberFormat.getIntegerInstance(PT_BR).format(h1 - h0) + " no mês" else null,
                    subColor = if (h1 != null && h0 != null) C.signed((h1 - h0).toDouble()) else C.Text3,
                )
            }
        }
    }

    if (b.realEstate > 0) {
        item {
            SectionTitle("Imóveis")
            Panel {
                val parts = listOf(
                    "Prontos para renda" to b.realEstateIncome,
                    "Em construção" to b.realEstateConstruction,
                    "Para venda" to b.realEstateForSale,
                    "Terrenos" to b.land,
                ).filter { it.second > 0 }
                val other = b.realEstate - parts.sumOf { it.second }
                val all = if (other / b.realEstate > 0.005) parts + ("Outros direitos" to other) else parts
                val colors = listOf(C.Mint, C.Amber, C.Sky, C.Lilac, C.Text3)
                SegmentBar(all.mapIndexed { i, p -> colors[i % 5] to p.second })
                all.forEachIndexed { i, (label, v) -> ValueRow(label, v, b.realEstate, colors[i % 5]) }
                if (b.rentReceivable > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text("Aluguéis a receber: ${b.rentReceivable.brlCompact()}", style = MaterialTheme.typography.bodySmall, color = C.Text2)
                }
            }
        }
    }

    item {
        SectionTitle("Passivo")
        Panel {
            val rows = listOf(
                "Rendimentos a distribuir" to b.dividendsPayable,
                "Obrigações por compra de imóveis" to b.propertyObligations,
                "Securitização de recebíveis" to b.securitization,
                "Taxa de administração a pagar" to b.adminFeePayable,
                "Provisões e contingências" to b.provisions,
                "Outros valores a pagar" to b.otherPayables,
            ).filter { it.second > 0 }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Total", style = MaterialTheme.typography.bodyMedium, color = C.Text2, modifier = Modifier.weight(1f))
                Text(b.liabilities.brlCompact(), style = MaterialTheme.typography.titleMedium, color = C.Text)
            }
            if (rows.isNotEmpty()) Spacer(Modifier.height(4.dp))
            rows.forEach { (label, v) -> ValueRow(label, v, b.liabilities, null) }
        }
    }

    val last = balances.takeLast(12)
    if (last.size >= 3) {
        item {
            SectionTitle("Evolução do VP por cota")
            Panel {
                val vps = last.map { it.vpCota ?: 0.0 }
                LineChart(vps, last.map { it.ref.monthShort().take(3) }, height = 120.dp) { i ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(last[i].ref.monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text3)
                            Text(vps[i].brl(), style = MaterialTheme.typography.headlineSmall, color = C.Text)
                        }
                        val first = vps.first()
                        if (first > 0) Text(
                            "${(vps[i] / first - 1).pct(2, true)} em ${last.size - 1} meses",
                            style = MaterialTheme.typography.labelMedium, color = C.signed(vps[i] - first),
                        )
                    }
                }
            }
        }
    }

    reports?.firstOrNull()?.let { r ->
        item {
            SectionTitle("Relatório gerencial", trailing = "Ver todos", onTrailing = onOpenReports)
            ReportCard(r)
        }
    }

    item { SourceNote("Informe Mensal Estruturado enviado pelo administrador à CVM (dados abertos).") }
}

// ------------------------------------------------------------------ aba Relatórios

fun LazyListScope.reportItems(list: List<Fnet.DocRef>?, loading: Boolean) {
    item { Spacer(Modifier.height(14.dp)) }
    when {
        list == null && loading -> item {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), color = C.Mint, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Buscando relatórios na B3…", style = MaterialTheme.typography.bodySmall, color = C.Text2)
            }
        }
        list.isNullOrEmpty() -> item {
            Text(
                if (list == null) "Não foi possível acessar a B3 agora." else "O fundo ainda não publicou relatórios gerenciais.",
                style = MaterialTheme.typography.bodyMedium, color = C.Text3, modifier = Modifier.padding(vertical = 20.dp),
            )
        }
        else -> {
            list.take(24).forEach { r ->
                item(key = "rep${r.id}") { ReportCard(r); Spacer(Modifier.height(8.dp)) }
            }
            item { SourceNote("Relatórios gerenciais publicados pelo fundo na B3 (FNET). Abrem em PDF no navegador.") }
        }
    }
}

// ------------------------------------------------------------------ peças

@Composable
private fun MonthDropdown(balances: List<Balance>, sel: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface)
                .border(1.dp, C.Stroke, RoundedCornerShape(12.dp)).clickable { open = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(balances[sel].ref.monthYear(), style = MaterialTheme.typography.labelLarge, color = C.Text)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, "Escolher mês", tint = C.Text2, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            balances.indices.reversed().forEach { i ->
                DropdownMenuItem(
                    text = { Text(balances[i].ref.monthYear(), color = if (i == sel) C.Mint else C.Text) },
                    onClick = { open = false; onSelect(i) },
                )
            }
        }
    }
}

@Composable
private fun ReportCard(r: Fnet.DocRef) {
    val ctx = LocalContext.current
    Panel(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Fnet.docUrl(r.id)))) } }, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Coral.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Description, null, tint = C.Coral, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reportTitle(r.reference), style = MaterialTheme.typography.titleSmall, color = C.Text)
                Text("Publicado em ${r.delivered.take(10)} · PDF", style = MaterialTheme.typography.bodySmall, color = C.Text3)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Abrir", tint = C.Text3)
        }
    }
}

@Composable
private fun SourceNote(text: String) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.Info, null, tint = C.Text3, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = C.Text3)
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier, color: Color = C.Text, sub: String? = null, subColor: Color = C.Text3) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = C.Text3, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor, maxLines = 1)
    }
}

@Composable
private fun ValueRow(label: String, value: Double, total: Double, dot: Color?) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) { Dot(dot, 9.dp); Spacer(Modifier.width(10.dp)) }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = C.Text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value.brlCompact(), style = MaterialTheme.typography.labelLarge, color = C.Text)
        if (total > 0) {
            Text((value / total).pct(1), style = MaterialTheme.typography.labelMedium, color = C.Text3, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
        }
    }
}

private fun reportTitle(ref: String): String {
    val d = runCatching {
        when {
            ref.length >= 10 -> LocalDate.parse(ref.take(10), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            ref.length == 7 -> LocalDate.parse("01/$ref", DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            else -> null
        }
    }.getOrNull()
    return if (d != null) "Relatório de ${d.monthYear().replaceFirstChar { it.lowercase() }}" else "Relatório gerencial"
}

private fun delta(now: Double?, before: Double?): Pair<String, Color>? {
    if (now == null || before == null || before == 0.0) return null
    val r = now / before - 1
    return r.pct(2, true) + " no mês" to C.signed(r)
}
