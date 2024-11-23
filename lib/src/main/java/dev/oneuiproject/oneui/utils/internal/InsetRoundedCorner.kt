package dev.oneuiproject.oneui.utils.internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.appcompat.util.SeslRoundedCorner
import androidx.core.graphics.Insets

@RestrictTo(RestrictTo.Scope.LIBRARY)
class InsetRoundedCorner(context: Context) : SeslRoundedCorner(context) {

    private val insetPaint = Paint().apply { style = Paint.Style.FILL }
    private val mCanvasBounds: Rect = Rect()

    override fun setRoundedCornerColor(corners: Int, @ColorInt color: Int) {
        super.setRoundedCornerColor(corners, color)
        insetPaint.setColor(color)
    }

    fun drawRoundedCorner(canvas: Canvas, insets: Insets) {
        canvas.getClipBounds(mCanvasBounds)
        drawRoundedCornerInternal(canvas, insets)
    }

    @SuppressLint("RestrictedApi")
    private fun drawRoundedCornerInternal(canvas: Canvas, insets: Insets) {
        val mRoundedCornerMode = roundedCorners
        val mRoundRadius = roundedCornerRadius

        val canvasLeft = mCanvasBounds.left
        val canvasTop = mCanvasBounds.top
        val canvasRight = mCanvasBounds.right
        val canvasBottom = mCanvasBounds.bottom

        val left = canvasLeft + insets.left
        val right = canvasRight - insets.right
        val top = canvasTop + insets.top
        val bottom = canvasBottom - insets.bottom

        if ((mRoundedCornerMode and ROUNDED_CORNER_TOP_LEFT) != 0) {
            mTopLeftRound.setBounds(left, top , left + mRoundRadius, mRoundRadius + top)
            mTopLeftRound.draw(canvas)
        }

        if ((mRoundedCornerMode and ROUNDED_CORNER_TOP_RIGHT) != 0) {
            mTopRightRound.setBounds(right - mRoundRadius, top, right, mRoundRadius + top)
            mTopRightRound.draw(canvas)
        }

        if ((mRoundedCornerMode and ROUNDED_CORNER_BOTTOM_LEFT) != 0) {
            mBottomLeftRound.setBounds(left, bottom - mRoundRadius, mRoundRadius + left, bottom)
            mBottomLeftRound.draw(canvas)
        }

        if ((mRoundedCornerMode and ROUNDED_CORNER_BOTTOM_RIGHT) != 0) {
            mBottomRightRound.setBounds(right - mRoundRadius, bottom - mRoundRadius, right, bottom)
            mBottomRightRound.draw(canvas)
        }

        if (insets.top > 0){
            canvas.drawRect(
                canvasLeft.toFloat(),
                canvasTop.toFloat(),
                canvasRight.toFloat(),
                top.toFloat(),
                insetPaint
            )
        }

        if (insets.bottom > 0){
            canvas.drawRect(
                canvasLeft.toFloat(),
                bottom.toFloat(),
                canvasRight.toFloat(),
                canvasBottom.toFloat(),
                insetPaint
            )
        }

        if (insets.left > 0){
            canvas.drawRect(
                canvasLeft.toFloat(),
                canvasTop.toFloat(),
                left.toFloat(),
                canvasBottom.toFloat(),
                insetPaint
            )
        }


        if (insets.right > 0){
            canvas.drawRect(
                right.toFloat(),
                canvasTop.toFloat(),
                canvasRight.toFloat(),
                canvasBottom.toFloat(),
                insetPaint
            )
        }
    }

    companion object {
        private const val TAG = "InsetRoundedCorner"
    }
}
