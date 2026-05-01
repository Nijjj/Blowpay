package com.example.minimalupi

import android.net.Uri
import java.util.Locale

object UpiParser {

    fun parse(raw: String): UpiScan? {
        val input = raw.trim()
        if (input.isBlank()) return null

        val uri = runCatching { Uri.parse(input) }.getOrNull() ?: return null
        val looksLikeUpi = uri.scheme?.equals("upi", ignoreCase = true) == true ||
            input.startsWith("upi://pay", ignoreCase = true)

        if (!looksLikeUpi) return null

        val payee = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (payee.isBlank()) return null

        val name = uri.getQueryParameter("pn")?.trim()?.takeIf { it.isNotBlank() }
        val amount = uri.getQueryParameter("am")?.trim()?.takeIf { it.isNotBlank() }

        return UpiScan(
            upiId = payee,
            payeeName = name,
            amount = amount
        )
    }

    fun normalizeName(name: String?): String {
        return name.orEmpty()
            .lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]"), "")
    }

    fun formatAmount(amount: String?): String {
        val cleaned = amount.orEmpty().trim()
        if (cleaned.isBlank()) return ""
        val numeric = cleaned.toDoubleOrNull() ?: return cleaned
        return if (numeric % 1.0 == 0.0) {
            numeric.toLong().toString()
        } else {
            cleaned
        }
    }

    fun displayName(scan: UpiScan): String {
        return scan.payeeName?.takeIf { it.isNotBlank() } ?: scan.upiId
    }
}
