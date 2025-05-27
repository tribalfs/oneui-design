@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
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
import dev.oneuiproject.oneui.dialog.ProgressDialog.Companion.STYLE_HORIZONTAL
import dev.oneuiproject.oneui.dialog.ProgressDialog.Companion.STYLE_SPINNER
import java.lang.ref.WeakReference
import java.text.NumberFormat
import androidx.appcompat.R as appcompatR

/**
 * A dialog showing a progress indicator and an optional text message or view.
 * Only a text message or a view can be used at the same time.
 *
 * The dialog can be made cancelable on back key press.
 *
 * The progress range is 0..10000.
 *
 * This version also supports the OneUI Design progress dialogs.
 *
 * @param context The context the dialog is to run in.
 * @param theme (Optional) Theme resource to use
 *
 * @see AlertDialog
 */
class ProgressDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes theme: Int = 0
) : AlertDialog(context, theme) {

    private var contentView: View? = null
    private var progressBar: SeslProgressBar? = null
    private var messageView: TextView? = null
    private var progressNumberView: TextView? = null
    private var progressPercentView: TextView? = null

    private var progressStyle = STYLE_SPINNER
    private var progressNumberFormat = "%1d/%1d"
    private var progressPercentFormat = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }
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
            contentView?.updatePadding(top = context.resources.getDimensionPixelSize(
                appcompatR.dimen.sesl_dialog_title_padding_bottom))
        }
    }


    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = context
        val inflater = LayoutInflater.from(context)
        when (progressStyle) {
            STYLE_HORIZONTAL -> {

                /* Use a separate handler to update the text views as they
                 * must be updated on the same thread that created them.
                 */
                viewUpdateHandler = PgHandler(this)
                contentView = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_horizontal, null).apply {
                    progressBar = findViewById<View>(R.id.progress) as SeslProgressBar
                    progressNumberView = findViewById<View>(R.id.progress_number) as TextView
                    progressPercentView = findViewById<View>(R.id.progress_percent) as TextView
                    messageView = findViewById<View>(R.id.message) as TextView
                }
                setView(contentView)
            }
            STYLE_CIRCLE -> {
                setTitle(null)
                window!!.setBackgroundDrawableResource(android.R.color.transparent)
                val view = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_circle, null)
                progressBar = view.findViewById<View>(R.id.progress) as SeslProgressBar
                messageView = view.findViewById<View>(R.id.message) as TextView
                setView(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_spinner, null)
                progressBar = view.findViewById<View>(R.id.progress) as SeslProgressBar
                messageView = view.findViewById<View>(R.id.message) as TextView
                setView(view)
            }
        }

        if (maxProgress > 0) {
            max = maxProgress
        }

        if (progressVal > 0) {
            progress = progressVal
        }

        if (secondaryProgressVal > 0) {
            secondaryProgress = secondaryProgressVal
        }

        if (incrementBy > 0) {
            incrementProgressBy(incrementBy)
        }

        if (incrementSecondaryBy > 0) {
            incrementSecondaryProgressBy(incrementSecondaryBy)
        }

        if (progressDrawable != null) {
            setProgressDrawable(progressDrawable)
        }

        if (indeterminateDrawable != null) {
            setIndeterminateDrawable(indeterminateDrawable)
        }

        if (message != null) {
            setMessage(message!!)
        }
        isIndeterminate = indeterminate
        onProgressChanged()
        super.onCreate(savedInstanceState)

        if (progressStyle == STYLE_CIRCLE) {
            @SuppressLint("PrivateResource")
            val size = context.resources.getDimensionPixelSize(
                appcompatR.dimen.sesl_progress_dialog_circle_size)
            window!!.apply {
                setGravity(Gravity.CENTER)
                setLayout(size, size)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        hasStarted = true
    }

    override fun onStop() {
        super.onStop()
        hasStarted = false
    }

    /**
     * The current progress, a value between 0 and [max]
     */
    var progress: Int
        get() = if (progressBar != null) {
            progressBar!!.progress
        } else progressVal

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
        get() = if (progressBar != null) {
            progressBar!!.secondaryProgress
        } else secondaryProgressVal

        set(secondaryProgress) {
            if (progressBar != null) {
                progressBar!!.secondaryProgress = secondaryProgress
                onProgressChanged()
            } else {
                secondaryProgressVal = secondaryProgress
            }
        }

    /**
     * The maximum allowed progress value. The default value is 100.
     */
    var max: Int
        get() = if (progressBar != null) {
            progressBar!!.max
        } else maxProgress

        set(max) {
            if (progressBar != null) {
                progressBar!!.max = max
                onProgressChanged()
            } else {
                maxProgress = max
            }
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
     */
    fun setProgressDrawable(d: Drawable?) {
        if (progressBar != null) {
            progressBar!!.progressDrawable = d
        } else {
            progressDrawable = d
        }
    }

    /**
     * Sets the drawable to be used to display the indeterminate progress value.
     *
     * @param d the drawable to be used
     *
     * @see SeslProgressBar.setProgressDrawable
     * @see .setIndeterminate
     */
    fun setIndeterminateDrawable(d: Drawable?) {
        if (progressBar != null) {
            progressBar!!.indeterminateDrawable = d
        } else {
            indeterminateDrawable = d
        }
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
        get() = if (progressBar != null) {
            progressBar!!.isIndeterminate
        } else indeterminate

        set(indeterminate) {
            if (progressBar != null) {
                progressBar!!.isIndeterminate = indeterminate
            } else {
                this@ProgressDialog.indeterminate = indeterminate
            }
        }

    /**
     * Sets the title text for this dialog's window.
     *
     * @param title the title text for the dialog's window
     */
    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        if (contentView != null) {
            @SuppressLint("PrivateResource")
            val paddingTop = context.resources
                .getDimensionPixelSize(
                    if (title.toString().isEmpty())  {
                        appcompatR.dimen.sesl_dialog_padding_vertical
                    } else appcompatR.dimen.sesl_dialog_title_padding_bottom
                )
            contentView!!.updatePadding(top = paddingTop)
        }
    }

    /**
     * Sets the message to be displayed in the progress dialog.
     *
     * @param message the message to be displayed
     */
    override fun setMessage(message: CharSequence) {
        if (progressBar != null) {
            when (progressStyle) {
                STYLE_HORIZONTAL -> {
                    messageView?.apply {
                        text = message
                        isVisible = message !== ""
                    } ?: run{
                        super.setMessage(message)
                    }
                }
                STYLE_CIRCLE -> {
                    messageView?.apply {
                        text = message
                        isVisible = message !== ""
                    }
                }
                else -> {
                    messageView?.text = message
                }
            }
        } else {
            this@ProgressDialog.message = message
        }
    }

    /**
     * Sets the style of this ProgressDialog, either [STYLE_SPINNER] or
     * [STYLE_HORIZONTAL]. The default is [STYLE_SPINNER].
     *
     *
     * **Note:** A ProgressDialog with style [STYLE_SPINNER]
     * is always indeterminate and will ignore the [ indeterminate][.setIndeterminate] setting.
     *
     * @param style the style of this ProgressDialog, either [STYLE_SPINNER] or
     * [STYLE_HORIZONTAL]
     */
    fun setProgressStyle(style: Int) {
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
        if (progressStyle == STYLE_HORIZONTAL) {
            if (viewUpdateHandler != null && !viewUpdateHandler!!.hasMessages(0)) {
                viewUpdateHandler!!.sendEmptyMessage(0)
            }
        }
    }

    companion object {
        const val STYLE_SPINNER = 0
        const val STYLE_HORIZONTAL = 1
        const val STYLE_CIRCLE = 2
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

    private class PgHandler(progressDialog: ProgressDialog) : Handler(Looper.getMainLooper()) {
        private val outerClassRef: WeakReference<ProgressDialog> = WeakReference(progressDialog)

        override fun handleMessage(msg: Message) {
            val outerClass = outerClassRef.get()
            if (outerClass != null) {
                super.handleMessage(msg)

                with(outerClass) {
                    /* Update the number and percent */
                    val progress = progressBar!!.progress
                    val max = progressBar!!.max

                    progressNumberView!!.text =  if (progressNumberView!!.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                        String.format(progressNumberFormat, max, progress)
                    } else {
                        String.format(progressNumberFormat, progress, max)
                    }

                    val percent = progress / max.toDouble()
                    progressPercentView!!.text = SpannableString(progressPercentFormat.format(percent)).apply {
                        setSpan(
                            StyleSpan(Typeface.NORMAL),
                            0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                }
            }
        }
    }

}