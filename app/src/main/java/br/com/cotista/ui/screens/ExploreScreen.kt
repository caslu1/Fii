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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Fund
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.TickerBadge
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.brlCompact
import br.com.cotista.util.num
import br.com.cotista.util.pct

private val sorts = listOf("Liquidez", "Maior DY", "Menor P/VP", "Maior alta", "A–Z")

@Composable
fun ExploreScreen(state: UiState, contentPadding: PaddingValues, onOpenFund: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var segment by rememberSaveable { mutableStateOf<String?>(null) }

    val liquid = remember(state.funds) { state.funds.values.filter { (it.quote?.volume ?: 0.0) > 50_000 } }
    val segments = remember(liquid) { liquid.groupingBy { it.segment }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(10) }
    val list = remember(liquid, query, sort, segment) {
        val q = query.trim()
        liquid.asSequence()
            .filter { segment == null || it.segment == segment }
            .filter { q.isEmpty() || it.ticker.contains(q, true) || it.name.contains(q, true) }
            .let { s ->
                when (sort) {
                    1 -> s.filter { (it.dyLast ?: 0.0) in 0.0..0.04 }.sortedByDescending { it.dyLast ?: 0.0 }
                    2 -> s.filter { (it.pvp ?: 0.0) > 0.2 }.sortedBy { it.pvp ?: 99.0 }
                    3 -> s.sortedByDescending { it.quote?.change ?: -1.0 }
                    4 -> s.sortedBy { it.ticker }
                    else -> s.sortedByDescending { it.quote?.volume ?: 0.0 }
                }
            }.toList()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Text("Explorar FIIs", style = MaterialTheme.typography.headlineSmall, color = C.Text)
            Spacer(Modifier.height(4.dp))
            Text("${liquid.size} fundos negociados na B3", style = MaterialTheme.typography.bodySmall, color = C.Text3)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Buscar por código ou nome") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = C.Text3) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Limpar") } },
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Mint, unfocusedBorderColor = C.Stroke,
                    focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface, cursorColor = C.Mint,
                ),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorts.size) { i -> Chip(sorts[i], sort == i) { sort = i } }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("Todos", segment == null) { segment = null } }
                items(segments) { s -> Chip(s, segment == s) { segment = if (segment == s) null else s } }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 14.dp)) {
                Text("FUNDO", style = MaterialTheme.typography.labelSmall, color = C.Text3, modifier = Modifier.weight(1f))
                Text("ÚLT. DY · P/VP", style = MaterialTheme.typography.labelSmall, color = C.Text3)
            }
            Spacer(Modifier.height(6.dp))
        }
        if (state.funds.isEmpty()) {
            item { Text(if (state.refreshing) "Carregando fundos da B3…" else "Sem dados no momento. Puxe para atualizar.", color = C.Text3, modifier = Modifier.padding(16.dp)) }
        }
        items(list, key = { it.ticker }) { f ->
            FundRow(f) { onOpenFund(f.ticker) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun FundRow(f: Fund, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickerBadge(f.ticker, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
                Spacer(Modifier.width(8.dp))
                Text(f.price?.brl() ?: "", style = MaterialTheme.typography.bodySmall, color = C.Text2)
                f.quote?.change?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(it.pct(2, true), style = MaterialTheme.typography.labelSmall, color = C.signed(it))
                }
            }
            Text(
                "${f.segment} · ${(f.quote?.volume ?: 0.0).brlCompact()}/dia",
                style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(f.dyLast?.pct() ?: "—", style = MaterialTheme.typography.titleSmall, color = C.Mint)
            val pvp = f.pvp
            Text(
                pvp?.let { "P/VP ${it.num(2)}" } ?: "P/VP —", style = MaterialTheme.typography.labelMedium,
                color = when { pvp == null -> C.Text3; pvp < 0.95 -> C.Mint; pvp > 1.05 -> C.Amber; else -> C.Text2 },
            )
        }
    }
}
