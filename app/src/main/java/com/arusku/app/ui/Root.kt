package com.arusku.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arusku.app.R
import com.arusku.app.data.APP_CREATOR
import com.arusku.app.data.Fmt
import com.arusku.app.data.Tx
import kotlinx.coroutines.delay
import java.time.YearMonth

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Beranda", Icons.Rounded.Home),
    Tab("Riwayat", Icons.Rounded.History),
    Tab("Rekap", Icons.Rounded.PieChart),
    Tab("Pengaturan", Icons.Rounded.Settings),
)

@Composable
fun ArusRoot() {
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1100)
        splash = false
    }

    // Izin menampilkan notifikasi ArusKu (Android 13+)
    val context = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Crossfade(targetState = splash, label = "splash") { showSplash ->
        if (showSplash) Splash() else MainShell()
    }
}

@Composable
private fun Splash() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(112.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF0F1A16)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(150.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("ArusKu", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Uangmu mengalir ke mana", color = cs.onSurfaceVariant)
        }
        Text(
            "by $APP_CREATOR",
            color = cs.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
        )
    }
}

@Composable
private fun MainShell() {
    val cs = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var month by remember { mutableStateOf(YearMonth.now(Fmt.zone)) }
    var filter by remember { mutableStateOf(TxFilter.ALL) }
    var editing by remember { mutableStateOf<Tx?>(null) }
    var adding by remember { mutableStateOf<EntryType?>(null) }
    var about by remember { mutableStateOf(false) }

    BackHandler(enabled = tab != 0) { tab = 0 }

    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            NavigationBar(containerColor = cs.surfaceContainerLow) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = cs.primaryContainer),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("Catat") },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    onClick = { adding = EntryType.TUNAI },
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                )
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeScreen(
                    onEdit = { editing = it },
                    onReport = { tab = 2 },
                    onReview = {
                        filter = TxFilter.REVIEW
                        tab = 1
                    },
                    onSettings = { tab = 3 },
                    onAbout = { about = true },
                )
                1 -> HistoryScreen(
                    month = month,
                    onMonth = { month = it },
                    filter = filter,
                    onFilter = { filter = it },
                    onEdit = { editing = it },
                )
                2 -> ReportScreen(month = month, onMonth = { month = it })
                else -> SettingsScreen(onAbout = { about = true })
            }
        }
    }

    editing?.let { tx ->
        TxEditor(initial = tx, startType = typeOf(tx), onDismiss = { editing = null })
    }
    adding?.let { type ->
        TxEditor(initial = null, startType = type, onDismiss = { adding = null })
    }
    if (about) AboutDialog(onDismiss = { about = false })
}
