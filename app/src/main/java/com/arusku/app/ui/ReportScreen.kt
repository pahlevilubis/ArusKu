package com.arusku.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arusku.app.data.Analysis
import com.arusku.app.data.MonthReport
import com.arusku.app.data.rp
import java.time.YearMonth

@Composable
fun ReportScreen(month: YearMonth, onMonth: (YearMonth) -> Unit) {
    val r = rememberLoad<MonthReport?>(month, initial = null) { Analysis.report(month) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenTitle("Rekap")
        MonthSwitcher(month, onMonth)

        when {
            r == null || r.month != month -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            r.txCount == 0 -> {
                Spacer(Modifier.height(8.dp))
                EmptyNote("Belum ada data di bulan ini.")
            }
            else -> ReportBody(r)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ReportBody(r: MonthReport) {
    val cs = MaterialTheme.colorScheme

    // Ringkasan
    Spacer(Modifier.height(8.dp))
    Panel {
        Row {
            StatBlock("Diisi", rp(r.masuk), Mint, Modifier.weight(1f))
            StatBlock("Terpakai", rp(r.terpakai), cs.onSurface, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            StatBlock(
                "Selisih",
                rp(r.masuk - r.terpakai - r.pindah),
                if (r.masuk - r.terpakai - r.pindah < 0) Rose else cs.onSurface,
                Modifier.weight(1f),
            )
            val base = if (r.isCurrent) r.prevSameTotal else r.prevTotal
            StatBlock(
                if (r.isCurrent) "Bln lalu (tgl yg sama)" else "Bulan lalu",
                if (base > 0) rp(base) else "—",
                cs.onSurfaceVariant,
                Modifier.weight(1f),
            )
        }
        if (r.masukLain > 0 || r.pindah > 0) {
            Spacer(Modifier.height(10.dp))
            if (r.masukLain > 0) Hint("Termasuk pemasukan lain ${rp(r.masukLain)}.")
            if (r.pindah > 0) Hint("Pindah ke rekening sendiri ${rp(r.pindah)} (tidak dihitung pengeluaran).")
        }
    }

    // Catatan otomatis
    if (r.insights.isNotEmpty()) {
        SectionTitle("Catatan bulan ini")
        Panel {
            r.insights.forEachIndexed { i, text ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = cs.outlineVariant)
                Row {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // Per kategori
    SectionTitle("Ke mana uangnya pergi")
    if (r.cats.isEmpty()) {
        EmptyNote("Belum ada pengeluaran.")
    } else {
        val max = r.cats.maxOf { it.total }
        Panel {
            r.cats.forEach { c ->
                val pct = if (r.terpakai > 0) c.total * 100 / r.terpakai else 0
                val base = if (r.isCurrent) c.prevSame else c.prev
                val change = when {
                    base <= 0 -> "baru bulan ini"
                    c.total >= base -> "▲ ${(c.total - base) * 100 / base}% dari bulan lalu"
                    else -> "▼ ${(base - c.total) * 100 / base}% dari bulan lalu"
                }
                CatLine(c, max, extra = "$pct% · ${c.count} transaksi · $change")
            }
        }
    }

    // Bocor halus
    if (r.leaks.isNotEmpty()) {
        SectionTitle("Bocor halus")
        Panel {
            Hint("Transaksi kecil (sampai ${rp(Analysis.LEAK_MAX)}) yang sering. Satu-satu nggak terasa, dijumlah lumayan.")
            Spacer(Modifier.height(8.dp))
            r.leaks.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Sky, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.label, fontWeight = FontWeight.Medium)
                        Hint("${l.count}× · rata-rata ${rp(l.total / l.count)}")
                    }
                    Text(rp(l.total), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Berulang
    if (r.recurring.isNotEmpty()) {
        SectionTitle("Pengeluaran berulang")
        Panel {
            Hint("Nominal sama muncul di beberapa bulan. Bisa jadi langganan atau tagihan rutin — cek apakah masih dipakai.")
            Spacer(Modifier.height(8.dp))
            r.recurring.forEach { x ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Repeat, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            x.note.ifBlank { x.category },
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Hint("${x.category} · muncul ${x.months} bulan")
                    }
                    Text(rp(x.amount), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Pola waktu
    if (r.terpakai > 0) {
        SectionTitle("Pola waktu")
        Panel {
            Text("Per minggu", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            BarList(r.weeks.map { Pair(it.label, it.total) }, Mint)
            Spacer(Modifier.height(16.dp))
            Text("Per waktu", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            BarList(r.dayParts.map { Pair(it.label, it.total) }, Sky)
            Spacer(Modifier.height(16.dp))
            Text("Rata-rata per hari", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            BarList(listOf(Pair("Hari kerja", r.weekdayAvg), Pair("Akhir pekan", r.weekendAvg)), Gold)
        }
    }

    // Rincian tunai & GoPay
    if (r.details.isNotEmpty()) {
        SectionTitle("Rincian tunai & GoPay")
        Panel {
            Hint("Tidak menambah total — hanya untuk melihat uang tunai/GoPay habisnya buat apa. Total ${rp(r.detailTotal)}.")
            Spacer(Modifier.height(4.dp))
            val max = r.details.maxOf { it.total }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                r.details.forEach { c -> CatLine(c, max, extra = "${c.count} catatan") }
            }
        }
    }
}
