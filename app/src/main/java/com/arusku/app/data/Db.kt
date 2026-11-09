package com.arusku.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Database lokal di HP. Tidak ada yang dikirim ke internet. */
class Db(context: Context) : SQLiteOpenHelper(context, "arusku.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE tx(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                time INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                kind TEXT NOT NULL,
                source TEXT NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                raw TEXT NOT NULL DEFAULT '',
                dedup TEXT NOT NULL DEFAULT '',
                review INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE INDEX tx_time ON tx(time)")
        db.execSQL("CREATE INDEX tx_dedup ON tx(dedup)")
        // Notifikasi yang transaksinya sudah dihapus pengguna — jangan dicatat ulang
        db.execSQL("CREATE TABLE ignored(dedup TEXT PRIMARY KEY)")
        // Kata yang diingat → kategori pilihan pengguna
        db.execSQL("CREATE TABLE rule(word TEXT PRIMARY KEY, category TEXT NOT NULL)")
        db.execSQL("CREATE TABLE budget(category TEXT PRIMARY KEY, amount INTEGER NOT NULL)")
        // Log notifikasi bank / e-wallet (untuk menyiapkan pembaca baru, mis. GoPay)
        db.execSQL(
            """CREATE TABLE nlog(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                time INTEGER NOT NULL,
                pkg TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versi pertama — belum ada migrasi
    }
}
