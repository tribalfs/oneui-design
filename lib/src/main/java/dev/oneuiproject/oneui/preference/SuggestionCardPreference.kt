@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.R.attr.preferenceStyle
import com.airbnb.lottie.LottieAnimationView
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue


class SuggestionCardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var mClosedListener: View.OnClickListener? = null

    private var mCloseButtonClickListener = View.OnClickListener{
        mClosedListener?.onClick(it)
        parent?.removePreference(this)
    }

    private var mActionButtonClickListener: View.OnClickListener? = null
    private lateinit var mItemView: View
    private var mLottieAnimationView: LottieAnimationView? = null
    private var mActionButtonContainer: LinearLayout? = null
    private var mActionButtonTextView: TextView? = null
    private var mActionButtonText: String? = null

    @RawRes
    private var animationResId: Int? = null
    private var animationAssetName: String? = null

    init {
        isSelectable = false
        layoutResource = R.layout.oui_des_preference_suggestion_card
        if (attrs != null) {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.SuggestionCardPreference
            ).use {a ->
                setActionButtonText(a.getText(R.styleable.SuggestionCardPreference_actionButtonText)?.toString())
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
        mItemView = preferenceViewHolder.itemView

        with(mItemView) {
            findViewById<ImageView>(R.id.exit_button).apply {
                setOnClickListener(mCloseButtonClickListener)
            }
            mLottieAnimationView = findViewById(R.id.lottie_view)
            mActionButtonContainer = findViewById<LinearLayout>(R.id.action_button_container).apply {
                setOnClickListener(mActionButtonClickListener)
            }
            mActionButtonTextView = findViewById<TextView>(R.id.action_button_text).apply {
                text = mActionButtonText
            }
        }
    }

    fun setOnClosedClickedListener(listener: View.OnClickListener?) {
        mClosedListener = listener
    }

    fun setActionButtonText(str: String?) {
        mActionButtonText = str
        mActionButtonTextView?.apply {
            isVisible = !mActionButtonText.isNullOrEmpty()
            text = str
        }
    }

    fun setActionButtonOnClickListener(onClickListener: View.OnClickListener) {
        mActionButtonClickListener = onClickListener
        mActionButtonContainer?.setOnClickListener(onClickListener)
    }

    @JvmOverloads
    fun startTurnOnAnimation(postAnimationButtonText: String? = null) {
        if (mLottieAnimationView == null || mActionButtonTextView == null) return

        mActionButtonTextView!!.animate()
            .alpha(0.0f)
            .setDuration(100L)
            .setInterpolator(LinearInterpolator())
            .withEndAction{
                postAnimationButtonText?.let{setActionButtonText(it)}
                mActionButtonTextView!!.apply {
                    animate()
                        .alpha(1.0f)
                        .setDuration(200L)
                        .setInterpolator(LinearInterpolator())
                        .start()

                    translationX -= mLottieAnimationView!!.layoutParams.width

                    animate()
                        .translationX(0.0f)
                        .setDuration(400L)
                        .setInterpolator(PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f))
                        .start()
                }
                mLottieAnimationView!!.apply {
                    setVisibility(View.VISIBLE)
                    setAnimation("sec_suggestions_done.json")
                    setRepeatCount(0)
                    playAnimation()
                }
            }
            .start()
    }

}