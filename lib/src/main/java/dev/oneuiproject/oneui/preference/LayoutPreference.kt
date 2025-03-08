@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.use
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.widget.RelativeLinksCard

class LayoutPreference : Preference {
    private var mRootView: View? = null
    private var mAllowDividerAbove = false
    private var mAllowDividerBelow = false
    private var isRelativeLinkView = false

    @SuppressLint("RestrictedApi")
    private val mClickListener = View.OnClickListener { this.performClick(it) }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr)
    }

    constructor(context: Context, resource: Int) : this(
        context,
        LayoutInflater.from(context).inflate(resource, null, false)
    )

    constructor(context: Context, view: View) : this(context, view,  view is RelativeLinksCard)

    constructor(context: Context, view: View, isRelativeLinkView: Boolean) : super(context) {
        setView(view)
        this.isRelativeLinkView = isRelativeLinkView
    }

    @SuppressLint("RestrictedApi", "PrivateResource")
    private fun init(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        context.obtainStyledAttributes(attrs, androidx.preference.R.styleable.Preference).use {
            mAllowDividerAbove = it.getBoolean(
                androidx.preference.R.styleable.Preference_allowDividerAbove,
                false
            )
            mAllowDividerBelow = it.getBoolean(
                androidx.preference.R.styleable.Preference_allowDividerBelow,
                false
            )
        }

        context.obtainStyledAttributes(
            attrs, androidx.preference.R.styleable.Preference, defStyleAttr, 0
        ).use {
            val layoutResource = it.getResourceId(
                androidx.preference.R.styleable.Preference_android_layout,
                0
            )
            require(layoutResource != 0) { "LayoutPreference requires a layout to be defined" }
            // Need to create view now so that findViewById can be called immediately.
            val view = LayoutInflater.from(context)
                .inflate(layoutResource, null, false)
            setView(view)
            isRelativeLinkView = view is RelativeLinksCard
        }

    }

    private fun setView(view: View) {
        layoutResource = R.layout.oui_preference_layout_frame
        mRootView = view
        shouldDisableView = false
    }

    fun getView(): View? = mRootView

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        if (isRelativeLinkView) {
            view.itemView.apply {
                setOnClickListener(null)
                isFocusable = false
                isClickable = false
            }
        } else {
            view.itemView.apply {
                setOnClickListener(mClickListener)
                val isSelectable = isSelectable
                isFocusable = isSelectable
                isClickable = isSelectable
            }
            view.isDividerAllowedAbove = mAllowDividerAbove
            view.isDividerAllowedBelow = mAllowDividerBelow
        }
        val layout = view.itemView as FrameLayout
        layout.removeAllViews()
        val parent = mRootView?.parent as? ViewGroup
        parent?.removeView(mRootView)
        layout.addView(mRootView)
    }

    fun <T : View?> findViewById(id: Int): T {
        return mRootView!!.findViewById(id)
    }

    fun setAllowDividerAbove(allowed: Boolean) {
        mAllowDividerAbove = allowed
    }

    fun setAllowDividerBelow(allowed: Boolean) {
        mAllowDividerBelow = allowed
    }
}