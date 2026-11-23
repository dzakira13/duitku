package com.example.util

import com.example.data.model.CurrencyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    fun format(
        amount: Double,
        currency: CurrencyOption,
        hidden: Boolean = false,
        showSign: Boolean = false
    ): String {
        if (hidden) {
            return "${currency.symbol} ••••••"
        }
        val locale = when (currency) {
            CurrencyOption.IDR -> Locale("id", "ID")
            CurrencyOption.USD -> Locale.US
            CurrencyOption.SGD -> Locale("en", "SG")
            CurrencyOption.MYR -> Locale("ms", "MY")
            CurrencyOption.EUR -> Locale.GERMANY
        }
        val symbols = DecimalFormatSymbols(locale)
        val pattern = if (currency.decimals == 0) "#,##0" else "#,##0.00"
        val formatter = DecimalFormat(pattern, symbols)
        val absFormatted = formatter.format(kotlin.math.abs(amount))
        val prefix = when {
            amount < 0 -> "-"
            showSign && amount > 0 -> "+"
            else -> ""
        }
        val space = if (currency == CurrencyOption.IDR || currency == CurrencyOption.MYR) " " else ""
        return "$prefix${currency.symbol}$space$absFormatted"
    }

    fun currentMonthKey(timestampMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(timestampMillis))
    }

    fun currentMonthLabel(monthKey: String = currentMonthKey()): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = parser.parse(monthKey) ?: return monthKey
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            monthKey
        }
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "Just now"
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        if (millis <= 0L) return "Today"
        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)

        cal.timeInMillis = millis
        if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
            return "Today"
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
            return "Yesterday"
        }
        val sdf = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
