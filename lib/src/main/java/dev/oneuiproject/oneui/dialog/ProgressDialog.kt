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
import java.lang.ref.WeakReference
import java.text.NumberFormat
import androidx.appcompat.R as appcompatR

class ProgressDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes theme: Int = 0
) : AlertDialog(context, theme) {

    private var mContentView: View? = null
    private var mProgress: SeslProgressBar? = null
    private var mMessageView: TextView? = null
    private var mProgressStyle = STYLE_SPINNER
    private var mProgressNumber: TextView? = null
    private var mProgressNumberFormat: String = "%1d/%1d"
    private var mProgressPercent: TextView? = null
    private var mProgressPercentFormat: NumberFormat = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }
    private var mMax = 0
    private var mProgressVal = 0
    private var mSecondaryProgressVal = 0
    private var mIncrementBy = 0
    private var mIncrementSecondaryBy = 0
    private var mProgressDrawable: Drawable? = null
    private var mIndeterminateDrawable: Drawable? = null
    private var mMessage: CharSequence? = null
    private var mIndeterminate = false
    private var mHasStarted = false
    private var mViewUpdateHandler: Handler? = null


    override fun show() {
        super.show()
        val dialogTitle = findViewById<DialogTitle>(appcompatR.id.alertTitle)
        if (dialogTitle?.text?.isNotEmpty() == true) {
            mContentView?.updatePadding(top = context.resources.getDimensionPixelSize(
                appcompatR.dimen.sesl_dialog_title_padding_bottom))
        }
    }


    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = context
        val inflater = LayoutInflater.from(context)
        when (mProgressStyle) {
            STYLE_HORIZONTAL -> {

                /* Use a separate handler to update the text views as they
                 * must be updated on the same thread that created them.
                 */
                mViewUpdateHandler = PgHandler(this)
                mContentView = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_horizontal, null).apply {
                    mProgress = findViewById<View>(R.id.progress) as SeslProgressBar
                    mProgressNumber = findViewById<View>(R.id.progress_number) as TextView
                    mProgressPercent = findViewById<View>(R.id.progress_percent) as TextView
                    mMessageView = findViewById<View>(R.id.message) as TextView
                }
                setView(mContentView)
            }
            STYLE_CIRCLE -> {
                setTitle(null)
                window!!.setBackgroundDrawableResource(android.R.color.transparent)
                val view = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_circle, null)
                mProgress = view.findViewById<View>(R.id.progress) as SeslProgressBar
                mMessageView = view.findViewById<View>(R.id.message) as TextView
                setView(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.oui_des_dialog_progress_dialog_spinner, null)
                mProgress = view.findViewById<View>(R.id.progress) as SeslProgressBar
                mMessageView = view.findViewById<View>(R.id.message) as TextView
                setView(view)
            }
        }

        if (mMax > 0) {
            max = mMax
        }

        if (mProgressVal > 0) {
            progress = mProgressVal
        }

        if (mSecondaryProgressVal > 0) {
            secondaryProgress = mSecondaryProgressVal
        }

        if (mIncrementBy > 0) {
            incrementProgressBy(mIncrementBy)
        }

        if (mIncrementSecondaryBy > 0) {
            incrementSecondaryProgressBy(mIncrementSecondaryBy)
        }

        if (mProgressDrawable != null) {
            setProgressDrawable(mProgressDrawable)
        }

        if (mIndeterminateDrawable != null) {
            setIndeterminateDrawable(mIndeterminateDrawable)
        }

        if (mMessage != null) {
            setMessage(mMessage!!)
        }
        isIndeterminate = mIndeterminate
        onProgressChanged()
        super.onCreate(savedInstanceState)

        if (mProgressStyle == STYLE_CIRCLE) {
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
        mHasStarted = true
    }

    override fun onStop() {
        super.onStop()
        mHasStarted = false
    }

    var progress: Int
        /**
         * Gets the current progress.
         *
         * @return the current progress, a value between 0 and [max]
         */
        get() = if (mProgress != null) {
            mProgress!!.progress
        } else mProgressVal

        /**
         * Sets the current progress.
         *
         * @param value the current progress, a value between 0 and [max]
         *
         * @see SeslProgressBar.setProgress
         */
        set(value) {
            if (mHasStarted) {
                mProgress!!.progress = value
                onProgressChanged()
            } else {
                mProgressVal = value
            }
        }

    var secondaryProgress: Int
        /**
         * Gets the current secondary progress.
         *
         * @return the current secondary progress, a value between 0 and [max]
         */
        get() = if (mProgress != null) {
            mProgress!!.secondaryProgress
        } else mSecondaryProgressVal

        /**
         * Sets the secondary progress.
         *
         * @param secondaryProgress the current secondary progress, a value between 0 and [max]
         *
         * @see SeslProgressBar.setSecondaryProgress
         */
        set(secondaryProgress) {
            if (mProgress != null) {
                mProgress!!.secondaryProgress = secondaryProgress
                onProgressChanged()
            } else {
                mSecondaryProgressVal = secondaryProgress
            }
        }

    var max: Int
        /**
         * Gets the maximum allowed progress value. The default value is 100.
         *
         * @return the maximum value
         */
        get() = if (mProgress != null) {
            mProgress!!.max
        } else mMax

        /**
         * Sets the maximum allowed progress value.
         */
        set(max) {
            if (mProgress != null) {
                mProgress!!.max = max
                onProgressChanged()
            } else {
                mMax = max
            }
        }

    /**
     * Increments the current progress value.
     *
     * @param diff the amount by which the current progress will be incremented,
     * up to [max]
     */
    fun incrementProgressBy(diff: Int) {
        if (mProgress != null) {
            mProgress!!.incrementProgressBy(diff)
            onProgressChanged()
        } else {
            mIncrementBy += diff
        }
    }

    /**
     * Increments the current secondary progress value.
     *
     * @param diff the amount by which the current secondary progress will be incremented,
     * up to [max]
     */
    fun incrementSecondaryProgressBy(diff: Int) {
        if (mProgress != null) {
            mProgress!!.incrementSecondaryProgressBy(diff)
            onProgressChanged()
        } else {
            mIncrementSecondaryBy += diff
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
        if (mProgress != null) {
            mProgress!!.progressDrawable = d
        } else {
            mProgressDrawable = d
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
        if (mProgress != null) {
            mProgress!!.indeterminateDrawable = d
        } else {
            mIndeterminateDrawable = d
        }
    }

    var isIndeterminate: Boolean
        /**
         * Whether this ProgressDialog is in indeterminate mode.
         *
         * @return true if the dialog is in indeterminate mode, false otherwise
         */
        get() = if (mProgress != null) {
            mProgress!!.isIndeterminate
        } else mIndeterminate

        /**
         * Change the indeterminate mode for this ProgressDialog. In indeterminate
         * mode, the progress is ignored and the dialog shows an infinite
         * animation instead.
         *
         *
         * **Note:** A ProgressDialog with style [STYLE_SPINNER]
         * is always indeterminate and will ignore this setting.
         *
         * @param indeterminate true to enable indeterminate mode, false otherwise
         *
         * @see [setProgressStyle]
         */
        set(indeterminate) {
            if (mProgress != null) {
                mProgress!!.isIndeterminate = indeterminate
            } else {
                mIndeterminate = indeterminate
            }
        }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        if (mContentView != null) {
            val paddingTop = context.resources
                .getDimensionPixelSize(
                    if (title.toString().isEmpty())  {
                        appcompatR.dimen.sesl_dialog_padding_vertical
                    } else appcompatR.dimen.sesl_dialog_title_padding_bottom
                )
            mContentView!!.updatePadding(top = paddingTop)
        }
    }

    override fun setMessage(message: CharSequence) {
        if (mProgress != null) {
            when (mProgressStyle) {
                STYLE_HORIZONTAL -> {
                    mMessageView?.apply {
                        text = message
                        isVisible = message !== ""
                    } ?: run{
                        super.setMessage(message)
                    }
                }
                STYLE_CIRCLE -> {
                    mMessageView!!.apply {
                        text = message
                        isVisible = message !== ""
                    }
                }
                else -> {
                    mMessageView!!.text = message
                }
            }
        } else {
            mMessage = message
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
        mProgressStyle = style
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
        mProgressNumberFormat = format
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
        mProgressPercentFormat = format
        onProgressChanged()
    }

    private fun onProgressChanged() {
        if (mProgressStyle == STYLE_HORIZONTAL) {
            if (mViewUpdateHandler != null && !mViewUpdateHandler!!.hasMessages(0)) {
                mViewUpdateHandler!!.sendEmptyMessage(0)
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
                    val progress = mProgress!!.progress
                    val max = mProgress!!.max

                    mProgressNumber!!.text =  if (mProgressNumber!!.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                        String.format(mProgressNumberFormat, max, progress)
                    } else {
                        String.format(mProgressNumberFormat, progress, max)
                    }

                    val percent = progress / max.toDouble()
                    mProgressPercent!!.text = SpannableString(mProgressPercentFormat.format(percent)).apply {
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