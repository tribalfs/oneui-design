package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.core.content.res.use

/**
 * Custom LinearLayout that you can set a check state
 * and propagates this state to its checkable children
 */
@SuppressLint("ResourceType")
open class CheckableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr, defStyleRes), Checkable {
    private var mDisabledAlpha: Float = 0.4f
    private var mChecked = false

    init {
        context.theme.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.disabledAlpha,
                android.R.attr.listPreferredItemHeightSmall,
                android.R.attr.selectableItemBackground,
            )
        )
            .use {
                mDisabledAlpha = it.getFloat(0, .4f)
                minimumHeight = it.getDimension(1, 0f).toInt()
                setBackgroundResource(it.getResourceId(2, 0))
            }

        isClickable = true
        isLongClickable = true
    }

    override fun setEnabled(enabled: Boolean) {
        if (enabled == isEnabled) return
        super.setEnabled(enabled)
        for (i in 0 until childCount) {
            getChildAt(i).alpha = if (enabled) 1.0f else mDisabledAlpha
        }
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked == checked) return
        mChecked = checked
        updateCheckableChildViews()
    }

    override fun isChecked(): Boolean = mChecked

    override fun toggle() {
        isChecked = !mChecked
    }

    private fun updateCheckableChildViews() {
        for (i in 0 until childCount) {
            ( getChildAt(i) as? Checkable)?.isChecked = mChecked
        }
    }
}



