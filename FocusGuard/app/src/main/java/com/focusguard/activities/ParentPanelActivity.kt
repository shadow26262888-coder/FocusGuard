package com.focusguard.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.focusguard.databinding.ActivityParentPanelBinding
import com.focusguard.utils.AppPrefs
import com.focusguard.utils.FirebaseManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Ebeveyn paneli:
 * - QR tarama veya manuel kod ile eşleşme
 * - Kilitle / Kilidi Aç butonları
 * - Master kod ayarlama
 */
class ParentPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentPanelBinding
    private var pairedChildId: String? = null

    // QR tarayıcı launcher
    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.etChildCode.setText(result.contents)
            pairWithChild(result.contents)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openQrScanner() else Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pairedChildId = AppPrefs.getPairedChildId(this)

        if (pairedChildId != null) {
            showPairedUI()
        } else {
            showPairingUI()
        }

        setupListeners()
    }

    private fun setupListeners() {
        // QR tarama
        binding.btnScanQr.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                openQrScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Manuel eşleşme
        binding.btnManualPair.setOnClickListener {
            val code = binding.etChildCode.text.toString().trim()
            if (code.length < 6) {
                Toast.makeText(this, "Geçerli bir kod girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pairWithChild(code)
        }

        // Kilitle
        binding.btnLock.setOnClickListener {
            val childId = pairedChildId ?: return@setOnClickListener
            FirebaseManager.setLockedState(childId, true) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Cihaz kilitlendi", Toast.LENGTH_SHORT).show()
                        binding.tvStatus.text = "Durum: KİLİTLİ 🔒"
                    }
                }
            }
            FirebaseManager.sendCommand(childId, "lock")
        }

        // Kilidi aç
        binding.btnUnlock.setOnClickListener {
            val childId = pairedChildId ?: return@setOnClickListener
            FirebaseManager.setLockedState(childId, false) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Kilit kaldırıldı", Toast.LENGTH_SHORT).show()
                        binding.tvStatus.text = "Durum: Açık 🔓"
                    }
                }
            }
            FirebaseManager.sendCommand(childId, "unlock")
        }

        // Master kod kaydet
        binding.btnSaveMasterCode.setOnClickListener {
            val code = binding.etMasterCode.text.toString().trim()
            if (code.length < 4) {
                Toast.makeText(this, "En az 4 haneli bir kod girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppPrefs.setMasterCode(this, code)
            pairedChildId?.let { childId ->
                FirebaseManager.sendCommand(childId, "mastercode:$code")
            }
            Toast.makeText(this, "Master kod kaydedildi", Toast.LENGTH_SHORT).show()
        }

        // Günlük kodu yenile
        binding.btnRefreshDailyCode.setOnClickListener {
            val childId = pairedChildId ?: return@setOnClickListener
            val newCode = AppPrefs.getDailyCode(this)
            FirebaseManager.updateDailyCode(childId, newCode)
            FirebaseManager.sendCommand(childId, "dailycode:$newCode")
            binding.tvDailyCode.text = "Günlük Kod: $newCode"
            Toast.makeText(this, "Günlük kod güncellendi: $newCode", Toast.LENGTH_LONG).show()
        }
    }

    private fun pairWithChild(childDeviceId: String) {
        val masterCode = binding.etMasterCode.text.toString().trim().ifEmpty { "000000" }
        binding.progressPairing.visibility = View.VISIBLE

        // parentDeviceId
        val parentId = "parent_${System.currentTimeMillis()}"

        FirebaseManager.pairParentWithChild(parentId, childDeviceId, masterCode) { success ->
            runOnUiThread {
                binding.progressPairing.visibility = View.GONE
                if (success) {
                    AppPrefs.setPairedChildId(this, childDeviceId)
                    AppPrefs.setMasterCode(this, masterCode)
                    pairedChildId = childDeviceId
                    // Çocuğa "eşleşildi" komutu gönder
                    FirebaseManager.sendCommand(childDeviceId, "paired")
                    showPairedUI()
                    Toast.makeText(this, "Eşleşme başarılı!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Eşleşme başarısız. Kodu kontrol edin.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openQrScanner() {
        val options = ScanOptions().apply {
            setPrompt("Çocuk cihazındaki QR kodu tarayın")
            setBeepEnabled(true)
            setOrientationLocked(false)
        }
        qrScanLauncher.launch(options)
    }

    private fun showPairingUI() {
        binding.layoutPairing.visibility = View.VISIBLE
        binding.layoutControl.visibility = View.GONE
    }

    private fun showPairedUI() {
        binding.layoutPairing.visibility = View.GONE
        binding.layoutControl.visibility = View.VISIBLE
        val dailyCode = AppPrefs.getDailyCode(this)
        binding.tvDailyCode.text = "Günlük Kod: $dailyCode"
        binding.tvPairedChildId.text = "Çocuk ID: $pairedChildId"
    }
}
