package dev.oneuiproject.oneui.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.appcompat.content.res.AppCompatResources
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import java.util.Hashtable

class QREncoder(private val mContext: Context, private val mContent: String) {

    private val dpToPx = 1.dpToPx(mContext.resources)

    private var mSize: Int = 200 * dpToPx
    private var mIcon: Drawable? = null
    private val mIconSize: Int = 40 * dpToPx
    private var mFrame = true

    private var mFGColor = Color.BLACK
    private var mBGColor = Color.parseColor("#fcfcfc")
    private var mTintAnchor = false
    private var mTintBorder = false

    /**
     * Sets the size of the QR code.
     *
     * @param size The size in pixels.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setSize(@Px size: Int) = apply { this.mSize = size }

    @Deprecated("Use roundedBorder() instead.", ReplaceWith("roundedFrame(apply)"))
    fun setFrame(frame: Boolean) = apply { this.mFrame = frame }

    /**
     * Sets the icon to be placed at the center of the QR code.
     *
     * @param id The drawable resource ID of the icon.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setIcon(@DrawableRes id: Int) = apply { setIcon(getDrawable(id)) }

    /**
     * Sets the icon to be placed at the center of the QR code.
     *
     * @param icon The drawable icon.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setIcon(icon: Drawable?) = apply { this.mIcon = icon }

    @Deprecated("Use setBackgroundColor() instead.", ReplaceWith("setBackgroundColor(color)"))
    fun setBGColor(color: Int) = apply { this.mBGColor = color }

    /**
     * Sets the background color of the QR code.
     *
     * @param color The color int.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setBackgroundColor(@ColorInt color: Int) = apply { this.mBGColor = color }

    /**
     * Sets whether to add a rounded border around the QR code.
     *
     * @param apply True to add a rounded border, false otherwise.
     * @return The current [QREncoder] instance for chaining.
     */
    fun roundedFrame(apply: Boolean) = apply { this.mFrame = apply }

    @Deprecated("Use setForegroundColor() instead.",
        ReplaceWith("setForegroundColor(color, tintAnchor, tintBorder)"))
    fun setFGColor(color: Int, tintAnchor: Boolean, tintBorder: Boolean)
            = setForegroundColor(color, tintAnchor, tintBorder)

    /**
     * Sets the foreground color of the QR code, with options to tint anchor and border.
     *
     * @param color The color int.
     * @param tintAnchor True to tint anchor points, false otherwise.
     * @param tintBorder True to tint the border, false otherwise.
     * @return The current [QREncoder] instance for chaining.
     */
    fun setForegroundColor(color: Int, tintAnchor: Boolean, tintBorder: Boolean) = apply {
        this.mFGColor = color
        this.mTintAnchor = tintAnchor
        this.mTintBorder = tintBorder
    }

    fun generate(): Bitmap? {
        try {
            val hashtable = Hashtable<EncodeHintType, String>()
            hashtable[EncodeHintType.CHARACTER_SET] = "utf-8"
            val matrix = Encoder.encode(mContent, ErrorCorrectionLevel.H, hashtable).matrix
            val qrcode = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888)
            qrcode.eraseColor(mBGColor)

            drawQrImage(qrcode, matrix)
            drawAnchor(qrcode, matrix)
            if (mIcon != null) drawIcon(qrcode)

            if (mFrame) return addFrame(qrcode)
            return qrcode
        } catch (e: WriterException) {
            Log.e("QREncoder", "Exception in encoding QR code")
            e.printStackTrace()
            return null
        }
    }

    private fun drawQrImage(qrcode: Bitmap, byteMatrix: ByteMatrix) {
        val canvas = Canvas(qrcode)
        val paint = paint
        paint.color = mFGColor
        val width = ((qrcode.width * 1.0) / byteMatrix.width).toFloat()
        val radius = (0.382 * width.toDouble()).toFloat()
        val offset = (width.toDouble() / 2.0).toFloat()
        for (i in 0 until byteMatrix.height) {
            for (i2 in 0 until byteMatrix.width) {
                if (byteMatrix[i2, i].toInt() == 1) {
                    canvas.drawCircle((i2 * width) + offset, (i * width) + offset, radius, paint)
                }
            }
        }
    }

    private fun drawAnchor(qrcode: Bitmap, byteMatrix: ByteMatrix) {
        val anchor = getBitmap(getDrawable(R.drawable.oui_qr_code_anchor)!!)
        val width = qrcode.width
        val height = qrcode.height

        val anchorWidth =
            (getAnchorWidth(byteMatrix) * (((width * 1.0) / byteMatrix.width).toFloat())).toInt()
        val paint = paint
        val canvas = Canvas(qrcode)
        canvas.drawRect(RectF(0.0f, 0.0f, anchorWidth.toFloat(), anchorWidth.toFloat()), paint)
        canvas.drawRect(
            RectF(
                (width - anchorWidth).toFloat(),
                0.0f,
                width.toFloat(),
                anchorWidth.toFloat()
            ), paint
        )
        canvas.drawRect(
            RectF(
                0.0f,
                (height - anchorWidth).toFloat(),
                anchorWidth.toFloat(),
                height.toFloat()
            ), paint
        )

        val anchorTint = Paint()
        anchorTint.isAntiAlias = true
        anchorTint.style = Paint.Style.FILL
        if (mTintAnchor) {
            anchorTint.setColorFilter(PorterDuffColorFilter(mFGColor, PorterDuff.Mode.SRC_IN))
        }

        val scaleBitmap = getScaleBitmap(anchor, (anchorWidth.toFloat()) / anchor.width)
        canvas.drawBitmap(scaleBitmap, 0.0f, 0.0f, anchorTint)
        canvas.drawBitmap(scaleBitmap, (width - anchorWidth).toFloat(), 0.0f, anchorTint)
        canvas.drawBitmap(scaleBitmap, 0.0f, (height - anchorWidth).toFloat(), anchorTint)
        scaleBitmap.recycle()
        anchor.recycle()
    }

    private fun getAnchorWidth(byteMatrix: ByteMatrix): Int {
        var i = 0
        var i2 = 0
        while (i2 < byteMatrix.width && byteMatrix[i2, 0].toInt() == 1) {
            i++
            i2++
        }
        return i
    }

private fun drawIcon(qrCode: Bitmap) {
        val height = mIconSize
        val width = mIconSize

        val iconTop = (qrCode.height / 2) - (height / 2)
        val iconLeft = (qrCode.width / 2) - (width / 2)
        val iconRadius = 20 * dpToPx
        val iconPadding = 5 * dpToPx
        val canvas = Canvas(qrCode)
        val paint = paint
        val rectF = RectF(
            (iconLeft - iconPadding).toFloat(),
            (iconTop - iconPadding).toFloat(),
            (width + iconLeft + iconPadding).toFloat(),
            (height + iconTop + iconPadding).toFloat()
        )
        canvas.drawRoundRect(rectF, iconRadius.toFloat(), iconRadius.toFloat(), paint)
        mIcon!!.setBounds(iconLeft, iconTop, iconLeft + width, iconTop + height)
        mIcon!!.draw(canvas)
    }

    private fun addFrame(qrcode: Bitmap): Bitmap {
        val border = 12 * dpToPx
        val radius= 32 * dpToPx

        val newWidth = qrcode.width + border * 2
        val newHeight = qrcode.height + border * 2
        val output = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = paint
        val rectF = RectF(0f, 0f, newWidth.toFloat(), newHeight.toFloat())
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)

        canvas.drawBitmap(qrcode, border.toFloat(), border.toFloat(), null)

        paint.color = if (mTintBorder) mFGColor else Color.parseColor("#d0d0d0")
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        rectF[1.0f, 1.0f, (newWidth - 1).toFloat()] = (newHeight - 1).toFloat()
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)

        return output
    }


    private val paint: Paint
        get() {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.color = mBGColor
            return paint
        }

    private fun getDrawable(@DrawableRes id: Int): Drawable? {
        return AppCompatResources.getDrawable(mContext, id)
    }

    private fun getBitmap(drawable: Drawable): Bitmap {
        val createBitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(createBitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return createBitmap
    }

    private fun getScaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
