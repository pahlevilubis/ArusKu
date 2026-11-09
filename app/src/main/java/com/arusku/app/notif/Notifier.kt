package com.arusku.app.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.arusku.app.MainActivity
import com.arusku.app.R
import com.arusku.app.data.Tx
import com.arusku.app.data.rp

/** Notifikasi dari ArusKu sendiri. */
object Notifier {
    const val CH_CATAT = "catat"
    const val CH_TANYA = "tanya"
    const val CH_BUDGET = "budget"

    private val MINT = 0xFF3DDC97.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_CATAT, "Transaksi tercatat", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Pemberitahuan kecil tiap transaksi BCA tercatat. Bisa dibalas untuk menambah catatan."
                },
                NotificationChannel(CH_TANYA, "Konfirmasi transfer", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Menanyakan apakah transfer ke rekening sendiri atau bayar orang."
                },
                NotificationChannel(CH_BUDGET, "Peringatan budget", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Muncul saat budget kategori terpakai 80% dan 100%."
                },
            )
        )
    }

    fun notifId(txId: Long): Int = (txId % 1_000_000).toInt() + 1_000

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun action(context: Context, txId: Long, act: String, code: Int, mutable: Boolean): PendingIntent {
        val i = Intent(context, ActionReceiver::class.java)
            .setAction(act)
            .setData(Uri.parse("arusku://tx/$txId/$act"))
            .putExtra(ActionReceiver.EXTRA_ID, txId)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE)
        return PendingIntent.getBroadcast(context, (txId * 10 + code).toInt(), i, flags)
    }

    fun txRecorded(context: Context, tx: Tx) {
        if (!canPost(context)) return
        val b: NotificationCompat.Builder
        when {
            tx.review -> {
                b = NotificationCompat.Builder(context, CH_TANYA)
                    .setContentTitle("Transfer ${rp(tx.amount)} dari BCA")
                    .setContentText("Ini ke rekening sendiri atau bayar orang?")
                    .addAction(
                        R.drawable.ic_stat_arus, "Rekening sendiri",
                        action(context, tx.id, ActionReceiver.ACT_SELF, 1, false)
                    )
                    .addAction(
                        R.drawable.ic_stat_arus, "Bayar orang",
                        action(context, tx.id, ActionReceiver.ACT_PAY, 2, false)
                    )
            }
            tx.kind.isIn -> {
                b = NotificationCompat.Builder(context, CH_CATAT)
                    .setContentTitle("${rp(tx.amount)} masuk ke BCA")
                    .setContentText("Dicatat sebagai isi dari rekening sendiri.")
                    .addAction(
                        R.drawable.ic_stat_arus, "Ini pemasukan lain",
                        action(context, tx.id, ActionReceiver.ACT_IN_OTHER, 3, false)
                    )
            }
            else -> {
                val input = RemoteInput.Builder(ActionReceiver.KEY_NOTE)
                    .setLabel("Catatan, misal: shopee sepatu")
                    .build()
                val reply = NotificationCompat.Action.Builder(
                    R.drawable.ic_stat_arus, "Tambah catatan",
                    action(context, tx.id, ActionReceiver.ACT_NOTE, 4, true)
                ).addRemoteInput(input).setAllowGeneratedReplies(false).build()
                val what = if (tx.note.isNotBlank()) "${tx.note} · ${tx.category}" else tx.category
                b = NotificationCompat.Builder(context, CH_CATAT)
                    .setContentTitle("${rp(tx.amount)} · $what")
                    .setContentText("Tercatat dari BCA. Balas untuk menambah catatan.")
                    .addAction(reply)
            }
        }
        b.setSmallIcon(R.drawable.ic_stat_arus)
            .setColor(MINT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context))
        try {
            NotificationManagerCompat.from(context).notify(notifId(tx.id), b.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun budget(context: Context, category: String, used: Long, budget: Long, level: Int) {
        if (!canPost(context)) return
        val title = if (level >= 100) "Budget $category sudah habis" else "Budget $category terpakai $level%"
        val text = "Terpakai ${rp(used)} dari ${rp(budget)} bulan ini."
        val b = NotificationCompat.Builder(context, CH_BUDGET)
            .setSmallIcon(R.drawable.ic_stat_arus)
            .setColor(MINT)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
        try {
            NotificationManagerCompat.from(context).notify(("budget$category").hashCode(), b.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancel(context: Context, txId: Long) {
        NotificationManagerCompat.from(context).cancel(notifId(txId))
    }
}
