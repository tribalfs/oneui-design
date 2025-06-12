@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextSwitcher
import androidx.annotation.IntRange
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.InternalLayoutInfo
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type
import dev.oneuiproject.oneui.widget.internal.BasicTextView

/**
 * A widget that displays two alternately selectable buttons horizontally.
 * It provides a sliding animation when a button is selected.
 *
 * The labels and icons for the buttons can be customized. A listener can be set
 * to be notified when a button is selected.
 *
 * XML Attributes:
 * - `app:selectedIndex`: (Integer) The index of the initially selected button (0 or 1). Default is 0.
 * - `app:button1Label`: (String) The label for the first button. Default is "Button 1".
 * - `app:button2Label`: (String) The label for the second button. Default is "Button 2".
 * - `app:button1Icon`: (Reference) The drawable for the first button's icon.
 * - `app:button2Icon`: (Reference) The drawable for the second button's icon.
 *
 * @constructor Creates a FloatingActionBar.
 * @param context The Context the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a
 *        reference to a style resource that supplies default values for
 *        the view. Can be 0 to not look for defaults.
 */
class FloatingActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FloatingActionBar"
        private const val ANIMATION_DURATION = 400L
    }

    /**
     * Interface definition for a callback to be invoked when a button is selected.
     */
    fun interface OnButtonSelectedListener {
        fun onButtonSelected(@IntRange(from = 0, to = 1) index: Int)
    }

    private lateinit var textSwitcher: TextSwitcher
    private lateinit var iconSwitcher: ImageSwitcher
    private lateinit var frontLayout: LinearLayout
    private lateinit var button1: LinearLayout
    private lateinit var button2: LinearLayout

    private lateinit var button1TextView: BasicTextView
    private lateinit var button2TextView: BasicTextView

    private var buttonIcons: Array<Drawable?> = arrayOfNulls(2)
    private var onButtonSelectedListener: OnButtonSelectedListener? = null
    private val bottomOffsetListener: (Float) -> Unit by lazy(LazyThreadSafetyMode.NONE) {
        { translationY = -it }
    }
    private var layoutLocationInfo: InternalLayoutInfo? = null

    var selectedIndex: Int = 0
        private set

    // Cache the target translation values after the first layout
    private var button1TranslationX = 0f
    private var button2TranslationX = 0f
    private var refreshTranslations = true

    init {
        inflate(context, R.layout.oui_des_widget_floating_action_bar, this)
        setBackgroundResource(R.drawable.oui_des_floating_action_bar_layout_bg)
        elevation = context.resources.getDimension(R.dimen.oui_des_floating_action_bar_elevation)
        initViews()
        initAttrs(context, attrs)
        initClickListeners()

        if (isInEditMode) {
            updateSwitchersForSelectedIndex()
            doOnLayout {
                frontLayout.translationX = calculateTargetTranslation(if (selectedIndex == 0) button1 else button2)
            }
        } else {
            doOnLayout {
                if (refreshTranslations) {
                    calculateTranslations()
                    refreshTranslations = false
                }
                applyTranslation()
                updateSwitchersForSelectedIndex()
            }
        }
    }

    private fun initViews() {
        frontLayout = findViewById(R.id.front_layout)
        textSwitcher = findViewById(R.id.front_text_switcher)
        iconSwitcher = findViewById(R.id.front_icon_switcher)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        button1TextView = button1.findViewById(R.id.text1)
        button2TextView = button2.findViewById(R.id.text2)
        iconSwitcher.setFactory {
            ImageView(context).apply {
                val enabledColor = ContextCompat.getColor(context,R.color.oui_des_floating_action_bar_selected_text_color)
                val disabledColor = ColorUtils.setAlphaComponent(enabledColor, (0.4f * 255).toInt())
                imageTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_enabled),
                            intArrayOf(-android.R.attr.state_enabled)
                        ),
                intArrayOf(enabledColor, disabledColor)
                )
            }
        }
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        if (attrs == null) return
        context.withStyledAttributes(attrs, R.styleable.FloatingActionBar) {
            selectedIndex = getInteger(R.styleable.FloatingActionBar_selectedIndex, 0)
            button1TextView.text = getString(R.styleable.FloatingActionBar_button1Label) ?: ""
            button2TextView.text = getString(R.styleable.FloatingActionBar_button2Label) ?: ""
            buttonIcons[0] = getDrawable(R.styleable.FloatingActionBar_button1Icon)
            buttonIcons[1] = getDrawable(R.styleable.FloatingActionBar_button2Icon)
        }
    }

    private fun initClickListeners() {
        button1.setOnClickListener {
            setSelectedButton(0)
        }
        button2.setOnClickListener {
            setSelectedButton(1)
        }
    }

    fun setSelectedButton(index: Int) {
        if (selectedIndex == index) return

        selectedIndex = index
        animateToSelected()
        onButtonSelectedListener?.onButtonSelected(index)
    }

    private fun applyTranslation() {
        frontLayout.translationX = if (selectedIndex == 0) button1TranslationX else button2TranslationX
    }

    private fun updateSwitchersForSelectedIndex() {
        val currentButtonTextView = if (selectedIndex == 0) button1TextView else button2TextView
        textSwitcher.setText(currentButtonTextView.text)
        iconSwitcher.setImageDrawable(buttonIcons[selectedIndex])
    }

    private fun animateToSelected() {
        if (!refreshTranslations) {
            // This can happen if animateToSelected is called before the first layout pass
            // (e.g., if set programmatically very early).
            // Post the animation to ensure first layout has occurred.
            post { performSelectionAnimation() }
            return
        }
        performSelectionAnimation()
    }

    private fun performSelectionAnimation() {
        val targetTranslation = if (selectedIndex == 0) button1TranslationX else button2TranslationX
        frontLayout.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            animate().also {
                it.translationX(targetTranslation)
                it.duration = ANIMATION_DURATION
                it.interpolator = CachedInterpolatorFactory.getOrCreate(Type.PATH_0_22_0_25_0_0_1_0)
                it.withStartAction { updateSwitchersForSelectedIndex() }
                it.withEndAction { frontLayout.setLayerType(View.LAYER_TYPE_NONE, null) }
                it.start()
            }
        }
    }

    private fun calculateTranslations() {
        button1TranslationX = calculateTargetTranslation(button1)
        button2TranslationX = calculateTargetTranslation(button2)
    }

    private fun calculateTargetTranslation(targetButton: View): Float {
        if (targetButton.width == 0 || frontLayout.width == 0) {
            Log.w(TAG, "Cannot calculate translation: views not measured yet.")
            return frontLayout.translationX
        }

        val targetButtonCenterX = targetButton.x + targetButton.width / 2f
        val frontLayoutCenterX = frontLayout.x + frontLayout.width / 2f
        return targetButtonCenterX - frontLayoutCenterX + frontLayout.translationX
    }

    /**
     * Sets the labels for both buttons and updates the currently displayed label if necessary.
     *
     * @param label1 The text for the first button (index 0).
     * @param label2 The text for the second button (index 1).
     */
    inline fun setButtonLabels(label1: String, label2: String) {
        setButtonLabel(0, label1)
        setButtonLabel(1, label2)
    }

    /**
     * Sets the label for a specific button.
     *
     * @param index The index of the button (0 for the first, 1 for the second).
     *              Must be within the range [0, 1].
     * @param label The text to display on the button.
     */
    fun setButtonLabel(@IntRange(from = 0, to = 1) index: Int, label: String) {
        when (index) {
            0 -> button1TextView.text = label
            1 -> button2TextView.text = label
            else -> return
        }
        if (selectedIndex == index) {
            textSwitcher.setText(label)
        }
    }

    /**
     * Retrieves the label of a specific button.
     *
     * @param index The index of the button (0 for the first, 1 for the second).
     *              Must be within the range [0, 1].
     * @return The label text of the specified button.
     * @throws IllegalArgumentException if the index is out of bounds.
     */
    fun getButtonLabel(@IntRange(from = 0, to = 1) index: Int): String {
        return when (index) {
            0 -> button1TextView.text.toString()
            1 -> button2TextView.text.toString()
            else -> throw IllegalArgumentException("Invalid button index: $index")
        }
    }

    /**
     * Sets the icons for both buttons.
     *
     * @param icon1 The drawable to set as the icon for the first button (index 0).
     * @param icon2 The drawable to set as the icon for the second button (index 1).
     */
    inline fun setButtonIcons(icon1: Drawable?, icon2: Drawable?) {
        setButtonIcon(0, icon1)
        setButtonIcon(1, icon2)
    }

    /**
     * Sets the icon for a specific button.
     *
     * @param index The index of the button (0 or 1).
     * @param icon The drawable to set as the button's icon.
     */
    fun setButtonIcon(@IntRange(from = 0, to = 1) index: Int, icon: Drawable?) {
        buttonIcons[index] = icon
        if (selectedIndex == index) {
            iconSwitcher.setImageDrawable(icon)
        }
    }

    /**
     * Retrieves the icon for a specific button.
     *
     * @param index The index of the button (0 for the first, 1 for the second).
     *              Must be within the range [0, 1].
     * @return The [Drawable] icon of the button at the specified index, or `null` if no icon is set.
     */
    fun getButtonIcon(@IntRange(from = 0, to = 1) index: Int): Drawable? = buttonIcons[index]

    /**
     * Sets a listener to be invoked when a button is selected.
     *
     * The listener will receive the index of the selected button (0 for the first button, 1 for the second).
     *
     * @param listener The listener that will be invoked.
     */
    fun setOnButtonSelectedListener(listener: OnButtonSelectedListener?) {
        this.onButtonSelectedListener = listener
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        handler.post { updateTranslations() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) updateTranslations()
    }

    private fun updateTranslations() {
        calculateTranslations()
        applyTranslation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerTBLBottomOffsetListener(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        registerTBLBottomOffsetListener(false)
    }

    private fun registerTBLBottomOffsetListener(register: Boolean) {
        layoutLocationInfo = layoutLocationInfo ?: getLayoutLocationInfo()
        layoutLocationInfo?.takeIf { it.isInsideTBLMainContainer }?.tblParent?.apply {
            if (register) {
                addOnBottomOffsetChangedListener(bottomOffsetListener)
            } else {
                removeOnBottomOffsetChangedListener(bottomOffsetListener)
                layoutLocationInfo = null
            }
        } ?: run { layoutLocationInfo = null }
    }
}