package com.example.minimalupi

object AppConfig {
    const val PREFS_NAME = "minimal_upi_prefs"
    const val KEY_PAYEES = "payees_json"
    const val KEY_TXS = "tx_json"
    const val KEY_IVR_NUMBER = "ivr_number"

    const val HOLD_TO_PAY_MS = 800L
    const val SEND_DELAY_MS = 240L
    const val HISTORY_WINDOW_MS = 24L * 60L * 60L * 1000L

    // Optional fallback. User settings value overrides this.
    const val IVR_NUMBER = ""
}
