package com.arusku.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class CatStat(
    val name: String,
    val total: Long,
    val count: Int,
    /** Total kategori ini sebulan penuh bulan lalu. */
    val prev: Long = 0,
    /** Total kategori ini di periode yang sama bulan lalu (adil untuk bulan berjalan). */
    val prevSame: Long = 0,
    val budget: Long? = null,
)

data class LeakStat(val label: String, val count: Int, val total: Long)
data class RecurStat(val amount: Long, val category: String, val months: Int, val note: String)
data class BarStat(val label: String, val total: Long)

data class MonthReport(
    val month: YearMonth,
    val isCurrent: Boolean,
    val masuk: Long,
    val masukSendiri: Long,
    val masukLain: Long,
    val terpakai: Long,
    val pindah: Long,
    val prevTotal: Long,
    val prevSameTotal: Long,
    val cats: List<CatStat>,
    val details: List<CatStat>,
    val detailTotal: Long,
    val leaks: List<LeakStat>,
    val recurring: List<RecurStat>,
    val weeks: List<BarStat>,
    val dayParts: List<BarStat>,
    val weekdayAvg: Long,
    val weekendAvg: Long,
    val insights: List<String>,
    val txCount: Int,
)

data class HomeData(val summary: Summary, val cats: List<CatStat>, val recent: List<Tx>) {
    companion object {
        fun empty() = HomeData(Summary.empty(), emptyList(), emptyList())
    }
}

object Analysis {
    /** Transaksi kecil yang sering = "bocor halus". */
    const val LEAK_MAX = 50_000L
    private const val LEAK_MIN_COUNT = 4

    fun home(): HomeData {
        val ym = YearMonth.now(Fmt.zone)
        val summary = Repo.summary(ym)
        val budgets = Repo.budgets()
        val out = Repo.txMonth(ym).filter { it.kind == Kind.OUT }
        val cats = out.groupBy { it.category }
            .map { (c, l) -> CatStat(c, l.sumOf { it.amount }, l.size, budget = budgets[c]) }
            .sortedByDescending { it.total }
        return HomeData(summary, cats, Repo.recent(6))
    }

    fun report(month: YearMonth): MonthReport {
        val today = LocalDate.now(Fmt.zone)
        val isCurrent = month == YearMonth.from(today)
        val (start, end) = Repo.range(month)
        val oldest = Repo.range(month.minusMonths(3)).first
        val all = Repo.txBetween(oldest, end)

        val cur = all.filter { it.time in start until end }
        val (ps, pe) = Repo.range(month.minusMonths(1))
        val prev = all.filter { it.time in ps until pe }

        val out = cur.filter { it.kind == Kind.OUT }
        val terpakai = out.sumOf { it.amount }
        val prevOut = prev.filter { it.kind == Kind.OUT }
        val prevTotal = prevOut.sumOf { it.amount }

        // Untuk bulan berjalan, bandingkan dengan tanggal yang sama bulan lalu
        val elapsedDay = if (isCurrent) today.dayOfMonth else 31
        val prevSameOut = prevOut.filter { Fmt.date(it.time).dayOfMonth <= elapsedDay }
        val prevSameTotal = prevSameOut.sumOf { it.amount }
        val prevByCat = prevOut.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        val prevSameByCat = prevSameOut.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }

        val budgets = Repo.budgets()
        val cats = out.groupBy { it.category }.map { (c, l) ->
            CatStat(c, l.sumOf { it.amount }, l.size, prevByCat[c] ?: 0, prevSameByCat[c] ?: 0, budgets[c])
        }.sortedByDescending { it.total }

        val masukSendiri = cur.filter { it.kind == Kind.IN_SELF }.sumOf { it.amount }
        val masukLain = cur.filter { it.kind == Kind.IN_OTHER }.sumOf { it.amount }
        val pindah = cur.filter { it.kind == Kind.SELF_OUT }.sumOf { it.amount }

        val detail = cur.filter { it.kind == Kind.DETAIL }
        val details = detail.groupBy { it.category }
            .map { (c, l) -> CatStat(c, l.sumOf { it.amount }, l.size) }
            .sortedByDescending { it.total }

        // Bocor halus: transaksi kecil yang sering
        val leaks = cur.filter { (it.kind == Kind.OUT || it.kind == Kind.DETAIL) && it.amount <= LEAK_MAX }
            .groupBy { leakKey(it) }
            .filter { it.value.size >= LEAK_MIN_COUNT }
            .map { (k, l) -> LeakStat(k, l.size, l.sumOf { it.amount }) }
            .sortedByDescending { it.total }
            .take(5)

        // Berulang: nominal & kategori sama, muncul di beberapa bulan (langganan / tagihan rutin)
        val recurring = all.filter { it.kind == Kind.OUT && it.amount >= 10_000 }
            .groupBy { Pair(it.amount, it.category) }
            .mapNotNull { (k, l) ->
                val perMonth = l.groupBy { Fmt.yearMonth(it.time) }
                val recent = perMonth.keys.any { it == month || it == month.minusMonths(1) }
                if (perMonth.size >= 2 && recent && perMonth.values.all { it.size <= 2 }) {
                    RecurStat(k.first, k.second, perMonth.size, l.firstOrNull { it.note.isNotBlank() }?.note ?: "")
                } else null
            }
            .sortedByDescending { it.amount }
            .take(8)

        // Pola per minggu
        val days = month.lengthOfMonth()
        val weekTotals = LongArray(5)
        out.forEach { tx ->
            val d = Fmt.date(tx.time).dayOfMonth
            weekTotals[((d - 1) / 7).coerceAtMost(4)] += tx.amount
        }
        val weeks = (0 until 5).filter { it * 7 + 1 <= days }.map { i ->
            val a = i * 7 + 1
            val b = minOf(i * 7 + 7, days)
            BarStat("Tgl $a–$b", weekTotals[i])
        }

        // Pola jam
        val parts = linkedMapOf("Pagi" to 0L, "Siang" to 0L, "Sore" to 0L, "Malam" to 0L)
        out.forEach { tx ->
            val h = java.time.Instant.ofEpochMilli(tx.time).atZone(Fmt.zone).hour
            val key = when (h) {
                in 4..9 -> "Pagi"
                in 10..14 -> "Siang"
                in 15..17 -> "Sore"
                else -> "Malam"
            }
            parts[key] = (parts[key] ?: 0L) + tx.amount
        }
        val dayParts = parts.map { BarStat(it.key, it.value) }

        // Hari kerja vs akhir pekan (rata-rata per hari)
        val lastDay = when {
            isCurrent -> today.dayOfMonth
            month.isAfter(YearMonth.from(today)) -> 0
            else -> days
        }
        var wdDays = 0
        var weDays = 0
        for (d in 1..lastDay) if (isWeekend(month.atDay(d).dayOfWeek)) weDays++ else wdDays++
        var wdTot = 0L
        var weTot = 0L
        out.forEach { tx -> if (isWeekend(Fmt.date(tx.time).dayOfWeek)) weTot += tx.amount else wdTot += tx.amount }
        val weekdayAvg = if (wdDays > 0) wdTot / wdDays else 0L
        val weekendAvg = if (weDays > 0) weTot / weDays else 0L

        val report = MonthReport(
            month = month,
            isCurrent = isCurrent,
            masuk = masukSendiri + masukLain,
            masukSendiri = masukSendiri,
            masukLain = masukLain,
            terpakai = terpakai,
            pindah = pindah,
            prevTotal = prevTotal,
            prevSameTotal = prevSameTotal,
            cats = cats,
            details = details,
            detailTotal = detail.sumOf { it.amount },
            leaks = leaks,
            recurring = recurring,
            weeks = weeks,
            dayParts = dayParts,
            weekdayAvg = weekdayAvg,
            weekendAvg = weekendAvg,
            insights = emptyList(),
            txCount = cur.size,
        )
        return report.copy(insights = insights(report))
    }

    private fun isWeekend(d: DayOfWeek) = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY

    private fun leakKey(tx: Tx): String {
        if (tx.note.isNotBlank()) {
            Categorizer.keyOf(tx.note)?.let { return it.replaceFirstChar { c -> c.uppercase() } }
        }
        return tx.category
    }

    private fun pct(a: Long, b: Long): Long = if (b == 0L) 0 else a * 100 / b

    private fun insights(r: MonthReport): List<String> {
        val list = mutableListOf<String>()
        val t = r.terpakai

        r.cats.firstOrNull()?.let { top ->
            if (t > 0) list += "Paling banyak untuk ${top.name}: ${rp(top.total)} (${pct(top.total, t)}% dari semua pengeluaran)."
        }

        val base = if (r.isCurrent) r.prevSameTotal else r.prevTotal
        if (base > 0 && t > 0) {
            val d = (t - base) * 100 / base
            val period = if (r.isCurrent) "periode yang sama bulan lalu" else "bulan lalu"
            if (d >= 10) list += "Total pengeluaran naik $d% dibanding $period."
            else if (d <= -10) list += "Total pengeluaran turun ${abs(d)}% dibanding $period. Mantap!"
        }

        val inc = r.cats.mapNotNull { c ->
            val b = if (r.isCurrent) c.prevSame else c.prev
            val diff = c.total - b
            if (b > 0 && diff >= 50_000 && diff * 100 / b >= 20) Pair(c, diff) else null
        }.maxByOrNull { it.second }
        inc?.let { (c, diff) ->
            val b = if (r.isCurrent) c.prevSame else c.prev
            list += "${c.name} naik ${diff * 100 / b}% (+${rp(diff)}) dibanding bulan lalu."
        }

        r.cats.filter { it.budget != null && it.total > it.budget }.forEach {
            list += "${it.name} sudah lewat budget: ${rp(it.total)} dari ${rp(it.budget ?: 0)}."
        }

        r.leaks.firstOrNull()?.let {
            list += "${it.label}: ${it.count} kali transaksi kecil, totalnya ${rp(it.total)}. Kecil-kecil, tapi lumayan."
        }

        if (r.weeks.size >= 3 && t > 0) {
            val mx = r.weeks.maxByOrNull { it.total }
            if (mx != null && pct(mx.total, t) >= 40) {
                list += "Paling boros di ${mx.label.lowercase()}: ${pct(mx.total, t)}% pengeluaran bulan ini."
            }
        }

        if (r.weekendAvg >= 50_000 && r.weekendAvg > r.weekdayAvg * 3 / 2) {
            list += "Rata-rata per hari di akhir pekan ${rp(r.weekendAvg)}, lebih tinggi dari hari kerja (${rp(r.weekdayAvg)})."
        }

        val peak = r.dayParts.maxByOrNull { it.total }
        if (peak != null && t > 0 && pct(peak.total, t) >= 45) {
            list += "Sebagian besar uang keluar di ${peak.label.lowercase()} hari (${pct(peak.total, t)}%)."
        }

        return list.take(6)
    }
}
