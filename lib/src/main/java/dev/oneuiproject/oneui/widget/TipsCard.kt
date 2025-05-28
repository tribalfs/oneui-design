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

/**
 * A custom view that displays tips or suggestions in a card format.
 *
 * This view can display a title, a summary, a cancel button, and a list of action buttons.
 *
 * ## Example usage:
 * In your xml:
 * ```xml
 * <dev.oneuiproject.oneui.widget.TipsCard
 *     android:id="@+id/tips_card"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * In your Kotlin code:
 * ```kotlin
 * val tipsCard = findViewById<TipsCard>(R.id.tips_card).apply{
 *     setTitle("Tip Title")
 *     setSummary("This is a helpful tip.")
 *     setOnCancelClickListener {
 *       // Handle cancel button click
 *     }
 *     addButton("Action 1") {
 *       // Handle action 1 click
 *     }
 *    addButton("Action 2") {
 *       // Handle action 2 click
 *     }
 * }
 * ```
 */
class TipsCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var cancelButton: ImageView
    private var bottomBar: LinearLayout
    private var titleView: TextView
    private var summaryView: TextView
    private var titleContainer: RelativeLayout

    init {
        setOrientation(VERTICAL)
        setPadding(0, 18.dpToPx(resources), 0, 0)
        LayoutInflater.from(context).inflate(R.layout.oui_des_widget_tips_card, this, true)
        cancelButton = findViewById<ImageView>(R.id.tips_cancel_button)
        bottomBar = findViewById(R.id.tips_bottom_bar)
        titleContainer = findViewById(R.id.tips_title_container)
        titleView = findViewById(android.R.id.title)
        summaryView = findViewById(android.R.id.summary)
    }

    /**
     * Sets the title of the TipsCard.
     *
     * @param title The title to be displayed. If null or empty, the title container will be hidden.
     */
    fun setTitle(title: CharSequence?){
        titleView.text = title
        titleContainer.isVisible = title?.isNotEmpty() == true
    }

    /**
     * Sets the summary text for the TipsCard.
     *
     * @param summary The CharSequence to display as the summary.
     *                If null or empty, the summary view will be empty.
     */
    fun setSummary(summary: CharSequence?) {
        summaryView.text = summary
    }

    /**
     * Sets the click listener for the cancel button.
     *
     * The cancel button is only visible if a listener is set.
     *
     * @param listener The click listener to set, or null to remove the listener and hide the button.
     */
    fun setOnCancelClickListener(listener: OnClickListener?) {
        cancelButton.apply {
            isVisible = listener != null
            setOnClickListener(listener)
        }
    }


    /**
     * Adds a button with the given text and click listener to the bottom bar of the card.
     *
     * @param text The text to display on the button.
     * @param listener The click listener for the button.
     * @return The [TextView] that was added as a button.
     */
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