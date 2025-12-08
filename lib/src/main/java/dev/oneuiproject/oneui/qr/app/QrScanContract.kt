package dev.oneuiproject.oneui.qr.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * ActivityResultContract for [QrScanActivity].
 *
 * Input: [QrScanConfig] describing how the scanner should behave.
 * Output: scanned QR content as String, or null if cancelled / failed.
 */
class QrScanContract : ActivityResultContract<QrScanConfig, String?>() {

    override fun createIntent(context: Context, input: QrScanConfig): Intent {
        return QrScanActivity.createIntent(
            context = context,
            title = input.title,
            requiredPrefix = input.requiredPrefix,
            regex = input.regex
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return intent.getStringExtra(QrScanActivity.EXTRA_QR_SCANNER_RESULT)
    }
}
