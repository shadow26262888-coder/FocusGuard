package com.focusguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp

class FocusGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)

            mgr.createNotificationChannel(
                NotificationChannel(
                    "lock_channel",
                    "Kilit Servisi",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Cihaz kilit durumu bildirimi" }
            )

            mgr.createNotificationChannel(
                NotificationChannel(
                    "monitor_channel",
                    "İzleme Servisi",
                    NotificationManager.IMPORTANCE_MIN
                ).apply { description = "Arka plan izleme servisi" }
            )
        }
    }
}
