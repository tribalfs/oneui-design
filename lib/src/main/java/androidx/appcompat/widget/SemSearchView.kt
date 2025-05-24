package androidx.appcompat.widget

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.isLightMode
import androidx.appcompat.R as appcompatR
import androidx.core.graphics.toColorInt

//We are placing this here to avoid using reflection
internal class SemSearchView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle
): SearchView(context, attrs, defStyleAttr){

    @JvmField
    var onCloseClickListener: ((View) -> Unit) ? = null

    override fun onCloseClicked() {
        onCloseClickListener?.let {
            clearFocus()
            it.invoke(this)
        } ?: super.onCloseClicked()
    }
}


internal fun SearchView.applyActionModeSearchStyle(){
    context.withStyledAttributes(
        attrs =  intArrayOf(appcompatR.attr.searchViewHintTextColor, android.R.attr.textColorPrimary)
    ) {
        val hintColor = getColor(0, ContextCompat.getColor(context,
            android.R.color.darker_gray))
        val textColor = getColor( @Suppress("ResourceType") 1,
            ContextCompat.getColor(context, R.color.oui_des_primary_text_color))
        setSearchViewColors(textColor, textColor, hintColor)
    }

    val backgroundColor = if (context.isLightMode()) {
        "#0D000000".toColorInt()
    } else {
        "#26ffffff".toColorInt()
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


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun SearchView.applyThemeColors(){
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
internal fun SearchView.setSearchViewColors(@ColorInt iconColor: Int, @ColorInt textColor: Int, @ColorInt hintColor: Int){
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
