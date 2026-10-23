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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.cotista.data.Fund
import br.com.cotista.data.Position
import br.com.cotista.ui.Holding
import br.com.cotista.ui.PortfolioSummary
import br.com.cotista.ui.UiState
import br.com.cotista.ui.components.ChangePill
import br.com.cotista.ui.components.EmptyState
import br.com.cotista.ui.components.Panel
import br.com.cotista.ui.components.SegmentBar
import br.com.cotista.ui.components.Stat
import br.com.cotista.ui.components.TickerBadge
import br.com.cotista.ui.components.masked
import br.com.cotista.ui.theme.C
import br.com.cotista.util.brl
import br.com.cotista.util.brlCota
import br.com.cotista.util.num
import br.com.cotista.util.parseDecimal
import br.com.cotista.util.pct
import br.com.cotista.util.qty

@Composable
fun PortfolioScreen(
    state: UiState,
    contentPadding: PaddingValues,
    onOpenFund: (String) -> Unit,
    onAdd: (String?) -> Unit,
    onRemove: (String) -> Unit,
) {
    val summary = remember(state.positions, state.funds) { PortfolioSummary(state.positions, state.funds) }
    val hide = state.hideValues

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Carteira", style = MaterialTheme.typography.headlineLarge, color = C.Text)
            Spacer(Modifier.height(8.dp))
        }
        if (state.positions.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Wallet, "Sua carteira está vazia", "Adicione os FIIs que você tem para acompanhar proventos, preço médio e resultado.") {
                    Button(onClick = { onAdd(null) }, colors = ButtonDefaults.buttonColors(containerColor = C.Mint, contentColor = C.Bg), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Adicionar FII")
                    }
                }
            }
            return@LazyColumn
        }
        item {
            Panel {
                Row {
                    Stat("Patrimônio", masked(hide, summary.value.brl()), Modifier.weight(1f))
                    Stat("Investido", masked(hide, summary.invested.brl()), Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    Stat("Resultado", masked(hide, summary.result.brl()), Modifier.weight(1f), C.signed(summary.result), summary.resultPct.pct(2, true))
                    Stat("Renda 12m", masked(hide, summary.last12.brl()), Modifier.weight(1f), C.Mint, "YoC ${summary.yieldOnCost.pct()}")
                }
                if (summary.value > 0) {
                    Spacer(Modifier.height(16.dp))
                    SegmentBar(summary.holdings.mapIndexed { i, h -> C.palette[i % C.palette.size] to h.value })
                }
            }
        }
        items(summary.holdings, key = { it.position.ticker }) { h ->
            HoldingCard(h, summary.value, hide, summary.incomeFor(h.position.ticker), { onOpenFund(h.position.ticker) }, { onAdd(h.position.ticker) }, { onRemove(h.position.ticker) })
        }
    }
}

@Composable
private fun HoldingCard(h: Holding, total: Double, hide: Boolean, income12: Double, onClick: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val p = h.position
    Panel(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TickerBadge(p.ticker)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.ticker, style = MaterialTheme.typography.titleMedium, color = C.Text)
                    Spacer(Modifier.width(8.dp))
                    ChangePill(h.fund?.quote?.change)
                }
                Text(h.fund?.name ?: "", style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Opções", tint = C.Text3) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Editar") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Remover") }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) }, onClick = { menu = false; onRemove() })
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Stat("Cotas", p.quantity.qty(), Modifier.weight(1f))
            Stat("Preço médio", p.avgPrice.brl(), Modifier.weight(1f))
            Stat("Atual", h.price.brl(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Stat("Saldo", masked(hide, h.value.brl()), Modifier.weight(1f), hint = if (total > 0) "${(h.value / total).pct(1)} da carteira" else null)
            Stat("Resultado", masked(hide, h.result.brl()), Modifier.weight(1f), C.signed(h.result), h.resultPct.pct(2, true))
            val last = h.fund?.lastDividend
            Stat("Últ. provento", last?.value?.brlCota() ?: "—", Modifier.weight(1f), C.Mint,
                if (hide) null else "12m ${income12.brl()}")
        }
    }
}

// ---------------------------------------------------------------- folha de adicionar/editar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPositionSheet(
    funds: List<Fund>,
    initialTicker: String?,
    existing: Position?,
    onDismiss: () -> Unit,
    onSave: (Position) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf(initialTicker ?: "") }
    var selected by remember { mutableStateOf(funds.firstOrNull { it.ticker == initialTicker }) }
    var qty by remember { mutableStateOf(existing?.quantity?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString().replace('.', ',') } ?: "") }
    var price by remember { mutableStateOf(existing?.avgPrice?.num(2) ?: selected?.price?.num(2) ?: "") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = C.Mint, unfocusedBorderColor = C.Stroke, focusedContainerColor = C.Surface2,
        unfocusedContainerColor = C.Surface2, cursorColor = C.Mint, focusedLabelColor = C.Mint,
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = C.Surface, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().imePadding()) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(C.Surface3))
            Spacer(Modifier.height(16.dp))
            Text(if (existing != null) "Editar ${existing.ticker}" else "Adicionar FII", style = MaterialTheme.typography.titleLarge, color = C.Text)
            Spacer(Modifier.height(16.dp))

            val sel = selected
            if (sel == null) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it.uppercase() },
                    label = { Text("Código ou nome do fundo") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = fieldColors, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                val q = query.trim()
                val results = remember(q, funds) {
                    if (q.isEmpty()) funds.take(8)
                    else funds.filter { it.ticker.contains(q, true) || it.name.contains(q, true) }
                        .sortedBy { if (it.ticker.startsWith(q, true)) 0 else 1 }.take(8)
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(results, key = { it.ticker }) { f ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                selected = f; query = f.ticker
                                if (price.isBlank()) price = f.price?.num(2) ?: ""
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TickerBadge(f.ticker, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
                                Text(f.name, style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(f.price?.brl() ?: "", style = MaterialTheme.typography.bodyMedium, color = C.Text2)
                        }
                    }
                }
                if (funds.isEmpty()) Text("Carregando lista de fundos da B3…", color = C.Text3, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Surface2)
                        .clickable(enabled = existing == null) { selected = null }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TickerBadge(sel.ticker, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sel.ticker, style = MaterialTheme.typography.titleSmall, color = C.Text)
                        Text(sel.name, style = MaterialTheme.typography.bodySmall, color = C.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(sel.price?.brl() ?: "—", style = MaterialTheme.typography.titleSmall, color = C.Text)
                        if (existing == null) Text("trocar", style = MaterialTheme.typography.labelSmall, color = C.Mint)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qty, onValueChange = { qty = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text("Quantidade de cotas") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text("Preço médio (R$)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                    )
                }
                val q = parseDecimal(qty)
                val pm = parseDecimal(price)
                Spacer(Modifier.height(12.dp))
                if (q != null && pm != null && q > 0) {
                    val last = sel.lastDividend?.value
                    Text(
                        "Total investido ${(q * pm).brl()}" + (last?.let { " · renda estimada ${(it * q).brl()}/mês" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = C.Text2,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { if (q != null && pm != null && q > 0 && pm > 0) onSave(Position(sel.ticker, q, pm)) },
                    enabled = q != null && pm != null && q > 0 && pm > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = C.Mint, contentColor = C.Bg),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(if (existing != null) "Salvar alterações" else "Adicionar à carteira") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
