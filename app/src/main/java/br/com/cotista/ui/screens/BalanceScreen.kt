package br.com.cotista.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Balance
import br.com.cotista.data.Fnet
import br.com.cotista.data.Fund
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.Divider
import br.com.cotista.ui.components.Donut
import br.com.cotista.ui.components.Dot
import br.com.cotista.ui.components.LineChart
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.SegmentBar
import br.com.cotista.ui.components.TickerBadge
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
import kotlin.math.abs

@Composable
fun BalanceScreen(
    ticker: String,
    state: UiState,
    onBack: () -> Unit,
    onLoadReports: (String) -> Unit,
) {
    val f = state.funds[ticker]
    LaunchedEffect(ticker, f != null) { if (f != null) onLoadReports(ticker) }
    val balances = f?.balances.orEmpty().filter { (it.netWorth ?: 0.0) > 0 }
    var sel by rememberSaveable(ticker, balances.size) { mutableIntStateOf(balances.lastIndex) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = C.Text) }
            Text("Balanço mensal", style = MaterialTheme.typography.titleMedium, color = C.Text, modifier = Modifier.weight(1f))
        }
        if (f == null || balances.isEmpty()) {
            Centered { Text("Este fundo ainda não tem informe mensal na CVM.", color = C.Text3) }
            return@Column
        }
        val b = balances[sel.coerceIn(0, balances.lastIndex)]
        val prev = balances.getOrNull(sel - 1)

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TickerBadge(ticker, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ticker, style = MaterialTheme.typography.titleMedium, color = C.Text)
                        Text(f.name, style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))
                MonthPicker(balances, sel) { sel = it }
                Spacer(Modifier.height(16.dp))
            }

            // Herói: patrimônio e VP/cota
            item {
                Panel(brush = C.heroBrush, glow = C.Mint, padding = 22.dp) {
                    Text(
                        "BALANÇO DE ${b.ref.monthYear().uppercase()}", style = MaterialTheme.typography.labelSmall, color = C.Mint,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Patrimônio líquido", style = MaterialTheme.typography.bodySmall, color = C.Text2)
                    Text(b.netWorth?.brlCompact() ?: "—", style = MaterialTheme.typography.displaySmall, color = C.Text)
                    DeltaLine(b.netWorth, prev?.netWorth, money = true)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile("VP por cota", b.vpCota?.brl() ?: "—", Modifier.weight(1f), sub = delta(b.vpCota, prev?.vpCota))
                        Tile("Ativo total", b.totalAssets?.brlCompact() ?: "—", Modifier.weight(1f), sub = delta(b.totalAssets, prev?.totalAssets))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val pvp = f.price?.let { p -> b.vpCota?.takeIf { it > 0 }?.let { p / it } }
                        Tile("P/VP hoje", pvp?.let { String.format(PT_BR, "%.2f", it) } ?: "—", Modifier.weight(1f))
                        Tile("Cotas emitidas", b.shares?.let { NumberFormat.getIntegerInstance(PT_BR).format(it.toLong()) } ?: "—", Modifier.weight(1f))
                    }
                }
            }

            // Resultado do mês
            item {
                SectionTitle("Resultado do mês")
                Panel {
                    Row {
                        Metric("Rentab. patrimonial", b.returnPatrimonial, Modifier.weight(1f), signed = true)
                        Metric("Rentab. efetiva", b.returnEffective, Modifier.weight(1f), signed = true)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Metric("Dividend yield", b.dy, Modifier.weight(1f), color = C.Mint)
                        Metric("Taxa de adm. no mês", b.adminFee, Modifier.weight(1f), decimals = 3, color = C.Text)
                    }
                    if ((b.amortization ?: 0.0) > 0) {
                        Spacer(Modifier.height(16.dp))
                        Metric("Amortização de cotas", b.amortization, Modifier.fillMaxWidth(), color = C.Lilac)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Patrimonial: variação do valor da cota no mês. Efetiva: patrimonial + rendimentos distribuídos.",
                        style = MaterialTheme.typography.labelSmall, color = C.Text3,
                    )
                }
            }

            // Composição do ativo
            item {
                SectionTitle("Onde o dinheiro está")
                Panel {
                    val comp = b.composition
                    val total = b.compositionTotal
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Donut(comp.mapIndexed { i, s -> C.palette[i % C.palette.size] to s.value }, size = 128.dp, thickness = 15.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (total > 0) (comp.first().value / total).pct(0) else "—", style = MaterialTheme.typography.titleLarge, color = C.Text)
                                Text(comp.firstOrNull()?.label?.lowercase()?.take(12) ?: "", style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1)
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            comp.take(6).forEachIndexed { i, s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Dot(C.palette[i % C.palette.size])
                                    Spacer(Modifier.width(8.dp))
                                    Text(s.label, style = MaterialTheme.typography.bodySmall, color = C.Text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text((s.value / total).pct(0), style = MaterialTheme.typography.labelMedium, color = C.Text)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    comp.forEachIndexed { i, s ->
                        ValueRow(s.label, s.value, total, C.palette[i % C.palette.size])
                    }
                }
            }

            // Detalhe dos imóveis
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
                        SegmentBar(all.mapIndexed { i, p -> listOf(C.Mint, C.Amber, C.Sky, C.Lilac, C.Text3)[i % 5] to p.second })
                        Spacer(Modifier.height(6.dp))
                        all.forEachIndexed { i, (label, v) ->
                            ValueRow(label, v, b.realEstate, listOf(C.Mint, C.Amber, C.Sky, C.Lilac, C.Text3)[i % 5])
                        }
                        if (b.rentReceivable > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("Aluguéis a receber: ${b.rentReceivable.brlCompact()}", style = MaterialTheme.typography.bodySmall, color = C.Text2)
                        }
                    }
                }
            }

            // Passivo
            item {
                SectionTitle("Passivo e alavancagem")
                Panel {
                    val lev = b.leverage
                    val levColor = when { lev == null -> C.Text; lev < 0.10 -> C.Mint; lev < 0.30 -> C.Amber; else -> C.Coral }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PASSIVO TOTAL", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                            Text(b.liabilities.brlCompact(), style = MaterialTheme.typography.headlineSmall, color = C.Text)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(lev?.pct(1) ?: "—", style = MaterialTheme.typography.headlineSmall, color = levColor)
                            Text("do patrimônio", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(C.Surface2)) {
                        Box(Modifier.fillMaxWidth(((lev ?: 0.0) / 0.5).toFloat().coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(50)).background(levColor))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            lev == null -> ""
                            lev < 0.10 -> "Endividamento baixo"
                            lev < 0.30 -> "Endividamento moderado"
                            else -> "Endividamento alto — confira os prazos no relatório gerencial"
                        },
                        style = MaterialTheme.typography.labelMedium, color = levColor,
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    val rows = listOf(
                        "Rendimentos a distribuir" to b.dividendsPayable,
                        "Obrigações por compra de imóveis" to b.propertyObligations,
                        "Securitização de recebíveis" to b.securitization,
                        "Taxa de administração a pagar" to b.adminFeePayable,
                        "Provisões e contingências" to b.provisions,
                        "Outros valores a pagar" to b.otherPayables,
                    ).filter { it.second > 0 }
                    rows.forEach { (label, v) -> ValueRow(label, v, b.liabilities, null) }
                    if (rows.isEmpty()) Text("Sem obrigações relevantes informadas.", style = MaterialTheme.typography.bodySmall, color = C.Text3, modifier = Modifier.padding(top = 10.dp))
                }
            }

            // Cotistas
            item {
                SectionTitle("Cotistas")
                Panel {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                            Text(b.holders?.let { NumberFormat.getIntegerInstance(PT_BR).format(it) } ?: "—", style = MaterialTheme.typography.titleLarge, color = C.Text)
                            val h1 = b.holders
                            val h0 = prev?.holders
                            val d = if (h1 != null && h0 != null) h1 - h0 else null
                            if (d != null) Text((if (d >= 0) "+" else "") + NumberFormat.getIntegerInstance(PT_BR).format(d) + " no mês", style = MaterialTheme.typography.labelMedium, color = C.signed(d.toDouble()))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("PESSOAS FÍSICAS", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                            val ht = b.holders
                            val hp = b.holdersPF
                            val pf = if (ht != null && hp != null && ht > 0) hp.toDouble() / ht else null
                            Text(pf?.pct(1) ?: "—", style = MaterialTheme.typography.titleLarge, color = C.Text)
                        }
                    }
                }
            }

            // Evolução
            val last = balances.takeLast(12)
            if (last.size >= 3) {
                item {
                    SectionTitle("Evolução do VP por cota")
                    Panel {
                        val vps = last.map { it.vpCota ?: 0.0 }
                        LineChart(vps, last.map { it.ref.monthShort().take(3) }) { i ->
                            Column {
                                Text(last[i].ref.monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text2)
                                Text(vps[i].brl(), style = MaterialTheme.typography.headlineSmall, color = C.Text)
                                val first = vps.first()
                                Text(
                                    if (first > 0) "${(vps[i] / first - 1).pct(2, true)} desde ${last.first().ref.monthShort().lowercase()}/${last.first().ref.year % 100}" else " ",
                                    style = MaterialTheme.typography.bodySmall, color = C.signed(vps[i] - first),
                                )
                            }
                        }
                    }
                }
            }

            // Relatórios gerenciais
            item {
                SectionTitle("Relatórios gerenciais")
                Reports(state.reports[ticker], state.reportsLoading == ticker)
            }

            item {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, null, tint = C.Text3, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Fonte: Informe Mensal Estruturado enviado pelo administrador à CVM (dados abertos). Relatórios gerenciais: B3/FNET.",
                        style = MaterialTheme.typography.labelSmall, color = C.Text3,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPicker(balances: List<Balance>, selected: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (balances.size - 4).coerceAtLeast(0))
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(balances, key = { _, b -> b.ref.toString() }) { i, b ->
            val sel = i == selected
            Column(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (sel) C.mintBrush else androidx.compose.ui.graphics.SolidColor(C.Surface))
                    .border(1.dp, if (sel) C.Mint.copy(alpha = 0f) else C.Stroke, RoundedCornerShape(16.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(b.ref.monthShort().take(3), style = MaterialTheme.typography.labelLarge, color = if (sel) C.Bg else C.Text)
                Text("${b.ref.year}", style = MaterialTheme.typography.labelSmall, color = if (sel) C.Bg.copy(alpha = 0.7f) else C.Text3)
            }
        }
    }
}

@Composable
private fun Reports(list: List<Fnet.DocRef>?, loading: Boolean) {
    val ctx = LocalContext.current
    Panel(padding = 4.dp) {
        when {
            list == null && loading -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), color = C.Mint, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Buscando relatórios na B3…", style = MaterialTheme.typography.bodySmall, color = C.Text2)
            }
            list.isNullOrEmpty() -> Text(
                if (list == null) "Não foi possível acessar a B3 agora." else "O fundo ainda não publicou relatórios gerenciais.",
                style = MaterialTheme.typography.bodySmall, color = C.Text3, modifier = Modifier.padding(16.dp),
            )
            else -> list.take(12).forEachIndexed { i, r ->
                if (i > 0) Divider(Modifier.padding(horizontal = 14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Fnet.docUrl(r.id)))) }
                    }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(C.Coral.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Description, null, tint = C.Coral, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(reportTitle(r.reference), style = MaterialTheme.typography.titleSmall, color = C.Text)
                        Text("Publicado em ${r.delivered.take(10)} · PDF", style = MaterialTheme.typography.bodySmall, color = C.Text3)
                    }
                    Icon(Icons.Rounded.OpenInNew, "Abrir", tint = C.Text3, modifier = Modifier.size(18.dp))
                }
            }
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

@Composable
private fun DeltaLine(now: Double?, before: Double?, money: Boolean) {
    if (now == null || before == null || before == 0.0) return
    val d = now - before
    val r = now / before - 1
    Text(
        (if (d >= 0) "+" else "−") + abs(d).brlCompact().removePrefix("-") + " (${r.pct(2, true)}) vs. mês anterior",
        style = MaterialTheme.typography.bodySmall, color = C.signed(d),
    )
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier, sub: Pair<String, Color>? = null) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = C.Text, maxLines = 1)
        if (sub != null) Text(sub.first, style = MaterialTheme.typography.labelSmall, color = sub.second, maxLines = 1)
    }
}

@Composable
private fun Metric(label: String, v: Double?, modifier: Modifier, signed: Boolean = false, decimals: Int = 2, color: Color? = null) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            v?.pct(decimals, signed) ?: "—", style = MaterialTheme.typography.titleLarge,
            color = color ?: if (signed) C.signed(v) else C.Text,
        )
    }
}

@Composable
private fun ValueRow(label: String, value: Double, total: Double, dot: Color?) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) { Dot(dot); Spacer(Modifier.width(8.dp)) }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = C.Text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value.brlCompact(), style = MaterialTheme.typography.labelLarge, color = C.Text)
        if (total > 0) {
            Spacer(Modifier.width(8.dp))
            Text((value / total).pct(1), style = MaterialTheme.typography.labelMedium, color = C.Text3, modifier = Modifier.width(46.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

/** Cartão-resumo usado no detalhe do fundo. */
@Composable
fun BalanceTeaser(f: Fund, onOpen: () -> Unit) {
    val list = f.balances.filter { (it.netWorth ?: 0.0) > 0 }
    val b = list.lastOrNull() ?: return
    val prev = list.getOrNull(list.size - 2)
    SectionTitle("Balanço mensal", trailing = "Ver completo", onTrailing = onOpen)
    Panel(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(b.ref.monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Mint, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Description, null, tint = C.Text3, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Informe CVM", style = MaterialTheme.typography.labelSmall, color = C.Text3)
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text("PATRIMÔNIO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                Text(b.netWorth?.brlCompact() ?: "—", style = MaterialTheme.typography.titleMedium, color = C.Text)
                delta(b.netWorth, prev?.netWorth)?.let { Text(it.first, style = MaterialTheme.typography.labelSmall, color = it.second) }
            }
            Column(Modifier.weight(1f)) {
                Text("VP/COTA", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                Text(b.vpCota?.brl() ?: "—", style = MaterialTheme.typography.titleMedium, color = C.Text)
                delta(b.vpCota, prev?.vpCota)?.let { Text(it.first, style = MaterialTheme.typography.labelSmall, color = it.second) }
            }
            Column(Modifier.weight(1f)) {
                Text("ALAVANCAGEM", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                Text(b.leverage?.pct(1) ?: "—", style = MaterialTheme.typography.titleMedium, color = C.Text)
            }
        }
        Spacer(Modifier.height(14.dp))
        val comp = b.composition
        SegmentBar(comp.mapIndexed { i, s -> C.palette[i % C.palette.size] to s.value })
        Spacer(Modifier.height(8.dp))
        Text(
            comp.take(3).joinToString(" · ") { "${it.label} ${(it.value / b.compositionTotal).pct(0)}" },
            style = MaterialTheme.typography.bodySmall, color = C.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
