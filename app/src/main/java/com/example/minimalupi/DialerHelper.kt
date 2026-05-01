package com.example.minimalupi

import android.content.Intent
import android.net.Uri

object DialerHelper {

    fun buildDialIntent(): Intent {
        val number = AppConfig.IVR_NUMBER.trim()
        return if (number.isBlank()) {
            Intent(Intent.ACTION_DIAL)
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
        }
    }
}
