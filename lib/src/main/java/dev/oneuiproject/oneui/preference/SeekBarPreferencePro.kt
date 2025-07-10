@file:Suppress("MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.RangeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SeslAbsSeekBar.NO_OVERLAP
import androidx.appcompat.widget.SeslProgressBar.MODE_LEVEL_BAR
import androidx.appcompat.widget.SeslSeekBar
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.SeslSeekBarDualColors
import dev.oneuiproject.oneui.ktx.setShowTickMarks
import dev.oneuiproject.oneui.ktx.updateDualColorRange
import dev.oneuiproject.oneui.widget.SeekBarPlus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

//Reference: Font size seekbar
/**
 * Preference based [SeekBarPreference] but with more options and flexibility
 *
 * If [isAdjustable] is set to true, shows + and - buttons at both ends of the seekbar
 *
 * @see [centerBasedSeekBar]
 * @see [rightLabel]
 * @see [leftLabel]
 * @see [getSeekBarIncrement]
 * @see [isAdjustable]
 *
 */
open class SeekBarPreferencePro @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    @SuppressLint("RestrictedApi")
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context, R.attr.seekBarPreferenceProStyle,
        androidx.preference.R.attr.seekBarPreferenceStyle
    ),
    defStyleRes: Int = 0
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes),
    View.OnClickListener,
    OnLongClickListener,
    OnTouchListener {

    private val preferenceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var longPressJob: Job? = null

    private var seekbarMode: Int = MODE_LEVEL_BAR
    private var isSeamLess: Boolean = false
    private var skipOnBind = false

    /**
     * If true, this sets [SeslSeekBar] to only put tick marks at the start, middle and end
     * regardless of the min and max value.
     * This will ignore [SeslSeekBar.setMode] value and will use [MODE_LEVEL_BAR][SeslSeekBar.MODE_LEVEL_BAR] mode instead.
     */
    var centerBasedSeekBar: Boolean = false
        set(centerBased) {
            if (field != centerBased) {
                field = centerBased
                notifyChanged()
            }
        }

    var leftLabel: CharSequence? = null
        set(charSequence) {
            if (field != charSequence) {
                field = charSequence
                notifyChanged()
            }
        }
    var rightLabel: CharSequence? = null
        set(charSequence) {
            if (field != charSequence) {
                field = charSequence
                notifyChanged()
            }
        }

    var stateDescription: CharSequence? = null

    /**
     * This does not apply when [centerBasedSeekBar] is true
     */
    var showTickMarks: Boolean = false
        set(show) {
            if (field != show) {
                field = show
                notifyChanged()
            }
        }


    var progressTintList: ColorStateList? = null
        set(progressTintList) {
            if (field != progressTintList) {
                field = progressTintList
                notifyChanged()
            }
        }

    var tickMarkTintList: ColorStateList? = null
        set(tickMarkTintList) {
            if (field != tickMarkTintList) {
                field = tickMarkTintList
                notifyChanged()
            }
        }


    private var overlapPoint: Int = NO_OVERLAP
    private var seekBarValueTextView: TextView? = null
    private var units: String? = null
    private var addButton: ImageView? = null
    private var deleteButton: ImageView? = null
    private var onSeekBarPreferenceChangeListener: OnSeekBarPreferenceChangeListener? = null
    private var internalSeekBarChangeListener: OnSeekBarPreferenceChangeListener =
        object : OnSeekBarPreferenceChangeListener {
            override fun onProgressChanged(
                seekBar: SeslSeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (showSeekBarValue) updateValueLabel(progress)
                stateDescription = getFormattedValue()
                onSeekBarPreferenceChangeListener?.onProgressChanged(seekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seslSeekBar: SeslSeekBar?) {
                onSeekBarPreferenceChangeListener?.onStartTrackingTouch(seslSeekBar)
            }

            override fun onStopTrackingTouch(seslSeekBar: SeslSeekBar?) {
                onSeekBarPreferenceChangeListener?.onStopTrackingTouch(seslSeekBar)
                if (showSeekBarValue) updateValueLabel(seslSeekBar?.progress ?: 0)
            }
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.SeekBarPreferencePro) {
            centerBasedSeekBar =
                getBoolean(R.styleable.SeekBarPreferencePro_centerBasedSeekBar, false)
            leftLabel = getString(R.styleable.SeekBarPreferencePro_leftLabelName)
            overlapPoint = getInt(R.styleable.SeekBarPreferencePro_overlapPoint, NO_OVERLAP)
            rightLabel = getString(R.styleable.SeekBarPreferencePro_rightLabelName)
            isSeamLess = getBoolean(R.styleable.SeekBarPreferencePro_seamlessSeekBar, false)
            seekbarMode = getInt(R.styleable.SeekBarPreferencePro_seekBarMode, MODE_LEVEL_BAR)
            showTickMarks = getBoolean(R.styleable.SeekBarPreferencePro_showTickMark, true)
            units = getString(R.styleable.SeekBarPreferencePro_units)
        }
        super.setOnSeekBarPreferenceChangeListener(internalSeekBarChangeListener)
    }


    override fun setOnSeekBarPreferenceChangeListener(onSeekBarPreferenceChangeListener: OnSeekBarPreferenceChangeListener?) {
        this.onSeekBarPreferenceChangeListener = onSeekBarPreferenceChangeListener
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        if (skipOnBind) return
        super.onBindViewHolder(holder)
        setupSeekBarPlus(seekBar as SeekBarPlus)

        seekBarValueTextView = (holder.findViewById(R.id.seekbar_value) as? TextView)
        setupSeekBarValueLabel()

        setupLabels(holder)

        addButton = (holder.findViewById(R.id.add_button) as? ImageView)
        setupAdjustButton(addButton, true)

        deleteButton = (holder.findViewById(R.id.delete_button) as? ImageView)
        setupAdjustButton(deleteButton, false)
    }

    private fun setupSeekBarPlus(seekBarPlus: SeekBarPlus) {
        seekBarPlus.apply sbp@{
            setSeamless(isSeamLess)
            if (!centerBasedSeekBar) {
                centerBasedBar = false
                setMode(seekbarMode)
                if (overlapPoint != NO_OVERLAP) {
                    updateDualColorRange(
                        overlapPoint - this@SeekBarPreferencePro.min,
                        SeslSeekBarDualColors.Custom()
                    )
                }
                setShowTickMarks(showTickMarks)
            }

            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (Build.VERSION.SDK_INT >= 30) {
                        this@SeekBarPreferencePro.stateDescription?.let {
                            info.stateDescription = it
                        }
                        RangeInfo(
                            RangeInfo.RANGE_TYPE_INT,
                            this@SeekBarPreferencePro.min.toFloat(),
                            this@SeekBarPreferencePro.max.toFloat(),
                            (this@SeekBarPreferencePro.min + progress).toFloat()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        RangeInfo.obtain(
                            RangeInfo.RANGE_TYPE_INT,
                            this@SeekBarPreferencePro.min.toFloat(),
                            this@SeekBarPreferencePro.max.toFloat(),
                            (this@SeekBarPreferencePro.min + progress).toFloat()
                        )
                    }.let {
                        info.rangeInfo = it
                    }
                }
            }
        }
    }

    private fun setupSeekBarValueLabel() {
        seekBarValueTextView?.let { textView ->
            if (showSeekBarValue) {
                textView.isVisible = true
                updateValueLabel( value)
            } else {
                textView.isVisible = false
            }
        }
    }

    private fun setupLabels(holder: PreferenceViewHolder) {
        if (leftLabel != null || rightLabel != null) {
            with(holder) {
                (findViewById(R.id.seekbar_label_area) as? LinearLayout)?.isVisible = true
                (findViewById(R.id.left_label) as? TextView)?.text = leftLabel
                (findViewById(R.id.right_label) as? TextView)?.text = rightLabel
            }
        }
    }

    private fun setupAdjustButton(button: ImageView?, isIncrement: Boolean) {
        button?.apply {
            isVisible = this@SeekBarPreferencePro.isAdjustable
            if (this@SeekBarPreferencePro.isAdjustable) {
                isEnabled = this@SeekBarPreferencePro.isEnabled
                alpha = if (this@SeekBarPreferencePro.isEnabled) 1.0f else 0.4f
                setOnClickListener(this@SeekBarPreferencePro)
                setOnLongClickListener(this@SeekBarPreferencePro)
                @SuppressLint("ClickableViewAccessibility") // Keep if necessary for specific touch handling
                setOnTouchListener(this@SeekBarPreferencePro)
            }
        }
    }

    private fun updateValueLabel(value: Int) = seekBarValueTextView?.text = getFormattedValue()

    override fun onClick(view: View) {
        when (view.id) {
            R.id.delete_button -> onDeleteButtonClicked()
            R.id.add_button -> onAddButtonClicked()
        }
    }

    override fun onLongClick(view: View): Boolean {
        longPressJob?.cancel()
        when (view.id) {
            R.id.delete_button -> longPressJob = preferenceScope.launch {
                while (isActive) {
                    onDeleteButtonClicked(); delay(300)
                }
            }
            R.id.add_button -> longPressJob = preferenceScope.launch {
                while (isActive) {
                    onAddButtonClicked(); delay(300)
                }
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            longPressJob?.cancel()
        }
        return false
    }

    private fun updateValueIfAllowed(newValue: Int) {
        if (!callChangeListener(newValue)) return
        skipOnBind = true
        value = newValue
        seekBar?.apply {
            progress = newValue - min
            performHapticFeedback(HAPTIC_CONSTANT_CURSOR_MOVE)
        }
        skipOnBind = false
    }

    private fun onDeleteButtonClicked() {
        if (min == value) return
        val newValue = (value - seekBarIncrement).coerceAtLeast(min)
        updateValueIfAllowed(newValue)
    }

    private fun onAddButtonClicked() {
        if (max == value) return
        val newValue = (value + seekBarIncrement).coerceAtMost(max)
        updateValueIfAllowed(newValue)
    }

    private fun getFormattedValue(): String = "$value${units ?: ""}"

    override fun onDetached() {
        super.onDetached()
        preferenceScope.cancel()
    }

    private companion object {
        private const val TAG = "SeekBarPreferencePro"

        private val HAPTIC_CONSTANT_CURSOR_MOVE by lazy {
            @SuppressLint("RestrictedApi")
            SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(41)
        }
    }
}
