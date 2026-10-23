package br.com.cotista.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.BarChart
import br.com.cotista.ui.components.ChangePill
import br.com.cotista.ui.components.Divider
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.Stat
import br.com.cotista.ui.components.StatGrid
import br.com.cotista.ui.components.Tag
import br.com.cotista.ui.components.TickerBadge
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.brlCompact
import br.com.cotista.util.brlCota
import br.com.cotista.util.ddMM
import br.com.cotista.util.ddMMyy
import br.com.cotista.util.monthShort
import br.com.cotista.util.monthYear
import br.com.cotista.util.num
import br.com.cotista.util.pct
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
) {
    LaunchedEffect(ticker, state.funds.isNotEmpty()) { onEnsure(ticker) }
    val f = state.funds[ticker]
    val position = state.positions.firstOrNull { it.ticker == ticker }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = C.Text) }
            Text(ticker, style = MaterialTheme.typography.titleMedium, color = C.Text, modifier = Modifier.weight(1f))
            if (state.loadingFund == ticker) CircularProgressIndicator(Modifier.size(18.dp).padding(end = 4.dp), color = C.Mint, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
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

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TickerBadge(ticker, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.titleMedium, color = C.Text, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(f.segment, C.Sky)
                            f.cvm?.management?.takeIf { it.isNotBlank() }?.let { Tag("Gestão ${it.lowercase()}", C.Text2) }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(f.price?.brl() ?: "—", style = MaterialTheme.typography.displaySmall, color = C.Text)
                    Spacer(Modifier.width(10.dp))
                    ChangePill(f.quote?.change, Modifier.padding(bottom = 8.dp))
                }
                state.quoteDate?.let { Text("Fechamento de ${it.ddMMyy()} · B3", style = MaterialTheme.typography.bodySmall, color = C.Text3) }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Panel {
                    StatGrid(
                        listOf(
                            Triple("Último rendimento", f.lastDividend?.value?.brlCota() ?: "—", C.Mint),
                            Triple("DY mensal", f.dyLast?.pct() ?: "—", C.Mint),
                            Triple("DY 12 meses", f.dy12m?.pct() ?: "—", C.Text),
                            Triple("P/VP", f.pvp?.num(2) ?: "—", when { f.pvp == null -> C.Text; f.pvp!! < 0.95 -> C.Mint; f.pvp!! > 1.05 -> C.Amber; else -> C.Text }),
                            Triple("VP por cota", f.cvm?.vpCota?.brl() ?: "—", C.Text),
                            Triple("Patrimônio", f.cvm?.netWorth?.brlCompact() ?: "—", C.Text),
                            Triple("Cotistas", f.cvm?.holders?.let { NumberFormat.getIntegerInstance(br.com.cotista.util.PT_BR).format(it) } ?: "—", C.Text),
                            Triple("Liquidez diária", f.quote?.volume?.brlCompact() ?: "—", C.Text),
                        ),
                    )
                }
            }

            item {
                SectionTitle("Rendimentos por cota")
                Panel {
                    BarChart(monthly, months.map { it.monthShort().take(3) }, highlightLast = false) { i ->
                        Column {
                            Text(months[i].monthYear(), style = MaterialTheme.typography.labelMedium, color = C.Text2)
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

            item { Simulator(f.lastDividend?.value, f.price) }

            val announced = f.dividends.filter { !it.estimated }.sortedByDescending { it.comDate ?: it.payDate }
            if (announced.isNotEmpty()) {
                item { SectionTitle("Avisos de proventos") }
                item {
                    Panel(padding = 4.dp) {
                        announced.take(14).forEachIndexed { i, d ->
                            if (i > 0) Divider(Modifier.padding(horizontal = 14.dp))
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

            f.cvm?.let { c ->
                item {
                    SectionTitle("Ficha (informe CVM)")
                    Panel {
                        InfoLine("Nome oficial", c.name)
                        InfoLine("CNPJ", formatCnpj(c.cnpj))
                        InfoLine("Administrador", c.admin.ifBlank { "—" })
                        if (c.mandate.isNotBlank()) InfoLine("Mandato", c.mandate)
                        InfoLine("Cotas emitidas", c.shares?.let { NumberFormat.getIntegerInstance(br.com.cotista.util.PT_BR).format(it.toLong()) } ?: "—")
                        InfoLine("Referência", c.refDate.monthYear())
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onAdd(ticker) },
                    colors = ButtonDefaults.buttonColors(containerColor = C.Mint, contentColor = C.Bg),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Icon(if (position != null) Icons.Rounded.Edit else Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (position != null) "Editar posição (${position.quantity.toLong()} cotas)" else "Adicionar à carteira")
                }
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
    Panel(brush = Brush.linearGradient(listOf(C.AmberDeep, C.Surface))) {
        Text("Quero receber por mês", style = MaterialTheme.typography.labelMedium, color = C.Text2)
        Text(target.toDouble().brl(), style = MaterialTheme.typography.headlineMedium, color = C.Amber)
        Slider(
            value = target, onValueChange = { target = (it / 50).toInt() * 50f }, valueRange = 100f..20000f,
            colors = SliderDefaults.colors(thumbColor = C.Amber, activeTrackColor = C.Amber, inactiveTrackColor = C.Surface3),
        )
        Row {
            Stat("Cotas necessárias", NumberFormat.getIntegerInstance(br.com.cotista.util.PT_BR).format(cotas), Modifier.weight(1f))
            Stat("Investimento", (cotas * price).brlCompact(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Número mágico: $magic cotas — a partir daí os rendimentos de um mês compram uma cota nova.",
            style = MaterialTheme.typography.bodySmall, color = C.Text2,
        )
        Text("Baseado no último rendimento (${lastDividend.brlCota()}). Rendimentos variam.", style = MaterialTheme.typography.labelSmall, color = C.Text3)
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
