package com.example.minimalupi

import java.util.Locale

object ProviderHeuristics {

    fun guess(upiId: String): String {
        val handle = upiId.substringAfter("@", "").lowercase(Locale.getDefault())

        return when {
            handle.isBlank() -> "Likely UPI app"
            handle.contains("ybl") || handle.contains("yesbank") -> "Likely PhonePe / YES Bank"
            handle.contains("axl") || handle.contains("axis") -> "Likely Axis Bank"
            handle.contains("oksbi") || handle.contains("sbi") -> "Likely SBI"
            handle.contains("icici") -> "Likely ICICI Bank"
            handle.contains("hdfc") -> "Likely HDFC Bank"
            handle.contains("paytm") -> "Likely Paytm Payments Bank"
            handle.contains("ibl") -> "Likely Indian Bank"
            handle.contains("upi") -> "Likely UPI app"
            else -> "Likely $handle"
        }
    }
}
