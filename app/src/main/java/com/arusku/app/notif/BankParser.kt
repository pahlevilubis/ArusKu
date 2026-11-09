package com.arusku.app.notif

import com.arusku.app.data.CAT_LAIN
import com.arusku.app.data.Kind
import com.arusku.app.data.Money

/**
 * Membaca notifikasi "Financial Diary" dari BCA mobile / myBCA. Contoh:
 *   "Financial Diary: Pengeluaran sebesar IDR 121,000.00 di kategori Belanja."
 *   "Pengeluaran sebesar IDR 36,000.00 di BUBUR AYAM SPESIAL 46 CAB untuk kategori Makanan dari ..."
 */
object BankParser {
    data class Parsed(val kind: Kind, val amount: Long, val category: String, val merchant: String?)

    private val GOJEK = setOf("com.gojek.app", "com.gojek.gopay")

    fun isBca(pkg: String): Boolean = pkg.startsWith("com.bca")

    /** Aplikasi yang notifikasinya disimpan di log (untuk menyiapkan pembaca baru). */
    fun isWatched(pkg: String): Boolean = isBca(pkg) || pkg in GOJEK

    private val mainRe = Regex(
        """(Pengeluaran|Pemasukan|Penerimaan|Uang\s+Masuk|Dana\s+Masuk)\s+sebesar\s+(?:IDR|Rp\.?)\s*([0-9][0-9.,]*)""",
        RegexOption.IGNORE_CASE
    )
    private val catRe = Regex(
        """kategori\s+(.+?)(?:\s+dari\s|\s+pada\s|\s+ke\s|\.\s|\.$|$)""",
        RegexOption.IGNORE_CASE
    )
    private val merchantRe = Regex("""\sdi\s+(.+?)\s+untuk\s+kategori""", RegexOption.IGNORE_CASE)
    private val spaceRe = Regex("\\s+")

    fun parseBca(text: String): Parsed? {
        val t = text.replace(spaceRe, " ").trim()
        val m = mainRe.find(t) ?: return null
        val amount = Money.parseBankAmount(m.groupValues[2]) ?: return null
        if (amount <= 0) return null
        val kind = if (m.groupValues[1].equals("Pengeluaran", ignoreCase = true)) Kind.OUT else Kind.IN_SELF
        val cat = catRe.find(t, m.range.last)?.groupValues?.get(1)
            ?.trim()?.trimEnd('.')?.take(40)
            ?.takeIf { it.isNotBlank() } ?: CAT_LAIN
        val merchant = merchantRe.find(t)?.groupValues?.get(1)?.trim()?.take(60)?.let(::titleCase)
        return Parsed(kind, amount, cat, merchant)
    }

    private fun titleCase(s: String): String =
        s.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    /** Kategori BCA yang perlu ditanyakan: transfer bisa ke rekening sendiri atau bayar orang. */
    fun needsReview(kind: Kind, category: String): Boolean =
        kind == Kind.OUT && category.contains("transfer", ignoreCase = true)
}
