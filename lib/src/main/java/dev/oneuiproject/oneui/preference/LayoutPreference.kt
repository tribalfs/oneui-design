@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.widget.RelativeLinksCard

class LayoutPreference : Preference {
    private var rootView: View? = null
    private var allowDividerAbove = false
    private var allowDividerBelow = false
    private var isRelativeLinkView = false

    @SuppressLint("RestrictedApi")
    private val clickListener = View.OnClickListener { this.performClick(it) }

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
        context.withStyledAttributes(attrs, androidx.preference.R.styleable.Preference) {
            allowDividerAbove = getBoolean(
                androidx.preference.R.styleable.Preference_allowDividerAbove,
                false
            )
            allowDividerBelow = getBoolean(
                androidx.preference.R.styleable.Preference_allowDividerBelow,
                false
            )
        }

        context.withStyledAttributes(attrs, androidx.preference.R.styleable.Preference, defStyleAttr, 0) {
            val layoutResource = getResourceId(
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
        layoutResource = R.layout.oui_des_preference_layout_frame
        rootView = view
        shouldDisableView = false
    }

    fun getView(): View? = rootView

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        if (isRelativeLinkView) {
            view.itemView.apply {
                setOnClickListener(null)
                isFocusable = false
                isClickable = false
            }
        } else {
            view.itemView.apply {
                setOnClickListener(clickListener)
                val isSelectable = isSelectable
                isFocusable = isSelectable
                isClickable = isSelectable
            }
            view.isDividerAllowedAbove = allowDividerAbove
            view.isDividerAllowedBelow = allowDividerBelow
        }
        val layout = view.itemView as FrameLayout
        layout.removeAllViews()
        val parent = rootView?.parent as? ViewGroup
        parent?.removeView(rootView)
        layout.addView(rootView)
    }

    fun <T : View?> findViewById(id: Int): T {
        return rootView!!.findViewById(id)
    }

    fun setAllowDividerAbove(allowed: Boolean) {
        allowDividerAbove = allowed
    }

    fun setAllowDividerBelow(allowed: Boolean) {
        allowDividerBelow = allowed
    }
}