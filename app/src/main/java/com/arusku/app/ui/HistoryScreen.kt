package com.arusku.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arusku.app.data.Fmt
import com.arusku.app.data.Kind
import com.arusku.app.data.Repo
import com.arusku.app.data.Tx
import com.arusku.app.data.rp
import java.time.YearMonth

enum class TxFilter(val label: String) {
    ALL("Semua"),
    OUT("Pengeluaran"),
    IN("Masuk"),
    DETAIL("Rincian tunai"),
    REVIEW("Perlu dicek"),
}

@Composable
fun HistoryScreen(
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    filter: TxFilter,
    onFilter: (TxFilter) -> Unit,
    onEdit: (Tx) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val list = rememberLoad(month, filter, initial = emptyList<Tx>()) {
        if (filter == TxFilter.REVIEW) Repo.reviewItems() else Repo.txMonth(month)
    }
    val shown = list.filter { tx ->
        when (filter) {
            TxFilter.ALL, TxFilter.REVIEW -> true
            TxFilter.OUT -> tx.kind == Kind.OUT || tx.kind == Kind.SELF_OUT
            TxFilter.IN -> tx.kind.isIn
            TxFilter.DETAIL -> tx.kind == Kind.DETAIL
        }
    }
    val byDay = shown.groupBy { Fmt.date(it.time) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
    ) {
        item { ScreenTitle("Riwayat") }
        if (filter != TxFilter.REVIEW) {
            item { MonthSwitcher(month, onMonth) }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TxFilter.entries.forEach { f -> Pill(f.label, f == filter) { onFilter(f) } }
            }
        }
        if (shown.isEmpty()) {
            item {
                EmptyNote(
                    if (filter == TxFilter.REVIEW) "Tidak ada transfer yang perlu dicek."
                    else "Belum ada transaksi di sini."
                )
            }
        }
        byDay.forEach { (date, txs) ->
            item(key = "d$date") {
                val keluar = txs.filter { it.kind == Kind.OUT }.sumOf { it.amount }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Fmt.day(date),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (keluar > 0) {
                        Text("Keluar ${rp(keluar)}", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
            items(txs, key = { it.id }) { tx -> TxRow(tx) { onEdit(tx) } }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
