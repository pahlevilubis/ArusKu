package com.arusku.app

import android.app.Application
import com.arusku.app.data.Repo
import com.arusku.app.notif.Notifier

class ArusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        Notifier.createChannels(this)
    }
}
