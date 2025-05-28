package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes

/**
 * Custom LinearLayout that you can set a check state
 * and propagates this state to its checkable children
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 */
@SuppressLint("ResourceType")
open class CheckableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr, defStyleRes), Checkable {
    private var disabledAlpha: Float = 0.4f
    private var _checked = false

    init {
        context.withStyledAttributes(
            attrs = intArrayOf(
                android.R.attr.disabledAlpha,
                android.R.attr.listPreferredItemHeightSmall,
                android.R.attr.selectableItemBackground,
            )
        ) {
            disabledAlpha = getFloat(0, .4f)
            minimumHeight = getDimension(1, 0f).toInt()
            setBackgroundResource(getResourceId(2, 0))
        }

        isClickable = true
        isLongClickable = true
    }

    override fun setEnabled(enabled: Boolean) {
        if (enabled == isEnabled) return
        super.setEnabled(enabled)
        for (i in 0 until childCount) {
            getChildAt(i).alpha = if (enabled) 1.0f else disabledAlpha
        }
    }

    override fun setChecked(checked: Boolean) {
        if (this._checked == checked) return
        this._checked = checked
        updateCheckableChildViews()
    }

    override fun isChecked(): Boolean = _checked

    override fun toggle() {
        isChecked = !_checked
    }

    private fun updateCheckableChildViews() {
        for (i in 0 until childCount) {
            (getChildAt(i) as? Checkable)?.isChecked = _checked
        }
    }
}



