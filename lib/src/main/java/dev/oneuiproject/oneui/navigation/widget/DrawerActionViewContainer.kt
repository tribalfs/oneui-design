package dev.oneuiproject.oneui.navigation.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.children


internal class DrawerActionViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var originalHeight: Int? = null
    private var mOffset: Float = 1f

    private companion object{
        private const val ACCELERATION_FACTOR = 0.5f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (originalHeight == null) {
            originalHeight = measuredHeight
        }
        val newHeight = (originalHeight!! * mOffset).toInt()
        setMeasuredDimension(measuredWidth, newHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val alphaFraction = ((mOffset - (1 - ACCELERATION_FACTOR)) / ACCELERATION_FACTOR).coerceIn(0f, 1f)
        val transY = (height - originalHeight!!).toFloat() * ACCELERATION_FACTOR
        for (child in children){
            child.translationY = transY
            child.alpha = alphaFraction
        }
    }

    fun setOffset(offset: Float) {
        if (mOffset == offset) return
        mOffset = offset
        //Call requestLayout() on the next UI thread cycle
        //to help avoid conflicts with ongoing animations.
        post { requestLayout() }
    }
}