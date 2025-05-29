@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.DialogTitle
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.dialog.ProgressDialog.Companion.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import androidx.appcompat.R as appcompatR

/**
 * A dialog showing a progress indicator and an optional text message or view.
 * Only a text message or a view can be used at the same time.
 *
 * @param context The context the dialog is to run in.
 * @param theme (Optional) Theme resource to use
 *
 * @see AlertDialog
 */
class ProgressDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes theme: Int = 0,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : AlertDialog(context, theme) {

    private var contentView: View? = null
    private var progressBar: SeslProgressBar? = null
    private var messageView: TextView? = null
    private var progressNumberView: TextView? = null
    private var progressPercentView: TextView? = null

    private var progressStyle: ProgressStyle = ProgressStyle.SPINNER
    private var progressNumberFormat = "%1d/%1d"
    private var progressPercentFormat =
        NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }
    private var maxProgress = 0
    private var progressVal = 0
    private var secondaryProgressVal = 0
    private var incrementBy = 0
    private var incrementSecondaryBy = 0
    private var progressDrawable: Drawable? = null
    private var indeterminateDrawable: Drawable? = null
    private var message: CharSequence? = null
    private var indeterminate = false
    private var hasStarted = false
    private var viewUpdateHandler: Handler? = null

    override fun show() {
        super.show()
        val dialogTitle = findViewById<DialogTitle>(appcompatR.id.alertTitle)
        if (dialogTitle?.text?.isNotEmpty() == true) {
            @SuppressLint("PrivateResource")
            contentView?.updatePadding(
                top = context.resources.getDimensionPixelSize(
                    appcompatR.dimen.sesl_dialog_title_padding_bottom
                )
            )
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        val inflater = LayoutInflater.from(context)
        when (progressStyle) {
            ProgressStyle.HORIZONTAL -> {
                contentView =
                    inflater.inflate(R.layout.oui_des_dialog_progress_dialog_horizontal, null).apply {
                        progressBar = findViewById<SeslProgressBar>(R.id.progress)
                        progressNumberView = findViewById<TextView>(R.id.progress_number)
                        progressPercentView = findViewById<TextView>(R.id.progress_percent)
                        messageView = findViewById<TextView>(R.id.message)
                    }
                setView(contentView)
            }

            ProgressStyle.CIRCLE -> {
                setTitle(null)
                window!!.setBackgroundDrawableResource(android.R.color.transparent)
                val view = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_circle, null)
                progressBar = view.findViewById<View>(R.id.progress) as SeslProgressBar
                messageView = view.findViewById<View>(R.id.message) as TextView
                setView(view)
            }

            ProgressStyle.SPINNER -> {
                val view = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_spinner, null)
                progressBar = view.findViewById<View>(R.id.progress) as SeslProgressBar
                messageView = view.findViewById<View>(R.id.message) as TextView
                setView(view)
            }
        }

        if (maxProgress > 0) max = maxProgress
        if (progressVal > 0) progress = progressVal
        if (secondaryProgressVal > 0) secondaryProgress = secondaryProgressVal
        if (incrementBy > 0) incrementProgressBy(incrementBy)
        if (incrementSecondaryBy > 0) incrementSecondaryProgressBy(incrementSecondaryBy)
        progressDrawable?.let { setProgressDrawable(it) }
        indeterminateDrawable?.let { setIndeterminateDrawable(it) }
        message?.let { setMessage(it) }
        isIndeterminate = indeterminate

        onProgressChanged()

        super.onCreate(savedInstanceState)

        if (progressStyle == ProgressStyle.CIRCLE) {
            @SuppressLint("PrivateResource")
            val size = context.resources.getDimensionPixelSize(
                appcompatR.dimen.sesl_progress_dialog_circle_size
            )
            window!!.apply { setGravity(Gravity.CENTER); setLayout(size, size) }
        }
    }

    override fun onStart() { super.onStart(); hasStarted = true }

    override fun onStop() { super.onStop(); hasStarted = false }

    /**
     * The current progress, a value between 0 and [max]
     */
    var progress: Int
        get() = progressBar?.progress ?: progressVal
        set(value) {
            if (hasStarted) {
                progressBar!!.progress = value
                onProgressChanged()
            } else {
                progressVal = value
            }
        }

    /**
     * The current secondary progress, a value between 0 and [max]
     */
    var secondaryProgress: Int
        get() = progressBar?.secondaryProgress ?: secondaryProgressVal
        set(secondaryProgress) {
            progressBar?.apply { this.secondaryProgress = secondaryProgress; onProgressChanged() }
                ?: run { secondaryProgressVal = secondaryProgress }
        }

    /**
     * The maximum allowed progress value. The default value is 100.
     */
    var max: Int
        get() = progressBar?.max ?: maxProgress
        set(max) {
            progressBar?.apply { this.max = max; onProgressChanged() }
                ?: run { maxProgress = max }
        }

    /**
     * Increments the current progress value.
     *
     * @param diff the amount by which the current progress will be incremented,
     * up to [max]
     */
    fun incrementProgressBy(diff: Int) {
        if (progressBar != null) {
            progressBar!!.incrementProgressBy(diff)
            onProgressChanged()
        } else {
            incrementBy += diff
        }
    }

    /**
     * Increments the current secondary progress value.
     *
     * @param diff the amount by which the current secondary progress will be incremented,
     * up to [max]
     */
    fun incrementSecondaryProgressBy(diff: Int) {
        if (progressBar != null) {
            progressBar!!.incrementSecondaryProgressBy(diff)
            onProgressChanged()
        } else {
            incrementSecondaryBy += diff
        }
    }

    /**
     * Sets the drawable to be used to display the progress value.
     *
     * @param d the drawable to be used
     *
     * @see SeslProgressBar.setProgressDrawable
     * @see isIndeterminate
     */
    fun setProgressDrawable(d: Drawable?) {
        progressBar?.apply { progressDrawable = d }
            ?: run { progressDrawable = d }
    }

    /**
     * Sets the drawable to be used to display the indeterminate progress value.
     *
     * @param d the drawable to be used
     *
     * @see SeslProgressBar.setProgressDrawable
     * @see isIndeterminate
     */
    fun setIndeterminateDrawable(d: Drawable?) {
        progressBar?.apply { indeterminateDrawable = d }
            ?: run { indeterminateDrawable = d }
    }

    /**
     * Whether this ProgressDialog is in indeterminate mode.
     * In indeterminate mode, the progress is ignored and the dialog shows an infinite
     * animation instead.
     *
     * **Note:** A ProgressDialog with style [STYLE_SPINNER]
     * is always indeterminate and will ignore this setting.
     *
     * @see [setProgressStyle]
     */
    var isIndeterminate: Boolean
        get() = progressBar?.isIndeterminate ?: indeterminate
        set(indeterminate) {
            progressBar?.apply { isIndeterminate = indeterminate }
                ?: run { this@ProgressDialog.indeterminate = indeterminate }
        }

    /**
     * Sets the title text for this dialog's window.
     *
     * @param title the title text for the dialog's window
     */
    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
            @SuppressLint("PrivateResource")
            contentView?.updatePadding(
                top = context.resources.getDimensionPixelSize(
                    if (title?.isEmpty() == true) {
                        appcompatR.dimen.sesl_dialog_padding_vertical
                    } else appcompatR.dimen.sesl_dialog_title_padding_bottom
                )
            )
    }

    /**
     * Sets the message to be displayed in the progress dialog.
     *
     * @param message the message to be displayed
     */
    override fun setMessage(message: CharSequence) {
        if (progressBar != null) {
            when (progressStyle) {
                ProgressStyle.HORIZONTAL -> {
                    messageView?.apply {
                        text = message; isVisible = message !== ""
                    } ?: run { super.setMessage(message) }
                }

                ProgressStyle.CIRCLE -> {
                    messageView?.apply {
                        text = message; isVisible = message !== ""
                    }
                }

                ProgressStyle.SPINNER -> {
                    messageView?.text = message
                }
            }
        } else {
            this@ProgressDialog.message = message
        }
    }

    /**
     * Sets the [style][ProgressStyle] of this ProgressDialog.
     * #### Important Notes:
     * - This only has effect when invoked prior to [show].
     * - A ProgressDialog with style [ProgressStyle.SPINNER]
     * is always indeterminate and will ignore the [isIndeterminate] setting.
     *
     * @param style the ProgressStyle of this ProgressDialog
     */
    fun setProgressStyle(style: ProgressStyle) {
        progressStyle = style
    }

    /**
     * Change the format of the small text showing current and maximum units
     * of progress.  The default is "%1d/%2d".
     * Should not be called during the number is progressing.
     * @param format A string passed to [String.format()][String.format];
     * use "%1d" for the current number and "%2d" for the maximum.  If null,
     * nothing will be shown.
     */
    fun setProgressNumberFormat(format: String) {
        progressNumberFormat = format
        onProgressChanged()
    }

    /**
     * Change the format of the small text showing the percentage of progress.
     * The default is
     * [NumberFormat.getPercentageInstnace().][NumberFormat.getPercentInstance]
     * Should not be called during the number is progressing.
     * @param format An instance of a [NumberFormat] to generate the
     * percentage text.  If null, nothing will be shown.
     */
    fun setProgressPercentFormat(format: NumberFormat) {
        progressPercentFormat = format
        onProgressChanged()
    }

    private fun onProgressChanged() {
        if (progressStyle == ProgressStyle.HORIZONTAL && !isIndeterminate) {
            coroutineScope.launch {
                updateProgressTextViews()
            }
        }
    }

    private fun updateProgressTextViews() {
        val currentProgressBar = progressBar ?: return
        val currentProgressNumberView = progressNumberView ?: return
        val currentProgressPercentView = progressPercentView ?: return

        val currentProgress = currentProgressBar.progress
        val currentMax = currentProgressBar.max

        val (firstFormatArg, secondFormatArg) = if (currentProgressNumberView.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            currentMax to currentProgress
        } else {
            currentProgress to currentMax
        }
        currentProgressNumberView.text =
            String.format(progressNumberFormat, firstFormatArg, secondFormatArg)

        if (currentMax > 0) {
            val percent = currentProgress / currentMax.toDouble()
            val formattedPercent = progressPercentFormat.format(percent)
            currentProgressPercentView.text = SpannableString(formattedPercent).apply {
                setSpan(
                    StyleSpan(Typeface.NORMAL),
                    0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            currentProgressPercentView.text = "N/A"
        }
    }

    enum class ProgressStyle {
        SPINNER,
        HORIZONTAL,
        CIRCLE
    }

    companion object {
        /**
         * Creates and shows a ProgressDialog.
         *
         * @param context the parent context
         * @param title the title text for the dialog's window
         * @param message the text to be displayed in the dialog
         * @param indeterminate true if the dialog should be [        indeterminate][.setIndeterminate], false otherwise
         * @param cancelable true if the dialog is [cancelable][.setCancelable],
         * false otherwise
         * @param cancelListener the [listener][.setOnCancelListener]
         * to be invoked when the dialog is canceled
         * @return the [ProgressDialog] instance
         */
        @JvmOverloads
        fun show(
            context: Context, title: CharSequence?,
            message: CharSequence, indeterminate: Boolean = false,
            cancelable: Boolean = false, cancelListener: DialogInterface.OnCancelListener? = null
        ): ProgressDialog {
            return ProgressDialog(context).apply {
                setTitle(title)
                setMessage(message)
                isIndeterminate = indeterminate
                setCancelable(cancelable)
                setOnCancelListener(cancelListener)
                show()
            }
        }
    }

}