package com.xhacker.cedalsmsrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

// Auto-restarts the relay after the phone reboots - it's meant to be left
// running unattended as Cedal's SMS gateway, so a normal restart (power
// outage, update, etc.) shouldn't silently stop delivery until someone
// notices and reopens the app.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("relay_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString(MainActivity.KEY_SERVER_URL, null) ?: return
        val secret = prefs.getString(MainActivity.KEY_SECRET, null) ?: return
        val serviceIntent = Intent(context, RelayService::class.java).apply {
            putExtra(RelayService.EXTRA_SERVER_URL, url)
            putExtra(RelayService.EXTRA_SECRET, secret)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
