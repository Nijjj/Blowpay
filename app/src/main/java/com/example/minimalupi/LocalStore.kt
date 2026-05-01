package com.example.minimalupi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalStore {

    private fun prefs(context: Context) =
        context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPayees(context: Context): List<PayeeRecord> {
        pruneExpired(context)
        val raw = prefs(context).getString(AppConfig.KEY_PAYEES, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }

        val items = mutableListOf<PayeeRecord>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            items += PayeeRecord(
                upiId = obj.optString("upiId"),
                lastSeenName = obj.optString("lastSeenName").takeIf { it.isNotBlank() },
                firstSeenAt = obj.optLong("firstSeenAt", System.currentTimeMillis()),
                lastSeenAt = obj.optLong("lastSeenAt", System.currentTimeMillis()),
                seenCount = obj.optInt("seenCount", 1)
            )
        }
        return items
    }

    fun upsertPayee(context: Context, upiId: String, name: String?) {
        val now = System.currentTimeMillis()
        val payees = loadPayees(context).toMutableList()
        val idx = payees.indexOfFirst { it.upiId.equals(upiId, ignoreCase = true) }

        val updated = if (idx >= 0) {
            val current = payees[idx]
            current.copy(
                lastSeenName = name?.takeIf { it.isNotBlank() } ?: current.lastSeenName,
                lastSeenAt = now,
                seenCount = current.seenCount + 1
            )
        } else {
            PayeeRecord(
                upiId = upiId,
                lastSeenName = name?.takeIf { it.isNotBlank() },
                firstSeenAt = now,
                lastSeenAt = now,
                seenCount = 1
            )
        }

        if (idx >= 0) payees[idx] = updated else payees.add(updated)
        savePayees(context, payees)
    }

    fun verify(context: Context, scan: UpiScan): VerificationState {
        val payee = loadPayees(context).firstOrNull { it.upiId.equals(scan.upiId, ignoreCase = true) }
        val currentName = scan.payeeName?.takeIf { it.isNotBlank() }
        val savedName = payee?.lastSeenName?.takeIf { it.isNotBlank() }

        return when {
            payee == null -> VerificationState(
                riskLevel = RiskLevel.YELLOW,
                label = "First time",
                detail = "New receiver"
            )

            currentName == null -> VerificationState(
                riskLevel = RiskLevel.YELLOW,
                label = "No name in QR",
                detail = "Receiver seen before"
            )

            savedName == null -> VerificationState(
                riskLevel = RiskLevel.YELLOW,
                label = "No saved name",
                detail = "Receiver seen before"
            )

            UpiParser.normalizeName(currentName) == UpiParser.normalizeName(savedName) -> VerificationState(
                riskLevel = RiskLevel.GREEN,
                label = "Known",
                detail = "Name matches past record"
            )

            else -> VerificationState(
                riskLevel = RiskLevel.RED,
                label = "Mismatch",
                detail = "Name changed from past record"
            )
        }
    }

    fun loadTransactions(context: Context): List<TransactionRecord> {
        pruneExpired(context)
        val raw = prefs(context).getString(AppConfig.KEY_TXS, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }

        val items = mutableListOf<TransactionRecord>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            items += TransactionRecord(
                upiId = obj.optString("upiId"),
                amount = obj.optString("amount"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
        return items.sortedByDescending { it.timestamp }
    }

    fun addTransaction(context: Context, upiId: String, amount: String) {
        val now = System.currentTimeMillis()
        val txs = loadTransactions(context).toMutableList()
        txs.add(
            TransactionRecord(
                upiId = upiId,
                amount = amount,
                timestamp = now
            )
        )
        saveTransactions(context, txs)
    }

    fun pruneExpired(context: Context) {
        val cutoff = System.currentTimeMillis() - AppConfig.HISTORY_WINDOW_MS
        val txs = loadTransactionsInternal(context).filter { it.timestamp >= cutoff }
        saveTransactions(context, txs)
    }

    private fun loadTransactionsInternal(context: Context): List<TransactionRecord> {
        val raw = prefs(context).getString(AppConfig.KEY_TXS, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }

        val items = mutableListOf<TransactionRecord>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            items += TransactionRecord(
                upiId = obj.optString("upiId"),
                amount = obj.optString("amount"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
        return items
    }

    private fun savePayees(context: Context, payees: List<PayeeRecord>) {
        val array = JSONArray()
        payees.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("upiId", item.upiId)
                    put("lastSeenName", item.lastSeenName ?: "")
                    put("firstSeenAt", item.firstSeenAt)
                    put("lastSeenAt", item.lastSeenAt)
                    put("seenCount", item.seenCount)
                }
            )
        }
        prefs(context).edit().putString(AppConfig.KEY_PAYEES, array.toString()).apply()
    }

    private fun saveTransactions(context: Context, txs: List<TransactionRecord>) {
        val cutoff = System.currentTimeMillis() - AppConfig.HISTORY_WINDOW_MS
        val filtered = txs.filter { it.timestamp >= cutoff }
        val array = JSONArray()
        filtered.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("upiId", item.upiId)
                    put("amount", item.amount)
                    put("timestamp", item.timestamp)
                }
            )
        }
        prefs(context).edit().putString(AppConfig.KEY_TXS, array.toString()).apply()
    }
}
