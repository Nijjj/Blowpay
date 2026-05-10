package com.example.minimalupi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var ivrChip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivrChip = findViewById(R.id.ivrChip)

        findViewById<Button>(R.id.openScannerButton).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.historyCard).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.scanCard).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.settingsCard).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.dialerCard).setOnClickListener {
            startActivity(DialerHelper.buildDialIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        val ivr = DialerHelper.getConfiguredNumber(this)
        ivrChip.text = if (ivr.isBlank()) "IVR: Dialer" else "IVR: $ivr"
    }
}
