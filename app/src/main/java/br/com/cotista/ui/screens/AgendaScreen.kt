package br.com.cotista.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Dividend
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.EmptyState
import br.com.cotista.ui.components.Tag
import br.com.cotista.ui.components.TickerBadge
import br.com.cotista.ui.components.masked
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.brlCota
import br.com.cotista.util.ddMM
import br.com.cotista.util.pct
import br.com.cotista.util.relativeDay
import br.com.cotista.util.weekdayShort
import java.time.LocalDate

@Composable
fun AgendaScreen(state: UiState, contentPadding: PaddingValues, onOpenFund: (String) -> Unit) {
    val mine = state.positions.associateBy { it.ticker }
    var filter by rememberSaveable { mutableIntStateOf(if (mine.isEmpty()) 1 else 0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()

    val base = remember(state.announcements, state.funds, filter, state.positions) {
        val all = state.announcements + state.funds.values.flatMap { it.exactDividends + it.dividends.filter { d -> !d.estimated && d.kind != "Rendimento" } }
        all.distinctBy { "${it.ticker}|${it.kind}|${it.comDate}|${it.value}" }
            .filter { filter == 1 || mine.containsKey(it.ticker) }
    }
    val upcoming = base.filter { it.payDate != null && !it.payDate.isBefore(today) }.sortedBy { it.payDate }
    val paid = base.filter { it.payDate != null && it.payDate.isBefore(today) }.sortedByDescending { it.payDate }.take(200)
    val list = if (tab == 0) upcoming else paid

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Text("Agenda de proventos", style = MaterialTheme.typography.headlineSmall, color = C.Text)
            Spacer(Modifier.height(4.dp))
            Text("Avisos oficiais publicados na B3 pelos administradores", style = MaterialTheme.typography.bodySmall, color = C.Text3)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Minha carteira", filter == 0) { filter = 0 }
                Chip("Todos os FIIs", filter == 1) { filter = 1 }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(4.dp),
            ) {
                Seg("A pagar (${upcoming.size})", tab == 0, Modifier.weight(1f)) { tab = 0 }
                Seg("Pagos", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            state.sync?.let { (d, t) ->
                Spacer(Modifier.height(12.dp))
                Text("Sincronizando avisos da B3: $d de $t", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { if (t > 0) d / t.toFloat() else 0f }, modifier = Modifier.fillMaxWidth(), color = C.Mint, trackColor = C.Surface2)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.EventBusy,
                    if (tab == 0) "Nenhum pagamento previsto" else "Nada por aqui ainda",
                    if (filter == 0 && mine.isEmpty()) "Adicione FIIs na carteira ou veja todos os fundos."
                    else if (state.refreshing) "Buscando avisos de rendimentos na B3…"
                    else "Os fundos costumam anunciar os rendimentos no último dia útil do mês.",
                )
            }
        }
        var lastDate: LocalDate? = null
        list.forEach { d ->
            val date = d.payDate!!
            if (date != lastDate) {
                val header = date
                lastDate = date
                item(key = "h$header$tab") {
                    Row(Modifier.padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(relativeDay(header, today), style = MaterialTheme.typography.titleSmall, color = if (header == today) C.Amber else C.Text)
                        Spacer(Modifier.width(8.dp))
                        Text("${header.weekdayShort()}, ${header.ddMM()}", style = MaterialTheme.typography.labelMedium, color = C.Text3)
                        Spacer(Modifier.weight(1f))
                        val sum = list.filter { it.payDate == header }.sumOf { (mine[it.ticker]?.quantity ?: 0.0) * it.value }
                        if (sum > 0) Text(masked(state.hideValues, sum.brl()), style = MaterialTheme.typography.labelLarge, color = C.Mint)
                    }
                }
            }
            item(key = "d${d.ticker}${d.kind}${d.comDate}${d.value}$tab") {
                AgendaRow(d, state.funds[d.ticker]?.price, mine[d.ticker]?.quantity, state.hideValues) { onOpenFund(d.ticker) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AgendaRow(d: Dividend, price: Double?, qty: Double?, hide: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickerBadge(d.ticker, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
                if (d.kind != "Rendimento") { Spacer(Modifier.width(6.dp)); Tag(d.kind, C.Lilac) }
                if (qty != null) { Spacer(Modifier.width(6.dp)); Tag("na carteira", C.Mint) }
            }
            Text(
                d.comDate?.let { "Data-com ${it.ddMM()}" } ?: "",
                style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(d.value.brlCota(), style = MaterialTheme.typography.titleSmall, color = C.Text)
            when {
                qty != null -> Text(masked(hide, "+" + (qty * d.value).brl()), style = MaterialTheme.typography.labelMedium, color = C.Mint)
                price != null && price > 0 && d.kind == "Rendimento" -> Text("DY ${(d.value / price).pct()}", style = MaterialTheme.typography.labelMedium, color = C.Text2)
            }
        }
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = C.MintDeep, selectedLabelColor = C.Mint,
            containerColor = C.Surface, labelColor = C.Text2,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = C.Stroke, selectedBorderColor = C.Mint.copy(alpha = 0.4f)),
    )
}

@Composable
private fun Seg(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label, style = MaterialTheme.typography.labelLarge,
        color = if (selected) C.Text else C.Text3,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) C.Surface3 else C.Surface)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}
