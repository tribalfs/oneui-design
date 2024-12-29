@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslProgressBar


/**
 * Show a progress bar as an overlay or as a replacement to this button.
 * @return the [SeslProgressBar] created and shown.
 */
@JvmOverloads
inline fun <T : Button> T.showProgress(asOverlay: Boolean = false): SeslProgressBar {
    val parentView = parent as ViewGroup
    val btnIndex = parentView.indexOfChild(this)
    val lp = layoutParams as MarginLayoutParams

    parentView.removeView(this)
    val context = context
    val progressSize = context.resources.getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_dialog_button_min_height)
    val progressView = SeslProgressBar(context, null, android.R.attr.progressBarStyleSmall)

    if (asOverlay) {
        val frameLayout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        progressView.apply {
            layoutParams = FrameLayout.LayoutParams(progressSize, progressSize).apply {
                gravity = Gravity.CENTER
            }
        }
        this.isClickable = false
        frameLayout.addView(
            this.apply {
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    width = lp.width
                    height = lp.height
                    marginStart = lp.marginStart
                    marginEnd = lp.marginEnd
                    topMargin = lp.topMargin
                    bottomMargin = lp.bottomMargin
                }
            }
        )
        frameLayout.addView(progressView)
        parentView.addView(frameLayout, btnIndex)
    }else{
        progressView.apply {
            layoutParams = lp.apply {
                width = progressSize
                height = progressSize
                gravity = Gravity.CENTER
            }
        }
        parentView.addView(progressView, btnIndex)
    }
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
fun <T: Button> T.onClickWithProgress(
    asOverlay: Boolean = false,
    listener: OnClickWithProgressListener
){
    val alertDialogParent = getAlertDialogParentOrNull()

    setOnClickListener { v: View? ->
        alertDialogParent?.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            getButton(BUTTON_POSITIVE)?.isEnabled = false
            getButton(BUTTON_NEGATIVE)?.isEnabled = false
            getButton(BUTTON_NEUTRAL)?.isEnabled = false
        }
        val pb =  showProgress(asOverlay)
        listener.onClick(v as Button?, pb)
    }
}

private fun Button.getAlertDialogParentOrNull(): AlertDialog? {
    var parent = parent
    while (parent != null && parent !is AlertDialog) {
        parent = (parent as? View)?.parent
    }
    return parent as? AlertDialog
}
