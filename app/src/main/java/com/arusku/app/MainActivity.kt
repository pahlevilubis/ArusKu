package com.arusku.app

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arusku.app.notif.BankListener
import com.arusku.app.ui.AppState
import com.arusku.app.ui.ArusRoot
import com.arusku.app.ui.ArusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            ArusTheme {
                ArusRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppState.resumed()
        // Sambungkan ulang pembaca notifikasi kalau sempat dimatikan sistem
        if (AppState.listenerEnabled(this)) {
            try {
                NotificationListenerService.requestRebind(ComponentName(this, BankListener::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
