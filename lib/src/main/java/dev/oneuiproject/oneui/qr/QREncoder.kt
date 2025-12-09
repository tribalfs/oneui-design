package dev.oneuiproject.oneui.qr

import android.content.Context
import dev.oneuiproject.oneui.qr.utils.QrEncoder

@Deprecated("Use dev.oneuiproject.oneui.qr.utils.QrEncoder instead")
class QREncoder(context: Context, content: String): QrEncoder(context, content)