@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.collection.MutableScatterMap
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.content.withStyledAttributes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.preference.internal.HorizontalRadioViewContainer
import dev.oneuiproject.oneui.utils.getRegularFont
import dev.oneuiproject.oneui.utils.getSemiBoldFont
import kotlin.math.roundToInt

/**
 * A Preference that allows the user to choose an option from a list of up to 3 items
 * displayed horizontally. Each item can optionally have an icon.
 *
 * This preference will store a string into the SharedPreferences.
 *
 * @attr ref R.styleable#HorizontalRadioPreference_viewType
 * @attr ref R.styleable#HorizontalRadioPreference_entries
 * @attr ref R.styleable#HorizontalRadioPreference_entryValues
 * @attr ref R.styleable#HorizontalRadioPreference_entriesImage
 * @attr ref R.styleable#HorizontalRadioPreference_entriesSubtitle
 */
class HorizontalRadioPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    private lateinit var entriesImage: IntArray
    private lateinit var entries: Array<CharSequence>
    private var entriesSubTitle: Array<CharSequence>? = null
    private lateinit var entryValues: Array<CharSequence>
    private var titleSize: Int = -1

    private var type: Int = IMAGE
    private var isDividerEnabled = false
    private var isColorFilterEnabled = false
    private var isTouchEffectEnabled = true
    private val paddingStartEnd: Int
    private val paddingTop: Int
    private val paddingBottom: Int
    private var containerLayout: HorizontalRadioViewContainer? = null
    private val isItemEnabledMap = MutableScatterMap<CharSequence, Boolean>()
    private val isItemHiddenMap = MutableScatterMap<CharSequence, Boolean>()

    private val selectedColor: Int = context.getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimaryDark)!!.data
    private val unselectedColor: Int = ContextCompat.getColor(context, R.color.oui_des_horizontalradiopref_text_unselected_color)

    init {
        context.withStyledAttributes(attrs, R.styleable.HorizontalRadioPreference) {
            type = getInt(R.styleable.HorizontalRadioPreference_viewType, IMAGE)
            entries = getTextArray(R.styleable.HorizontalRadioPreference_entries)
            entryValues = getTextArray(R.styleable.HorizontalRadioPreference_entryValues)

            when (type) {
                IMAGE -> {
                    val entriesImageResId = getResourceId(R.styleable.HorizontalRadioPreference_entriesImage, 0)
                    if (entriesImageResId != 0) {
                        context.resources.obtainTypedArray(entriesImageResId).use { ta ->
                            entriesImage = IntArray(ta.length())
                            for (i in 0 until ta.length()) {
                                entriesImage[i] = ta.getResourceId(i, 0)
                            }
                        }
                    }
                }
                NO_IMAGE -> {
                    val entriesSubtitle = getResourceId(R.styleable.HorizontalRadioPreference_entriesSubtitle, 0)
                    if (entriesSubtitle != 0) {
                        entriesSubTitle = getTextArray(R.styleable.HorizontalRadioPreference_entriesSubtitle)
                    }
                }
            }
        }

        isSelectable = false

        with(getContext().resources) {
            paddingStartEnd = getDimension(R.dimen.oui_des_horizontalradiopref_padding_start_end).toInt()
            paddingTop = getDimension(R.dimen.oui_des_horizontalradiopref_padding_top).toInt()
            paddingBottom = getDimension(R.dimen.oui_des_horizontalradiopref_padding_bottom).toInt()
            layoutResource = R.layout.oui_des_preference_horizontal_radio_layout
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val itemSize = entries.size

        require(itemSize <= 3) { "Out of index" }

        containerLayout = holder.itemView.findViewById(R.id.horizontal_radio_layout)

        for ((mIndex, mValue) in entryValues.withIndex()) {

            val itemLayout = containerLayout!!.findViewById<ViewGroup>(getResId(mIndex))
            when (type) {
                IMAGE -> {
                    itemLayout.findViewById<ImageView>(R.id.icon).setImageResource(entriesImage[mIndex])
                    itemLayout.findViewById<TextView>(R.id.icon_title).apply {
                        text = entries[mIndex]
                    }
                    itemLayout.findViewById<View>(R.id.image_frame).visibility = View.VISIBLE
                }
                NO_IMAGE -> {
                    itemLayout.findViewById<TextView>(R.id.title).apply {
                        if (titleSize != -1){
                            textSize = titleSize.toFloat()
                        }
                        text = entries[mIndex]
                    }
                    val subTitleView = itemLayout.findViewById<TextView>(R.id.sub_title)
                    if (entriesSubTitle == null){
                        subTitleView.visibility = View.GONE
                    }else{
                        subTitleView.text = entriesSubTitle!![mIndex]
                    }
                    itemLayout.findViewById<View>(R.id.text_frame).visibility = View.VISIBLE
                }
            }

            itemLayout.visibility = View.VISIBLE

            if (!isTouchEffectEnabled) {
                itemLayout.background = null
            }

            itemLayout.setOnTouchListener { v: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isTouchEffectEnabled) {
                            v.alpha = 0.6f
                        }
                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isTouchEffectEnabled) {
                            v.alpha = 1.0f
                        }
                        v.callOnClick()
                        return@setOnTouchListener false
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (!isTouchEffectEnabled) {
                            v.alpha = 1.0f
                        }
                        return@setOnTouchListener false
                    }
                }
                false
            }

            itemLayout.setOnKeyListener { v: View, keyCode: Int, event: KeyEvent ->
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (!isTouchEffectEnabled) {
                                v.alpha = 0.6f
                            }
                            return@setOnKeyListener true
                        }

                        KeyEvent.ACTION_UP -> {
                            if (!isTouchEffectEnabled) {
                                v.alpha = 1.0f
                            }
                            v.playSoundEffect(SoundEffectConstants.CLICK)
                            v.callOnClick()
                            return@setOnKeyListener false
                        }
                    }
                }
                false
            }

            itemLayout.setOnClickListener {
                if (callChangeListener(mValue)) {
                    this.value = mValue as String
                }
            }

            var itemPadding = paddingStartEnd

            if (!isDividerEnabled) {
                itemPadding = (itemPadding / 2f).roundToInt()
            }

            when (mIndex) {
                0 -> itemLayout.setPadding(paddingStartEnd, paddingTop, itemPadding, paddingBottom)
                itemSize - 1 -> itemLayout.setPadding(itemPadding, paddingTop, paddingStartEnd, paddingBottom)
                else -> itemLayout.setPadding(itemPadding, paddingTop, itemPadding, paddingBottom)
            }
        }
        invalidate()
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index)!!
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            return superState
        }
        return SavedState(superState).apply {
            value = this@HorizontalRadioPreference.value
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value =  getPersistedString(defaultValue?.toString())
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        value = state.value
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        invalidate()
    }

    fun setViewType(viewType: Int) {
        type = viewType
    }

    val entry: CharSequence?
        get() {
            val index = valueIndex
            return if (index >= 0) entries[index] else null
        }

    var value: String? = null
        set(value) {
            if (value != field){
                field = value
                persistString(value)
                notifyChanged()
                invalidate()
            }
        }

    private val valueIndex: Int get() = findIndexOfValue(value)

    fun setEntryEnabled(entry: String, enabled: Boolean) {
        isItemEnabledMap[entry] = enabled
        invalidate()
    }

    fun setEntryHidden(entry: String, enabled: Boolean) {
        isItemHiddenMap[entry] = enabled
        invalidate()
    }

    fun setDividerEnabled(enabled: Boolean) {
        isDividerEnabled = enabled
    }

    fun setColorFilterEnabled(enabled: Boolean) {
        isColorFilterEnabled = enabled
    }

    fun setTouchEffectEnabled(enabled: Boolean) {
        isTouchEffectEnabled = enabled
    }

    fun findIndexOfValue(value: String?): Int {
        if (value != null) {
            return entryValues.apply { reverse() }.indexOf(value)
        }
        return -1
    }

    private fun invalidate() {
        if (containerLayout == null) return
        for ((mIndex, mValue) in entryValues.withIndex()) {
            val itemLayout = containerLayout!!.findViewById<ViewGroup>(getResId(mIndex))
            val selected = mValue == this@HorizontalRadioPreference.value
            when (type) {
                IMAGE -> {
                    with(itemLayout.findViewById<ImageView>(R.id.icon)) {
                        if (isColorFilterEnabled ) {
                            setColorFilter(if (selected) { selectedColor } else { unselectedColor })
                        }
                    }
                    with(itemLayout.findViewById<TextView>(R.id.icon_title)){
                        isSelected = selected
                        typeface = if (selected) getSemiBoldFont() else getRegularFont()
                    }

                    itemLayout.findViewById<View>(R.id.image_frame).visibility = View.VISIBLE
                }
                NO_IMAGE -> {
                    itemLayout.findViewById<TextView>(R.id.title).apply {
                        isSelected = selected
                        typeface = if (selected) getSemiBoldFont() else getRegularFont()
                    }
                    itemLayout.findViewById<TextView>(R.id.sub_title).isSelected = selected
                    itemLayout.findViewById<View>(R.id.text_frame).visibility = View.VISIBLE
                }
            }

            itemLayout.findViewById<RadioButton>(R.id.radio_button).apply {
                isChecked = selected
                if (!isTouchEffectEnabled) jumpDrawablesToCurrentState()
            }

            with( itemLayout) {
                isItemHiddenMap[mValue]?.let { visibility = if (it) View.GONE else View.VISIBLE }
                isEnabled = (isItemEnabledMap[mValue] != false && isEnabled)
                alpha = if (isEnabled) 1.0f else .6f
            }
            containerLayout!!.setDividerEnabled(isDividerEnabled)
        }
    }

    private fun getResId(index: Int): Int {
        when (index) {
            0 -> return R.id.item1
            1 -> return R.id.item2
            2 -> return R.id.item3
        }
        throw IllegalArgumentException("Out of index")
    }

    private class SavedState : BaseSavedState {
        var value: String? = null

        constructor(source: Parcel) : super(source) {
            value = source.readString()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

        companion object CREATOR: Parcelable.Creator<SavedState?> {
            override fun createFromParcel(p0: Parcel): SavedState {
                return SavedState(p0)
            }

            override fun newArray(p0: Int): Array<SavedState?> {
                return arrayOfNulls(p0)
            }
        }
    }

    private companion object {
        private const val IMAGE = 0
        private const val NO_IMAGE = 1
    }
}