package com.arusku.app.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

fun rp(v: Long): String = Money.rp(v)

object Money {
    /** 121000 → "Rp121.000" */
    fun rp(v: Long): String = (if (v < 0) "−Rp" else "Rp") + group(abs(v))

    /** 121000 → "121.000" */
    fun group(v: Long): String {
        val s = abs(v).toString()
        val sb = StringBuilder()
        for (i in s.indices) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(s[i])
        }
        return (if (v < 0) "-" else "") + sb.toString()
    }

    /** Ringkas untuk grafik: Rp1,2 jt / Rp396 rb */
    fun short(v: Long): String {
        val a = abs(v)
        val sign = if (v < 0) "−" else ""
        return when {
            a >= 1_000_000 -> sign + "Rp" + oneDecimal(a / 1_000_000.0) + " jt"
            a >= 1_000 -> sign + "Rp" + (a / 1_000) + " rb"
            else -> sign + "Rp" + a
        }
    }

    private fun oneDecimal(d: Double): String {
        val r = (d * 10).roundToLong() / 10.0
        return if (r % 1.0 == 0.0) r.toLong().toString()
        else String.format(Locale.US, "%.1f", r).replace('.', ',')
    }

    /**
     * Angka dari notifikasi bank. Bisa "121,000.00" (format BCA), "121.000,00", "121.000", "36000".
     * Dua digit terakhir setelah pemisah dianggap sen dan dibuang.
     */
    fun parseBankAmount(raw: String): Long? {
        var s = raw.trim().trimEnd('.', ',')
        if (s.isEmpty()) return null
        val lastSep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
        if (lastSep >= 0) {
            val tail = s.substring(lastSep + 1)
            if (tail.length in 1..2 && tail.all { it.isDigit() }) s = s.substring(0, lastSep)
        }
        return s.filter { it.isDigit() }.toLongOrNull()
    }
}

data class QuickResult(val amount: Long?, val note: String)

/**
 * Pengurai ketikan singkat: "bakso 25rb", "parkir 5", "bensin 50k", "kopi 18.000", "gaji 1,5jt".
 * Angka kecil tanpa satuan (di bawah 1000) dianggap ribuan: "parkir 5" = Rp5.000.
 */
object QuickParse {
    private val numRe = Regex("""^(\d+(?:[.,]\d+)*)(rb|ribu|k|jt|juta)?$""", RegexOption.IGNORE_CASE)
    private val unitRe = Regex("""^(rb|ribu|k|jt|juta)$""", RegexOption.IGNORE_CASE)
    private val thousandsRe = Regex("""^\d{1,3}([.,]\d{3})+$""")

    fun parse(input: String): QuickResult {
        val tokens = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        var amount: Long? = null
        var amountIdx = -1
        var unitIdx = -1
        for (i in tokens.indices.reversed()) {
            val t = cleanToken(tokens[i])
            val m = numRe.matchEntire(t) ?: continue
            var unit = m.groupValues[2]
            var uIdx = -1
            if (unit.isEmpty() && i + 1 < tokens.size && unitRe.matches(cleanToken(tokens[i + 1]))) {
                unit = cleanToken(tokens[i + 1])
                uIdx = i + 1
            }
            val v = toValue(m.groupValues[1], unit.lowercase(), smallIsThousands = true) ?: continue
            amount = v
            amountIdx = i
            unitIdx = uIdx
            break
        }
        val note = tokens.filterIndexed { i, _ -> i != amountIdx && i != unitIdx }
            .joinToString(" ")
            .trim()
        return QuickResult(amount?.takeIf { it > 0 }, note)
    }

    /** Untuk kolom "Jumlah" saat mengubah transaksi: "36.000", "36000", "36rb". */
    fun amountOf(input: String, smallIsThousands: Boolean): Long? {
        val t = cleanToken(input.replace(" ", ""))
        val m = numRe.matchEntire(t) ?: return null
        return toValue(m.groupValues[1], m.groupValues[2].lowercase(), smallIsThousands)?.takeIf { it > 0 }
    }

    private fun cleanToken(t: String): String {
        var s = t.trim().trimEnd(',', '.', ';', '!')
        if (s.startsWith("rp", ignoreCase = true)) s = s.substring(2).trimStart('.', ' ')
        return s
    }

    private fun toValue(num: String, unit: String, smallIsThousands: Boolean): Long? = when (unit) {
        "jt", "juta" -> num.replace(',', '.').toDoubleOrNull()?.let { (it * 1_000_000).roundToLong() }
        "rb", "ribu", "k" -> num.replace(',', '.').toDoubleOrNull()?.let { (it * 1_000).roundToLong() }
        else -> if (thousandsRe.matches(num)) {
            num.filter { it.isDigit() }.toLongOrNull()
        } else {
            val n = num.replace(',', '.').toDoubleOrNull()
            when {
                n == null -> null
                smallIsThousands && n < 1000 -> (n * 1000).roundToLong()
                else -> n.roundToLong()
            }
        }
    }
}
