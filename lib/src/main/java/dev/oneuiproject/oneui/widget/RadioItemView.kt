@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.SeslCheckedTextView
import androidx.core.content.res.use
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R

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
    private var mTopDivider: View
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    /**
     *  Show divider on top. True by default
     */
    var showTopDivider: Boolean
        get() = mTopDivider.isVisible
        set(value) {
            mTopDivider.isVisible = value
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

    fun setOnCheckedChangeWidgetListener(listener: OnCheckedChangeListener?){
        onCheckedChangeListener =  listener
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.oui_des_widget_radio_item, this@RadioItemView)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        checkedTextView = findViewById(R.id.checkedTextView)

        mTopDivider = findViewById(R.id.top_divider)

        attrs?.let{
            context.obtainStyledAttributes(
                attrs,
                R.styleable.RadioItemView,
                defStyleAttr,
                defStyleRes
            ).use {a ->
                isEnabled = a.getBoolean(R.styleable.RadioItemView_android_enabled, true)
                isChecked = a.getBoolean(R.styleable.RadioItemView_android_checked, false)
                title = a.getText(R.styleable.RadioItemView_title)
                showTopDivider = a.getBoolean(R.styleable.RadioItemView_showTopDivider, true).also {
                    mTopDivider.isVisible = it
                }
            }
        }
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked == checked) return
        super.setChecked(checked)
        onCheckedChangeListener?.onCheckedChanged(this, isChecked)
    }
}