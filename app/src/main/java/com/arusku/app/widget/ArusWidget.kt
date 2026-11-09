package com.arusku.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.arusku.app.MainActivity
import com.arusku.app.QuickAddActivity
import com.arusku.app.R
import com.arusku.app.data.Repo
import com.arusku.app.data.rp

/** Widget layar utama: sisa uang + tombol catat cepat untuk uang tunai. */
class ArusWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Repo.init(context.applicationContext)
        val views = build(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, ArusWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { mgr.updateAppWidget(it, views) }
        }

        private fun build(context: Context): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.widget_arus)
            val s = try {
                Repo.summary()
            } catch (e: Exception) {
                null
            }
            if (s != null) {
                rv.setTextViewText(R.id.w_label, if (s.sisaIsSaldo) "Perkiraan saldo BCA" else "Sisa bulan ini")
                rv.setTextViewText(R.id.w_sisa, rp(s.sisa))
                rv.setTextViewText(R.id.w_terpakai, "Terpakai bulan ini ${rp(s.terpakai)}")
            }
            val open = PendingIntent.getActivity(
                context, 100,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.w_head, open)
            rv.setOnClickPendingIntent(R.id.w_ketik, quick(context, "", 101))
            rv.setOnClickPendingIntent(R.id.w_parkir, quick(context, "parkir ", 102))
            rv.setOnClickPendingIntent(R.id.w_bensin, quick(context, "bensin ", 103))
            rv.setOnClickPendingIntent(R.id.w_makan, quick(context, "makan ", 104))
            rv.setOnClickPendingIntent(R.id.w_kopi, quick(context, "kopi ", 105))
            return rv
        }

        private fun quick(context: Context, preset: String, code: Int): PendingIntent {
            val i = Intent(context, QuickAddActivity::class.java)
                .putExtra(QuickAddActivity.EXTRA_PRESET, preset)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return PendingIntent.getActivity(
                context, code, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
