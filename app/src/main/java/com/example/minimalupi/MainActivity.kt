package com.example.minimalupi

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialCardView>(R.id.openScannerCard).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.newPaymentCard).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.ivrAssistantCard).setOnClickListener {
            startActivity(DialerHelper.buildDialIntent(this))
        }

        findViewById<MaterialCardView>(R.id.scanFab).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<TextView>(R.id.settingsNav).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
