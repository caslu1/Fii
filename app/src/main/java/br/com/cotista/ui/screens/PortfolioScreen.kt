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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import br.com.cotista.ui.components.Divider
import br.com.cotista.ui.components.Donut
import br.com.cotista.ui.components.Dot
import br.com.cotista.ui.components.ListPanel
import br.com.cotista.ui.components.ListRow
import br.com.cotista.ui.components.PillButton
import br.com.cotista.ui.components.ScreenHeader
import br.com.cotista.ui.components.SectionTitle
import br.com.cotista.ui.components.SegmentedTabs
import br.com.cotista.util.ddMM
import br.com.cotista.util.relativeDay
import java.time.LocalDate
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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Carteira") {
                if (state.positions.isNotEmpty()) PillButton("Adicionar", Icons.Rounded.Add, { onAdd(null) })
            }
        }
        if (state.positions.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Wallet, "Sua carteira está vazia", "Adicione os FIIs que você tem para acompanhar proventos, preço médio e resultado.") {
                    PillButton("Adicionar FII", Icons.Rounded.Add, { onAdd(null) })
                }
            }
            return@LazyColumn
        }

        // Resumo
        item {
            Panel {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("PATRIMÔNIO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                        Text(masked(hide, summary.value.brl()), style = MaterialTheme.typography.headlineMedium, color = C.Text)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(masked(hide, (if (summary.result >= 0) "+" else "") + summary.result.brl()), style = MaterialTheme.typography.titleSmall, color = C.signed(summary.result))
                        Text(summary.resultPct.pct(2, true), style = MaterialTheme.typography.labelMedium, color = C.signed(summary.result))
                    }
                }
                if (summary.value > 0) {
                    Spacer(Modifier.height(14.dp))
                    SegmentBar(summary.holdings.mapIndexed { i, h -> C.palette[i % C.palette.size] to h.value })
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    Stat("Investido", masked(hide, summary.invested.brl()), Modifier.weight(1f))
                    Stat("Renda/mês", masked(hide, summary.projectedMonthly.brl()), Modifier.weight(1f), C.Mint)
                    Stat("Yield on cost", summary.yieldOnCost.pct(), Modifier.weight(1f))
                }
            }
        }

        item { SegmentedTabs(listOf("Posições", "Proventos", "Alocação"), tab, { tab = it }) }

        when (tab) {
            0 -> {
                item {
                    Row(Modifier.padding(horizontal = 4.dp)) {
                        Text("FUNDO", style = MaterialTheme.typography.labelSmall, color = C.Text3, modifier = Modifier.weight(1f))
                        Text("SALDO · RESULTADO", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                    }
                }
                item {
                    ListPanel(summary.holdings) { h ->
                        val p = h.position
                        val monthly = (h.fund?.lastDividend?.value ?: 0.0) * p.quantity
                        ListRow(
                            ticker = p.ticker, title = p.ticker,
                            subtitle = "${p.quantity.qty()} cotas" + if (monthly > 0) " · ${masked(hide, monthly.brl())}/mês" else "",
                            value = masked(hide, h.value.brl()),
                            valueSub = h.resultPct.pct(2, true), valueSubColor = C.signed(h.result),
                            onClick = { onOpenFund(p.ticker) },
                            onLongClick = { menuFor = p.ticker },
                            trailing = {
                                Box {
                                    DropdownMenu(expanded = menuFor == p.ticker, onDismissRequest = { menuFor = null }) {
                                        DropdownMenuItem(text = { Text("Editar") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menuFor = null; onAdd(p.ticker) })
                                        DropdownMenuItem(text = { Text("Remover") }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) }, onClick = { menuFor = null; onRemove(p.ticker) })
                                    }
                                }
                            },
                        )
                    }
                }
                item {
                    Text(
                        "Toque num fundo para ver proventos, balanço e relatórios. Segure para editar ou remover.",
                        style = MaterialTheme.typography.bodySmall, color = C.Text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            1 -> {
                item {
                    Row {
                        Stat("Este mês", masked(hide, summary.thisMonth.brl()), Modifier.weight(1f), C.Mint)
                        Stat("12 meses", masked(hide, summary.last12.brl()), Modifier.weight(1f))
                        Stat("Média/mês", masked(hide, summary.avgMonthly.brl()), Modifier.weight(1f))
                    }
                }
                if (summary.upcoming.isNotEmpty()) {
                    item { SectionTitle("A receber") }
                    item {
                        ListPanel(summary.upcoming) { pay ->
                            val d = pay.dividend
                            ListRow(
                                ticker = d.ticker, title = d.ticker,
                                subtitle = "${relativeDay(d.payDate!!, today)} · ${d.value.brlCota()} por cota",
                                value = masked(hide, "+" + pay.amount.brl()), valueColor = C.Mint,
                                onClick = { onOpenFund(d.ticker) },
                            )
                        }
                    }
                }
                if (summary.recentPaid.isNotEmpty()) {
                    item { SectionTitle("Recebidos recentemente") }
                    item {
                        ListPanel(summary.recentPaid) { pay ->
                            val d = pay.dividend
                            ListRow(
                                ticker = d.ticker, title = d.ticker,
                                subtitle = "Pago em ${d.payDate!!.ddMM()} · ${d.value.brlCota()} por cota",
                                value = masked(hide, pay.amount.brl()),
                                onClick = { onOpenFund(d.ticker) },
                            )
                        }
                    }
                }
                item { SectionTitle("Por fundo nos últimos 12 meses") }
                item {
                    val rows = summary.holdings.map { it to summary.incomeFor(it.position.ticker) }.sortedByDescending { it.second }
                    ListPanel(rows) { (h, v) ->
                        ListRow(
                            ticker = h.position.ticker, title = h.position.ticker,
                            subtitle = if (summary.last12 > 0) "${(v / summary.last12).pct(1)} da sua renda" else "",
                            value = masked(hide, v.brl()), valueColor = C.Text,
                            valueSub = if (h.invested > 0) "YoC ${(v / h.invested).pct()}" else null,
                            onClick = { onOpenFund(h.position.ticker) },
                        )
                    }
                }
            }
            else -> {
                item {
                    Panel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Donut(summary.holdings.mapIndexed { i, h -> C.palette[i % C.palette.size] to h.value }, size = 128.dp, thickness = 15.dp) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${summary.holdings.size}", style = MaterialTheme.typography.headlineSmall, color = C.Text)
                                    Text(if (summary.holdings.size == 1) "fundo" else "fundos", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                summary.holdings.take(7).forEachIndexed { i, h ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Dot(C.palette[i % C.palette.size])
                                        Spacer(Modifier.width(8.dp))
                                        Text(h.position.ticker, style = MaterialTheme.typography.bodySmall, color = C.Text2, modifier = Modifier.weight(1f))
                                        Text(if (summary.value > 0) (h.value / summary.value).pct(1) else "—", style = MaterialTheme.typography.labelMedium, color = C.Text)
                                    }
                                }
                            }
                        }
                    }
                }
                item { SectionTitle("Por segmento") }
                item {
                    Panel {
                        SegmentBar(summary.bySegment.mapIndexed { i, (_, v) -> C.palette[(i + 3) % C.palette.size] to v })
                        summary.bySegment.forEachIndexed { i, (seg, v) ->
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Dot(C.palette[(i + 3) % C.palette.size])
                                Spacer(Modifier.width(8.dp))
                                Text(seg, style = MaterialTheme.typography.bodyMedium, color = C.Text2, modifier = Modifier.weight(1f))
                                Text(masked(hide, v.brl()), style = MaterialTheme.typography.labelLarge, color = C.Text)
                                Spacer(Modifier.width(8.dp))
                                Text(if (summary.value > 0) (v / summary.value).pct(1) else "—", style = MaterialTheme.typography.labelMedium, color = C.Text3,
                                    modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
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
