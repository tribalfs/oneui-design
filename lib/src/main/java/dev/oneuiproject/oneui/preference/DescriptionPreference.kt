package dev.oneuiproject.oneui.preference

import android.R.attr.mode
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.util.SeslRoundedCorner
import androidx.core.content.withStyledAttributes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R
import androidx.preference.R as prefR

/**
 * A Preference that is used to show a description for a PreferenceCategory or a PreferenceScreen.
 *
 * - It has three [position modes][PositionMode] which can be set using app:positionMode
 * or [setPositionMode] function.
 * - It also has a roundedCorners attribute that can be used to set the rounded corners of the background.
 * The default value is SeslRoundedCorner.ROUNDED_CORNER_ALL.
 *
 * ## Example usage:
 * ```xml
 * <DescriptionPreference
 *     app:title="This is a description"
 *     app:positionMode="firstItem"
 *     app:roundedCorners="all" />
 * ```
 * @param context The Context this preference is associated with.
 * @param attrs The attributes of the XML tag that is inflating the preference.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 * that supplies default values for the view. Can be 0 to not look for defaults.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 */
class DescriptionPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Enum class for the position mode of the description preference.
     *
     * @property NORMAL The description is shown with normal margins. This is the default.
     * @property FIRST_ITEM The description is shown with larger top margin. Used when it's the first item in a category.
     * @property SUBHEADER The description is shown with smaller top margin. Used when it's a subheader.
     */
    enum class PositionMode{ NORMAL, FIRST_ITEM, SUBHEADER }

    private var _positionMode: PositionMode = PositionMode.NORMAL

    init {
        isSelectable = false

        if (attrs != null) {
            context.withStyledAttributes(attrs, prefR.styleable.Preference) {
                @SuppressLint("PrivateResource")
                layoutResource = getResourceId(
                    prefR.styleable.Preference_android_layout,
                    R.layout.oui_des_preference_unclickable_layout
                )
            }

            context.withStyledAttributes(attrs, R.styleable.DescriptionPreference) {
                seslSetSubheaderRoundedBackground(
                    getInt(R.styleable.DescriptionPreference_roundedCorners, SeslRoundedCorner.ROUNDED_CORNER_ALL)
                )
                getInt(R.styleable.DescriptionPreference_positionMode, 0).let {
                    _positionMode = when(it){
                        0 -> PositionMode.NORMAL
                        1 -> PositionMode.FIRST_ITEM
                        2 -> PositionMode.SUBHEADER
                        else -> throw IllegalArgumentException("Invalid position mode: $mode")
                    }
                }
            }
        } else {
            layoutResource = R.layout.oui_des_preference_unclickable_layout
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
        val res = context.resources
        when (_positionMode) {
            PositionMode.FIRST_ITEM -> {
                top = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_first_margin_top)
                bottom = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_first_margin_bottom)
            }

            PositionMode.SUBHEADER -> {
                top = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_subheader_margin_top)
                bottom = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_subheader_margin_bottom)
            }

            PositionMode.NORMAL -> {
                top = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_margin_top)
                bottom = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_margin_bottom)
            }
        }

        val horizontal = res.getDimensionPixelSize(R.dimen.oui_des_unclickablepref_text_padding_start_end)

        lp.setMargins(horizontal, top, horizontal, bottom)
        titleTextView.layoutParams = lp

        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
    }

    @Deprecated("Use setPositionMode(mode: PositionMode) instead.")
    fun setPositionMode(mode: Int) {
        _positionMode = when(mode){
            0 -> PositionMode.NORMAL
            1 -> PositionMode.FIRST_ITEM
            2 -> PositionMode.SUBHEADER
            else -> throw IllegalArgumentException("Invalid position mode: $mode")
        }
    }


    /**
     * Sets the position mode for the description.
     * This affects the top and bottom margins of the description text.
     *
     * @param mode The [PositionMode] to set.
     */
    fun setPositionMode(mode: PositionMode) {
        _positionMode = mode
    }
}
