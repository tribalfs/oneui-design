@file:Suppress("MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.use
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity

open class MarginsTabLayout @JvmOverloads constructor(
    mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.tabStyle
) : TabLayout(mContext, attrs, defStyleAttr) {

    private var isSubTabStyle = false

    @JvmField
    internal var mRemeasure = false

    val activity: AppCompatActivity by lazy(LazyThreadSafetyMode.NONE) { context.appCompatActivity!! }

    var sideMargin: Int = resources.getDimension(R.dimen.oui_tab_layout_default_side_padding).toInt()
        set(value) {
            if (field == value) return
            field = value
            if (applySideMargin) {
                updateMargin()
            }else{
                Log.w(TAG, "setSideMargin called without applyMargin set to true")
            }
        }

    var applySideMargin = false
        set(value) {
            if (field == value) return
            field = value
            updateMargin()
        }

    init{
        context.obtainStyledAttributes(attrs, R.styleable.MarginsTabLayout).use{
            sideMargin = it.getDimensionPixelSize(R.styleable.MarginsTabLayout_sideMargin, sideMargin)
            applySideMargin = it.getBoolean(R.styleable.MarginsTabLayout_applySideMargin, true)
            val mDepthStyle = it.getInteger(R.styleable.MarginsTabLayout_seslDepthStyle, 1 /*DEPTH_TYPE_MAIN*/)
            if (mDepthStyle == 2 /*SubTab*/){
                seslSetSubTabStyle()
            }
        }
    }

    override fun seslSetSubTabStyle() {
        if (isSubTabStyle) return
        super.seslSetSubTabStyle()
        isSubTabStyle = true
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidateTabLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mRemeasure = true
        invalidateTabLayout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRemeasure = true
        invalidateTabLayout()
    }

    open fun invalidateTabLayout() = updateMargin()

    private fun updateMargin() {
        val layoutParams = layoutParams as? MarginLayoutParams ?: return
        val marginToApply = if (applySideMargin) sideMargin else 0
        if (layoutParams.marginStart != marginToApply || layoutParams.marginEnd != marginToApply) {
            layoutParams.marginStart = marginToApply
            layoutParams.marginEnd = marginToApply
            setLayoutParams(layoutParams)
        }
    }


    companion object{
        private const val TAG = "MarginsTabLayout"
    }


}

