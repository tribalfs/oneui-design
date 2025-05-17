@file:Suppress("unused")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx

class TipsCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var cancelButton: ImageView
    private var bottomBar: LinearLayout

    init {
        setOrientation(VERTICAL)
        setPadding(0, 18.dpToPx(resources), 0, 0)
        LayoutInflater.from(context).inflate(R.layout.oui_des_widget_tips_card, this, true)
        cancelButton = findViewById<ImageView>(R.id.tips_cancel_button)
        bottomBar = findViewById(R.id.tips_bottom_bar)
    }

    fun setTitle(title: CharSequence?){
        findViewById<TextView>(android.R.id.title).text = title
        findViewById<RelativeLayout>(R.id.tips_title_container).isVisible = title?.isNotEmpty() == true
    }

    fun setSummary(summary: CharSequence?) {
        findViewById<TextView>(android.R.id.summary).text = summary
    }

    fun setOnCancelClickListener(listener: OnClickListener?) {
        cancelButton.apply {
            isVisible = listener != null
            setOnClickListener(listener)
        }
    }


    fun addButton(text: CharSequence?, listener: OnClickListener?): TextView {
        val txtView = TextView(context, null, 0, R.style.OneUI_TipsCardTextButtonStyle).apply {
            setText(text)
            setOnClickListener(listener)
        }
        addButton(txtView)
        return txtView
    }

    @RestrictTo(Scope.LIBRARY)
    internal fun addButton(txtView: TextView){
        bottomBar.isVisible = true
        findViewById<View>(R.id.tips_empty_bottom)?.let { removeView(it) }
        bottomBar.addView(txtView)
    }
}