package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.util.SeslRoundedCorner
import androidx.core.content.res.use
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R

class DescriptionPreference @JvmOverloads constructor(
    private val context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) :
    Preference(context, attrs, defStyleAttr, defStyleRes) {
    private var mPositionMode = POSITION_NORMAL

    init {
        isSelectable = false

        if (attrs != null) {
            context.obtainStyledAttributes(attrs, androidx.preference.R.styleable.Preference).use {
                layoutResource = it.getResourceId(
                    androidx.preference.R.styleable.Preference_android_layout,
                    R.layout.oui_preference_unclickable_layout
                )
            }

            context.obtainStyledAttributes(attrs, R.styleable.DescriptionPreference).use {
                seslSetSubheaderRoundedBackground(
                    it.getInt(R.styleable.DescriptionPreference_roundedCorners, SeslRoundedCorner.ROUNDED_CORNER_ALL)
                )
                mPositionMode = it.getInt(R.styleable.DescriptionPreference_positionMode, POSITION_NORMAL)
            }
        } else {
            layoutResource = R.layout.oui_preference_unclickable_layout
            seslSetSubheaderRoundedBackground(SeslRoundedCorner.ROUNDED_CORNER_ALL)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val titleTextView = holder.findViewById(R.id.title) as TextView?
        titleTextView!!.text = title
        titleTextView.visibility = View.VISIBLE

        val lp = titleTextView.layoutParams as LinearLayout.LayoutParams

        val top: Int
        val bottom: Int
        when (mPositionMode) {
            POSITION_FIRST_ITEM -> {
                top = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_first_margin_top)
                bottom = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_first_margin_bottom)
            }

            POSITION_SUBHEADER -> {
                top = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_subheader_margin_top)
                bottom = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_subheader_margin_bottom)
            }

            POSITION_NORMAL -> {
                top = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_margin_top)
                bottom = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_margin_bottom)
            }

            else -> {
                top = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_margin_top)
                bottom = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_margin_bottom)
            }
        }

        val horizontal = context.resources.getDimensionPixelSize(R.dimen.oui_unclickablepref_text_padding_start_end)

        lp.setMargins(horizontal, top, horizontal, bottom)
        titleTextView.layoutParams = lp

        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
    }

    fun setPositionMode(mode: Int) {
        mPositionMode = mode
    }

    companion object {
        private const val POSITION_NORMAL = 0
        private const val POSITION_FIRST_ITEM = 1
        private const val POSITION_SUBHEADER = 2
    }
}
