package com.example.minimalupi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var ivrInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        ivrInput = findViewById(R.id.ivrInput)
        ivrInput.setText(DialerHelper.getConfiguredNumber(this))

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            DialerHelper.setConfiguredNumber(this, ivrInput.text?.toString().orEmpty())
            Toast.makeText(this, "IVR saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.resetButton).setOnClickListener {
            DialerHelper.setConfiguredNumber(this, "")
            ivrInput.setText("")
            Toast.makeText(this, "IVR reset", Toast.LENGTH_SHORT).show()
        }
    }
}
