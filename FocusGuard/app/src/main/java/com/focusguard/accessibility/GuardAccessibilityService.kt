package com.focusguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusguard.services.LockOverlayService
import com.focusguard.utils.AppPrefs

/**
 * Accessibility servisi iki iş yapar:
 * 1. Uygulamayı silmeye çalışan ekranı algılar ve geri döner
 * 2. Kilit aktifken başka uygulama öne çıkmaya çalışırsa kilit ekranını geri getirir
 */
class GuardAccessibilityService : AccessibilityService() {

    // Paket kaldırıcı paketler
    private val uninstallPackages = setOf(
        "com.android.packageinstaller",
        "com.android.settings",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller"
    )

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = null // Tüm paketleri izle
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        // 1. Silme ekranı tespiti
        if (pkg in uninstallPackages) {
            val source = event.source
            if (source != null && containsUninstallText(source)) {
                // Geri git (silme işlemini iptal et)
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }

        // 2. Kilit aktifken başka app öne çıktıysa geri getir
        if (AppPrefs.isLocked(this) &&
            pkg != applicationContext.packageName &&
            pkg != "com.android.dialer" &&   // Arama uygulamasına izin ver
            pkg != "com.android.phone" &&
            pkg != "com.samsung.android.incallui") {
            LockOverlayService.start(this)
        }
    }

    override fun onInterrupt() {}

    private fun containsUninstallText(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val keywords = listOf("uninstall", "kaldır", "sil", "delete", "remove")
        if (keywords.any { text.contains(it) || contentDesc.contains(it) }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsUninstallText(child)) return true
        }
        return false
    }
}
