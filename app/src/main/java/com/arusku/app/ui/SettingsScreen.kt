package com.arusku.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arusku.app.data.APP_CREATOR
import com.arusku.app.data.Prefs
import com.arusku.app.data.Repo
import com.arusku.app.data.rp
import com.arusku.app.notif.BankListener
import java.time.LocalDate

@Composable
fun SettingsScreen(onAbout: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val tick by AppState.resumeTick.collectAsState()
    val listenerOn = remember(tick) { AppState.listenerEnabled(context) }

    var confirm by remember { mutableStateOf(Prefs.confirm) }
    var askSaldo by remember { mutableStateOf(false) }
    var budgetFor by remember { mutableStateOf<String?>(null) }
    var showLog by remember { mutableStateOf(false) }

    val categories = rememberLoad(initial = emptyList<String>()) { Repo.budgetCategories() }
    val budgets = rememberLoad(initial = emptyMap<String, Long>()) { Repo.budgets() }
    val balance = rememberLoad<Long?>(initial = null) { Repo.balance() }
    val logCount = rememberLoad(initial = 0) { Repo.logCount() }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            Repo.runAsync {
                val ok = Repo.exportCsv(uri)
                Repo.toast(if (ok) "Data tersimpan ke CSV" else "Gagal menyimpan CSV")
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenTitle("Pengaturan")

        // ------------------------------------------------ baca notifikasi
        SectionTitle("Baca notifikasi BCA")
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (listenerOn) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (listenerOn) Mint else Gold,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (listenerOn) "Aktif — transaksi BCA tercatat otomatis" else "Belum aktif",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (!listenerOn) {
                Text("1. Ketuk \"Buka akses notifikasi\", cari ArusKu, lalu aktifkan.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "2. Kalau tombolnya abu-abu atau muncul \"Setelan dibatasi\": ketuk \"Info aplikasi\" → ⋮ (kanan atas) → " +
                        "\"Izinkan setelan terbatas\". Lalu ulangi langkah 1.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Hint("Yang dibaca hanya notifikasi BCA (dan GoPay untuk log). Notifikasi lain diabaikan dan tidak disimpan.")
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { openListenerSettings(context) }) { Text("Buka akses notifikasi") }
                OutlinedButton(onClick = { openAppInfo(context) }) { Text("Info aplikasi") }
            }
            Spacer(Modifier.height(10.dp))
            Hint("Supaya tetap jalan di HP Samsung: Info aplikasi → Baterai → Tidak dibatasi.")
        }

        // ------------------------------------------------ notifikasi ArusKu
        SectionTitle("Notifikasi dari ArusKu")
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Konfirmasi tiap transaksi", fontWeight = FontWeight.SemiBold)
                    Hint("Muncul notif kecil yang bisa dibalas untuk menambah catatan, misal \"shopee sepatu\".")
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = confirm, onCheckedChange = {
                    confirm = it
                    Prefs.confirm = it
                })
            }
            Spacer(Modifier.height(8.dp))
            Hint("Pertanyaan transfer (rekening sendiri atau bayar orang) tetap selalu muncul.")
        }

        // ------------------------------------------------ saldo
        SectionTitle("Saldo BCA")
        Panel {
            Text(
                if (balance != null) "Perkiraan saldo sekarang: ${rp(balance)}" else "Saldo awal belum diisi",
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Hint("Kalau beda dengan saldo asli di app BCA, ketik saldo yang benar. Hitungan dilanjutkan dari situ.")
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(onClick = { askSaldo = true }) {
                Text(if (balance != null) "Koreksi saldo" else "Isi saldo")
            }
        }

        // ------------------------------------------------ budget
        SectionTitle("Budget per kategori")
        Panel {
            Hint("Ketuk kategori untuk mengatur budget bulanan. Kamu dapat peringatan saat terpakai 80% dan 100%.")
            Spacer(Modifier.height(6.dp))
            categories.forEach { cat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { budgetFor = cat }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(categoryIcon(cat), contentDescription = null, tint = catColor(cat), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(cat, modifier = Modifier.weight(1f))
                    val b = budgets[cat]
                    Text(
                        if (b != null) rp(b) else "Atur",
                        color = if (b != null) cs.onSurface else cs.primary,
                        fontWeight = if (b != null) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        // ------------------------------------------------ data
        SectionTitle("Data")
        Panel {
            SettingRow(Icons.Rounded.FileDownload, "Ekspor ke CSV", "Simpan semua catatan sebagai cadangan / buka di Excel") {
                exporter.launch("ArusKu-${LocalDate.now()}.csv")
            }
            SettingRow(Icons.Rounded.Notifications, "Log notifikasi bank ($logCount)", "Notifikasi BCA & GoPay yang terbaca — untuk menyiapkan pembaca baru") {
                showLog = true
            }
            Spacer(Modifier.height(6.dp))
            Hint("Semua data hanya tersimpan di HP ini. Tidak ada yang dikirim ke internet.")
        }

        // ------------------------------------------------ tentang
        SectionTitle("Tentang")
        Panel {
            SettingRow(Icons.Rounded.Info, "ArusKu", "Dibuat oleh $APP_CREATOR", onClick = onAbout)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (askSaldo) SaldoDialog(current = balance, onDismiss = { askSaldo = false })
    budgetFor?.let { cat -> BudgetDialog(cat, budgets[cat], onDismiss = { budgetFor = null }) }
    if (showLog) LogDialog(onDismiss = { showLog = false })
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Hint(subtitle)
        }
    }
}

fun openListenerSettings(context: Context) {
    val generic = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val intent = if (Build.VERSION.SDK_INT >= 30) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, BankListener::class.java).flattenToString()
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    } else generic
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            context.startActivity(generic)
        } catch (_: Exception) {
        }
    }
}

fun openAppInfo(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}
