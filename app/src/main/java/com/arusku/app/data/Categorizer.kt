package com.arusku.app.data

/** Menebak kategori dari catatan singkat. Pilihan kamu sebelumnya selalu didahulukan. */
object Categorizer {
    val DEFAULTS = listOf(
        "Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan",
        "Kesehatan", "Pendidikan", "Sosial", CAT_LAIN
    )

    private val KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "Makanan" to listOf(
            "makan", "bakso", "mie", "mi", "nasi", "kopi", "teh", "jajan", "sarapan", "soto", "sate",
            "ayam", "bubur", "roti", "gorengan", "minum", "snack", "martabak", "warteg", "padang",
            "gofood", "grabfood", "shopeefood", "es", "kue", "seblak", "pecel", "lontong", "burger",
            "pizza", "geprek", "susu", "galon", "aqua", "cemilan", "siomay", "batagor", "boba"
        ),
        "Transportasi" to listOf(
            "bensin", "pertalite", "pertamax", "parkir", "tol", "ojek", "ojol", "gojek", "goride",
            "gocar", "grab", "angkot", "bus", "krl", "kereta", "taksi", "taxi", "busway",
            "transjakarta", "mrt", "lrt", "bengkel", "servis", "service", "oli", "ban", "cuci"
        ),
        "Belanja" to listOf(
            "belanja", "indomaret", "alfamart", "alfamidi", "sayur", "pasar", "sabun", "shampo",
            "shopee", "tokopedia", "tokped", "lazada", "baju", "sepatu", "celana", "superindo",
            "hypermart", "giant", "detergen", "tisu"
        ),
        "Tagihan" to listOf(
            "listrik", "pln", "token", "pulsa", "kuota", "paket", "internet", "wifi", "indihome",
            "pdam", "iuran", "bpjs", "cicilan", "kredit", "asuransi", "sewa", "kos", "kontrakan"
        ),
        "Hiburan" to listOf(
            "nonton", "bioskop", "film", "game", "netflix", "spotify", "youtube", "langganan",
            "karaoke", "liburan", "wisata", "rekreasi"
        ),
        "Kesehatan" to listOf("obat", "apotek", "apotik", "dokter", "klinik", "vitamin", "rumahsakit"),
        "Pendidikan" to listOf("sekolah", "spp", "buku", "les", "kursus", "kuliah", "seragam", "fotokopi"),
        "Sosial" to listOf(
            "sedekah", "infaq", "infak", "zakat", "kondangan", "sumbangan", "amal", "kado", "hadiah",
            "arisan", "takziah", "kotak"
        ),
    )

    private val splitRe = Regex("[^a-z0-9]+")

    fun words(note: String): List<String> = note.lowercase().split(splitRe).filter { it.isNotEmpty() }

    /** Kata kunci untuk mengingat pilihan kategori: kata pertama yang bukan angka. */
    fun keyOf(note: String): String? = words(note).firstOrNull { it.length >= 2 && !it.all(Char::isDigit) }

    fun guess(note: String, learned: (String) -> String?): String {
        val ws = words(note)
        if (ws.isEmpty()) return CAT_LAIN
        for (w in ws) learned(w)?.let { return it }
        for (w in ws) {
            for ((cat, keys) in KEYWORDS) {
                if (keys.any { k -> w == k || (k.length >= 4 && w.startsWith(k)) }) return cat
            }
        }
        return CAT_LAIN
    }
}
