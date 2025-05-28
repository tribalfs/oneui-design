@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SeslCheckedTextView
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.utils.SemTouchFeedbackAnimator

/**
 * A custom view that provides a radio button-like item for use with [RadioItemViewGroup]
 * that animates the entire view when pressed.
 *
 * @param context The Context the view is running in, through which it can
 * access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for
 * the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that
 * supplies default values for the view, used only if
 * defStyleAttr is 0 or can not be found in the theme. Can be 0
 * to not look for defaults.
 */
class RadioItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : CheckableLinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    fun interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param radioItem The RadioItemView whose state has changed.
         * @param isChecked  The new checked state of radioItemView.
         */
        fun onCheckedChanged(radioItem: RadioItemView, isChecked: Boolean)
    }

    private var checkedTextView: SeslCheckedTextView
    private var topDivider: View
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    @RequiresApi(29)
    private lateinit var semTouchFeedbackAnimator: SemTouchFeedbackAnimator

    /**
     *  Show divider on top. True by default
     */
    var showTopDivider: Boolean
        get() = topDivider.isVisible
        set(value) {
            topDivider.isVisible = value
        }

    var title: CharSequence?
        get() = checkedTextView.text?.toString()
        set(value) {
            if (checkedTextView.text != value) {
                checkedTextView.text = value
            }
        }

    fun setTiTle(value: SpannableString) {
        checkedTextView.text = value
    }

    fun setOnCheckedChangeWidgetListener(listener: OnCheckedChangeListener?) {
        onCheckedChangeListener = listener
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.oui_des_widget_radio_item, this@RadioItemView)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        checkedTextView = findViewById(R.id.checkedTextView)

        topDivider = findViewById(R.id.top_divider)

        attrs?.let {
            context.withStyledAttributes(
                attrs,
                R.styleable.RadioItemView,
                defStyleAttr,
                defStyleRes
            ) {
                isEnabled = getBoolean(R.styleable.RadioItemView_android_enabled, true)
                isChecked = getBoolean(R.styleable.RadioItemView_android_checked, false)
                title = getText(R.styleable.RadioItemView_title)
                showTopDivider = getBoolean(R.styleable.RadioItemView_showTopDivider, true).also {
                    topDivider.isVisible = it
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 29) {
            semTouchFeedbackAnimator = SemTouchFeedbackAnimator(this)
        }
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked == checked) return
        super.setChecked(checked)
        onCheckedChangeListener?.onCheckedChanged(this, isChecked)
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            semTouchFeedbackAnimator.animate(motionEvent)
        }
        return super.dispatchTouchEvent(motionEvent)
    }
}