package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RemoteViews.RemoteView
import androidx.core.content.res.use
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.qr.QREncoder

@RemoteView
class QRImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : ImageView(context, attrs,defStyleAttr, defStyleRes) {

    init {
        attrs?.let{
            context.obtainStyledAttributes(
                attrs,
                R.styleable.QRImageView,
                defStyleAttr,
                defStyleRes
            ).use {a ->
                val content = a.getString(R.styleable.QRImageView_qrContent)
                val bgColor = a.getColor(R.styleable.QRImageView_qrBackgroundColor, Color.parseColor("#fcfcfc"))
                val fgColor = a.getColor(R.styleable.QRImageView_qrForegroundColor, Color.BLACK)
                val icon = a.getDrawable(R.styleable.QRImageView_qrIcon)
                val roundedBorder = a.getBoolean(R.styleable.QRImageView_qrRoundedBorder, true)
                val size = a.getDimensionPixelSize(R.styleable.QRImageView_qrSize, -1)
                val tintAnchor = a.getBoolean(R.styleable.QRImageView_qrTintAnchor, false)
                val tintBorder = a.getBoolean(R.styleable.QRImageView_qrTintBorder, false)

                val builder = QREncoder(context, content ?: "")
                    .apply {
                        if (size != -1) {
                            setSize(size)
                        }
                        setIcon(icon)
                        roundedFrame(roundedBorder)
                        setBackgroundColor(bgColor)
                        setForegroundColor(fgColor, tintAnchor, tintBorder)
                    }

                setImageBitmap(
                    builder.generate()
                )
            }
        }
    }
}