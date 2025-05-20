package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.Px
import androidx.appcompat.util.SeslRoundedCorner
import androidx.core.content.res.use
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R

class InsetPreferenceCategory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) :
    PreferenceCategory(context, attrs) {
    private var customHeight = 0

    init {
        @SuppressLint("PrivateResource")
        customHeight = context.resources
            .getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_list_subheader_min_height)

        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.InsetPreferenceCategory).use {a ->
                customHeight = a.getDimensionPixelSize(R.styleable.InsetPreferenceCategory_height, customHeight)
                seslSetSubheaderRoundedBackground(
                    a.getInt(R.styleable.InsetPreferenceCategory_roundedCorners, SeslRoundedCorner.ROUNDED_CORNER_ALL)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, customHeight)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    fun setHeight(@Px height: Int) { if (height >= 0) customHeight = height }
}