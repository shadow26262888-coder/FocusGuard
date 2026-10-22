package com.focusguard.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.focusguard.utils.AppPrefs

/**
 * Cihaz Yöneticisi yetkilerini yönetir.
 * Uygulama silinmek istenirse engeller.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        AppPrefs::class.java.getDeclaredField("KEY_DEVICE_ADMIN_ENABLED").also {
            // Yetki alındı kaydı
        }
        Toast.makeText(context, "Cihaz yönetici yetkisi verildi", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Bu uygulamayı devre dışı bırakmak için ebeveyn koduna ihtiyacınız var."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Yetki kaldırıldı — acil çıkış veya ebeveyn kaldırdı
        Toast.makeText(context, "Cihaz yönetici yetkisi kaldırıldı", Toast.LENGTH_SHORT).show()
    }
}
