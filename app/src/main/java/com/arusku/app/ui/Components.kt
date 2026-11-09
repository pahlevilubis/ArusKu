package com.arusku.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.arusku.app.data.Fmt
import com.arusku.app.data.Kind
import com.arusku.app.data.Repo
import com.arusku.app.data.Source
import com.arusku.app.data.Tx
import com.arusku.app.data.rp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.time.YearMonth

/** Status aplikasi yang perlu dicek ulang tiap kali app dibuka lagi. */
object AppState {
    val resumeTick = MutableStateFlow(0)

    fun resumed() {
        resumeTick.value = resumeTick.value + 1
    }

    fun listenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

/** Muat data dari database di background, dan muat ulang otomatis setiap data berubah. */
@Composable
fun <T> rememberLoad(vararg keys: Any?, initial: T, load: () -> T): T {
    val ver by Repo.changes.collectAsState()
    val loader by rememberUpdatedState(load)
    val allKeys = arrayOf<Any?>(ver, *keys)
    val state: State<T> = produceState(initial, *allKeys) {
        value = withContext(Dispatchers.IO) { loader() }
    }
    return state.value
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Text(
                action,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

@Composable
fun EmptyNote(text: String) {
    Panel {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val f = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) cs.primaryContainer else cs.surfaceContainerHighest,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurface,
        border = if (selected) BorderStroke(1.dp, cs.primary) else null,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun MonthSwitcher(month: YearMonth, onChange: (YearMonth) -> Unit) {
    val now = YearMonth.now(Fmt.zone)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Bulan sebelumnya")
        }
        Text(
            Fmt.month(month),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange(month.plusMonths(1)) }, enabled = month.isBefore(now)) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Bulan berikutnya")
        }
    }
}

@Composable
fun StatBlock(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

fun categoryIcon(category: String): ImageVector {
    val c = category.lowercase()
    return when {
        c.contains("makan") || c.contains("minum") -> Icons.Rounded.Restaurant
        c.contains("transport") || c.contains("bensin") -> Icons.Rounded.LocalGasStation
        c.contains("belanja") -> Icons.Rounded.ShoppingBag
        c.contains("tagihan") || c.contains("listrik") -> Icons.Rounded.Bolt
        c.contains("hiburan") -> Icons.Rounded.Movie
        c.contains("kesehatan") -> Icons.Rounded.LocalHospital
        c.contains("pendidikan") -> Icons.Rounded.School
        c.contains("sosial") -> Icons.Rounded.VolunteerActivism
        c.contains("transfer") -> Icons.Rounded.SwapHoriz
        c.contains("tunai") -> Icons.Rounded.Payments
        else -> Icons.Rounded.Payments
    }
}

private fun txIcon(tx: Tx): Pair<ImageVector, Color> = when (tx.kind) {
    Kind.IN_SELF, Kind.IN_OTHER -> Pair(Icons.Rounded.Savings, Mint)
    Kind.SELF_OUT -> Pair(Icons.Rounded.SwapHoriz, Sky)
    Kind.DETAIL -> Pair(if (tx.source == Source.GOPAY) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.Payments, catColor(tx.category))
    Kind.OUT -> Pair(categoryIcon(tx.category), catColor(tx.category))
}

fun amountLabel(tx: Tx): String = when (tx.kind) {
    Kind.OUT -> "−" + rp(tx.amount)
    Kind.IN_SELF, Kind.IN_OTHER -> "+" + rp(tx.amount)
    Kind.SELF_OUT -> "⇄ " + rp(tx.amount)
    Kind.DETAIL -> rp(tx.amount)
}

@Composable
fun TxRow(tx: Tx, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val (icon, tint) = txIcon(tx)
    val sub = buildList {
        if (tx.note.isNotBlank()) add(tx.category)
        if (tx.kind == Kind.DETAIL) add("rincian")
        add(tx.source.label)
        add(Fmt.time(tx.time))
    }.joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                amountLabel(tx),
                fontWeight = FontWeight.SemiBold,
                color = when (tx.kind) {
                    Kind.IN_SELF, Kind.IN_OTHER -> Mint
                    Kind.OUT -> cs.onSurface
                    else -> cs.onSurfaceVariant
                },
            )
            if (tx.review) Text("Perlu dicek", color = Gold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CatLine(stat: com.arusku.app.data.CatStat, maxTotal: Long, extra: String? = null) {
    val cs = MaterialTheme.colorScheme
    val budget = stat.budget
    val color = catColor(stat.name)
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(stat.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(rp(stat.total), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        if (budget != null && budget > 0) {
            val f = stat.total.toFloat() / budget
            Bar(f, if (f >= 1f) Rose else if (f >= 0.8f) Gold else color)
            Spacer(Modifier.height(4.dp))
            Hint(
                if (stat.total > budget) "Lewat ${rp(stat.total - budget)} dari budget ${rp(budget)}"
                else "Sisa budget ${rp(budget - stat.total)} dari ${rp(budget)}",
                color = if (f >= 1f) Rose else cs.onSurfaceVariant,
            )
        } else {
            Bar(if (maxTotal > 0) stat.total.toFloat() / maxTotal else 0f, color)
        }
        if (extra != null) {
            Spacer(Modifier.height(4.dp))
            Hint(extra)
        }
    }
}

@Composable
fun BarList(items: List<Pair<String, Long>>, color: Color) {
    val max = items.maxOfOrNull { it.second } ?: 0L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(76.dp), maxLines = 1)
                Bar(
                    if (max > 0) value.toFloat() / max else 0f,
                    color,
                    modifier = Modifier.weight(1f),
                    height = 10.dp,
                )
                Text(
                    com.arusku.app.data.Money.short(value),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(76.dp),
                    maxLines = 1,
                )
            }
        }
    }
}
