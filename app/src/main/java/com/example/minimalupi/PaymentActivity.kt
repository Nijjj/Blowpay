package com.example.minimalupi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PaymentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UPI_ID = "extra_upi_id"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_AMOUNT = "extra_amount"
    }

    private lateinit var recipientText: TextView
    private lateinit var upiText: TextView
    private lateinit var amountInput: EditText
    private lateinit var payButton: Button

    private var upiId: String = ""
    private var name: String = ""
    private var scanAmount: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        recipientText = findViewById(R.id.recipientText)
        upiText = findViewById(R.id.upiText)
        amountInput = findViewById(R.id.amountInput)
        payButton = findViewById(R.id.payButton)

        upiId = intent.getStringExtra(EXTRA_UPI_ID).orEmpty()
        name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        scanAmount = intent.getStringExtra(EXTRA_AMOUNT).orEmpty()

        recipientText.text = if (name.isNotBlank()) name else upiId
        upiText.text = upiId

        if (scanAmount.isNotBlank()) amountInput.setText(scanAmount)
        amountInput.setSelection(amountInput.text?.length ?: 0)
        amountInput.requestFocus()
        amountInput.imeOptions = EditorInfo.IME_ACTION_DONE

        updateButtonLabel()

        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateButtonLabel()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        payButton.setOnClickListener {
            val amount = amountInput.text?.toString().orEmpty().trim()
            if (amount.isBlank()) {
                amountInput.error = "Add amount"
                return@setOnClickListener
            }

            startActivity(
                Intent(this, ConfirmActivity::class.java).apply {
                    putExtra(ConfirmActivity.EXTRA_UPI_ID, upiId)
                    putExtra(ConfirmActivity.EXTRA_NAME, name)
                    putExtra(ConfirmActivity.EXTRA_AMOUNT, amount)
                }
            )
        }
    }

    private fun updateButtonLabel() {
        val amount = amountInput.text?.toString().orEmpty().trim()
        payButton.text = if (amount.isBlank()) {
            getString(R.string.pay_button_default)
        } else {
            "Pay ₹$amount"
        }
    }
}
