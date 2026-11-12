package br.com.cotista.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Fund
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.Divider
import br.com.cotista.ui.components.ListRow
import br.com.cotista.ui.components.PillButton
import br.com.cotista.ui.components.ScreenHeader
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.TickerBadge
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.num
import br.com.cotista.util.pct

private val sorts = listOf("Liquidez", "Maior DY", "Menor P/VP", "Maior alta", "A–Z")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(state: UiState, contentPadding: PaddingValues, onOpenFund: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var segment by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf(false) }
    val mine = state.positions.map { it.ticker }.toSet()

    val liquid = remember(state.funds) { state.funds.values.filter { (it.quote?.volume ?: 0.0) > 50_000 } }
    val segments = remember(liquid) { liquid.groupingBy { it.segment }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(12) }
    val list = remember(liquid, query, sort, segment) {
        val q = query.trim()
        liquid.asSequence()
            .filter { segment == null || it.segment == segment }
            .filter { q.isEmpty() || it.ticker.contains(q, true) || it.name.contains(q, true) }
            .let { s ->
                when (sort) {
                    1 -> s.filter { (it.dyLast ?: 0.0) in 0.0..0.04 }.sortedByDescending { it.dyLast ?: 0.0 }
                    2 -> s.filter { it.pvp != null }.sortedBy { it.pvp }
                    3 -> s.sortedByDescending { it.quote?.change ?: -1.0 }
                    4 -> s.sortedBy { it.ticker }
                    else -> s.sortedByDescending { it.quote?.volume ?: 0.0 }
                }
            }.toList()
    }
    val top = remember(liquid) {
        liquid.filter { (it.quote?.volume ?: 0.0) > 1_000_000 && (it.dyLast ?: 0.0) in 0.004..0.025 }.sortedByDescending { it.dyLast }.take(10)
    }
    val filters = (if (sort != 0) 1 else 0) + (if (segment != null) 1 else 0)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item { ScreenHeader("Explorar", "${liquid.size} fundos negociados na B3") }
        item {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Código ou nome do fundo") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = C.Text3) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Limpar") } },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = C.Mint, unfocusedBorderColor = C.Stroke,
                        focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface, cursorColor = C.Mint,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.MintDeep)
                        .border(1.dp, C.Mint.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).clickable { sheet = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Tune, "Filtros e ordenação", tint = C.Mint)
                    if (filters > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp).clip(RoundedCornerShape(50)).background(C.Mint),
                            contentAlignment = Alignment.Center,
                        ) { Text("$filters", style = MaterialTheme.typography.labelSmall, color = C.Bg) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ordenado por ", style = MaterialTheme.typography.labelMedium, color = C.Text3)
                Spacer(Modifier.width(4.dp))
                Text(
                    sorts[sort], style = MaterialTheme.typography.labelMedium, color = C.Mint,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(C.MintDeep).clickable { sheet = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                )
                segment?.let { s ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$s  ✕", style = MaterialTheme.typography.labelMedium, color = C.Sky,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(C.Sky.copy(alpha = 0.12f)).clickable { segment = null }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        if (top.isNotEmpty() && query.isBlank()) {
            item {
                SectionTitle("Maiores dividend yields")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(top, key = { "top" + it.ticker }) { f ->
                        Column(
                            Modifier.width(148.dp).clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(C.forKey(f.ticker.take(4)).copy(alpha = 0.20f), C.Surface)))
                                .border(1.dp, C.borderBrush, RoundedCornerShape(22.dp))
                                .clickable { onOpenFund(f.ticker) }.padding(14.dp),
                        ) {
                            Text(f.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
                            Text(f.segment, style = MaterialTheme.typography.labelSmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(12.dp))
                            Text(f.dyLast?.pct() ?: "—", style = MaterialTheme.typography.headlineSmall, color = C.Mint)
                            Text("ao mês · ${f.price?.brl() ?: ""}", style = MaterialTheme.typography.labelMedium, color = C.Text2, maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("FUNDO · PREÇO", style = MaterialTheme.typography.labelSmall, color = C.Text3, modifier = Modifier.weight(1f))
                Text("DY MÊS · P/VP", style = MaterialTheme.typography.labelSmall, color = C.Text3)
            }
        }
        if (state.funds.isEmpty()) {
            item { Text(if (state.refreshing) "Carregando fundos da B3…" else "Sem dados no momento. Puxe para atualizar.", color = C.Text3, modifier = Modifier.padding(16.dp)) }
        } else if (list.isEmpty()) {
            item { Text("Nenhum fundo encontrado.", color = C.Text3, modifier = Modifier.padding(16.dp)) }
        }
        itemsIndexed(list, key = { _, f -> f.ticker }) { i, f ->
            val first = i == 0
            val last = i == list.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (first) 22.dp else 0.dp, topEnd = if (first) 22.dp else 0.dp,
                bottomStart = if (last) 22.dp else 0.dp, bottomEnd = if (last) 22.dp else 0.dp,
            )
            Column(Modifier.fillMaxWidth().clip(shape).background(C.Surface).padding(vertical = if (first || last) 2.dp else 0.dp)) {
                if (!first) Divider(Modifier.padding(horizontal = 14.dp))
                FundRow(f, f.ticker in mine) { onOpenFund(f.ticker) }
            }
        }
    }

    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = C.Surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("Ordenar por", style = MaterialTheme.typography.titleMedium, color = C.Text)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sorts.forEachIndexed { i, s -> Chip(s, sort == i) { sort = i } }
                }
                Spacer(Modifier.height(20.dp))
                Text("Segmento", style = MaterialTheme.typography.titleMedium, color = C.Text)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Todos", segment == null) { segment = null }
                    segments.forEach { s -> Chip(s, segment == s) { segment = if (segment == s) null else s } }
                }
                Spacer(Modifier.height(24.dp))
                PillButton("Ver ${list.size} fundos", null, { sheet = false }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun FundRow(f: Fund, inPortfolio: Boolean = false, onClick: () -> Unit) {
    val change = f.quote?.change
    ListRow(
        ticker = f.ticker, title = f.ticker,
        tag = if (inPortfolio) "NA CARTEIRA" else null,
        subtitle = (f.price?.brl() ?: "") + (change?.let { "  " + it.pct(2, true) } ?: ""),
        value = f.dyLast?.pct() ?: "—", valueColor = C.Mint,
        valueSub = f.pvp?.num(2) ?: "—",
        valueSubColor = when { f.pvp == null -> C.Text3; f.pvp!! < 0.95 -> C.Mint; f.pvp!! > 1.05 -> C.Amber; else -> C.Text2 },
        onClick = onClick,
    )
}
