package com.focusguard.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.focusguard.R
import com.focusguard.receivers.MonitorServiceHolder
import com.focusguard.utils.AppPrefs
import com.focusguard.utils.FirebaseManager
import java.util.Calendar

/**
 * Çocuk cihazında sürekli çalışan arka plan servisi:
 * 1. Firebase komutlarını (lock/unlock/mastercode/dailycode) dinler
 * 2. 18:00 - 08:00 zaman kuralını kontrol eder
 * 3. Kilit durumunu dakikada bir denetler
 */
class MonitorService : Service() {

    companion object {
        private const val NOTIF_ID = 1002
        private const val CHECK_INTERVAL_MS = 60_000L // 1 dakika

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val timeCheckRunnable = object : Runnable {
        override fun run() {
            checkTimeBasedLock()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        MonitorServiceHolder.instance = this
        startForegroundNotification()
        startListeningFirebase()
        handler.post(timeCheckRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        MonitorServiceHolder.instance = null
        handler.removeCallbacks(timeCheckRunnable)
        val deviceId = AppPrefs.getChildDeviceId(this) ?: return
        FirebaseManager.stopListening(deviceId)
    }

    // -----------------------------------------------------------------------

    private fun startForegroundNotification() {
        val notif = NotificationCompat.Builder(this, "monitor_channel")
            .setContentTitle("FocusGuard")
            .setContentText("Ebeveyn denetimi aktif")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    private fun startListeningFirebase() {
        val deviceId = AppPrefs.getChildDeviceId(this) ?: return

        FirebaseManager.listenForCommands(deviceId) { cmd ->
            handleCommand(cmd)
        }

        FirebaseManager.listenForLockedState(deviceId) { locked ->
            AppPrefs.setLocked(this, locked)
            if (locked) {
                LockOverlayService.start(this)
            } else {
                LockOverlayService.stop(this)
            }
        }
    }

    private fun handleCommand(cmd: String) {
        when {
            cmd == "lock" -> {
                AppPrefs.setLocked(this, true)
                LockOverlayService.start(this)
            }
            cmd == "unlock" -> {
                AppPrefs.setLocked(this, false)
                LockOverlayService.stop(this)
            }
            cmd == "emergency" -> performEmergencyExit()
            cmd.startsWith("mastercode:") -> {
                val newCode = cmd.removePrefix("mastercode:")
                AppPrefs.setMasterCode(this, newCode)
            }
        }
    }

    /** 18:00 - 08:00 arası otomatik kilitleme */
    private fun checkTimeBasedLock() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val lockStart = AppPrefs.getLockStartHour(this)   // default 18
        val lockEnd = AppPrefs.getLockEndHour(this)       // default 8

        val shouldBeLocked = hour >= lockStart || hour < lockEnd

        if (shouldBeLocked && !AppPrefs.isLocked(this)) {
            AppPrefs.setLocked(this, true)
            LockOverlayService.start(this)
        }
    }

    /** Acil çıkış: tüm kısıtlamaları kaldır */
    fun performEmergencyExit() {
        AppPrefs.setEmergencyDisabled(this, true)
        AppPrefs.setLocked(this, false)

        val childId = AppPrefs.getChildDeviceId(this)
        val parentId = AppPrefs.getPairedParentId(this)

        if (childId != null && parentId != null) {
            FirebaseManager.unpairDevice(childId, parentId)
        }

        LockOverlayService.stop(this)

        AppPrefs.clearAll(this)
        stop(this)
    }
}
