@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ButtonBarLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.view.isInvisible
import dev.oneuiproject.oneui.design.R


/**
 * Shows a [SeslProgressBar] as an overlay or replacement for this button.
 *
 * This function handles the UI transformation by:
 * 1. Capturing the button's current dimensions and [ViewGroup.LayoutParams].
 * 2. Wrapping the button in a [FrameLayout] that is fixed to the button's pixel dimensions.
 * 3. Adding a centered [SeslProgressBar] to the [FrameLayout].
 * 4. Stashing the original [LayoutParams] and dimensions in view tags for restoration.
 *
 * Use [hideProgress] to revert these changes and restore the button to its original state.
 *
 * @param asOverlay If true, the button remains visible and the progress bar is shown on top.
 *                  If false (default), the button is hidden ([View.GONE] and alpha = 0) and the
 *                  progress bar replaces it.
 * @return The created [SeslProgressBar] instance.
 * @throws IllegalStateException If the button does not have a parent [ViewGroup].
 */
@JvmOverloads
fun <T : Button> T.showProgress(asOverlay: Boolean = false): SeslProgressBar {
    val parentView = parent as? ViewGroup ?: throw IllegalStateException("Button must have a parent")
    val btnIndex = parentView.indexOfChild(this)
    val btnWidth = width
    val btnHeight = height
    val originalLp = layoutParams

    // Save state
    setTag(R.id.tag_button_show_progress_lp, originalLp)
    setTag(R.id.tag_button_original_width, originalLp.width)
    setTag(R.id.tag_button_original_height, originalLp.height)

    // Freeze dimensions to current measured pixels
    val frozenLp = when (originalLp) {
        is LinearLayoutCompat.LayoutParams -> LinearLayoutCompat.LayoutParams(originalLp).apply {
            width = btnWidth
            height = btnHeight
        }
        is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(originalLp).apply {
            width = btnWidth
            height = btnHeight
        }
        is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(originalLp).apply {
            width = btnWidth
            height = btnHeight
        }
        else -> ViewGroup.LayoutParams(originalLp).apply {
            width = btnWidth
            height = btnHeight
        }
    }

    val frameLayout = FrameLayout(parentView.context).apply {
        layoutParams = frozenLp
        background = null
    }

    val progressView = SeslProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
        layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        background = null
    }

    parentView.removeView(this)
    parentView.addView(frameLayout, btnIndex)

    // The button inside should fill the frozen container
    this.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    frameLayout.addView(this)

    isInvisible = !asOverlay

    frameLayout.addView(progressView)

    return progressView
}

/**
 * Hides the progress bar and restores the button to its original state.
 *
 * This function:
 * 1. Restores the button's original [LayoutParams] (including [WRAP_CONTENT] or [MATCH_PARENT] specs).
 * 2. Restores the button's visibility and alpha.
 * 3. Re-inserts the button into its original parent [ViewGroup] at its original position.
 * 4. Removes the temporary [FrameLayout] and [SeslProgressBar].
 *
 * This function has no effect if [showProgress] was not previously called on this button.
 */
fun <T : Button> T.hideProgress() {
    val originalLp = getTag(R.id.tag_button_show_progress_lp) as? ViewGroup.LayoutParams ?: return
    val originalWidth = getTag(R.id.tag_button_original_width) as? Int ?: return
    val originalHeight = getTag(R.id.tag_button_original_height) as? Int ?: return

    val frameLayout = parent as? FrameLayout ?: return
    val parentOfFrame = frameLayout.parent as? ViewGroup ?: return
    val frameIndex = parentOfFrame.indexOfChild(frameLayout)

    // Restore original dimensions and properties
    originalLp.width = originalWidth
    originalLp.height = originalHeight

    visibility = View.VISIBLE

    frameLayout.removeView(this)
    parentOfFrame.removeView(frameLayout)

    this.layoutParams = originalLp
    parentOfFrame.addView(this, frameIndex)

    // Clear state
    setTag(R.id.tag_button_show_progress_lp, null)
    setTag(R.id.tag_button_original_width, null)
    setTag(R.id.tag_button_original_height, null)
}

fun interface OnClickWithProgressListener {
    fun onClick(button: Button?, progressBar: SeslProgressBar)
}

/**
 * Show a progress bar as an overlay or as a replacement to this button
 * and invoke the provided [listener][OnClickWithProgressListener] when this button is clicked.
 *
 * When this button belongs to an [AlertDialog], the alert dialog buttons are disabled.
 *
 * @param asOverlay (optional) Set to true to show the progress bar as an overlay
 * or false to show it as a replacement to this button. Default is false.
 * @param listener The [OnClickWithProgressListener] to be invoked when this button is clicked.
 */
@JvmOverloads
fun <T: Button> T.setOnClickListenerWithProgress(
    asOverlay: Boolean = false,
    listener: OnClickWithProgressListener
){
    setOnClickListener { v: View? ->
        @Suppress("RestrictedApi")
        (parent as? ButtonBarLayout)?.apply {
            findViewById<Button?>(android.R.id.button1)?.isEnabled = false//Positive
            findViewById<Button?>(android.R.id.button2)?.isEnabled = false//Negative
            findViewById<Button?>(android.R.id.button3)?.isEnabled = false//Neutral
        }

        val pb = showProgress(asOverlay)
        listener.onClick(v as Button?, pb)
    }
}



