package com.example.minimalupi

import android.content.Context
import android.content.Intent
import android.net.Uri

object DialerHelper {

    fun getConfiguredNumber(context: Context): String {
        val prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(AppConfig.KEY_IVR_NUMBER, "").orEmpty().trim()
        return if (saved.isBlank()) AppConfig.IVR_NUMBER.trim() else saved
    }

    fun setConfiguredNumber(context: Context, number: String) {
        val normalized = number.trim()
        context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AppConfig.KEY_IVR_NUMBER, normalized)
            .apply()
    }

    fun buildDialIntent(context: Context): Intent {
        val number = getConfiguredNumber(context)
        return if (number.isBlank()) {
            Intent(Intent.ACTION_DIAL)
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
        }
    }
}
