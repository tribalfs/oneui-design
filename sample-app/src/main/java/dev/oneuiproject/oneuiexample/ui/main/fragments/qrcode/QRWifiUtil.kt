package dev.oneuiproject.oneuiexample.ui.main.fragments.qrcode


data class WifiQrInfo(
    val ssid: String,
    val password: String?,
    val authType: String?,
    val hidden: Boolean
)

fun parseWifiQrContent(content: String): WifiQrInfo? {
    if (!content.startsWith("WIFI:", ignoreCase = true)) return null

    val body = content.removePrefix("WIFI:").trimEnd(';')

    var ssid: String? = null
    var password: String? = null
    var authType: String? = null
    var hidden = false

    body.split(';').forEach { pair ->
        if (pair.isEmpty()) return@forEach
        val key = pair.substringBefore(':')
        val value = pair.substringAfter(':', "")

        when (key.uppercase()) {
            "S" -> ssid = value
            "P" -> password = value
            "T" -> authType = value
            "H" -> hidden = value.equals("true", ignoreCase = true)
        }
    }

    val finalSsid = ssid ?: return null
    return WifiQrInfo(
        ssid = finalSsid,
        password = password,
        authType = authType,
        hidden = hidden
    )
}
