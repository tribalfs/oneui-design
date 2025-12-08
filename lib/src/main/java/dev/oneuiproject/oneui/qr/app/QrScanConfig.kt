package dev.oneuiproject.oneui.qr.app

/**
 * Configuration for launching [QrScanActivity].
 *
 * @param title          Optional title shown in the scanner UI.
 * @param requiredPrefix Optional required prefix for the scanned content (e.g. "WIFI:", "https://").
 * @param regex          Optional regex pattern the scanned content must match.
 */
data class QrScanConfig(
    val title: String? = null,
    val requiredPrefix: String? = null,
    val regex: String? = null
)