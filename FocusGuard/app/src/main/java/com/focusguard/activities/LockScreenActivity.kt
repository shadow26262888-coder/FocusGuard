package com.focusguard.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focusguard.databinding.ActivityLockScreenBinding
import com.focusguard.services.LockOverlayService
import com.focusguard.utils.AppPrefs

/**
 * Tam ekran siyah kilit ekranı.
 * - Geri / Home / Recents tuşlarını engeller
 * - Kod doğrulama
 * - Sadece arama yapılabilir
 * - Master kod gizli alanı
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kilit ekranının üstüne çık (gerçek kilit ekranını da geç)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Kod gir butonu
        binding.btnSubmitCode.setOnClickListener {
            val input = binding.etCode.text.toString().trim()
            if (AppPrefs.isCodeValid(this, input)) {
                unlock()
            } else {
                Toast.makeText(this, "Yanlış kod!", Toast.LENGTH_SHORT).show()
                binding.etCode.text?.clear()
            }
        }

        // Arama butonu (sağ alt)
        binding.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            startActivity(intent)
        }

        // Master kod kısayolu
        binding.tvMasterCodeHint.setOnClickListener {
            binding.layoutMasterCode.visibility =
                if (binding.layoutMasterCode.visibility == View.VISIBLE) View.GONE
                else View.VISIBLE
        }

        binding.btnSubmitMasterCode.setOnClickListener {
            val masterInput = binding.etMasterCode.text.toString().trim()
            val master = AppPrefs.getMasterCode(this)
            if (master != null && masterInput == master) {
                unlock()
            } else {
                Toast.makeText(this, "Master kod hatalı", Toast.LENGTH_SHORT).show()
                binding.etMasterCode.text?.clear()
            }
        }
    }

    private fun unlock() {
        AppPrefs.setLocked(this, false)
        LockOverlayService.stop(this)
        Toast.makeText(this, "Kilit kaldırıldı", Toast.LENGTH_SHORT).show()
        finish()
    }

    // Geri tuşunu devre dışı bırak
    override fun onBackPressed() {
        // Hiçbir şey yapma
    }

    // Home / Recents engelle: LockOverlayService zaten üste getirir
    override fun onPause() {
        super.onPause()
        if (AppPrefs.isLocked(this)) {
            // Ön plana geri dön
            val i = Intent(this, LockScreenActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AppPrefs.isLocked(this)) {
            val i = Intent(this, LockScreenActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
        }
    }

    // Donanım tuşlarını yakala (ses hariç — VolumeKeyReceiver halleder)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true   // tüket, geçirme
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
