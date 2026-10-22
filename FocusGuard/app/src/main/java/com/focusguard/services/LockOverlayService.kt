package com.focusguard.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.focusguard.R
import com.focusguard.activities.LockScreenActivity
import com.focusguard.utils.AppPrefs

/**
 * SYSTEM_ALERT_WINDOW yetkisiyle tam ekran siyah overlay çizer.
 * Bu servis LockScreenActivity'yi sürekli önde tutar.
 * Overlay, Home / Recents sonrasında ekrana geri döner.
 */
class LockOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LockOverlayService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockOverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        showOverlay()
        bringLockScreenToFront()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Öldürülürse yeniden başlat
        bringLockScreenToFront()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    // -----------------------------------------------------------------------

    private fun startForegroundNotification() {
        val notif = NotificationCompat.Builder(this, "lock_channel")
            .setContentTitle("Cihaz Kilitli")
            .setContentText("FocusGuard aktif")
            .setSmallIcon(R.drawable.ic_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    /** Siyah tam ekran overlay penceresi ekle */
    private fun showOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val view = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            // Dokunuşları geçirme; LockScreenActivity üstte olduğu için dokunuşlar ona gider
        }

        try {
            windowManager?.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // İzin yoksa sessizce geç (LockScreenActivity yine de çalışır)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    /** LockScreenActivity'yi en üste getir */
    private fun bringLockScreenToFront() {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        startActivity(intent)
    }
}
