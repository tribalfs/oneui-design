package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RemoteViews.RemoteView
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.qr.QREncoder

/**
 * An [ImageView] subclass that displays a QR code.
 *
 * This view renders a QR code based on the provided content and allows for
 * customization of its appearance.
 * - `qrContent`: The content to be encoded in the QR code.
 * - `qrBackgroundColor`: The background color of the QR code.
 * - `qrForegroundColor`: The foreground color of the QR code.
 * - `qrIcon`: An icon to be displayed at the center of the QR code.
 * - `qrRoundedFrame`: Whether the QR code should have a rounded frame.
 * - `qrSize`: The size of the QR code in pixels.
 * - `qrTintAnchor`: Whether the anchor points of the QR code should be tinted with the foreground color.
 * - `qrTintFrame`: Whether the frame of the QR code should be tinted with the foreground color.
 *
 * **Important:** To apply changes made to these properties programmatically after the
 * view has been initially displayed, you must call [invalidate] to show the changes.
 * If `qrSize` is changed, calling [requestLayout] before [invalidate] may also be necessary
 * to ensure the view's dimensions are correctly updated
 * # Example usage:
 * **1. XML Layout:**
 * ```xml
 * <dev.oneuiproject.oneui.widget.QRImageView
 *    android:id="@+id/qr_image_1"
 *    android:layout_width="wrap_content"
 *    android:layout_height="wrap_content"
 *    app:qrIcon="@mipmap/ic_launcher"
 *    app:qrContent="https://github.com/tribalfs/sesl-androidx"/>
 * ```
 * **2. Programmatically:**
 * ```kotlin
 * // In your Activity or Fragment
 * // Instantiate a QRImageView
 * val qrImageView = QRImageView(context).apply {
 *      layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
 *      // Set mandatory content equivalent to app:qrContent
 *      setContent("Hello, OneUI!")
 *      // Customize size equivalent to app:qrSize
 *      setSize(300)
 *      // Equivalent to app:qrForegroundColor
 *      setForegroundColor(Color.parseColor("#3DDC84"))
 *      // Equivalent to app:qrBackgroundColor
 *      setBackgroundColor(Color.WHITE)
 *      // Equivalent to app:qrIcon
 *      setIcon(ContextCompat.getDrawable(context, R.drawable.my_icon))
 *      // Equivalent to app:qrRoundedFrame
 *      setRoundedFrame(true)
 *      // Equivalent to app:qrTintAnchor
 *      tintAnchor(false)
 *  }
 *
 *  // Add to your layout
 *  yourLinearLayout.addView(qrImageView)
 *
 *  // To update content later:
 *  qrImageView.setContent("New QR Data")
 *  qrImageView.invalidate()
 *  ```
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 */
@RemoteView
class QRImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : ImageView(context, attrs,defStyleAttr, defStyleRes) {

    private var content: String? = null
    @ColorInt
    private var bgColor: Int = -1
    @ColorInt
    private var fgColor: Int = -1
    private var icon: Drawable? = null
    private var roundedFrame: Boolean = true
    private var size: Int = -1
    private var tintAnchor = false
    private var tintFrame = false
    private var regenerate = false

    init {
        attrs?.let{
            context.withStyledAttributes(
                attrs,
                R.styleable.QRImageView,
                defStyleAttr,
                defStyleRes
            ) {
                content = getString(R.styleable.QRImageView_qrContent)
                bgColor = getColor(R.styleable.QRImageView_qrBackgroundColor, "#fcfcfc".toColorInt())
                fgColor = getColor(R.styleable.QRImageView_qrForegroundColor, Color.BLACK)
                icon = getDrawable(R.styleable.QRImageView_qrIcon)
                roundedFrame = getBoolean(R.styleable.QRImageView_qrRoundedFrame, true)
                size = getDimensionPixelSize(R.styleable.QRImageView_qrSize, -1)
                tintAnchor = getBoolean(R.styleable.QRImageView_qrTintAnchor, false)
                tintFrame = getBoolean(R.styleable.QRImageView_qrTintFrame, false)

                updateImage()
            }
        }
    }

    private fun updateImage(){
        val qrEncoder = QREncoder(context, content ?: "").apply {
            if (size != -1) setSize(size)
            setIcon(icon)
            roundedFrame(roundedFrame)
            setBackgroundColor(bgColor)
            setForegroundColor(fgColor, tintAnchor, tintFrame)
        }
        regenerate = false
        //This internally calls super.invalidate()
        setImageBitmap(qrEncoder.generate())
    }

    /**
     * Sets the new content to be encoded in the QR code.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param content The new content to encode in the QR code. Can be null.
     */
    fun setContent(content: String?)  {
        if (this.content != content) {
            this.content = content
            regenerate = true
        }
    }

    /**
     * Sets a new icon to be displayed at the center of the QR code.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param icon The new [Drawable] icon to display. Can be null.
     */
    fun setIcon(icon: Drawable?)  {
        if (this.icon != icon) {
            this.icon = icon
            regenerate = true
        }
    }

    /**
     * Updates the size of the QR code.
     *
     * Sets a new size for the QR code.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param size The new size in pixels. Must be non-negative.
     * @return This [QRImageView] instance for method chaining.
     */
    fun setSize(@Px @IntRange(from = 0) size: Int)  {
        if (this.size != size) {
            this.size = size
            regenerate = true
        }
    }

    /**
     * Sets whether the QR code should have a rounded frame.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param roundedFrame True to enable a rounded frame, false otherwise.
     */
    fun setRoundedFrame(roundedFrame: Boolean){
        if (this.roundedFrame != roundedFrame) {
            this.roundedFrame = roundedFrame
            regenerate = true
        }
    }

    /**
     * Sets a new foreground color for the QR code.
     * This color will also be applied to the anchor points and frame if tinting is enabled.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param fgColor The new foreground color as an integer color value.
     */
    fun setForegroundColor(@ColorInt fgColor: Int) {
        if (this.fgColor != fgColor) {
            this.fgColor = fgColor
            regenerate = true
        }
    }

    /**
     * Sets whether the anchor points of the QR code should be tinted with the foreground color.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param tint True to enable tinting of the anchor points, false otherwise.
     */
    fun tintAnchor(tint: Boolean) {
        if (this.tintAnchor != tint) {
            this.tintAnchor = tint
            regenerate = true
        }
    }


    /**
     * Sets whether the frame of the QR code should be tinted with the foreground color.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param tint True to enable tinting of the frame, false otherwise.
     */
    fun tintFrame(tint: Boolean) {
        if (this.tintFrame != tint) {
            this.tintFrame = tint
            regenerate = true
        }
    }

    /**
     * Sets a new background color for the QR code.
     *
     * Call [invalidate] to apply the changes.
     *
     * @param bgColor The new background color as an integer color value.
     */
    override fun setBackgroundColor(@ColorInt bgColor: Int)  {
        if (this.bgColor != bgColor) {
            this.bgColor = bgColor
            regenerate = true
        }
    }

    /**
     * Invalidates the view. This is required to be invoked when any properties of the QR code have been changed
     * (e.g., content, icon, size, colors) to reflect the changes visually.
     */
    override fun invalidate(){
        if (regenerate) {
            updateImage()
        }else{
            super.invalidate()
        }
    }
}