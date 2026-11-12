package br.com.cotista.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.BarChart
import br.com.cotista.ui.components.ChangePill
import br.com.cotista.ui.components.ListPanel
import br.com.cotista.ui.components.MetricCard
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.PillButton
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.Stat
import br.com.cotista.ui.components.Tag
import br.com.cotista.ui.components.TickerBadge
import br.com.cotista.ui.components.UnderlineTabs
import br.com.cotista.ui.components.masked
import br.com.cotista.ui.theme.C
import br.com.cotista.util.PT_BR
import br.com.cotista.util.brl
import br.com.cotista.util.brlCompact
import br.com.cotista.util.brlCota
import br.com.cotista.util.ddMM
import br.com.cotista.util.ddMMyy
import br.com.cotista.util.monthShort
import br.com.cotista.util.monthYear
import br.com.cotista.util.num
import br.com.cotista.util.pct
import br.com.cotista.util.qty
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.math.ceil

@Composable
fun FundDetailScreen(
    ticker: String,
    state: UiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onEnsure: (String) -> Unit,
    onLoadReports: (String) -> Unit = {},
    initialTab: Int = 0,
) {
    LaunchedEffect(ticker, state.funds.isNotEmpty()) { onEnsure(ticker); onLoadReports(ticker) }
    val f = state.funds[ticker]
    val position = state.positions.firstOrNull { it.ticker == ticker }
    var tab by rememberSaveable(ticker) { mutableIntStateOf(initialTab) }
    val balances = f?.balances.orEmpty().filter { (it.netWorth ?: 0.0) > 0 }
    var balSel by rememberSaveable(ticker, balances.size) { mutableIntStateOf(balances.lastIndex) }
    val hide = state.hideValues

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = C.Text) }
                Text(ticker, style = MaterialTheme.typography.titleMedium, color = C.Text, modifier = Modifier.weight(1f))
                if (state.loadingFund == ticker || state.reportsLoading == ticker) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = C.Mint, strokeWidth = 2.dp)
                    Spacer(Modifier.width(16.dp))
                }
            }
            if (f == null) {
                Centered { Text("Fundo não encontrado nos dados da B3.", color = C.Text3) }
                return@Column
            }
            val today = LocalDate.now()
            val months = (11 downTo 0).map { today.withDayOfMonth(1).minusMonths(it.toLong()) }
            val effective = f.effectiveDividends
            val usingEstimates = f.exactDividends.isEmpty() && effective.isNotEmpty()
            val monthly = months.map { m -> effective.filter { d -> d.refDate?.let { it.year == m.year && it.month == m.month } == true }.sumOf { it.value } }

            LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 110.dp)) {
                // identidade + preço
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TickerBadge(ticker, 54.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.titleMedium, color = C.Text, maxLines = 2)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag(f.segment, C.Sky)
                                f.cvm?.management?.takeIf { it.isNotBlank() }?.let { Tag("Gestão ${it.lowercase()}", C.Text2) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(f.price?.brl() ?: "—", style = MaterialTheme.typography.displaySmall, color = C.Text)
                            state.quoteDate?.let { Text("Fechamento de ${it.ddMMyy()} · B3", style = MaterialTheme.typography.bodySmall, color = C.Text3) }
                        }
                        ChangePill(f.quote?.change, Modifier.padding(bottom = 22.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    UnderlineTabs(listOf("Resumo", "Proventos", "Balanço", "Relatórios"), tab, { tab = it })
                }

                when (tab) {
                    0 -> {
                        if (position != null) {
                            item {
                                Spacer(Modifier.height(16.dp))
                                val value = position.quantity * (f.price ?: position.avgPrice)
                                val res = value / (position.quantity * position.avgPrice) - 1
                                val monthlyIncome = (f.lastDividend?.value ?: 0.0) * position.quantity
                                Panel(brush = C.heroBrush, glow = C.Mint, padding = 16.dp, onClick = { onAdd(ticker) }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("SUA POSIÇÃO", style = MaterialTheme.typography.labelSmall, color = C.Mint, modifier = Modifier.weight(1f))
                                        Text("Editar", style = MaterialTheme.typography.labelLarge, color = C.Mint)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row {
                                        Stat("Cotas", position.quantity.qty(), Modifier.weight(1f))
                                        Stat("Saldo", masked(hide, value.brl()), Modifier.weight(1f), hint = res.pct(2, true))
                                        Stat("Renda/mês", masked(hide, monthlyIncome.brl()), Modifier.weight(1f), C.Mint)
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetricCard("Último rendimento", f.lastDividend?.value?.brlCota() ?: "—", f.dyLast?.let { "DY ${it.pct()} no mês" }, Modifier.weight(1f), C.Mint)
                                    MetricCard("DY 12 meses", f.dy12m?.pct() ?: "—", "sobre o preço atual", Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val pvp = f.pvp
                                    MetricCard("P/VP", pvp?.num(2) ?: "—", f.cvm?.vpCota?.let { "VP ${it.brl()} por cota" }, Modifier.weight(1f),
                                        when { pvp == null -> C.Text; pvp < 0.95 -> C.Mint; pvp > 1.05 -> C.Amber; else -> C.Text })
                                    MetricCard("Liquidez", f.quote?.volume?.brlCompact() ?: "—", "negociados por dia", Modifier.weight(1f))
                                }
                            }
                        }
                        item {
                            SectionTitle("Rendimento por cota", trailing = "Ver proventos", onTrailing = { tab = 1 })
                            Panel {
                                BarChart(monthly, months.map { it.monthShort().take(3) }, height = 80.dp, highlightLast = false) { i ->
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(months[i].monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text3, modifier = Modifier.weight(1f))
                                        Text(if (monthly[i] > 0) monthly[i].brlCota() else "—", style = MaterialTheme.typography.titleMedium, color = C.Text)
                                    }
                                }
                            }
                        }
                        item { Simulator(f.lastDividend?.value, f.price) }
                        f.cvm?.let { c ->
                            item {
                                SectionTitle("Ficha do fundo")
                                Panel {
                                    InfoLine("Nome oficial", c.name)
                                    InfoLine("CNPJ", formatCnpj(c.cnpj))
                                    InfoLine("Administrador", c.admin.ifBlank { "—" })
                                    InfoLine("Patrimônio", c.netWorth?.brlCompact() ?: "—")
                                    InfoLine("Cotistas", c.holders?.let { NumberFormat.getIntegerInstance(PT_BR).format(it) } ?: "—")
                                    InfoLine("Cotas emitidas", c.shares?.let { NumberFormat.getIntegerInstance(PT_BR).format(it.toLong()) } ?: "—")
                                }
                            }
                        }
                    }
                    1 -> {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Panel {
                                BarChart(monthly, months.map { it.monthShort().take(3) }, highlightLast = false) { i ->
                                    Column {
                                        Text(months[i].monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text3)
                                        Text(if (monthly[i] > 0) monthly[i].brlCota() else "—", style = MaterialTheme.typography.headlineSmall, color = C.Text)
                                        val p = f.price
                                        Text(
                                            if (p != null && monthly[i] > 0) "DY ${(monthly[i] / p).pct()} sobre o preço atual" else " ",
                                            style = MaterialTheme.typography.bodySmall, color = C.Text3,
                                        )
                                    }
                                }
                                if (usingEstimates) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Info, null, tint = C.Amber, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Estimado pelo dividend yield do informe mensal da CVM.", style = MaterialTheme.typography.bodySmall, color = C.Text2)
                                    }
                                }
                            }
                        }
                        val announced = f.dividends.filter { !it.estimated }.sortedByDescending { it.comDate ?: it.payDate }.take(24)
                        if (announced.isNotEmpty()) {
                            item {
                                SectionTitle("Avisos de proventos")
                                ListPanel(announced) { d ->
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(d.value.brlCota(), style = MaterialTheme.typography.titleSmall, color = C.Text)
                                                if (d.kind != "Rendimento") { Spacer(Modifier.width(6.dp)); Tag(d.kind, C.Lilac) }
                                            }
                                            Text(d.comDate?.let { "Data-com ${it.ddMMyy()}" } ?: "", style = MaterialTheme.typography.bodySmall, color = C.Text3)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            val pd = d.payDate
                                            Text(pd?.let { "Pagamento ${it.ddMM()}" } ?: "", style = MaterialTheme.typography.labelMedium,
                                                color = if (pd != null && !pd.isBefore(today)) C.Amber else C.Text2)
                                            val p = f.price
                                            if (p != null && d.kind == "Rendimento") Text("DY ${(d.value / p).pct()}", style = MaterialTheme.typography.bodySmall, color = C.Text3)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> balanceItems(balances, balSel, { balSel = it }, state.reports[ticker], { tab = 3 })
                    else -> reportItems(state.reports[ticker], state.reportsLoading == ticker)
                }
            }
        }

        if (f != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg, C.Bg)))
                    .navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 14.dp),
            ) {
                PillButton(
                    if (position != null) "Comprar mais / editar posição" else "Adicionar à carteira",
                    if (position != null) Icons.Rounded.Edit else Icons.Rounded.Add,
                    { onAdd(ticker) },
                    Modifier.fillMaxWidth().height(54.dp),
                )
            }
        }
    }
}

@Composable
private fun Simulator(lastDividend: Double?, price: Double?) {
    if (lastDividend == null || price == null || lastDividend <= 0 || price <= 0) return
    var target by remember { mutableFloatStateOf(1000f) }
    val cotas = ceil(target / lastDividend).toLong()
    val magic = ceil(price / lastDividend).toLong()
    SectionTitle("Simulador de renda")
    Panel(brush = C.amberBrush) {
        Text("Quero receber por mês", style = MaterialTheme.typography.labelMedium, color = C.Text2)
        Text(target.toDouble().brl(), style = MaterialTheme.typography.headlineMedium, color = C.Amber)
        Slider(
            value = target, onValueChange = { target = (it / 50).toInt() * 50f }, valueRange = 100f..20000f,
            colors = SliderDefaults.colors(thumbColor = C.Amber, activeTrackColor = C.Amber, inactiveTrackColor = C.Surface3),
        )
        Row {
            Stat("Cotas necessárias", NumberFormat.getIntegerInstance(PT_BR).format(cotas), Modifier.weight(1f))
            Stat("Investimento", (cotas * price).brlCompact(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Número mágico: $magic cotas — a partir daí os rendimentos de um mês compram uma cota nova.",
            style = MaterialTheme.typography.bodySmall, color = C.Text2,
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = C.Text3, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = C.Text, modifier = Modifier.weight(1f))
    }
}

private fun formatCnpj(d: String): String =
    if (d.length == 14) "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}" else d
