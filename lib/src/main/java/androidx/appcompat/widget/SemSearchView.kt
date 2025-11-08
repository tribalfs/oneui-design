package androidx.appcompat.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Property
import android.view.View
import androidx.activity.BackEventCompat
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import androidx.core.view.animation.PathInterpolatorCompat
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.isLightMode
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import androidx.appcompat.R as appcompatR

//We are placing this here to avoid using reflection
internal class SemSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle
) : SearchView(context, attrs, defStyleAttr), BackHandler {

    @JvmField
    var onCloseClickListener: ((View) -> Unit)? = null
    internal var searchModeOBPBehavior = CLEAR_DISMISS
    internal var predictiveBackEnabled = true

    private var backProgress: Property<View, Float>? = null
    private var backAnimator: ObjectAnimator? = null
    private var currentBackProgress: Float = 0f

    private val backInterpolator = PathInterpolatorCompat.create(0f, 0f, 0f, 1f)

    override fun onCloseClicked() {
        onCloseClickListener?.let {
            clearFocus()
            it.invoke(this)
        } ?: super.onCloseClicked()
    }

    override fun startBackProgress(backEvent: BackEventCompat) {
        if (!predictiveBackEnabled) return
        val hasQuery = query?.isNotEmpty() == true
        if (hasQuery || searchModeOBPBehavior != CLEAR_CLOSE) {
            backAnimator?.cancel()
            backAnimator = createBackAnimator(hasQuery)
        }
    }

    private fun createBackAnimator(hasQuery: Boolean): ObjectAnimator {
        val clearMode = hasQuery && searchModeOBPBehavior != DISMISS
        val target: View = if (clearMode) mSearchSrcTextView else (this.parent as? Toolbar) ?: this
        val interpolate: (Float) -> Float = if (clearMode) {
            {v -> 1.0f - v * 0.90f }
        } else {
            {v -> 1.0f - (v * 1.25f).coerceAtMost(1f) }
        }

        backProgress = object : Property<View, Float>(
            Float::class.java, "backProgress"
        ) {
            override fun get(view: View): Float = currentBackProgress
            override fun set(view: View, value: Float) {
                currentBackProgress = value
                target.alpha = interpolate(value)
            }
        }

        return ObjectAnimator.ofFloat(target, backProgress, 0.0f, 1.0f)
    }

    override fun updateBackProgress(backEvent: BackEventCompat) {
        backAnimator?.currentPlayTime = (backInterpolator.getInterpolation(backEvent.progress)
                * backAnimator!!.duration).toLong()
    }

    override fun handleBackInvoked() = resetView()

    override fun cancelBackProgress() = resetView()

    private fun resetView() {
        if (backAnimator == null) return

        val target = backAnimator!!.target as View
        val currentProgress = (backProgress as Property<View, Float>).get(this)
        backAnimator?.cancel()
        backAnimator = null

        ObjectAnimator
            .ofFloat(target, backProgress, currentProgress, 0f).apply {
                duration = 200
                interpolator = backInterpolator
                start()
            }
    }
}


internal fun SearchView.applyActionModeSearchStyle() {
    context.withStyledAttributes(
        attrs = intArrayOf(appcompatR.attr.searchViewHintTextColor, android.R.attr.textColorPrimary)
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
        withStyledAttributes(null, intArrayOf(
            appcompatR.attr.searchViewHintTextColor,
            appcompatR.attr.searchViewIconColor,
            appcompatR.attr.searchViewTextColor
        )) {
            val hintColor = getColor(0, 0)
            val iconColor = getColor(@Suppress("ResourceType") 1, 0)
            val textColor = getColor(@Suppress("ResourceType") 2, 0)
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
