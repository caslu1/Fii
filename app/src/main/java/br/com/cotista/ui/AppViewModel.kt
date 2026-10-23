package br.com.cotista.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.cotista.data.CvmInforme
import br.com.cotista.data.Dividend
import br.com.cotista.data.Fund
import br.com.cotista.data.PortfolioStore
import br.com.cotista.data.Position
import br.com.cotista.data.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val ready: Boolean = false,
    val refreshing: Boolean = false,
    val status: String = "Preparando…",
    val warnings: List<String> = emptyList(),
    val quoteDate: LocalDate? = null,
    val cvmRef: LocalDate? = null,
    val funds: Map<String, Fund> = emptyMap(),
    val positions: List<Position> = emptyList(),
    val announcements: List<Dividend> = emptyList(),
    val sync: Pair<Int, Int>? = null,
    val hideValues: Boolean = false,
    val loadingFund: String? = null,
    val manualRefresh: Boolean = false,
) {
    val fundList: List<Fund> get() = funds.values.sortedByDescending { it.quote?.volume ?: 0.0 }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val store = PortfolioStore(app)

    private val _state = MutableStateFlow(UiState(positions = store.load(), hideValues = store.hideValues))
    val state = _state.asStateFlow()

    private var quotes: Repository.QuotesResult? = null
    private var cvm: CvmInforme.Data? = null
    private val fnet = HashMap<String, Dividend>()   // chave de deduplicação -> provento
    private var job: Job? = null
    private val loadedFunds = HashSet<String>()

    init { refresh(false) }

    fun refresh(force: Boolean) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _state.update { it.copy(refreshing = true, manualRefresh = force, warnings = emptyList(), status = "Baixando cotações da B3 e informes da CVM…") }
            val warnings = mutableListOf<String>()
            val qd = async { runCatching { repo.quotes(force) } }
            val cd = async { runCatching { repo.cvm(force) } }
            qd.await().onSuccess { quotes = it }.onFailure { warnings += "Cotações da B3 indisponíveis agora" }
            cd.await().onSuccess { cvm = it }.onFailure { warnings += "Informes da CVM indisponíveis agora" }
            rebuild()
            _state.update { it.copy(ready = true, manualRefresh = false, status = "Buscando avisos de rendimentos…", warnings = warnings.toList()) }

            // 1) histórico completo dos fundos da carteira
            if (force) loadedFunds.clear()
            syncPortfolio(force)

            // 2) agenda geral de avisos recentes (em etapas)
            for (count in listOf(120, 450)) {
                val r = runCatching {
                    repo.recentAnnouncements(count, force) { done, total -> _state.update { it.copy(sync = done to total) } }
                }
                r.onSuccess { merge(it); rebuild() }
                    .onFailure {
                        if (!warnings.contains(FNET_WARN)) warnings += FNET_WARN
                        _state.update { s -> s.copy(warnings = warnings.toList()) }
                    }
                if (r.isFailure) break
            }
            _state.update { it.copy(refreshing = false, sync = null, status = "") }
        }
    }

    private suspend fun syncPortfolio(force: Boolean) {
        val tickers = _state.value.positions.map { it.ticker }.filter { it !in loadedFunds }
        for (t in tickers) loadFundHistory(t, force)
    }

    /** Carrega histórico de avisos FNET de um fundo (ao abrir o detalhe ou adicionar na carteira). */
    fun ensureFund(ticker: String) {
        if (ticker in loadedFunds) return
        viewModelScope.launch { loadFundHistory(ticker, false) }
    }

    private suspend fun loadFundHistory(ticker: String, force: Boolean) {
        val cnpj = _state.value.funds[ticker]?.cvm?.cnpj ?: return
        _state.update { it.copy(loadingFund = ticker) }
        runCatching { repo.fundAnnouncements(cnpj, 30, force) }.onSuccess {
            merge(it); loadedFunds += ticker; rebuild()
        }
        _state.update { it.copy(loadingFund = null) }
    }

    private fun merge(list: List<Dividend>) {
        val isinToTicker = quotes?.quotes?.values?.associate { it.isin to it.ticker } ?: emptyMap()
        synchronized(fnet) {
            for (raw in list) {
                val d = if (raw.ticker.length == 12 && raw.ticker.startsWith("BR")) {
                    val t = isinToTicker[raw.ticker] ?: continue
                    raw.copy(ticker = t)
                } else raw
                val key = "${d.ticker}|${d.kind}|${d.comDate ?: d.payDate}"
                val old = fnet[key]
                if (old == null || (d.docId ?: 0) > (old.docId ?: 0)) fnet[key] = d
            }
        }
    }

    private fun rebuild() {
        val q = quotes
        val c = cvm
        val all: List<Dividend> = synchronized(fnet) { fnet.values.toList() }
        val byTicker = all.groupBy { it.ticker }
        val funds = HashMap<String, Fund>()
        q?.quotes?.values?.forEach { quote ->
            val cnpj = c?.isinToCnpj?.get(quote.isin)
            val cvmFund = cnpj?.let { c?.latest?.get(it) }
            val history = cnpj?.let { c?.history?.get(it) }.orEmpty()
            val estimated = history.mapNotNull { p ->
                val dy = p.dyMonth ?: return@mapNotNull null
                val vp = p.vpCota ?: return@mapNotNull null
                if (dy <= 0 || vp <= 0) return@mapNotNull null
                Dividend(quote.ticker, dy * vp, null, p.ref.plusMonths(1).withDayOfMonth(15), estimated = true)
            }
            funds[quote.ticker] = Fund(
                ticker = quote.ticker, quote = quote, cvm = cvmFund, history = history,
                dividends = byTicker[quote.ticker].orEmpty() + estimated,
            )
        }
        val announcements = all.filter { funds.containsKey(it.ticker) }
            .sortedByDescending { it.comDate ?: it.payDate }
        _state.update {
            it.copy(funds = funds, announcements = announcements, quoteDate = q?.date, cvmRef = c?.refDate)
        }
    }

    // ------------------------------------------------------------------ carteira

    fun savePosition(p: Position) {
        val list = _state.value.positions.filter { it.ticker != p.ticker } + p
        store.save(list)
        _state.update { it.copy(positions = list) }
        ensureFund(p.ticker)
    }

    fun removePosition(ticker: String) {
        val list = _state.value.positions.filter { it.ticker != ticker }
        store.save(list)
        _state.update { it.copy(positions = list) }
    }

    fun toggleHide() {
        val v = !_state.value.hideValues
        store.hideValues = v
        _state.update { it.copy(hideValues = v) }
    }

    companion object {
        const val FNET_WARN = "Avisos de rendimentos da B3 indisponíveis agora — mostrando estimativas da CVM"
    }
}
