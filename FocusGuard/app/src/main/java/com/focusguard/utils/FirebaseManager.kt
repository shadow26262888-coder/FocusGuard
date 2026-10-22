package com.focusguard.utils

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Realtime Database ile tüm iletişimi yönetir.
 *
 * Düğüm yapısı:
 *   /devices/{deviceId}/
 *       role: "child"|"parent"
 *       pairedWith: "{otherDeviceId}"
 *       locked: true|false
 *       dailyCode: "123456"
 *       masterCode: "654321"
 *       command: "lock"|"unlock"|"emergency"
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"
    private val db get() = FirebaseDatabase.getInstance().reference

    // -----------------------------------------------------------------------
    // Eşleşme
    // -----------------------------------------------------------------------

    /** Çocuk cihazını DB'ye kaydet, eşleşme kodu / QR için hazırla */
    fun registerChildDevice(
        context: Context,
        deviceId: String,
        dailyCode: String,
        onComplete: (Boolean) -> Unit
    ) {
        val data = mapOf(
            "role" to "child",
            "pairedWith" to "",
            "locked" to false,
            "dailyCode" to dailyCode,
            "command" to ""
        )
        db.child("devices").child(deviceId).setValue(data)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                Log.e(TAG, "registerChild failed", it)
                onComplete(false)
            }
    }

    /** Ebeveyn: çocuk cihazını bulup iki tarafı birbirine bağla */
    fun pairParentWithChild(
        parentDeviceId: String,
        childDeviceId: String,
        masterCode: String,
        onComplete: (Boolean) -> Unit
    ) {
        // Önce çocuğun var olduğunu doğrula
        db.child("devices").child(childDeviceId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onComplete(false)
                    return@addOnSuccessListener
                }
                val updates = mapOf(
                    "devices/$parentDeviceId/role" to "parent",
                    "devices/$parentDeviceId/pairedWith" to childDeviceId,
                    "devices/$parentDeviceId/masterCode" to masterCode,
                    "devices/$childDeviceId/pairedWith" to parentDeviceId,
                    "devices/$childDeviceId/masterCode" to masterCode
                )
                db.updateChildren(updates)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener {
                        Log.e(TAG, "pair failed", it)
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                Log.e(TAG, "pairParentWithChild get failed", it)
                onComplete(false)
            }
    }

    // -----------------------------------------------------------------------
    // Komutlar (Parent → Child)
    // -----------------------------------------------------------------------

    fun sendCommand(childDeviceId: String, command: String, onComplete: (Boolean) -> Unit = {}) {
        db.child("devices").child(childDeviceId).child("command").setValue(command)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun setLockedState(childDeviceId: String, locked: Boolean, onComplete: (Boolean) -> Unit = {}) {
        db.child("devices").child(childDeviceId).child("locked").setValue(locked)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateDailyCode(childDeviceId: String, code: String) {
        db.child("devices").child(childDeviceId).child("dailyCode").setValue(code)
    }

    // -----------------------------------------------------------------------
    // Dinleyiciler (Child tarafında)
    // -----------------------------------------------------------------------

    private var commandListener: ValueEventListener? = null
    private var lockedListener: ValueEventListener? = null

    fun listenForCommands(
        deviceId: String,
        onCommand: (String) -> Unit
    ) {
        val ref = db.child("devices").child(deviceId).child("command")
        commandListener?.let { ref.removeEventListener(it) }
        commandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cmd = snapshot.getValue(String::class.java) ?: return
                if (cmd.isNotEmpty()) {
                    onCommand(cmd)
                    // Komutu temizle
                    ref.setValue("")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "listenForCommands cancelled", error.toException())
            }
        }
        ref.addValueEventListener(commandListener!!)
    }

    fun listenForLockedState(
        deviceId: String,
        onLockedChanged: (Boolean) -> Unit
    ) {
        val ref = db.child("devices").child(deviceId).child("locked")
        lockedListener?.let { ref.removeEventListener(it) }
        lockedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locked = snapshot.getValue(Boolean::class.java) ?: false
                onLockedChanged(locked)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "listenForLockedState cancelled", error.toException())
            }
        }
        ref.addValueEventListener(lockedListener!!)
    }

    fun stopListening(deviceId: String) {
        commandListener?.let {
            db.child("devices").child(deviceId).child("command").removeEventListener(it)
        }
        lockedListener?.let {
            db.child("devices").child(deviceId).child("locked").removeEventListener(it)
        }
        commandListener = null
        lockedListener = null
    }

    // -----------------------------------------------------------------------
    // Günlük kod senkronizasyonu (Child tarafında oku)
    // -----------------------------------------------------------------------

    fun fetchDailyCode(deviceId: String, onResult: (String?) -> Unit) {
        db.child("devices").child(deviceId).child("dailyCode").get()
            .addOnSuccessListener { onResult(it.getValue(String::class.java)) }
            .addOnFailureListener { onResult(null) }
    }

    // -----------------------------------------------------------------------
    // Eşleşme kaldırma (acil çıkış)
    // -----------------------------------------------------------------------

    fun unpairDevice(deviceId: String, pairedId: String) {
        db.child("devices").child(deviceId).removeValue()
        db.child("devices").child(pairedId).child("pairedWith").setValue("")
    }
}
