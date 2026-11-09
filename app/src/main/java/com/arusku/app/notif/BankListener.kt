package com.arusku.app.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.arusku.app.data.Repo
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Membaca notifikasi yang masuk. Hanya notifikasi BCA (dan GoPay/Gojek untuk log) yang diproses;
 * notifikasi lain (WhatsApp, game, dll.) langsung diabaikan dan tidak disimpan.
 */
class BankListener : NotificationListenerService() {
    private val worker = Executors.newSingleThreadExecutor()

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Ambil juga notifikasi BCA yang masih ada di panel (misal saat ArusKu baru diaktifkan)
        val active = try {
            activeNotifications
        } catch (e: Exception) {
            null
        }
        active?.forEach { sbn -> worker.execute { safeHandle(sbn) } }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        worker.execute { safeHandle(sbn) }
    }

    private fun safeHandle(sbn: StatusBarNotification) {
        try {
            handle(sbn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handle(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val ex = n.extras ?: return
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n").orEmpty()
        val body = (if (big.length >= text.length) big else text).ifBlank { lines }.trim()
        if (body.isEmpty()) return

        val isDiary = body.contains("Financial Diary", ignoreCase = true)
        if (!BankParser.isWatched(pkg) && !isDiary) return

        Repo.init(applicationContext)
        Repo.logNotif(pkg, title, body)

        // Untuk sekarang yang dihitung hanya BCA. GoPay cukup masuk log dulu.
        if (!BankParser.isBca(pkg)) return
        val parsed = BankParser.parseBca(body) ?: return

        val whenMs = n.`when`
        val time = if (whenMs > 0 && abs(whenMs - sbn.postTime) < 2L * 24 * 3600 * 1000) whenMs else sbn.postTime
        val dedup = "$pkg|$whenMs|${body.hashCode()}"
        Repo.recordFromBank(parsed, body, time, dedup)
    }
}
