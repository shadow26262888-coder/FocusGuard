package com.focusguard.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.focusguard.services.MonitorService
import com.focusguard.utils.AppPrefs

/**
 * Ses tuşuna 6-7 kez hızlı basıldığında acil çıkış tetikler.
 *
 * android.media.VOLUME_CHANGED_ACTION sinyalini dinler.
 * 2 saniye içinde 6 basış = acil çıkış.
 */
class VolumeKeyReceiver : BroadcastReceiver() {

    companion object {
        private const val REQUIRED_PRESSES = 6
        private const val WINDOW_MS = 2000L

        private var pressCount = 0
        private val handler = Handler(Looper.getMainLooper())
        private var resetRunnable: Runnable? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.media.VOLUME_CHANGED_ACTION") return

        // Acil çıkış zaten yapılmışsa tekrar yapma
        if (AppPrefs.isEmergencyDisabled(context)) return

        pressCount++

        // Zamanlayıcıyı sıfırla
        resetRunnable?.let { handler.removeCallbacks(it) }
        resetRunnable = Runnable { pressCount = 0 }.also {
            handler.postDelayed(it, WINDOW_MS)
        }

        if (pressCount >= REQUIRED_PRESSES) {
            pressCount = 0
            resetRunnable?.let { handler.removeCallbacks(it) }
            triggerEmergencyExit(context)
        }
    }

    private fun triggerEmergencyExit(context: Context) {
        // MonitorService üzerinden temiz çıkış yap
        val serviceIntent = Intent(context, MonitorService::class.java).apply {
            action = "EMERGENCY_EXIT"
        }
        // MonitorService zaten çalışıyorsa direkt çağır
        val monitorService = MonitorServiceHolder.instance
        if (monitorService != null) {
            monitorService.performEmergencyExit()
        } else {
            // Servis çalışmıyorsa manuel temizlik
            AppPrefs.setEmergencyDisabled(context, true)
            AppPrefs.setLocked(context, false)
            AppPrefs.clearAll(context)
        }
    }
}

/** MonitorService'e referans tutmak için basit holder */
object MonitorServiceHolder {
    var instance: MonitorService? = null
}
