package dev.oneuiproject.oneui.layout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.SeslSwitchBar
import dev.oneuiproject.oneui.design.R

/**
 * [ToolbarLayout] with a [SeslSwitchBar].
 */
class SwitchBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null) : ToolbarLayout(context, attrs) {

    /**
     * Returns the [SeslSwitchBar] in this layout.
     */
    val switchBar: SeslSwitchBar
    private val mSBLContainer: FrameLayout?

    init {
        LayoutInflater.from(context).inflate(R.layout.oui_layout_switchbarlayout, mMainContainer, true)
        switchBar = findViewById(R.id.switchbarlayout_switchbar)
        mSBLContainer = findViewById(R.id.switchbarlayout_container)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (mSBLContainer == null) {
            super.addView(child, index, params)
        } else {
            if ((params as ToolbarLayoutParams).layoutLocation == MAIN_CONTENT) {
                mSBLContainer.addView(child, params)
            } else {
                super.addView(child, index, params)
            }
        }
    }

    companion object {
        private const val TAG = "SwitchBarLayout"
        private const val MAIN_CONTENT = 0
    }
}
