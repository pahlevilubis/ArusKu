@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.arusku.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arusku.app.data.Categorizer
import com.arusku.app.data.Kind
import com.arusku.app.data.QuickParse
import com.arusku.app.data.Repo
import com.arusku.app.data.Source
import com.arusku.app.data.Tx
import com.arusku.app.data.rp
import com.arusku.app.ui.ArusTheme
import com.arusku.app.ui.Hint
import com.arusku.app.ui.Pill
import kotlinx.coroutines.delay

/** Layar catat cepat yang dibuka dari widget. */
class QuickAddActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PRESET = "preset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Repo.init(applicationContext)
        val preset = intent.getStringExtra(EXTRA_PRESET).orEmpty()
        setContent {
            ArusTheme {
                QuickSheet(preset = preset, onDone = { finish() }, onSaved = { msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    finish()
                })
            }
        }
    }
}

@Composable
private fun QuickSheet(preset: String, onDone: () -> Unit, onSaved: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var field by remember { mutableStateOf(TextFieldValue(preset, selection = TextRange(preset.length))) }
    var bca by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val parsed = remember(field.text) { QuickParse.parse(field.text) }
    val guessed = remember(parsed.note) { Repo.guessCategory(parsed.note) }
    val category = chosen ?: guessed

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focus.requestFocus()
            keyboard?.show()
        } catch (_: Exception) {
        }
    }

    fun save() {
        val amount = parsed.amount ?: return
        val note = parsed.note.trim()
        val tx = Tx(
            time = System.currentTimeMillis(),
            amount = amount,
            kind = if (bca) Kind.OUT else Kind.DETAIL,
            source = if (bca) Source.MANUAL else Source.TUNAI,
            category = category,
            note = note,
        )
        Repo.save(tx)
        if (chosen != null && note.isNotBlank()) {
            Categorizer.keyOf(note)?.let { Repo.learn(it, category) }
        }
        onSaved("Tercatat: ${rp(amount)} · $category")
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = cs.surfaceContainerHigh,
            // Ketukan di dalam lembar ini tidak menutup layar
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Catat cepat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("ArusKu", color = cs.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Uang tunai", !bca) { bca = false }
                    Pill("Pengeluaran BCA", bca) { bca = true }
                }
                Spacer(Modifier.height(6.dp))
                Hint(
                    if (bca) "Untuk transaksi BCA yang tidak tercatat otomatis. Dihitung pengeluaran."
                    else "Rincian uang tunai — tidak menambah total (tarik tunainya sudah dihitung dari BCA)."
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    placeholder = { Text("bakso 25rb") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    parsed.amount?.let { rp(it) } ?: "Ketik jumlahnya, misal 25rb",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (parsed.amount != null) cs.primary else cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val options = if (category in Categorizer.DEFAULTS) Categorizer.DEFAULTS else Categorizer.DEFAULTS + category
                    options.forEach { c -> Pill(c, c == category) { chosen = c } }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text("Batal") }
                    Button(onClick = { save() }, enabled = parsed.amount != null) { Text("Simpan") }
                }
            }
        }
    }
}
