package com.arusku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arusku.app.data.APP_CREATOR
import com.arusku.app.data.Analysis
import com.arusku.app.data.Fmt
import com.arusku.app.data.HomeData
import com.arusku.app.data.Summary
import com.arusku.app.data.Tx
import com.arusku.app.data.rp
import java.time.LocalDate

@Composable
fun HomeScreen(
    onEdit: (Tx) -> Unit,
    onReport: () -> Unit,
    onReview: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val tick by AppState.resumeTick.collectAsState()
    val listenerOn = remember(tick) { AppState.listenerEnabled(context) }
    val home = rememberLoad(initial = HomeData.empty()) { Analysis.home() }
    var askSaldo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Judul + kredit pembuat
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ArusKu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "by $APP_CREATOR",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.primary,
                )
            }
            IconButton(onClick = onAbout) {
                Icon(Icons.Rounded.Info, contentDescription = "Tentang ArusKu", tint = cs.onSurfaceVariant)
            }
        }
        Text(
            Fmt.month(home.summary.month),
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (!listenerOn) {
            ActionCard(
                title = "Aktifkan baca notifikasi BCA",
                text = "Supaya setiap transaksi BCA tercatat sendiri tanpa perlu diketik.",
                button = "Atur sekarang",
                color = Gold,
                onClick = onSettings,
            )
            Spacer(Modifier.height(12.dp))
        }

        BalanceCard(home.summary)

        if (!home.summary.sisaIsSaldo) {
            Spacer(Modifier.height(12.dp))
            ActionCard(
                title = "Isi saldo BCA sekali saja",
                text = "Supaya angka sisa sama dengan saldo aslimu. Lihat saldo di app BCA, lalu ketik di sini.",
                button = "Isi saldo",
                color = Sky,
                onClick = { askSaldo = true },
            )
        }

        if (home.summary.review > 0) {
            Spacer(Modifier.height(12.dp))
            ActionCard(
                title = "${home.summary.review} transfer perlu dicek",
                text = "Ke rekening sendiri atau bayar orang? Transfer ke rekening sendiri tidak dihitung pengeluaran.",
                button = "Cek sekarang",
                color = Gold,
                onClick = onReview,
            )
        }

        SectionTitle("Pengeluaran per kategori", "Lihat rekap", onReport)
        if (home.cats.isEmpty()) {
            EmptyNote("Belum ada pengeluaran bulan ini.")
        } else {
            val max = home.cats.maxOf { it.total }
            Panel {
                home.cats.take(5).forEach { CatLine(it, max) }
                if (home.cats.size > 5) Hint("+${home.cats.size - 5} kategori lain di Rekap")
            }
        }

        SectionTitle("Transaksi terakhir")
        if (home.recent.isEmpty()) {
            EmptyNote("Belum ada transaksi. Transaksi BCA akan muncul di sini otomatis.")
        } else {
            Panel {
                home.recent.forEach { tx -> TxRow(tx) { onEdit(tx) } }
            }
        }
        Spacer(Modifier.height(110.dp))
    }

    if (askSaldo) SaldoDialog(current = null, onDismiss = { askSaldo = false })
}

@Composable
private fun BalanceCard(s: Summary) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.surfaceContainer)))
                .padding(20.dp)
        ) {
            Text(
                if (s.sisaIsSaldo) "Perkiraan saldo BCA" else "Sisa bulan ini (masuk − keluar)",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onPrimaryContainer,
            )
            Text(
                rp(s.sisa),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (s.sisa < 0) Rose else cs.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(projection(s), style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer)
            Spacer(Modifier.height(16.dp))
            Row {
                StatBlock("Diisi bulan ini", rp(s.masuk), Mint, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                StatBlock("Terpakai", rp(s.terpakai), cs.onSurface, Modifier.weight(1f))
            }
            if (s.pindah > 0) {
                Spacer(Modifier.height(10.dp))
                Hint("Pindah ke rekening sendiri ${rp(s.pindah)} — tidak dihitung pengeluaran.")
            }
        }
    }
}

/** "Dengan pola belanja sekarang, sisa uangmu habis sekitar tanggal 24." */
private fun projection(s: Summary): String {
    if (s.terpakai <= 0) return "Belum ada pengeluaran bulan ini."
    if (s.sisa <= 0) return "Sisa uang sudah habis. Pengeluaran bulan ini ${rp(s.terpakai)}."
    val perDay = s.terpakai.toDouble() / s.daysElapsed.coerceAtLeast(1)
    val perDayText = "±${rp(perDay.toLong())}/hari"
    val daysLeft = (s.sisa / perDay).toLong()
    val today = LocalDate.now(Fmt.zone)
    val habis = today.plusDays(daysLeft)
    val end = s.month.atEndOfMonth()
    return if (habis.isAfter(end)) {
        "Dengan pola belanja sekarang ($perDayText), cukup sampai akhir bulan."
    } else {
        "Dengan pola belanja sekarang ($perDayText), sisa uangmu habis sekitar tanggal ${habis.dayOfMonth}."
    }
}

@Composable
private fun ActionCard(title: String, text: String, button: String, color: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onClick) { Text(button) }
        }
    }
}
