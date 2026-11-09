package com.arusku.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Format tanggal & jam dalam Bahasa Indonesia. */
object Fmt {
    val ID: Locale = Locale.forLanguageTag("id-ID")
    val zone: ZoneId get() = ZoneId.systemDefault()

    private val monthF = DateTimeFormatter.ofPattern("MMMM yyyy", ID)
    private val monthOnlyF = DateTimeFormatter.ofPattern("MMMM", ID)
    private val dayF = DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)
    private val timeF = DateTimeFormatter.ofPattern("HH:mm", ID)
    private val dateTimeF = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", ID)
    private val csvDateF = DateTimeFormatter.ofPattern("yyyy-MM-dd", ID)

    fun month(ym: YearMonth): String = ym.format(monthF).replaceFirstChar { it.uppercase() }
    fun monthOnly(ym: YearMonth): String = ym.format(monthOnlyF).replaceFirstChar { it.uppercase() }
    fun day(d: LocalDate): String = d.format(dayF).replaceFirstChar { it.uppercase() }
    fun time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(timeF)
    fun dateTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(dateTimeF)
    fun csvDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(csvDateF)
    fun date(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun yearMonth(ms: Long): YearMonth = YearMonth.from(Instant.ofEpochMilli(ms).atZone(zone))
}
