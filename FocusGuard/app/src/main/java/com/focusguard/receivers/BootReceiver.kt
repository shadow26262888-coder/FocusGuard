package com.focusguard.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusguard.services.LockOverlayService
import com.focusguard.services.MonitorService
import com.focusguard.utils.AppPrefs

/**
 * Cihaz yeniden başladığında servisleri başlat.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        val role = AppPrefs.getRole(context) ?: return
        if (role != "child") return
        if (!AppPrefs.isPaired(context)) return
        if (AppPrefs.isEmergencyDisabled(context)) return

        // İzleme servisini başlat
        MonitorService.start(context)

        // Kilit durumuna göre overlay başlat
        if (AppPrefs.isLocked(context)) {
            LockOverlayService.start(context)
        }
    }
}
