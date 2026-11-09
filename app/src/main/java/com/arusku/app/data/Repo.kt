package com.arusku.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.arusku.app.notif.BankParser
import com.arusku.app.notif.Notifier
import com.arusku.app.widget.ArusWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.Executors

/** Ringkasan satu bulan untuk Beranda & widget. */
data class Summary(
    val month: YearMonth,
    val masuk: Long,
    val terpakai: Long,
    val pindah: Long,
    /** Perkiraan saldo BCA (kalau saldo awal sudah diisi), atau masuk − keluar bulan ini. */
    val sisa: Long,
    val sisaIsSaldo: Boolean,
    val daysElapsed: Int,
    val daysInMonth: Int,
    val review: Int,
) {
    companion object {
        fun empty() = Summary(YearMonth.now(), 0, 0, 0, 0, false, 1, 30, 0)
    }
}

data class LogRow(val time: Long, val pkg: String, val title: String, val body: String)

object Repo {
    private lateinit var app: Context
    private lateinit var db: Db
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val rules = HashMap<String, String>()

    @Volatile
    private var ready = false

    private val _changes = MutableStateFlow(0)

    /** Naik setiap ada perubahan data — layar & widget memuat ulang. */
    val changes: StateFlow<Int> get() = _changes

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        app = context.applicationContext
        Prefs.init(app)
        db = Db(app)
        loadRules()
        ready = true
    }

    fun runAsync(block: () -> Unit) {
        io.execute {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toast(msg: String) {
        main.post { Toast.makeText(app, msg, Toast.LENGTH_SHORT).show() }
    }

    fun bump() {
        _changes.update { it + 1 }
        try {
            ArusWidget.refresh(app)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- query helper

    private const val COLS = "id,time,amount,kind,source,category,note,raw,dedup,review"

    private fun Cursor.tx() = Tx(
        id = getLong(0),
        time = getLong(1),
        amount = getLong(2),
        kind = Kind.of(getString(3)),
        source = Source.of(getString(4)),
        category = getString(5) ?: CAT_LAIN,
        note = getString(6) ?: "",
        raw = getString(7) ?: "",
        dedup = getString(8) ?: "",
        review = getInt(9) == 1,
    )

    private fun args(a: Array<out Any>): Array<String> = a.map { it.toString() }.toTypedArray()

    private fun list(sql: String, vararg a: Any): List<Tx> =
        db.readableDatabase.rawQuery(sql, args(a)).use { c ->
            val out = ArrayList<Tx>()
            while (c.moveToNext()) out.add(c.tx())
            out
        }

    private fun long(sql: String, vararg a: Any): Long =
        db.readableDatabase.rawQuery(sql, args(a)).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }

    private fun strings(sql: String, vararg a: Any): List<String> =
        db.readableDatabase.rawQuery(sql, args(a)).use { c ->
            val out = ArrayList<String>()
            while (c.moveToNext()) c.getString(0)?.let { out.add(it) }
            out
        }

    private fun values(tx: Tx) = ContentValues().apply {
        put("time", tx.time)
        put("amount", tx.amount)
        put("kind", tx.kind.code)
        put("source", tx.source.code)
        put("category", tx.category)
        put("note", tx.note)
        put("raw", tx.raw)
        put("dedup", tx.dedup)
        put("review", if (tx.review) 1 else 0)
    }

    // ---------------------------------------------------------------- periode

    fun range(ym: YearMonth): Pair<Long, Long> {
        val z = Fmt.zone
        val start = ym.atDay(1).atStartOfDay(z).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(z).toInstant().toEpochMilli()
        return Pair(start, end)
    }

    // ---------------------------------------------------------------- baca transaksi

    fun txBetween(start: Long, end: Long): List<Tx> =
        list("SELECT $COLS FROM tx WHERE time>=? AND time<? ORDER BY time DESC", start, end)

    fun txMonth(ym: YearMonth): List<Tx> {
        val (s, e) = range(ym)
        return txBetween(s, e)
    }

    fun recent(n: Int): List<Tx> = list("SELECT $COLS FROM tx ORDER BY time DESC LIMIT $n")

    fun reviewItems(): List<Tx> = list("SELECT $COLS FROM tx WHERE review=1 ORDER BY time DESC")

    fun allTx(): List<Tx> = list("SELECT $COLS FROM tx ORDER BY time ASC")

    fun get(id: Long): Tx? = list("SELECT $COLS FROM tx WHERE id=?", id).firstOrNull()

    fun categoryChoices(): List<String> {
        val used = strings(
            "SELECT category FROM tx WHERE kind IN ('OUT','DETAIL') GROUP BY category ORDER BY COUNT(*) DESC"
        )
        return (Categorizer.DEFAULTS + used).distinct()
    }

    fun budgetCategories(): List<String> {
        val used = strings("SELECT category FROM tx WHERE kind='OUT' GROUP BY category ORDER BY SUM(amount) DESC")
        return (used + budgets().keys + Categorizer.DEFAULTS).distinct()
    }

    // ---------------------------------------------------------------- tulis transaksi

    fun save(tx: Tx) {
        val w = db.writableDatabase
        if (tx.id == 0L) w.insert("tx", null, values(tx))
        else w.update("tx", values(tx), "id=?", arrayOf(tx.id.toString()))
        bump()
        if (tx.kind == Kind.OUT) checkBudget(tx.category, tx.time)
    }

    fun delete(tx: Tx) {
        val w = db.writableDatabase
        if (tx.dedup.isNotBlank()) {
            w.insertWithOnConflict(
                "ignored", null,
                ContentValues().apply { put("dedup", tx.dedup) },
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
        w.delete("tx", "id=?", arrayOf(tx.id.toString()))
        bump()
    }

    fun setNote(id: Long, note: String) {
        db.writableDatabase.update(
            "tx", ContentValues().apply { put("note", note.trim().take(80)) },
            "id=?", arrayOf(id.toString())
        )
        bump()
    }

    fun setKind(id: Long, kind: Kind) {
        val tx = get(id) ?: return
        val cat = when (kind) {
            Kind.SELF_OUT -> CAT_PINDAH
            Kind.IN_SELF -> CAT_ISI
            Kind.IN_OTHER -> CAT_MASUK_LAIN
            else -> tx.category
        }
        db.writableDatabase.update(
            "tx",
            ContentValues().apply {
                put("kind", kind.code)
                put("category", cat)
                put("review", 0)
            },
            "id=?", arrayOf(id.toString())
        )
        bump()
    }

    fun confirmReview(id: Long) {
        db.writableDatabase.update(
            "tx", ContentValues().apply { put("review", 0) },
            "id=?", arrayOf(id.toString())
        )
        bump()
    }

    // ---------------------------------------------------------------- dari notifikasi BCA

    fun recordFromBank(p: BankParser.Parsed, raw: String, time: Long, dedup: String) {
        // Notifikasi yang sama bisa muncul lebih dari sekali — jangan sampai tercatat dobel
        if (long("SELECT COUNT(*) FROM tx WHERE dedup=?", dedup) > 0) return
        if (long("SELECT COUNT(*) FROM ignored WHERE dedup=?", dedup) > 0) return
        if (long("SELECT COUNT(*) FROM tx WHERE raw=? AND time BETWEEN ? AND ?", raw, time - 90_000, time + 90_000) > 0) return

        val review = BankParser.needsReview(p.kind, p.category)
        val tx = Tx(
            time = time,
            amount = p.amount,
            kind = p.kind,
            source = Source.BCA,
            category = if (p.kind.isIn) CAT_ISI else p.category,
            note = p.merchant ?: "",
            raw = raw,
            dedup = dedup,
            review = review,
        )
        val id = db.writableDatabase.insert("tx", null, values(tx))
        if (id < 0) return
        bump()
        val saved = tx.copy(id = id)
        if (p.kind == Kind.OUT) checkBudget(saved.category, time)
        if (review || Prefs.confirm) Notifier.txRecorded(app, saved)
    }

    // ---------------------------------------------------------------- kategori yang diingat

    private fun loadRules() {
        synchronized(rules) {
            rules.clear()
            db.readableDatabase.rawQuery("SELECT word, category FROM rule", null).use { c ->
                while (c.moveToNext()) rules[c.getString(0)] = c.getString(1)
            }
        }
    }

    fun guessCategory(note: String): String = Categorizer.guess(note) { w -> synchronized(rules) { rules[w] } }

    fun learn(word: String, category: String) {
        db.writableDatabase.insertWithOnConflict(
            "rule", null,
            ContentValues().apply {
                put("word", word)
                put("category", category)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        synchronized(rules) { rules[word] = category }
    }

    // ---------------------------------------------------------------- budget

    fun budgets(): Map<String, Long> =
        db.readableDatabase.rawQuery("SELECT category, amount FROM budget", null).use { c ->
            val out = HashMap<String, Long>()
            while (c.moveToNext()) out[c.getString(0)] = c.getLong(1)
            out
        }

    fun setBudget(category: String, amount: Long) {
        val w = db.writableDatabase
        if (amount <= 0) {
            w.delete("budget", "category=?", arrayOf(category))
        } else {
            w.insertWithOnConflict(
                "budget", null,
                ContentValues().apply {
                    put("category", category)
                    put("amount", amount)
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
        bump()
    }

    private fun checkBudget(category: String, time: Long) {
        val budget = budgets()[category] ?: return
        if (budget <= 0) return
        val ym = Fmt.yearMonth(time)
        val (s, e) = range(ym)
        val used = long(
            "SELECT COALESCE(SUM(amount),0) FROM tx WHERE kind='OUT' AND category=? AND time>=? AND time<?",
            category, s, e
        )
        for (level in listOf(100, 80)) {
            if (used * 100 >= budget * level) {
                val key = "alert_${ym}_${category}_$level"
                if (!Prefs.alerted(key)) {
                    Prefs.markAlerted(key)
                    if (level == 100) Prefs.markAlerted("alert_${ym}_${category}_80")
                    Notifier.budget(app, category, used, budget, level)
                }
                break
            }
        }
    }

    // ---------------------------------------------------------------- ringkasan & saldo

    /** Perkiraan saldo BCA: saldo yang diketik + uang masuk − uang keluar sesudahnya. */
    fun balance(): Long? {
        if (!Prefs.saldoSet) return null
        val since = Prefs.saldoTime
        val masuk = long("SELECT COALESCE(SUM(amount),0) FROM tx WHERE kind IN ('IN_SELF','IN_OTHER') AND time>?", since)
        val keluar = long("SELECT COALESCE(SUM(amount),0) FROM tx WHERE kind IN ('OUT','SELF_OUT') AND time>?", since)
        return Prefs.saldo + masuk - keluar
    }

    fun summary(ym: YearMonth = YearMonth.now(Fmt.zone)): Summary {
        val (s, e) = range(ym)
        val masuk = long("SELECT COALESCE(SUM(amount),0) FROM tx WHERE kind IN ('IN_SELF','IN_OTHER') AND time>=? AND time<?", s, e)
        val terpakai = long("SELECT COALESCE(SUM(amount),0) FROM tx WHERE kind='OUT' AND time>=? AND time<?", s, e)
        val pindah = long("SELECT COALESCE(SUM(amount),0) FROM tx WHERE kind='SELF_OUT' AND time>=? AND time<?", s, e)
        val saldo = balance()
        val today = LocalDate.now(Fmt.zone)
        val thisMonth = YearMonth.from(today)
        val elapsed = when {
            ym == thisMonth -> today.dayOfMonth
            ym.isAfter(thisMonth) -> 1
            else -> ym.lengthOfMonth()
        }
        val review = long("SELECT COUNT(*) FROM tx WHERE review=1").toInt()
        return Summary(
            month = ym,
            masuk = masuk,
            terpakai = terpakai,
            pindah = pindah,
            sisa = saldo ?: (masuk - terpakai - pindah),
            sisaIsSaldo = saldo != null,
            daysElapsed = elapsed,
            daysInMonth = ym.lengthOfMonth(),
            review = review,
        )
    }

    fun setSaldo(amount: Long) {
        Prefs.setSaldo(amount, System.currentTimeMillis())
        bump()
    }

    // ---------------------------------------------------------------- log notifikasi

    private val secretRe = Regex(
        """\b(OTP|PIN)\b|kode\s+(verifikasi|rahasia|otentikasi|akses|aktivasi)""",
        RegexOption.IGNORE_CASE
    )

    fun logNotif(pkg: String, title: String, body: String) {
        if (body.isBlank()) return
        // Kode rahasia (OTP/PIN) tidak pernah disimpan
        if (secretRe.containsMatchIn(body) || secretRe.containsMatchIn(title)) return
        val now = System.currentTimeMillis()
        if (long("SELECT COUNT(*) FROM nlog WHERE body=? AND time>?", body, now - 120_000) > 0) return
        val w = db.writableDatabase
        w.insert("nlog", null, ContentValues().apply {
            put("time", now)
            put("pkg", pkg)
            put("title", title.take(200))
            put("body", body.take(1000))
        })
        w.execSQL("DELETE FROM nlog WHERE id NOT IN (SELECT id FROM nlog ORDER BY id DESC LIMIT 100)")
    }

    fun notifLog(): List<LogRow> =
        db.readableDatabase.rawQuery("SELECT time, pkg, title, body FROM nlog ORDER BY id DESC", null).use { c ->
            val out = ArrayList<LogRow>()
            while (c.moveToNext()) out.add(LogRow(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)))
            out
        }

    fun logCount(): Int = long("SELECT COUNT(*) FROM nlog").toInt()

    fun clearLog() {
        db.writableDatabase.delete("nlog", null, null)
        bump()
    }

    // ---------------------------------------------------------------- ekspor

    fun exportCsv(uri: Uri): Boolean = try {
        val rows = allTx()
        val out = app.contentResolver.openOutputStream(uri) ?: throw IllegalStateException("tidak bisa menulis")
        out.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write("tanggal,jam,jenis,sumber,kategori,catatan,jumlah\n")
            for (tx in rows) {
                val cols = listOf(
                    Fmt.csvDate(tx.time), Fmt.time(tx.time), tx.kind.label, tx.source.label,
                    tx.category, tx.note, tx.amount.toString()
                )
                w.write(cols.joinToString(",") { csv(it) })
                w.write("\n")
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    private fun csv(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
