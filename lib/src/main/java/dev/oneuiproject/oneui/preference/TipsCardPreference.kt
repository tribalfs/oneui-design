@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.R.attr.preferenceStyle
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue

class TipsCardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var mCancelBtnOCL: View.OnClickListener? = null
    private val mBottomBarBtns = ArrayList<TextView>()
    private lateinit var mItemView: View
    private var mCancelButton: ImageView? = null
    private var mEmptyBottom: View? = null
    private var mBottomBar: LinearLayout? = null

    init {
        isSelectable = false
        layoutResource = R.layout.oui_des_preference_tips_layout
        val primaryTextColor = ResourcesCompat.getColorStateList(context.resources,
            context.getThemeAttributeValue(android.R.attr.textColorPrimary)!!.resourceId,
            context.theme)
        seslSetSummaryColor(primaryTextColor)
    }

    override fun onBindViewHolder(preferenceViewHolder: PreferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder)
        mItemView = preferenceViewHolder.itemView

        // workaround since we can't use setSelectable here
        onPreferenceClickListener?.let {pcl ->
            mItemView.setOnClickListener {
                pcl.onPreferenceClick(this@TipsCardPreference)
            }
        }

        with(mItemView) {
            findViewById<RelativeLayout>(R.id.tips_title_container).apply {
                isVisible  = !TextUtils.isEmpty(title)
            }

            mCancelButton = findViewById<ImageView>(R.id.tips_cancel_button).apply {
                isVisible  = mCancelBtnOCL != null
                setOnClickListener(mCancelBtnOCL)
            }

            mEmptyBottom = findViewById(R.id.tips_empty_bottom)
            mBottomBar = findViewById(R.id.tips_bottom_bar)
        }

        if (mBottomBarBtns.size > 0) {
            mBottomBar!!.visibility = View.VISIBLE
            (mItemView as ViewGroup).removeView(mEmptyBottom)
            mEmptyBottom = null
            for (txtView in mBottomBarBtns) {
                mBottomBar!!.addView(txtView)
            }
            mBottomBarBtns.clear()
        }
    }

    fun addButton(text: CharSequence?, listener: View.OnClickListener?): TextView {
        val txtView = TextView(context, null, 0, R.style.OneUI_TipsCardTextButtonStyle).apply {
            setText(text)
            setOnClickListener(listener)
        }
        if (mBottomBar != null) {
            mBottomBar!!.isVisible = true
            mEmptyBottom?.apply {
                (mItemView as ViewGroup?)!!.removeView(mEmptyBottom)
                mEmptyBottom = null
            }
            mBottomBar!!.addView(txtView)
        }else{
            mBottomBarBtns.add(txtView)
        }
        return txtView
    }

    fun setOnCancelClickListener(listener: View.OnClickListener?) {
        mCancelBtnOCL = listener
        mCancelButton?.apply {
            isVisible = listener != null
            setOnClickListener(listener)
        }
    }
}