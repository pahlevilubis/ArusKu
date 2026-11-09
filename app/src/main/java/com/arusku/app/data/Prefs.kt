package com.arusku.app.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("arusku", Context.MODE_PRIVATE)
    }

    /** Munculkan notif kecil tiap transaksi BCA tercatat (bisa dibalas untuk tambah catatan). */
    var confirm: Boolean
        get() = sp.getBoolean("confirm", true)
        set(v) = sp.edit().putBoolean("confirm", v).apply()

    /** Saldo BCA yang diketik pengguna, sebagai titik awal hitungan "sisa". */
    val saldoSet: Boolean get() = sp.contains("saldo")
    val saldo: Long get() = sp.getLong("saldo", 0L)
    val saldoTime: Long get() = sp.getLong("saldo_time", 0L)

    fun setSaldo(amount: Long, at: Long) {
        sp.edit().putLong("saldo", amount).putLong("saldo_time", at).apply()
    }

    fun alerted(key: String): Boolean = sp.getBoolean(key, false)
    fun markAlerted(key: String) {
        sp.edit().putBoolean(key, true).apply()
    }
}
