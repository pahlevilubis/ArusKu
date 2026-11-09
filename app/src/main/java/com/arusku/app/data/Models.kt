package com.arusku.app.data

/**
 * Jenis transaksi. Aturan hitungnya:
 * - OUT       : uang keluar dari BCA ke pihak lain → dihitung pengeluaran
 * - SELF_OUT  : uang keluar dari BCA ke rekening sendiri → TIDAK dihitung pengeluaran
 * - IN_SELF   : uang masuk ke BCA dari rekening sendiri ("isi BCA")
 * - IN_OTHER  : uang masuk lainnya (refund, dibayar teman, dll)
 * - DETAIL    : rincian tunai / GoPay → cuma "habisnya buat apa", tidak menambah total
 */
enum class Kind(val code: String, val label: String) {
    OUT("OUT", "Pengeluaran"),
    SELF_OUT("SELF_OUT", "Pindah ke rekening sendiri"),
    IN_SELF("IN_SELF", "Isi dari rekening sendiri"),
    IN_OTHER("IN_OTHER", "Pemasukan lain"),
    DETAIL("DETAIL", "Rincian");

    val isIn: Boolean get() = this == IN_SELF || this == IN_OTHER

    companion object {
        fun of(code: String?): Kind = entries.firstOrNull { it.code == code } ?: OUT
    }
}

enum class Source(val code: String, val label: String) {
    BCA("BCA", "BCA"),
    MANUAL("MANUAL", "BCA (manual)"),
    TUNAI("TUNAI", "Tunai"),
    GOPAY("GOPAY", "GoPay");

    companion object {
        fun of(code: String?): Source = entries.firstOrNull { it.code == code } ?: MANUAL
    }
}

data class Tx(
    val id: Long = 0,
    val time: Long,
    val amount: Long,
    val kind: Kind,
    val source: Source,
    val category: String,
    val note: String = "",
    val raw: String = "",
    val dedup: String = "",
    val review: Boolean = false,
) {
    val title: String get() = note.ifBlank { category }
}

const val CAT_ISI = "Isi BCA"
const val CAT_MASUK_LAIN = "Pemasukan lain"
const val CAT_PINDAH = "Pindah dana"
const val CAT_LAIN = "Lainnya"

/** Nama pembuat aplikasi — tampil di layar pembuka, Beranda, dan Tentang. */
const val APP_CREATOR = "Pahlevi Lubis"
