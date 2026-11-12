package br.com.cotista.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.ui.PortfolioSummary
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.BarChart
import br.com.cotista.ui.components.DateBox
import br.com.cotista.ui.components.GlassTile
import br.com.cotista.ui.components.ListPanel
import br.com.cotista.ui.components.ListRow
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.PillButton
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.Tag
import br.com.cotista.ui.components.masked
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.brlCota
import br.com.cotista.util.ddMM
import br.com.cotista.util.monthName
import br.com.cotista.util.monthShort
import br.com.cotista.util.monthYear
import br.com.cotista.util.pct
import br.com.cotista.util.relativeDay
import br.com.cotista.util.weekdayShort
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: UiState,
    contentPadding: PaddingValues,
    onOpenFund: (String) -> Unit,
    onAdd: (String?) -> Unit,
    onToggleHide: () -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenCarteira: () -> Unit = {},
) {
    val summary = remember(state.positions, state.funds) { PortfolioSummary(state.positions, state.funds) }
    val hide = state.hideValues
    val today = LocalDate.now()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(C.mintBrush), contentAlignment = Alignment.Center) {
                    Text("C", style = MaterialTheme.typography.titleLarge, color = C.Bg)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val h = LocalTime.now().hour
                    Text(when (h) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" },
                        style = MaterialTheme.typography.labelMedium, color = C.Text3)
                    Text("Sua renda passiva", style = MaterialTheme.typography.titleLarge, color = C.Text)
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface2).clickable(onClick = onToggleHide),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Ocultar valores", tint = C.Text2, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(18.dp))
        }

        if (state.positions.isEmpty()) {
            item { Onboarding(state, onAdd) }
            item { MarketPulse(state, onOpenFund) }
            item { SourcesFooter(state) }
            return@LazyColumn
        }

        // Herói
        item {
            val animated by animateFloatAsState(summary.thisMonth.toFloat(), tween(1100, easing = FastOutSlowInEasing), label = "hero")
            Panel(brush = C.heroBrush, glow = C.Mint, padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PROVENTOS DE ${today.monthName().uppercase()}", style = MaterialTheme.typography.labelSmall, color = C.Mint,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    if (summary.thisMonthHasEstimates) { Spacer(Modifier.width(6.dp)); Tag("estimado", C.Amber) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${summary.holdings.size} ${if (summary.holdings.size == 1) "fundo" else "fundos"}",
                        style = MaterialTheme.typography.labelMedium, color = C.Text2,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(masked(hide, animated.toDouble().brl()), style = MaterialTheme.typography.displayMedium, color = C.Text)
                Text(
                    if (hide) "Projeção R$ •••/mês" else "Projeção de ${summary.projectedMonthly.brl()}/mês pelo último provento",
                    style = MaterialTheme.typography.bodySmall, color = C.Text2,
                )
                if (summary.thisMonth > 0) {
                    Spacer(Modifier.height(14.dp))
                    val frac = (summary.thisMonthReceived / summary.thisMonth).toFloat().coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(50)).background(C.mintBrush))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Recebido ${masked(hide, summary.thisMonthReceived.brl())}", style = MaterialTheme.typography.labelLarge, color = C.Mint, modifier = Modifier.weight(1f))
                        Text("A receber ${masked(hide, summary.thisMonthPending.brl())}", style = MaterialTheme.typography.labelLarge, color = C.Text2)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTile("12 meses", masked(hide, summary.last12.brl()), Modifier.weight(1f))
                    GlassTile("Yield on cost", summary.yieldOnCost.pct(), Modifier.weight(1f), C.Mint)
                    GlassTile("Média/mês", masked(hide, summary.avgMonthly.brl()), Modifier.weight(1f))
                }
            }
        }

        // Próximo pagamento
        item {
            SectionTitle("Próximo pagamento", trailing = "Ver agenda", onTrailing = onOpenAgenda)
            val next = summary.upcoming.groupBy { it.dividend.payDate!! }.toSortedMap().entries.firstOrNull()
            if (next == null) {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = C.Text3)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (state.refreshing) "Buscando avisos de rendimentos na B3…"
                            else "Nenhum pagamento anunciado ainda. Os fundos costumam divulgar no fim do mês.",
                            style = MaterialTheme.typography.bodyMedium, color = C.Text2,
                        )
                    }
                }
            } else {
                val (date, pays) = next
                val tickers = pays.map { it.dividend.ticker }.distinct()
                val names = when (tickers.size) {
                    1 -> tickers[0]
                    2 -> "${tickers[0]} e ${tickers[1]}"
                    else -> "${tickers[0]}, ${tickers[1]} e +${tickers.size - 2}"
                }
                Panel(onClick = { if (tickers.size == 1) onOpenFund(tickers[0]) else onOpenAgenda() }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DateBox(date.weekdayShort(), date.dayOfMonth, if (date == today) C.Mint else C.Amber)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(names, style = MaterialTheme.typography.titleSmall, color = C.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                relativeDay(date, today) + " · " + if (tickers.size == 1) "${pays[0].dividend.value.brlCota()} por cota" else "${tickers.size} fundos",
                                style = MaterialTheme.typography.bodySmall, color = C.Text3,
                            )
                        }
                        Text(masked(hide, "+" + pays.sumOf { it.amount }.brl()), style = MaterialTheme.typography.titleMedium, color = C.Mint)
                    }
                }
            }
        }

        // Renda mês a mês
        item {
            SectionTitle("Renda mês a mês")
            Panel {
                BarChart(
                    values = summary.monthly,
                    labels = summary.months.map { it.monthShort().take(3) },
                    height = 110.dp,
                ) { i ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.months[i].monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text3)
                            Text(masked(hide, summary.monthly[i].brl()), style = MaterialTheme.typography.headlineSmall, color = C.Text)
                        }
                        if (summary.avgMonthly > 0 && !hide) {
                            Text("média ${summary.avgMonthly.brl()}", style = MaterialTheme.typography.labelMedium, color = C.Text3)
                        }
                    }
                }
            }
        }

        // Atalhos
        item {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Panel(Modifier.weight(1f), onClick = onOpenCarteira, padding = 14.dp) {
                    Text("PATRIMÔNIO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        masked(hide, "R$ " + NumberFormat.getIntegerInstance(br.com.cotista.util.PT_BR).format(summary.value.toLong())),
                        style = MaterialTheme.typography.titleMedium, color = C.Text, maxLines = 1,
                    )
                    Text(summary.resultPct.pct(2, true) + " no total", style = MaterialTheme.typography.labelMedium, color = C.signed(summary.result))
                }
                val top = summary.holdings.firstOrNull()
                Panel(Modifier.weight(1f), onClick = { top?.let { onOpenFund(it.position.ticker) } }, padding = 14.dp) {
                    Text("MAIOR POSIÇÃO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                    Spacer(Modifier.height(4.dp))
                    Text(top?.position?.ticker ?: "—", style = MaterialTheme.typography.titleMedium, color = C.Text)
                    Text(
                        if (top != null && summary.value > 0) "${(top.value / summary.value).pct(1)} da carteira" else " ",
                        style = MaterialTheme.typography.labelMedium, color = C.Text2,
                    )
                }
            }
        }

        item { SourcesFooter(state) }
    }
}

@Composable
private fun Onboarding(state: UiState, onAdd: (String?) -> Unit) {
    Panel(brush = C.heroBrush, glow = C.Mint, padding = 22.dp) {
        Text("Acompanhe seus FIIs com dados oficiais", style = MaterialTheme.typography.titleLarge, color = C.Text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Cotações da B3, informes mensais da CVM e avisos de rendimentos publicados pelos próprios fundos. Adicione suas cotas para ver quanto vai receber e quando.",
            style = MaterialTheme.typography.bodyMedium, color = C.Text2,
        )
        Spacer(Modifier.height(18.dp))
        PillButton("Adicionar meu primeiro FII", Icons.Rounded.Add, { onAdd(null) })
        if (state.funds.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("MAIS NEGOCIADOS HOJE", style = MaterialTheme.typography.labelSmall, color = C.Text2)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.fundList.take(10), key = { it.ticker }) { f ->
                    Text(
                        "+ ${f.ticker}", color = C.Text, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.25f)).clickable { onAdd(f.ticker) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketPulse(state: UiState, onOpenFund: (String) -> Unit) {
    val recent = state.announcements.filter { it.kind == "Rendimento" }.take(8)
    if (recent.isEmpty()) return
    SectionTitle("Rendimentos anunciados agora")
    ListPanel(recent) { d ->
        val price = state.funds[d.ticker]?.price
        ListRow(
            ticker = d.ticker, title = d.ticker,
            subtitle = d.payDate?.let { "Paga em ${it.ddMM()}" } ?: "",
            value = d.value.brlCota(), valueColor = C.Mint,
            valueSub = if (price != null && price > 0) "DY ${(d.value / price).pct()}" else null,
            onClick = { onOpenFund(d.ticker) },
        )
    }
}

@Composable
fun SourcesFooter(state: UiState) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val parts = buildList {
            state.quoteDate?.let { add("Cotações B3 de ${it.ddMM()}") }
            state.cvmRef?.let { add("Informes CVM ref. ${it.monthShort().lowercase()}/${it.year}") }
            add("Avisos B3/FNET")
        }
        Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = C.Text3, fontWeight = FontWeight.Normal)
        Spacer(Modifier.height(4.dp))
        Text("Rentabilidade passada não garante resultados futuros.", style = MaterialTheme.typography.labelSmall, color = C.Text3, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
