package dev.oneuiproject.oneui.widget.internal

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SelectableAnimatedDrawable : AnimatedStateListDrawableCompat() {

    @Px
    private var radius = -1f
    @ColorInt
    private var selectedColor: Int = -1
    private var backgroundPaint: Paint? = null

    init {
        backgroundPaint = Paint().apply {
            color = Color.TRANSPARENT
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    override fun inflate(
        context: Context,
        resources: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Resources.Theme?
    ) {
        selectedColor = ColorUtils.setAlphaComponent(ContextCompat.getColor(context,
            context.getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimary)!!.resourceId), (.8f * 255).toInt())
        super.inflate(context, resources, parser, attrs, theme)
    }

    private val shapeBounds = RectF()

    override fun draw(canvas: Canvas) {
        (callback as? ImageView)?.let { imageView ->
            imageView.drawable?.let { drawable ->
                val drawableBounds = RectF(drawable.bounds)
                imageView.imageMatrix.mapRect(shapeBounds, drawableBounds)
                val radiusPx = if (radius == -1f) shapeBounds.height()/2f else radius
                canvas.drawRoundRect(shapeBounds, radiusPx, radiusPx, backgroundPaint!!)
            }
        }
        super.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        backgroundPaint?.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        super.setColorFilter(colorFilter)
        backgroundPaint?.setColorFilter(colorFilter)
    }

    private var backgroundAnimator: ValueAnimator? = null

    override fun onStateChange(stateSet: IntArray): Boolean {
        return super.onStateChange(stateSet).also {
            if (!it) return@also
            backgroundAnimator?.cancel()
            backgroundPaint?.apply {
                if (stateSet.contains(android.R.attr.state_selected)) {
                    if (color == selectedColor) return@also
                    color = selectedColor
                } else {
                    if (color == Color.TRANSPARENT) return@also
                    backgroundAnimator = ValueAnimator.ofArgb(selectedColor, Color.TRANSPARENT).apply {
                        duration = 250
                        startDelay = 100
                        addUpdateListener { animator ->
                            color = animator.animatedValue as Int
                            invalidateSelf()
                        }
                        start()
                    }
                }
            }
        }
    }

    override fun jumpToCurrentState() {
        super.jumpToCurrentState()
        backgroundAnimator?.cancel()
        backgroundPaint?.apply {
            if (state.contains(android.R.attr.state_selected)) {
                if (color == selectedColor) return
                color = selectedColor
            } else {
                if (color == Color.TRANSPARENT) return
                color = Color.TRANSPARENT
            }
        }
    }

    fun setCornerRadius(@Px radius: Float){
        this.radius = radius
        invalidateSelf()
    }

    companion object{
        private val LOG_TAG: String = SelectableAnimatedDrawable::class.java.simpleName

        fun create(
            context: Context,
            @DrawableRes resId: Int,
            theme: Theme?
        ): SelectableAnimatedDrawable? {
            try {
                val res = context.resources
                val parser: XmlPullParser = res.getXml(@Suppress("ResourceType") resId)
                val attrs = Xml.asAttributeSet(parser)

                var type: Int
                while ((parser.next().also { type = it }) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {/*Empty loop*/ }
                if (type != XmlPullParser.START_TAG) throw XmlPullParserException("No start tag found")

                parser.name.let {
                    if (it != "animated-selector") throw XmlPullParserException("${parser.positionDescription}:" +
                            " invalid animated-selector tag $it.") }

                return SelectableAnimatedDrawable().apply {
                    inflate(context, res, parser, attrs, theme)
                }
            } catch (e: XmlPullParserException) {
                Log.e(LOG_TAG, "parser error", e)
            } catch (e: IOException) {
                Log.e(LOG_TAG, "parser error", e)
            }
            return null
        }
    }
}
