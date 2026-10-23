package br.com.cotista.ui.screens

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
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.ui.Payment
import br.com.cotista.ui.PortfolioSummary
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.BarChart
import br.com.cotista.ui.components.Divider
import br.com.cotista.ui.components.Donut
import br.com.cotista.ui.components.Dot
import br.com.cotista.ui.components.EmptyState
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.Stat
import br.com.cotista.ui.components.Tag
import br.com.cotista.ui.components.TickerBadge
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
) {
    val summary = remember(state.positions, state.funds) { PortfolioSummary(state.positions, state.funds) }
    val hide = state.hideValues
    val today = LocalDate.now()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(C.mintBrush),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Savings, null, tint = C.Bg, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val h = LocalTime.now().hour
                    Text(when (h) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" },
                        style = MaterialTheme.typography.bodySmall, color = C.Text3)
                    Text("Sua renda passiva", style = MaterialTheme.typography.titleLarge, color = C.Text)
                }
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface2).clickable(onClick = onToggleHide),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Ocultar valores", tint = C.Text2, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (state.positions.isEmpty()) {
            item { Onboarding(state, onAdd) }
            item { MarketPulse(state, onOpenFund) }
            return@LazyColumn
        }

        // Herói: proventos do mês
        item {
            val animated by animateFloatAsState(summary.thisMonth.toFloat(), tween(1100, easing = FastOutSlowInEasing), label = "hero")
            Panel(brush = C.heroBrush, glow = C.Mint, padding = 22.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PROVENTOS DE ${today.monthName().uppercase()}", style = MaterialTheme.typography.labelSmall, color = C.Mint,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    if (summary.thisMonthHasEstimates) { Spacer(Modifier.width(8.dp)); Tag("estimado", C.Amber) }
                }
                Spacer(Modifier.height(14.dp))
                Text(masked(hide, animated.toDouble().brl()), style = MaterialTheme.typography.displayMedium, color = C.Text)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (hide) "Projeção R$ •••/mês" else "Projeção: ${summary.projectedMonthly.brl()}/mês pelo último provento",
                    style = MaterialTheme.typography.bodySmall, color = C.Text2,
                )
                if (summary.thisMonth > 0) {
                    Spacer(Modifier.height(18.dp))
                    val frac = (summary.thisMonthReceived / summary.thisMonth).toFloat().coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.28f))) {
                        Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(50)).background(C.mintBrush))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Recebido ${masked(hide, summary.thisMonthReceived.brl())}", style = MaterialTheme.typography.labelMedium, color = C.Mint, modifier = Modifier.weight(1f))
                        Text("A receber ${masked(hide, summary.thisMonthPending.brl())}", style = MaterialTheme.typography.labelMedium, color = C.Text2)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroTile("12 meses", masked(hide, summary.last12.brl()), Modifier.weight(1f))
                    HeroTile("Yield on cost", summary.yieldOnCost.pct(), Modifier.weight(1f), C.Mint)
                    HeroTile("DY carteira", summary.dy12.pct(), Modifier.weight(1f))
                }
            }
        }

        // Patrimônio
        item {
            Spacer(Modifier.height(12.dp))
            Panel {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("PATRIMÔNIO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                        Spacer(Modifier.height(4.dp))
                        Text(masked(hide, summary.value.brl()), style = MaterialTheme.typography.headlineLarge, color = C.Text)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            (if (hide) "" else (if (summary.dayChange >= 0) "+" else "") + summary.dayChange.brl() + " no pregão"),
                            style = MaterialTheme.typography.bodySmall, color = C.signed(summary.dayChange),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("RESULTADO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                        Spacer(Modifier.height(4.dp))
                        Text(masked(hide, summary.result.brl()), style = MaterialTheme.typography.titleMedium, color = C.signed(summary.result))
                        Text(summary.resultPct.pct(2, true), style = MaterialTheme.typography.bodySmall, color = C.signed(summary.result))
                    }
                }
            }
        }

        // Gráfico 12 meses
        item {
            SectionTitle("Renda mês a mês")
            Panel {
                BarChart(
                    values = summary.monthly,
                    labels = summary.months.map { it.monthShort().take(3) },
                ) { i ->
                    Column {
                        Text(summary.months[i].monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text2)
                        Text(masked(hide, summary.monthly[i].brl()), style = MaterialTheme.typography.headlineSmall, color = C.Text)
                        Text(
                            if (summary.avgMonthly > 0 && !hide) "Média mensal: ${summary.avgMonthly.brl()}" else " ",
                            style = MaterialTheme.typography.bodySmall, color = C.Text3,
                        )
                    }
                }
            }
        }

        // Próximos pagamentos
        item {
            SectionTitle("Próximos pagamentos", trailing = "Agenda", onTrailing = onOpenAgenda)
        }
        if (summary.upcoming.isEmpty()) {
            item {
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
            }
        } else {
            item {
                Panel(padding = 6.dp) {
                    summary.upcoming.take(6).forEachIndexed { i, p ->
                        if (i > 0) Divider(Modifier.padding(horizontal = 12.dp))
                        PaymentRow(p, hide, today) { onOpenFund(p.holding.position.ticker) }
                    }
                }
            }
        }

        // Alocação
        if (summary.bySegment.isNotEmpty() && summary.value > 0) {
            item {
                SectionTitle("Diversificação")
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val slices = summary.bySegment.mapIndexed { i, (_, v) -> C.palette[i % C.palette.size] to v }
                        Donut(slices, size = 120.dp, thickness = 14.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${summary.holdings.size}", style = MaterialTheme.typography.headlineSmall, color = C.Text)
                                Text(if (summary.holdings.size == 1) "fundo" else "fundos", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            summary.bySegment.take(6).forEachIndexed { i, (seg, v) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Dot(C.palette[i % C.palette.size])
                                    Spacer(Modifier.width(8.dp))
                                    Text(seg, style = MaterialTheme.typography.bodySmall, color = C.Text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text((v / summary.value).pct(0), style = MaterialTheme.typography.labelMedium, color = C.Text)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SourcesFooter(state) }
    }
}

@Composable
private fun HeroTile(label: String, value: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = C.Text) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
    }
}

@Composable
fun PaymentRow(p: Payment, hide: Boolean, today: LocalDate, onClick: () -> Unit) {
    val d = p.dividend
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickerBadge(d.ticker, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
            Text(
                "${d.value.brlCota()}/cota" + (d.comDate?.let { " · data-com ${it.ddMM()}" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = C.Text3,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(masked(hide, p.amount.brl()), style = MaterialTheme.typography.titleSmall, color = C.Mint)
            d.payDate?.let {
                Text(relativeDay(it, today), style = MaterialTheme.typography.labelMedium, color = if (it == today) C.Amber else C.Text2)
            }
        }
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
        Button(
            onClick = { onAdd(null) },
            colors = ButtonDefaults.buttonColors(containerColor = C.Mint, contentColor = C.Bg),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Adicionar meu primeiro FII")
        }
        if (state.funds.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Mais negociados hoje", style = MaterialTheme.typography.labelMedium, color = C.Text3)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.fundList.take(10), key = { it.ticker }) { f ->
                    Text(
                        "+ ${f.ticker}", color = C.Text, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(C.Surface3).clickable { onAdd(f.ticker) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
    Panel(padding = 6.dp) {
        recent.forEachIndexed { i, d ->
            if (i > 0) Divider(Modifier.padding(horizontal = 12.dp))
            val price = state.funds[d.ticker]?.price
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenFund(d.ticker) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TickerBadge(d.ticker, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
                    Text(d.payDate?.let { "Paga em ${it.ddMM()}" } ?: "", style = MaterialTheme.typography.bodySmall, color = C.Text3)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(d.value.brlCota(), style = MaterialTheme.typography.titleSmall, color = C.Mint)
                    if (price != null && price > 0) Text("DY ${(d.value / price).pct()}", style = MaterialTheme.typography.labelMedium, color = C.Text2)
                }
            }
        }
    }
    SourcesFooter(state)
}

@Composable
fun SourcesFooter(state: UiState) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val parts = buildList {
            state.quoteDate?.let { add("Cotações B3 de ${it.ddMM()}") }
            state.cvmRef?.let { add("Informes CVM ref. ${it.monthShort().lowercase()}/${it.year}") }
            add("Avisos de rendimentos B3/FNET")
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
