package com.example.minimalupi

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, ScannerActivity::class.java))
        finish()
    }
}
