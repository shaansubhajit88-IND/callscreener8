package com.example.callscreener

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var autoBlockSwitch: Switch
    private lateinit var btnOpenCallSettings: Button
    private lateinit var tvInfo: TextView

    private val requiredPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )

    private val requestPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // nothing required here; user will grant permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        autoBlockSwitch = findViewById(R.id.switch_auto_block)
        btnOpenCallSettings = findViewById(R.id.btn_call_settings)
        tvInfo = findViewById(R.id.tv_info)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        autoBlockSwitch.isChecked = prefs.getBoolean("auto_block", false)

        autoBlockSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_block", isChecked).apply()
        }

        btnOpenCallSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }

        findViewById<Button>(R.id.btn_request_perms).setOnClickListener {
            requestNecessaryPermissions()
        }

        showInfoText()
    }

    private fun requestNecessaryPermissions() {
        val missing = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermLauncher.launch(missing.toTypedArray())
        } else {
            AlertDialog.Builder(this)
                .setTitle("Permissions")
                .setMessage("All SMS permissions already granted.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showInfoText() {
        tvInfo.text = """
            1. Request SMS permissions.
            2. Set this app as 'Call screening app' in Default apps.
            3. Toggle Auto-block unknown callers as needed.
            4. Use Ask Purpose from the incoming notification (sends SMS).
            Test once from another phone.
        """.trimIndent()
    }
}
