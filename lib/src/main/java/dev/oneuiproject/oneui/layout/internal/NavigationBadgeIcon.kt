package dev.oneuiproject.oneui.layout.internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View.LAYOUT_DIRECTION_RTL
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.layout.ToolbarLayout.Badge
import dev.oneuiproject.oneui.utils.badgeCountToText
import dev.oneuiproject.oneui.utils.getNormalFont
import androidx.appcompat.R as appcompatR

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("PrivateResource")
class NavigationBadgeIcon(private val context: Context) : Drawable() {

    private val mCirclePaint = Paint().apply {
        color = ContextCompat.getColor(context, appcompatR.color.sesl_badge_background_color)
        isAntiAlias = true
    }
    private val res = context.resources

    private val mTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, appcompatR.color.sesl_menu_badge_text_color)
        textAlign = Paint.Align.CENTER
        setTypeface(getNormalFont())
        textSize = res.getDimensionPixelSize(appcompatR.dimen.sesl_menu_item_badge_text_size).toFloat()
        isAntiAlias = true
    }

    private var badge: Badge = Badge.None()
    private var mIsLandscape = false
    private val cornerRadius = 9f.dpToPx(context.resources).toFloat()
    private val defaultWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_default_width)
    private val additionalWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_additional_width)
    private val originalTextSize = res.getDimensionPixelSize(appcompatR.dimen.sesl_menu_item_badge_text_size).toFloat()

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val isRTL = context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL

        if (badge is Badge.Dot) {
            val x = (if (isRTL) DOT_BADGE_OFFSET else width - DOT_BADGE_OFFSET).toFloat()
            val y = DOT_BADGE_OFFSET
            val radius = 7f
            canvas.drawCircle(x, y, radius, mCirclePaint)
        } else if (badge is Badge.Numeric) {
            val mBadgeText = (badge as Badge.Numeric).count.badgeCountToText()!!
            val x = (if (isRTL) N_BADGE_OFFSET else width - N_BADGE_OFFSET).toFloat()
            val y = N_BADGE_OFFSET

            val halfWidth = ((defaultWidth + mBadgeText.length * additionalWidth)) / 2
            val halfHeight = ((defaultWidth + additionalWidth)) / 2
            val left = x - halfWidth
            val right = x + halfWidth
            val top = y - halfHeight
            val bottom = y + halfHeight
            canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, mCirclePaint)

            val textBounds = Rect()
            mTextPaint.textSize = originalTextSize
            mTextPaint.getTextBounds(mBadgeText, 0, mBadgeText.length, textBounds)
            canvas.drawText(mBadgeText, x, y + textBounds.height() / 2f, mTextPaint)
        }
    }

    fun setBadge(badge: Badge) {
        if (this.badge == badge) return
        this.badge = badge
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        mCirclePaint.alpha = alpha
        mTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mCirclePaint.setColorFilter(colorFilter)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    companion object{
        private const val DOT_BADGE_OFFSET = 0F
        private const val N_BADGE_OFFSET = 4F
    }
}
