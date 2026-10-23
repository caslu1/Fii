package br.com.cotista.ui

import br.com.cotista.data.Dividend
import br.com.cotista.data.Fund
import br.com.cotista.data.Position
import java.time.LocalDate

data class Holding(val position: Position, val fund: Fund?) {
    val price: Double get() = fund?.price ?: position.avgPrice
    val value: Double get() = position.quantity * price
    val invested: Double get() = position.quantity * position.avgPrice
    val result: Double get() = value - invested
    val resultPct: Double get() = if (invested > 0) value / invested - 1 else 0.0
}

data class Payment(val holding: Holding, val dividend: Dividend) {
    val amount: Double get() = holding.position.quantity * dividend.value
}

class PortfolioSummary(positions: List<Position>, funds: Map<String, Fund>, today: LocalDate = LocalDate.now()) {
    val holdings = positions.map { Holding(it, funds[it.ticker]) }.sortedByDescending { it.value }
    val value = holdings.sumOf { it.value }
    val invested = holdings.sumOf { it.invested }
    val result = value - invested
    val resultPct = if (invested > 0) value / invested - 1 else 0.0
    val dayChange = holdings.sumOf { h ->
        val q = h.fund?.quote
        val prev = q?.prevClose
        if (q != null && prev != null) (q.price - prev) * h.position.quantity else 0.0
    }

    private val payments: List<Payment> = holdings.flatMap { h ->
        h.fund?.effectiveDividends.orEmpty().map { Payment(h, it) }
    }

    /** 12 meses terminando no mês atual. */
    val months: List<LocalDate> = (11 downTo 0).map { today.withDayOfMonth(1).minusMonths(it.toLong()) }

    val monthly: List<Double> = months.map { m ->
        payments.filter { p -> p.dividend.refDate?.let { it.year == m.year && it.month == m.month } == true }.sumOf { it.amount }
    }

    val thisMonth: Double = monthly.last()
    val thisMonthHasEstimates: Boolean = payments.any { p ->
        p.dividend.estimated && p.dividend.refDate?.let { it.year == today.year && it.month == today.month } == true
    }
    val last12: Double = monthly.sum()
    val avgMonthly: Double = monthly.dropLast(1).takeLast(11).filter { it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }
    val yieldOnCost: Double = if (invested > 0) last12 / invested else 0.0
    val dy12: Double = if (value > 0) last12 / value else 0.0

    val upcoming: List<Payment> = payments
        .filter { !it.dividend.estimated && (it.dividend.payDate?.let { d -> !d.isBefore(today) } == true) }
        .sortedBy { it.dividend.payDate }

    val recentPaid: List<Payment> = payments
        .filter { !it.dividend.estimated && (it.dividend.payDate?.let { d -> d.isBefore(today) && d.isAfter(today.minusDays(45)) } == true) }
        .sortedByDescending { it.dividend.payDate }

    val bySegment: List<Pair<String, Double>> = holdings.groupBy { it.fund?.segment ?: "Outros" }
        .map { (k, v) -> k to v.sumOf { it.value } }
        .sortedByDescending { it.second }

    /** Renda mensal projetada com base no último provento de cada fundo. */
    val projectedMonthly: Double = holdings.sumOf { h -> (h.fund?.lastDividend?.value ?: 0.0) * h.position.quantity }

    fun incomeFor(ticker: String): Double = payments.filter { it.holding.position.ticker == ticker }.let { list ->
        months.sumOf { m -> list.filter { p -> p.dividend.refDate?.let { it.year == m.year && it.month == m.month } == true }.sumOf { it.amount } }
    }
}
