package br.com.cotista.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

val PT_BR: Locale = Locale("pt", "BR")

private val currency: NumberFormat
    get() = NumberFormat.getCurrencyInstance(PT_BR).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

fun Double.brl(): String = currency.format(this).replace(' ', ' ')

/** Para valores por cota (ex.: R$ 0,0875). */
fun Double.brlCota(): String {
    val nf = NumberFormat.getCurrencyInstance(PT_BR)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = if (abs(this) < 1) 4 else 2
    return nf.format(this).replace(' ', ' ')
}

fun Double.brlCompact(): String {
    val a = abs(this)
    return when {
        a >= 1e9 -> "R$ " + (this / 1e9).num(1) + " bi"
        a >= 1e6 -> "R$ " + (this / 1e6).num(1) + " mi"
        a >= 1e3 -> "R$ " + (this / 1e3).num(1) + " mil"
        else -> brl()
    }
}

fun Double.num(decimals: Int = 2): String {
    val nf = NumberFormat.getNumberInstance(PT_BR)
    nf.minimumFractionDigits = decimals
    nf.maximumFractionDigits = decimals
    return nf.format(this)
}

fun Double.qty(): String {
    val nf = NumberFormat.getNumberInstance(PT_BR)
    nf.maximumFractionDigits = 2
    return nf.format(this)
}

/** this é fração (0.0123 = 1,23%). */
fun Double.pct(decimals: Int = 2, signed: Boolean = false): String {
    val v = this * 100
    val s = v.num(decimals) + "%"
    return if (signed && v > 0) "+$s" else s
}

private val ddMM = DateTimeFormatter.ofPattern("dd/MM", PT_BR)
private val ddMMyy = DateTimeFormatter.ofPattern("dd/MM/yy", PT_BR)
private val monthShort = DateTimeFormatter.ofPattern("MMM", PT_BR)
private val monthYear = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)
private val monthFull = DateTimeFormatter.ofPattern("MMMM", PT_BR)
private val weekday = DateTimeFormatter.ofPattern("EEE", PT_BR)

fun LocalDate.ddMM(): String = format(ddMM)
fun LocalDate.ddMMyy(): String = format(ddMMyy)
fun LocalDate.monthShort(): String = format(monthShort).replace(".", "").replaceFirstChar { it.uppercase() }
fun LocalDate.monthYear(): String = format(monthYear).replaceFirstChar { it.uppercase() }
fun LocalDate.monthName(): String = format(monthFull)
fun LocalDate.weekdayShort(): String = format(weekday).replace(".", "").replaceFirstChar { it.uppercase() }

fun relativeDay(d: LocalDate, today: LocalDate = LocalDate.now()): String {
    val diff = java.time.temporal.ChronoUnit.DAYS.between(today, d)
    return when (diff) {
        0L -> "Hoje"
        1L -> "Amanhã"
        -1L -> "Ontem"
        in 2L..6L -> "Em $diff dias"
        else -> d.ddMM()
    }
}

fun parseDecimal(raw: String?): Double? {
    if (raw == null) return null
    var s = raw.trim().replace("\"", "")
    if (s.isEmpty()) return null
    // "1.234,56" -> "1234.56" ; "0,0085" -> "0.0085" ; "1234.56" mantém
    if (s.contains(',')) s = s.replace(".", "").replace(',', '.')
    return s.toDoubleOrNull()
}
