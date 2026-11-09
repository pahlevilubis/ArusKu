package com.arusku.app.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.arusku.app.data.Kind
import com.arusku.app.data.Repo

/** Menangani tombol & balasan di notifikasi ArusKu. */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_ID = "tx_id"
        const val KEY_NOTE = "note"
        const val ACT_NOTE = "com.arusku.app.NOTE"
        const val ACT_SELF = "com.arusku.app.SELF"
        const val ACT_PAY = "com.arusku.app.PAY"
        const val ACT_IN_OTHER = "com.arusku.app.IN_OTHER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context.applicationContext)
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id <= 0) return
        when (intent.action) {
            ACT_NOTE -> {
                val note = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_NOTE)?.toString()
                if (!note.isNullOrBlank()) {
                    Repo.setNote(id, note)
                    Repo.toast("Catatan disimpan")
                }
            }
            ACT_SELF -> {
                Repo.setKind(id, Kind.SELF_OUT)
                Repo.toast("Dicatat sebagai pindah ke rekening sendiri")
            }
            ACT_PAY -> {
                Repo.confirmReview(id)
                Repo.toast("Dicatat sebagai pengeluaran")
            }
            ACT_IN_OTHER -> {
                Repo.setKind(id, Kind.IN_OTHER)
                Repo.toast("Dicatat sebagai pemasukan lain")
            }
        }
        Notifier.cancel(context, id)
    }
}
