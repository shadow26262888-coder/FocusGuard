package com.focusguard.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.focusguard.databinding.ActivityRoleSelectionBinding
import com.focusguard.services.LockOverlayService
import com.focusguard.services.MonitorService
import com.focusguard.utils.AppPrefs

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Eğer zaten bir rol seçilmişse ve eşleşme tamamsa doğru ekrana yönlendir
        val role = AppPrefs.getRole(this)
        if (role != null && AppPrefs.isPaired(this)) {
            navigateToRole(role)
            finish()
            return
        }

        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnChild.setOnClickListener {
            AppPrefs.setRole(this, "child")
            startActivity(Intent(this, ChildPairingActivity::class.java))
        }

        binding.btnParent.setOnClickListener {
            AppPrefs.setRole(this, "parent")
            startActivity(Intent(this, ParentPanelActivity::class.java))
        }
    }

    private fun navigateToRole(role: String) {
        if (role == "child") {
            // Child: kilit durumunu kontrol et, servisleri başlat
            MonitorService.start(this)
            if (AppPrefs.isLocked(this)) {
                LockOverlayService.start(this)
            }
        } else {
            startActivity(Intent(this, ParentPanelActivity::class.java))
        }
    }
}
