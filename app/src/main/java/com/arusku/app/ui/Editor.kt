@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.arusku.app.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arusku.app.data.CAT_LAIN
import com.arusku.app.data.CAT_ISI
import com.arusku.app.data.CAT_MASUK_LAIN
import com.arusku.app.data.CAT_PINDAH
import com.arusku.app.data.Categorizer
import com.arusku.app.data.Fmt
import com.arusku.app.data.Kind
import com.arusku.app.data.Money
import com.arusku.app.data.QuickParse
import com.arusku.app.data.Repo
import com.arusku.app.data.Source
import com.arusku.app.data.Tx
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** Jenis catatan yang bisa dipilih saat mencatat / mengubah. */
enum class EntryType(val label: String) {
    OUT("Pengeluaran BCA"),
    TUNAI("Rincian tunai"),
    IN_SELF("Isi BCA"),
    IN_OTHER("Pemasukan lain"),
    SELF_OUT("Ke rek. sendiri");

    val hasCategory: Boolean get() = this == OUT || this == TUNAI

    val fixedCategory: String
        get() = when (this) {
            IN_SELF -> CAT_ISI
            IN_OTHER -> CAT_MASUK_LAIN
            SELF_OUT -> CAT_PINDAH
            else -> ""
        }

    val help: String
        get() = when (this) {
            OUT -> "Uang keluar dari BCA yang tidak tercatat otomatis. Dihitung pengeluaran."
            TUNAI -> "Uang tunai habis buat apa. Tidak menambah total, karena tarik tunainya sudah dihitung dari BCA."
            IN_SELF -> "Transfer dari rekening sendiri (HSBC / bank lain) ke BCA."
            IN_OTHER -> "Uang masuk lain: refund, dibayar teman, dll."
            SELF_OUT -> "Uang keluar dari BCA ke rekening sendiri. Tidak dihitung pengeluaran."
        }
}

fun typeOf(tx: Tx): EntryType = when (tx.kind) {
    Kind.OUT -> EntryType.OUT
    Kind.DETAIL -> EntryType.TUNAI
    Kind.IN_SELF -> EntryType.IN_SELF
    Kind.IN_OTHER -> EntryType.IN_OTHER
    Kind.SELF_OUT -> EntryType.SELF_OUT
}

private fun kindSource(type: EntryType, old: Tx?): Pair<Kind, Source> {
    val wasBca = old?.source == Source.BCA
    return when (type) {
        EntryType.OUT -> Pair(Kind.OUT, if (wasBca) Source.BCA else Source.MANUAL)
        EntryType.TUNAI -> Pair(Kind.DETAIL, if (old?.source == Source.GOPAY) Source.GOPAY else Source.TUNAI)
        EntryType.IN_SELF -> Pair(Kind.IN_SELF, if (wasBca) Source.BCA else Source.MANUAL)
        EntryType.IN_OTHER -> Pair(Kind.IN_OTHER, if (wasBca) Source.BCA else Source.MANUAL)
        EntryType.SELF_OUT -> Pair(Kind.SELF_OUT, if (wasBca) Source.BCA else Source.MANUAL)
    }
}

/** Dialog untuk mencatat transaksi baru (initial = null) atau mengubah yang sudah ada. */
@Composable
fun TxEditor(initial: Tx?, startType: EntryType, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isNew = initial == null

    var type by remember { mutableStateOf(startType) }
    var quick by remember { mutableStateOf(TextFieldValue("")) }
    var amountText by remember { mutableStateOf(initial?.let { Money.group(it.amount) } ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var category by remember {
        mutableStateOf(
            if (initial != null && (initial.kind == Kind.OUT || initial.kind == Kind.DETAIL)) initial.category else ""
        )
    }
    var catTouched by remember { mutableStateOf(!isNew) }
    var newCat by remember { mutableStateOf(false) }
    var newCatText by remember { mutableStateOf("") }
    var time by remember { mutableLongStateOf(initial?.time ?: System.currentTimeMillis()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val choices = rememberLoad(initial = Categorizer.DEFAULTS) { Repo.categoryChoices() }
    val focus = remember { FocusRequester() }

    val parsed = remember(quick.text) { QuickParse.parse(quick.text) }
    val amount: Long? = if (isNew) parsed.amount else QuickParse.amountOf(amountText, smallIsThousands = false)
    val finalNote = (if (isNew) parsed.note else note).trim()
    val guessed = remember(finalNote) { Repo.guessCategory(finalNote) }
    val shownCat = when {
        !type.hasCategory -> type.fixedCategory
        catTouched && category.isNotBlank() -> category
        else -> guessed
    }

    LaunchedEffect(Unit) {
        if (isNew) {
            delay(200)
            try {
                focus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    fun save() {
        val amt = amount ?: return
        val (kind, source) = kindSource(type, initial)
        val cat = if (type.hasCategory) shownCat.ifBlank { CAT_LAIN } else type.fixedCategory
        val tx = Tx(
            id = initial?.id ?: 0L,
            time = time,
            amount = amt,
            kind = kind,
            source = source,
            category = cat,
            note = finalNote,
            raw = initial?.raw ?: "",
            dedup = initial?.dedup ?: "",
            review = false,
        )
        val learnWord = if (type.hasCategory && catTouched && finalNote.isNotBlank()) Categorizer.keyOf(finalNote) else null
        Repo.runAsync {
            Repo.save(tx)
            if (learnWord != null) Repo.learn(learnWord, cat)
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        title = { Text(if (isNew) "Catat" else "Ubah transaksi", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EntryType.entries.forEach { t -> Pill(t.label, t == type) { type = t } }
                }
                Spacer(Modifier.height(8.dp))
                Hint(type.help)
                Spacer(Modifier.height(12.dp))

                if (isNew) {
                    OutlinedTextField(
                        value = quick,
                        onValueChange = { quick = it },
                        label = { Text("Ketik singkat") },
                        placeholder = { Text(if (type.hasCategory) "bakso 25rb" else "2jt dari HSBC") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { save() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Jumlah (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        placeholder = { Text("misal: shopee sepatu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    amount?.let { Money.rp(it) } ?: "Belum ada jumlah",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (amount != null) cs.primary else cs.onSurfaceVariant,
                )
                if (isNew && finalNote.isNotBlank()) Hint(finalNote)

                if (type.hasCategory) {
                    Spacer(Modifier.height(12.dp))
                    Text("Kategori", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    val options = if (shownCat.isNotBlank() && shownCat !in choices) choices + shownCat else choices
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        options.forEach { c ->
                            Pill(c, c == shownCat && !newCat) {
                                category = c
                                catTouched = true
                                newCat = false
                            }
                        }
                        Pill("+ Baru", newCat) { newCat = true }
                    }
                    if (newCat) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newCatText,
                            onValueChange = {
                                newCatText = it
                                category = it.trim().take(30)
                                catTouched = true
                            },
                            label = { Text("Nama kategori baru") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = { pickDate(context, time) { time = it } }) {
                        Text(Fmt.dateTime(time))
                    }
                }

                if (initial != null && initial.raw.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Hint("Dari notifikasi: ${initial.raw}")
                }

                if (initial != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (confirmDelete) {
                                Repo.runAsync { Repo.delete(initial) }
                                onDismiss()
                            } else {
                                confirmDelete = true
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = cs.error),
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (confirmDelete) "Ketuk lagi untuk menghapus" else "Hapus transaksi")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = amount != null, onClick = { save() }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

fun pickDate(context: Context, current: Long, onPicked: (Long) -> Unit) {
    val zone = Fmt.zone
    val dt = Instant.ofEpochMilli(current).atZone(zone)
    val dialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val picked = ZonedDateTime.of(LocalDate.of(y, m + 1, d), dt.toLocalTime(), zone)
            onPicked(picked.toInstant().toEpochMilli())
        },
        dt.year, dt.monthValue - 1, dt.dayOfMonth,
    )
    dialog.datePicker.maxDate = System.currentTimeMillis()
    dialog.show()
}
