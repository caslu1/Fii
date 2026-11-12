package br.com.cotista.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Dividend
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.EmptyState
import br.com.cotista.ui.components.ListPanel
import br.com.cotista.ui.components.ListRow
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.ScreenHeader
import br.com.cotista.ui.components.SegmentedTabs
import br.com.cotista.ui.components.masked
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.brlCota
import br.com.cotista.util.ddMM
import br.com.cotista.util.monthYear
import br.com.cotista.util.pct
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun AgendaScreen(state: UiState, contentPadding: PaddingValues, onOpenFund: (String) -> Unit) {
    val mine = state.positions.associateBy { it.ticker }
    var filter by rememberSaveable { mutableIntStateOf(if (mine.isEmpty()) 1 else 0) }
    val today = LocalDate.now()
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthStr)
    var selectedStr by rememberSaveable { mutableStateOf<String?>(null) }

    val all = remember(state.announcements, state.funds, filter, state.positions) {
        val list = state.announcements + state.funds.values.flatMap { it.exactDividends + it.dividends.filter { d -> !d.estimated && d.kind != "Rendimento" } }
        list.filter { it.payDate != null }
            .distinctBy { "${it.ticker}|${it.kind}|${it.comDate}|${it.value}" }
            .filter { filter == 1 || mine.containsKey(it.ticker) }
    }
    val inMonth = all.filter { YearMonth.from(it.payDate!!) == month }
    val byDay = inMonth.groupBy { it.payDate!!.dayOfMonth }
    fun amount(d: Dividend) = (mine[d.ticker]?.quantity ?: 0.0) * d.value
    val monthTotal = inMonth.sumOf { amount(it) }

    // dia selecionado: o escolhido, senão o próximo pagamento do mês, senão o último
    val selected: LocalDate? = selectedStr?.let(LocalDate::parse)?.takeIf { YearMonth.from(it) == month }
        ?: byDay.keys.sorted().map { month.atDay(it) }.let { days -> days.firstOrNull { !it.isBefore(today) } ?: days.lastOrNull() }
    val dayItems = selected?.let { s -> inMonth.filter { it.payDate == s }.sortedByDescending { amount(it) } }.orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Agenda", "Avisos oficiais publicados na B3") }
        item { SegmentedTabs(listOf("Minha carteira", "Todos os FIIs"), filter, { filter = it; selectedStr = null }) }

        state.sync?.let { (d, t) ->
            item {
                Column {
                    Text("Sincronizando avisos da B3: $d de $t", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { if (t > 0) d / t.toFloat() else 0f }, modifier = Modifier.fillMaxWidth(), color = C.Mint, trackColor = C.Surface2)
                }
            }
        }

        item {
            Panel(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NavButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior") { monthStr = month.minusMonths(1).toString(); selectedStr = null }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(month.atDay(1).monthYear(), style = MaterialTheme.typography.titleMedium, color = C.Text)
                        Text(
                            when {
                                filter == 0 && monthTotal > 0 -> masked(state.hideValues, monthTotal.brl()) + " no mês"
                                inMonth.isNotEmpty() -> "${inMonth.size} pagamentos"
                                else -> "Sem pagamentos"
                            },
                            style = MaterialTheme.typography.labelMedium, color = if (inMonth.isNotEmpty()) C.Mint else C.Text3,
                        )
                    }
                    NavButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo mês") { monthStr = month.plusMonths(1).toString(); selectedStr = null }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = C.Text3)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val offset = month.atDay(1).dayOfWeek.value % 7   // domingo = 0
                val cells = offset + month.lengthOfMonth()
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 7) {
                            val day = r * 7 + c - offset + 1
                            Box(Modifier.weight(1f).height(44.dp)) {
                                if (day in 1..month.lengthOfMonth()) {
                                    val date = month.atDay(day)
                                    DayCell(
                                        day = day,
                                        weekend = c == 0 || c == 6,
                                        isToday = date == today,
                                        isSelected = date == selected,
                                        dots = byDay[day]?.size ?: 0,
                                        paid = date.isBefore(today),
                                    ) { selectedStr = date.toString() }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(C.Mint, "Pago")
                    Legend(C.Amber, "A pagar")
                }
            }
        }

        if (selected == null || dayItems.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.EventBusy, "Nenhum pagamento neste mês",
                    if (filter == 0 && mine.isEmpty()) "Adicione FIIs na carteira ou veja todos os fundos."
                    else if (state.refreshing) "Buscando avisos de rendimentos na B3…"
                    else "Os fundos costumam anunciar os rendimentos no último dia útil do mês.",
                )
            }
        } else {
            item {
                val total = dayItems.sumOf { amount(it) }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        selected.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", br.com.cotista.util.PT_BR)).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium, color = C.Text, modifier = Modifier.weight(1f),
                    )
                    if (total > 0) Text(masked(state.hideValues, "+" + total.brl()), style = MaterialTheme.typography.titleSmall, color = C.Mint)
                }
            }
            item {
                ListPanel(dayItems) { d ->
                    val qty = mine[d.ticker]?.quantity
                    val price = state.funds[d.ticker]?.price
                    ListRow(
                        ticker = d.ticker, title = d.ticker,
                        tag = if (d.kind != "Rendimento") d.kind else null,
                        subtitle = "${d.value.brlCota()} por cota" + (d.comDate?.let { " · data-com ${it.ddMM()}" } ?: ""),
                        value = when {
                            qty != null -> masked(state.hideValues, "+" + (qty * d.value).brl())
                            price != null && price > 0 && d.kind == "Rendimento" -> "DY ${(d.value / price).pct()}"
                            else -> d.value.brlCota()
                        },
                        valueColor = C.Mint,
                        onClick = { onOpenFund(d.ticker) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, weekend: Boolean, isToday: Boolean, isSelected: Boolean, dots: Int, paid: Boolean, onClick: () -> Unit) {
    val has = dots > 0
    val dotColor = if (paid) C.Mint else C.Amber
    val shape = RoundedCornerShape(12.dp)
    var m = Modifier.fillMaxSize().clip(shape)
    m = when {
        isSelected && has -> m.background(dotColor)
        has -> m.background(dotColor.copy(alpha = 0.10f))
        else -> m
    }
    if (isToday) m = m.border(1.5.dp, C.Text, shape)
    m = m.clickable(enabled = has, onClick = onClick)
    Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            "$day", style = MaterialTheme.typography.labelLarge,
            color = when {
                isSelected && has -> C.Bg
                has || isToday -> C.Text
                weekend -> C.Text3.copy(alpha = 0.7f)
                else -> C.Text2
            },
        )
        if (has) {
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(minOf(dots, 3)) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(if (isSelected) C.Bg else dotColor))
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = C.Text2) }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = C.Text3)
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label, style = MaterialTheme.typography.labelLarge,
        color = if (selected) C.Mint else C.Text2,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) C.MintDeep else C.Surface)
            .border(1.dp, if (selected) C.Mint.copy(alpha = 0.4f) else C.Stroke, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
