package com.arusku.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arusku.app.R
import com.arusku.app.data.APP_CREATOR
import com.arusku.app.data.Fmt
import com.arusku.app.data.LogRow
import com.arusku.app.data.Money
import com.arusku.app.data.QuickParse
import com.arusku.app.data.Repo
import com.arusku.app.data.rp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
        } catch (e: Exception) {
            "-"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        icon = {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF0F1A16)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(96.dp))
            }
        },
        title = {
            Text("ArusKu", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pencatat arus uang, offline", color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("Versi $version", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Text("Dibuat oleh", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(
                    APP_CREATOR,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text("© 2026 $APP_CREATOR", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

/** Isi / koreksi saldo BCA. */
@Composable
fun SaldoDialog(current: Long?, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.let { Money.group(it) } ?: "") }
    val amount = QuickParse.amountOf(text, smallIsThousands = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(if (current == null) "Isi saldo BCA" else "Koreksi saldo BCA", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Buka app BCA, lihat saldo sekarang, lalu ketik di sini. Setelah itu ArusKu menghitung sendiri dari transaksi yang masuk dan keluar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Saldo sekarang (Rp)") },
                    placeholder = { Text("misal 3.250.000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (amount != null) {
                    Spacer(Modifier.height(6.dp))
                    Hint(rp(amount))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = amount != null, onClick = {
                if (amount != null) {
                    Repo.runAsync { Repo.setSaldo(amount) }
                    onDismiss()
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun BudgetDialog(category: String, current: Long?, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.let { Money.group(it) } ?: "") }
    val amount = QuickParse.amountOf(text, smallIsThousands = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Budget $category", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Batas pengeluaran per bulan untuk kategori ini.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Budget per bulan") },
                    placeholder = { Text("misal 1,5jt atau 800rb") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (amount != null) {
                    Spacer(Modifier.height(6.dp))
                    Hint(rp(amount))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = amount != null, onClick = {
                if (amount != null) {
                    Repo.runAsync { Repo.setBudget(category, amount) }
                    onDismiss()
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            if (current != null) {
                TextButton(onClick = {
                    Repo.runAsync { Repo.setBudget(category, 0) }
                    onDismiss()
                }) { Text("Hapus budget") }
            } else {
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        },
    )
}

@Composable
fun LogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val rows = rememberLoad(initial = emptyList<LogRow>()) { Repo.notifLog() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        title = { Text("Log notifikasi bank", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Hint("100 notifikasi terakhir dari BCA & GoPay. Kode OTP/PIN tidak pernah disimpan. Salin dan kirim ke Claude kalau mau menambah pembaca baru.")
                Spacer(Modifier.height(8.dp))
                if (rows.isEmpty()) {
                    Text("Belum ada.", color = cs.onSurfaceVariant)
                }
                rows.forEachIndexed { i, r ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)
                    Text("${Fmt.dateTime(r.time)} · ${r.pkg}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    if (r.title.isNotBlank()) Text(r.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text(r.body, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = rows.isNotEmpty(), onClick = {
                val text = rows.joinToString("\n\n") { "[${Fmt.dateTime(it.time)}] ${it.pkg}\n${it.title}\n${it.body}" }
                copy(context, text)
            }) { Text("Salin semua") }
        },
        dismissButton = {
            TextButton(onClick = {
                Repo.runAsync { Repo.clearLog() }
                onDismiss()
            }) { Text("Hapus log") }
        },
    )
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText("ArusKu", text))
    Toast.makeText(context, "Tersalin", Toast.LENGTH_SHORT).show()
}
