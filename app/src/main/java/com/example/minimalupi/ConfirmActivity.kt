package com.example.minimalupi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConfirmActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UPI_ID = "extra_upi_id"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_AMOUNT = "extra_amount"
    }

    private lateinit var amountText: TextView
    private lateinit var nameText: TextView
    private lateinit var upiText: TextView
    private lateinit var providerText: TextView
    private lateinit var riskText: TextView
    private lateinit var holdButton: Button
    private lateinit var sendingOverlay: android.view.View

    private var holdJob: Job? = null
    private lateinit var upiId: String
    private var name: String = ""
    private lateinit var amount: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)

        amountText = findViewById(R.id.confirmAmount)
        nameText = findViewById(R.id.confirmName)
        upiText = findViewById(R.id.confirmUpi)
        providerText = findViewById(R.id.confirmProvider)
        riskText = findViewById(R.id.confirmRisk)
        holdButton = findViewById(R.id.holdButton)
        sendingOverlay = findViewById(R.id.sendingOverlay)

        upiId = intent.getStringExtra(EXTRA_UPI_ID).orEmpty()
        name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        amount = intent.getStringExtra(EXTRA_AMOUNT).orEmpty()

        val displayName = name.takeIf { it.isNotBlank() } ?: "Unknown Receiver"
        val scan = UpiScan(upiId = upiId, payeeName = name.takeIf { it.isNotBlank() }, amount = amount)
        val verification = LocalStore.verify(this, scan)
        val providerGuess = ProviderHeuristics.guess(upiId)

        amountText.text = "₹$amount"
        nameText.text = displayName
        upiText.text = upiId
        providerText.text = providerGuess
        riskText.text = verification.label

        when (verification.riskLevel) {
            RiskLevel.GREEN -> {
                riskText.setTextColor(Color.parseColor("#4CAF50"))
                providerText.setTextColor(Color.parseColor("#A5D6A7"))
            }
            RiskLevel.YELLOW -> {
                riskText.setTextColor(Color.parseColor("#FBC02D"))
                providerText.setTextColor(Color.parseColor("#FFE082"))
            }
            RiskLevel.RED -> {
                riskText.setTextColor(Color.parseColor("#EF5350"))
                providerText.setTextColor(Color.parseColor("#FFCDD2"))
            }
        }

        findViewById<TextView>(R.id.confirmDetail).text = verification.detail

        holdButton.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    vibrateTap()
                    holdJob?.cancel()
                    holdJob = lifecycleScope.launch {
                        delay(AppConfig.HOLD_TO_PAY_MS)
                        triggerSend()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdJob?.cancel()
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerSend() {
        holdButton.isEnabled = false
        sendingOverlay.visibility = android.view.View.VISIBLE

        LocalStore.upsertPayee(this, upiId, name.takeIf { it.isNotBlank() })
        LocalStore.addTransaction(this, upiId, amount)

        lifecycleScope.launch {
            delay(AppConfig.SEND_DELAY_MS)
            startActivity(DialerHelper.buildDialIntent())
            finish()
        }
    }

    private fun vibrateTap() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (_: Throwable) {
        }
    }
}
