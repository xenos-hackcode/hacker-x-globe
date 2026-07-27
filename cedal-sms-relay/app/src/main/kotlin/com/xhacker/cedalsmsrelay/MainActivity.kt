package com.xhacker.cedalsmsrelay

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xhacker.cedalsmsrelay.databinding.ActivityMainBinding

// The whole app: paste the cedal-server URL + the shared SMS_RELAY_SECRET
// once, grant SEND_SMS, hit Start - the phone (and its real SIM) then acts
// as Cedal's SMS gateway instead of Twilio (see RelayService/PendingSmsJobs
// server-side). Meant to be left running on a spare phone.
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) startRelay() else updateStatus("Permissions denied - can't send SMS without them.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences("relay_prefs", MODE_PRIVATE)

        binding.serverUrlInput.setText(prefs.getString(KEY_SERVER_URL, ""))
        binding.secretInput.setText(prefs.getString(KEY_SECRET, ""))
        updateStatus(if (RelayService.isRunning) "Running - watching for pending SMS." else "Stopped.")

        binding.startButton.setOnClickListener {
            val url = binding.serverUrlInput.text.toString().trim().trimEnd('/')
            val secret = binding.secretInput.text.toString().trim()
            if (url.isBlank() || secret.isBlank()) {
                updateStatus("Enter both the server URL and the shared secret first.")
                return@setOnClickListener
            }
            prefs.edit().putString(KEY_SERVER_URL, url).putString(KEY_SECRET, secret).apply()
            requestPermissionsThenStart()
        }
        binding.stopButton.setOnClickListener {
            stopService(Intent(this, RelayService::class.java))
            updateStatus("Stopped.")
        }
    }

    private fun requestPermissionsThenStart() {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.SEND_SMS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isEmpty()) startRelay() else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun startRelay() {
        val url = prefs.getString(KEY_SERVER_URL, "") ?: ""
        val secret = prefs.getString(KEY_SECRET, "") ?: ""
        val intent = Intent(this, RelayService::class.java).apply {
            putExtra(RelayService.EXTRA_SERVER_URL, url)
            putExtra(RelayService.EXTRA_SECRET, secret)
        }
        ContextCompat.startForegroundService(this, intent)
        updateStatus("Running - watching for pending SMS.")
    }

    private fun updateStatus(text: String) {
        binding.statusText.text = text
    }

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_SECRET = "secret"
    }
}
