package br.com.cotista.data

import java.time.LocalDate

/** Cotação do pregão (arquivo COTAHIST da B3). */
data class Quote(
    val ticker: String,
    val isin: String,
    val shortName: String,
    val price: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val prevClose: Double?,
    val volume: Double,
    val trades: Int,
) {
    val change: Double? get() = prevClose?.takeIf { it > 0 }?.let { price / it - 1 }
}

/** Dados do informe mensal enviado à CVM. */
data class CvmFund(
    val cnpj: String,
    val isin: String,
    val name: String,
    val segment: String,
    val mandate: String,
    val management: String,
    val admin: String,
    val refDate: LocalDate,
    val vpCota: Double?,
    val dyMonth: Double?,
    val holders: Int?,
    val netWorth: Double?,
    val shares: Double?,
)

data class CvmPoint(val ref: LocalDate, val dyMonth: Double?, val vpCota: Double?)

/** Um provento por cota. */
data class Dividend(
    val ticker: String,
    val value: Double,
    val comDate: LocalDate?,
    val payDate: LocalDate?,
    val kind: String = "Rendimento",
    val estimated: Boolean = false,
    val docId: Long? = null,
    val fundName: String = "",
) {
    /** Data usada para agrupar por mês. */
    val refDate: LocalDate? get() = payDate ?: comDate
}

data class Position(val ticker: String, val quantity: Double, val avgPrice: Double)

/** FII consolidado: cotação + informe CVM + proventos. */
data class Fund(
    val ticker: String,
    val quote: Quote?,
    val cvm: CvmFund?,
    val history: List<CvmPoint>,
    val dividends: List<Dividend>,
    val balances: List<Balance> = emptyList(),
) {
    val name: String get() = cvm?.name?.takeIf { it.isNotBlank() }?.let(::prettyName) ?: quote?.shortName ?: ticker
    val price: Double? get() = quote?.price
    val pvp: Double? get() {
        val p = price ?: return null
        val vp = cvm?.vpCota ?: return null
        val r = if (vp > 0) p / vp else return null
        return if (r in 0.2..5.0) r else null   // descarta informes com VP inconsistente
    }
    val segment: String get() = cvm?.segment?.takeIf { it.isNotBlank() }?.let(::prettySegment) ?: "Outros"

    val exactDividends: List<Dividend> get() = dividends.filter { !it.estimated && it.kind == "Rendimento" }

    /** Proventos usados nos cálculos: exatos (FNET) quando existem, senão estimados (CVM). */
    val effectiveDividends: List<Dividend>
        get() = exactDividends.ifEmpty { dividends.filter { it.estimated } }

    val lastDividend: Dividend? get() = effectiveDividends.maxByOrNull { it.comDate ?: it.payDate ?: LocalDate.MIN }

    val dyLast: Double? get() {
        val p = price ?: return null
        val d = lastDividend ?: return cvm?.let { c -> if (c.dyMonth != null && c.vpCota != null && p > 0) c.dyMonth * c.vpCota / p else null }
        val dy = if (p > 0 && d.value > 0) d.value / p else return null
        return if (dy >= 0.0005) dy else null
    }

    fun sumLast12m(today: LocalDate = LocalDate.now()): Double {
        val from = today.withDayOfMonth(1).minusMonths(11)
        return effectiveDividends.filter { val r = it.refDate; r != null && !r.isBefore(from) && !r.isAfter(today.plusMonths(1)) }
            .sumOf { it.value }
    }

    val dy12m: Double? get() {
        val p = price ?: return null
        val s = sumLast12m()
        return if (p > 0 && s > 0) s / p else null
    }
}

fun prettyName(raw: String): String {
    var s = " " + raw.trim().uppercase().replace(Regex("\\s+"), " ") + " "
    val junk = listOf(
        "FUNDO DE INVESTIMENTO IMOBILIÁRIO", "FUNDO DE INVESTIMENTO IMOBILIARIO", "FUNDO DE INVESTIMENTO",
        "RESPONSABILIDADE LIMITADA", "RESP LTDA", "RESP. LTDA", " RL ", " FII ", " FI ", " - ", " – ",
    )
    for (j in junk) s = s.replace(j, " ")
    val words = s.trim().split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return raw
    val small = setOf("DE", "DA", "DO", "DAS", "DOS", "E")
    return words.mapIndexed { i, w ->
        val vowels = w.count { it in "AEIOUÁÉÍÓÚÂÊÔÃÕ" }
        when {
            i > 0 && w in small -> w.lowercase()
            w.length <= 4 && (vowels == 0 || w.length <= 2) -> w
            w.any { it.isDigit() } -> w
            else -> w.lowercase().replaceFirstChar { it.uppercase() }
        }
    }.joinToString(" ")
}

fun prettySegment(raw: String): String {
    val s = raw.trim().lowercase()
    return when {
        s.contains("lajes") || s.contains("escrit") -> "Lajes corporativas"
        s.contains("shopping") -> "Shoppings"
        s.contains("log") -> "Logística"
        s.contains("híbrido") || s.contains("hibrido") || s.contains("multi") -> "Híbrido"
        s.contains("títulos") || s.contains("titulos") || s.contains("val. mob") || s.contains("valores mob") -> "Papel (recebíveis)"
        s.contains("resid") -> "Residencial"
        s.contains("hosp") -> "Hospitalar"
        s.contains("hotel") -> "Hotel"
        s.contains("educ") -> "Educacional"
        s.contains("agro") -> "Agro"
        s.contains("varejo") || s.contains("renda urbana") -> "Renda urbana"
        s.isBlank() -> "Outros"
        else -> raw.trim().lowercase().replaceFirstChar { it.uppercase() }
    }
}

/** Balanço mensal do FII, a partir do informe mensal estruturado da CVM. */
data class Balance(
    val ref: LocalDate,
    val totalAssets: Double?,
    val netWorth: Double?,
    val shares: Double?,
    val vpCota: Double?,
    val adminFee: Double?,          // fração do PL no mês
    val returnEffective: Double?,   // rentabilidade efetiva do mês
    val returnPatrimonial: Double?, // variação patrimonial do mês
    val dy: Double?,
    val amortization: Double?,
    val holders: Int?,
    val holdersPF: Int?,
    // ativo
    val liquidity: Double,
    val realEstate: Double,
    val realEstateIncome: Double,
    val realEstateConstruction: Double,
    val realEstateForSale: Double,
    val land: Double,
    val cri: Double,
    val lci: Double,
    val fiiShares: Double,
    val companies: Double,
    val otherSecurities: Double,
    val receivables: Double,
    val rentReceivable: Double,
    // passivo
    val liabilities: Double,
    val dividendsPayable: Double,
    val adminFeePayable: Double,
    val propertyObligations: Double,
    val securitization: Double,
    val provisions: Double,
    val otherPayables: Double = 0.0,
) {
    data class Slice(val label: String, val value: Double)

    /** Composição do ativo, do maior para o menor. */
    val composition: List<Slice>
        get() {
            val parts = listOf(
                Slice("Imóveis", realEstate),
                Slice("CRI", cri),
                Slice("Cotas de outros FIIs", fiiShares),
                Slice("LCI / LIG", lci),
                Slice("Ações e SPEs", companies),
                Slice("Caixa e renda fixa", liquidity),
                Slice("Valores a receber", receivables),
                Slice("Outros títulos", otherSecurities),
            )
            val known = parts.sumOf { it.value }
            val total = totalAssets ?: known
            val rest = total - known
            val withRest = if (total > 0 && rest / total > 0.005) parts + Slice("Outros", rest) else parts
            return withRest.filter { it.value > 0 }.sortedByDescending { it.value }
        }

    val compositionTotal: Double get() = composition.sumOf { it.value }

    /** Passivo sobre o patrimônio líquido. */
    val leverage: Double? get() = netWorth?.takeIf { it > 0 }?.let { liabilities / it }

    val mainExposure: String
        get() = composition.firstOrNull()?.label ?: "—"
}
