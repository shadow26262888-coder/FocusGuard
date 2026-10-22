package com.focusguard.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focusguard.databinding.ActivityChildPairingBinding
import com.focusguard.services.MonitorService
import com.focusguard.utils.AppPrefs
import com.focusguard.utils.FirebaseManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.UUID

/**
 * Çocuk cihazı: kendine özgü 6 haneli kod ve QR oluşturur,
 * ebeveyn eşleşmesini bekler.
 */
class ChildPairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildPairingBinding
    private lateinit var deviceId: String
    private lateinit var pairingCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cihaza özgü ID (kalıcı)
        deviceId = AppPrefs.getChildDeviceId(this) ?: run {
            val id = UUID.randomUUID().toString().replace("-", "").take(16)
            AppPrefs.setChildDeviceId(this, id)
            id
        }

        // 6 haneli gösterme kodu (deviceId'den türetilmiş)
        pairingCode = deviceId.filter { it.isDigit() }.take(6).padStart(6, '0')

        showLoadingState(true)

        // Firebase'e kaydet
        val dailyCode = AppPrefs.getDailyCode(this)
        FirebaseManager.registerChildDevice(this, deviceId, dailyCode) { success ->
            runOnUiThread {
                showLoadingState(false)
                if (success) {
                    displayPairingInfo()
                    waitForPairing()
                } else {
                    Toast.makeText(this, "Bağlantı hatası. İnternet bağlantınızı kontrol edin.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayPairingInfo() {
        binding.tvPairingCode.text = pairingCode
        binding.tvDeviceId.text = "Cihaz ID: $deviceId"

        // QR kod oluştur (deviceId içeriyor)
        try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(deviceId, BarcodeFormat.QR_CODE, 400, 400)
            val bitmap = BarcodeEncoder().createBitmap(matrix)
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            binding.ivQrCode.visibility = View.GONE
        }
    }

    /** Ebeveynin eşleşmesini Firebase'den dinle */
    private fun waitForPairing() {
        FirebaseManager.listenForCommands(deviceId) { cmd ->
            if (cmd == "paired") {
                runOnUiThread {
                    Toast.makeText(this, "Eşleşme tamamlandı!", Toast.LENGTH_SHORT).show()
                    MonitorService.start(this)
                    finish()
                }
            }
        }
    }

    private fun showLoadingState(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.contentGroup.visibility = if (loading) View.GONE else View.VISIBLE
    }
}
