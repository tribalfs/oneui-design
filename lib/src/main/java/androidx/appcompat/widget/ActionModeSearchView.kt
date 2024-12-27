package androidx.appcompat.widget

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.isLightMode
import androidx.appcompat.R as appcompatR

//We are placing this here to avoid using reflection
internal class  ActionModeSearchView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle
): SearchView(context, attrs, defStyleAttr){

    @JvmField
    var onCloseClickListener: ((View) -> Unit) ? = null

    init {
        setIconifiedByDefault(false)
        context.theme.obtainStyledAttributes(intArrayOf(
            appcompatR.attr.searchViewHintTextColor,
            appcompatR.attr.searchViewTextColor
        )).use {
            val hintColor = it.getColor(0, ContextCompat.getColor(context,
                android.R.color.darker_gray))
            val textColor = it.getColor( @Suppress("ResourceType") 1,
                ContextCompat.getColor(context, R.color.oui_primary_text_color))
            mSearchSrcTextView.setHintTextColor(hintColor)
            mSearchSrcTextView.setTextColor(textColor)
            mVoiceButton.setColorFilter(textColor)
            mCloseButton.setColorFilter(textColor)
        }

        val backgroundColor = if (context.isLightMode()) {
            Color.parseColor("#0D000000")
        } else {
            Color.parseColor("#26ffffff")
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val res = context.resources
            cornerRadius = res.getDimension(
                @Suppress("PrivateResource")
                appcompatR.dimen.sesl_rounded_corner_radius)
            setColor(backgroundColor)
        }
    }

    public override fun onCloseClicked() {
        clearFocus()
        onCloseClickListener?.invoke(this)
    }

    override fun setVisibility(visibility: Int) {
        if (visibility == VISIBLE) {
            post { onSearchClicked() }
        }
        super.setVisibility(visibility)
    }

    fun setQuery(query: CharSequence) {
        setQuery(query, true)
    }

}


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SearchView.applyThemeColors(){
    with (context!!) {
        theme.obtainStyledAttributes(
            intArrayOf(
                appcompatR.attr.searchViewHintTextColor,
                appcompatR.attr.searchViewIconColor,
                appcompatR.attr.searchViewTextColor
            )
        ).use {
            val hintColor = it.getColor(0, 0)
            val iconColor = it.getColor(@Suppress("ResourceType") 1, 0)
            val textColor = it.getColor(@Suppress("ResourceType") 2, 0)
            setSearchViewColors(iconColor, textColor, hintColor)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SearchView.setSearchViewColors(@ColorInt iconColor: Int, @ColorInt textColor: Int, @ColorInt hintColor: Int){
    mSearchSrcTextView.setTextColor(textColor)
    mSearchSrcTextView.setHintTextColor(hintColor)
    mVoiceButton.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
    mCloseButton.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
    mGoButton.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
    mSearchButton.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
    /*try {
            val fieldList = SearchView::class.java.declaredFields
            for (f in fieldList) {
                val fieldType = f.type
                when (fieldType.simpleName){
                    ImageView::class.simpleName -> {
                        f.isAccessible = true
                        (f.get(this) as ImageView).setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
                    }
                    SearchView.SearchAutoComplete::class.simpleName -> {
                        f.isAccessible = true
                        (f.get(this) as SearchView.SearchAutoComplete).setTextColor(textColor)
                        (f.get(this) as SearchView.SearchAutoComplete).setHintTextColor(hintColor)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setRoundedColor $e")
        }*/
}
