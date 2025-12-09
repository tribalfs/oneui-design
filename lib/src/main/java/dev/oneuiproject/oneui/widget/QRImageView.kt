package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import dev.oneuiproject.oneui.qr.widget.QrImageView

@Deprecated("Use dev.oneuiproject.oneui.qr.widget.QrImageView instead")
open class QRImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : QrImageView(context, attrs,defStyleAttr, defStyleRes)