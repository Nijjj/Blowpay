package com.example.minimalupi

data class UpiScan(
    val upiId: String,
    val payeeName: String? = null,
    val amount: String? = null
)

data class PayeeRecord(
    val upiId: String,
    val lastSeenName: String? = null,
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val seenCount: Int = 1
)

data class TransactionRecord(
    val upiId: String,
    val amount: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RiskLevel {
    GREEN,
    YELLOW,
    RED
}

data class VerificationState(
    val riskLevel: RiskLevel,
    val label: String,
    val detail: String
)
