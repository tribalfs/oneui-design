@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.R.attr.preferenceStyle
import com.airbnb.lottie.LottieAnimationView
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.LINEAR_INTERPOLATOR
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.PATH_0_22_0_25_0_0_1_0

/**
 * A Preference that provides a layout that looks like a suggestion card.
 * @attr ref R.styleable#SuggestionCardPreference_actionButtonText
 */
class SuggestionCardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var closedListener: View.OnClickListener? = null

    private var closeButtonClickListener = View.OnClickListener{
        closedListener?.onClick(it)
        parent?.removePreference(this)
    }

    private var actionButtonClickListener: View.OnClickListener? = null
    private lateinit var itemView: View
    private var lottieAnimationView: LottieAnimationView? = null
    private var actionButtonContainer: LinearLayout? = null
    private var actionButtonTextView: TextView? = null
    private var actionButtonText: String? = null

    @RawRes
    private var animationResId: Int? = null
    private var animationAssetName: String? = null

    init {
        isSelectable = false
        layoutResource = R.layout.oui_des_preference_suggestion_card
        if (attrs != null) {
            context.withStyledAttributes(
                attrs,
                R.styleable.SuggestionCardPreference
            ) {
                setActionButtonText(getText(R.styleable.SuggestionCardPreference_actionButtonText)?.toString())
            }
        }
        setIcon(R.drawable.oui_des_preference_suggestion_card_icon)
        val primaryTextColor = ResourcesCompat.getColorStateList(context.resources,
            context.getThemeAttributeValue(android.R.attr.textColorPrimary)!!.resourceId,
            context.theme)
        seslSetSummaryColor(primaryTextColor)
    }

    override fun onBindViewHolder(preferenceViewHolder: PreferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder)
        itemView = preferenceViewHolder.itemView

        with(itemView) {
            findViewById<ImageView>(R.id.exit_button).apply {
                setOnClickListener(closeButtonClickListener)
            }
            lottieAnimationView = findViewById(R.id.lottie_view)
            actionButtonContainer = findViewById<LinearLayout>(R.id.action_button_container).apply {
                setOnClickListener(actionButtonClickListener)
            }
            actionButtonTextView = findViewById<TextView>(R.id.action_button_text).apply {
                text = actionButtonText
            }
        }
    }

    fun setOnClosedClickedListener(listener: View.OnClickListener?) {
        closedListener = listener
    }

    fun setActionButtonText(str: String?) {
        actionButtonText = str
        actionButtonTextView?.apply {
            isVisible = !actionButtonText.isNullOrEmpty()
            text = str
        }
    }

    fun setActionButtonOnClickListener(onClickListener: View.OnClickListener) {
        actionButtonClickListener = onClickListener
        actionButtonContainer?.setOnClickListener(onClickListener)
    }

    @JvmOverloads
    fun startTurnOnAnimation(postAnimationButtonText: String? = null) {
        if (lottieAnimationView == null || actionButtonTextView == null) return

        actionButtonTextView!!.animate()
            .alpha(0.0f)
            .setDuration(100L)
            .setInterpolator(CachedInterpolatorFactory.getOrCreate(LINEAR_INTERPOLATOR))
            .withEndAction{
                postAnimationButtonText?.let{setActionButtonText(it)}
                actionButtonTextView!!.apply {
                    animate()
                        .alpha(1.0f)
                        .setDuration(200L)
                        .setInterpolator(CachedInterpolatorFactory.getOrCreate(LINEAR_INTERPOLATOR))
                        .start()

                    translationX -= lottieAnimationView!!.layoutParams.width

                    animate()
                        .translationX(0.0f)
                        .setDuration(400L)
                        .setInterpolator(CachedInterpolatorFactory.getOrCreate(PATH_0_22_0_25_0_0_1_0))
                        .start()
                }
                lottieAnimationView!!.apply {
                    setVisibility(View.VISIBLE)
                    setAnimation("sec_suggestions_done.json")
                    setRepeatCount(0)
                    playAnimation()
                }
            }
            .start()
    }

}