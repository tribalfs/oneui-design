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
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.view.isInvisible


/**
 * Show a progress bar as an overlay or as a replacement to this button.
 * @return the [SeslProgressBar] created and shown.
 */
@JvmOverloads
inline fun <T : Button> T.showProgress(asOverlay: Boolean = false): SeslProgressBar {
    val parentView = parent as ViewGroup
    val btnIndex = parentView.indexOfChild(this)
    val btnWidth = width
    val btnHeight = height

    val context = context
    val buttonLp = layoutParams

    val frameLayout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(btnWidth, btnHeight)
    }

    val progressView = SeslProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
        layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
    }

    parentView.removeView(this)
    parentView.addView(frameLayout, btnIndex)
    frameLayout.layoutParams = buttonLp
    frameLayout.addView(this, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

    isInvisible = !asOverlay

    frameLayout.addView(progressView)

    return progressView
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



