package com.focusguard.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Tüm uygulama tercihlerini yöneten singleton.
 */
object AppPrefs {

    private const val PREFS_NAME = "focus_guard_prefs"

    // Keys
    const val KEY_ROLE = "role"                         // "child" | "parent" | null
    const val KEY_CHILD_DEVICE_ID = "child_device_id"
    const val KEY_PAIRED_CHILD_ID = "paired_child_id"   // parent tarafında
    const val KEY_PAIRED_PARENT_ID = "paired_parent_id" // child tarafında
    const val KEY_MASTER_CODE = "master_code"
    const val KEY_DAILY_CODE = "daily_code"
    const val KEY_DAILY_CODE_DATE = "daily_code_date"
    const val KEY_IS_LOCKED = "is_locked"
    const val KEY_EMERGENCY_DISABLED = "emergency_disabled"
    const val KEY_LOCK_START_HOUR = "lock_start_hour"
    const val KEY_LOCK_END_HOUR = "lock_end_hour"
    const val KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled"
    const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setRole(context: Context, role: String?) =
        prefs(context).edit().putString(KEY_ROLE, role).apply()

    fun getRole(context: Context): String? =
        prefs(context).getString(KEY_ROLE, null)

    fun setChildDeviceId(context: Context, id: String) =
        prefs(context).edit().putString(KEY_CHILD_DEVICE_ID, id).apply()

    fun getChildDeviceId(context: Context): String? =
        prefs(context).getString(KEY_CHILD_DEVICE_ID, null)

    fun setPairedChildId(context: Context, id: String) =
        prefs(context).edit().putString(KEY_PAIRED_CHILD_ID, id).apply()

    fun getPairedChildId(context: Context): String? =
        prefs(context).getString(KEY_PAIRED_CHILD_ID, null)

    fun setPairedParentId(context: Context, id: String) =
        prefs(context).edit().putString(KEY_PAIRED_PARENT_ID, id).apply()

    fun getPairedParentId(context: Context): String? =
        prefs(context).getString(KEY_PAIRED_PARENT_ID, null)

    fun setMasterCode(context: Context, code: String) =
        prefs(context).edit().putString(KEY_MASTER_CODE, code).apply()

    fun getMasterCode(context: Context): String? =
        prefs(context).getString(KEY_MASTER_CODE, null)

    fun setLocked(context: Context, locked: Boolean) =
        prefs(context).edit().putBoolean(KEY_IS_LOCKED, locked).apply()

    fun isLocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_LOCKED, false)

    fun setEmergencyDisabled(context: Context, disabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_EMERGENCY_DISABLED, disabled).apply()

    fun isEmergencyDisabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EMERGENCY_DISABLED, false)

    fun setLockHours(context: Context, startHour: Int, endHour: Int) {
        prefs(context).edit()
            .putInt(KEY_LOCK_START_HOUR, startHour)
            .putInt(KEY_LOCK_END_HOUR, endHour)
            .apply()
    }

    fun getLockStartHour(context: Context): Int =
        prefs(context).getInt(KEY_LOCK_START_HOUR, 18)

    fun getLockEndHour(context: Context): Int =
        prefs(context).getInt(KEY_LOCK_END_HOUR, 8)

    /**
     * Her 24 saatte bir otomatik yenilenen günlük kod.
     * Tarih değiştiyse yeni kod üretir.
     */
    fun getDailyCode(context: Context): String {
        val today = todayString()
        val savedDate = prefs(context).getString(KEY_DAILY_CODE_DATE, "")
        if (savedDate != today) {
            val newCode = generateDailyCode()
            prefs(context).edit()
                .putString(KEY_DAILY_CODE, newCode)
                .putString(KEY_DAILY_CODE_DATE, today)
                .apply()
            return newCode
        }
        return prefs(context).getString(KEY_DAILY_CODE, generateDailyCode()) ?: generateDailyCode()
    }

    private fun todayString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun generateDailyCode(): String {
        // 6 haneli numerik kod (0-999999)
        return (100000..999999).random().toString()
    }

    fun isCodeValid(context: Context, input: String): Boolean {
        val daily = getDailyCode(context)
        val master = getMasterCode(context)
        return input == daily || (master != null && input == master)
    }

    /** Eşleşme tamamlanmış mı? */
    fun isPaired(context: Context): Boolean {
        val role = getRole(context) ?: return false
        return if (role == "child") getPairedParentId(context) != null
        else getPairedChildId(context) != null
    }

    /** Tüm verileri temizle (acil çıkışta) */
    fun clearAll(context: Context) =
        prefs(context).edit().clear().apply()
}
