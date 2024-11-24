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
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.preference.internal.HorizontalRadioViewContainer
import dev.oneuiproject.oneui.utils.getNormalFont
import dev.oneuiproject.oneui.utils.getBoldFont
import kotlin.math.roundToInt

class HorizontalRadioPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    private lateinit var mEntriesImage: IntArray
    private lateinit var mEntries: Array<CharSequence>
    private var mEntriesSubTitle: Array<CharSequence>? = null
    private lateinit var mEntryValues: Array<CharSequence>
    private var titleSize: Int = -1

    private var mType: Int = IMAGE
    private var mIsDividerEnabled = false
    private var mIsColorFilterEnabled = false
    private var mIsTouchEffectEnabled = true
    private val paddingStartEnd: Int
    private val paddingTop: Int
    private val paddingBottom: Int
    private var mContainerLayout: HorizontalRadioViewContainer? = null
    private val mIsItemEnabledMap = MutableScatterMap<CharSequence, Boolean>()
    private val mIsItemHiddenMap = MutableScatterMap<CharSequence, Boolean>()

    private val selectedColor: Int = context.getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimaryDark)!!.data
    private val unselectedColor: Int = ContextCompat.getColor(context, R.color.oui_horizontalradiopref_text_unselected_color)

    init {
        context.obtainStyledAttributes(attrs, R.styleable.HorizontalRadioPreference).use{
            mType = it.getInt(R.styleable.HorizontalRadioPreference_viewType, IMAGE)
            mEntries = it.getTextArray(R.styleable.HorizontalRadioPreference_entries)
            mEntryValues = it.getTextArray(R.styleable.HorizontalRadioPreference_entryValues)

            when (mType) {
                IMAGE -> {
                    val entriesImageResId = it.getResourceId(R.styleable.HorizontalRadioPreference_entriesImage, 0)
                    if (entriesImageResId != 0) {
                        context.resources.obtainTypedArray(entriesImageResId).use { ta ->
                            mEntriesImage = IntArray(ta.length())
                            for (i in 0 until ta.length()) {
                                mEntriesImage[i] = ta.getResourceId(i, 0)
                            }
                        }
                    }
                }
                NO_IMAGE -> {
                    val entriesSubtitle = it.getResourceId(R.styleable.HorizontalRadioPreference_entriesSubtitle, 0)
                    if (entriesSubtitle != 0) {
                        mEntriesSubTitle = it.getTextArray(R.styleable.HorizontalRadioPreference_entriesSubtitle)
                    }
                }
            }
        }

        isSelectable = false

        with(getContext().resources) {
            paddingStartEnd = getDimension(R.dimen.oui_horizontalradiopref_padding_start_end).toInt()
            paddingTop = getDimension(R.dimen.oui_horizontalradiopref_padding_top).toInt()
            paddingBottom = getDimension(R.dimen.oui_horizontalradiopref_padding_bottom).toInt()
            layoutResource = R.layout.oui_preference_horizontal_radio_layout
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val itemSize = mEntries.size

        require(itemSize <= 3) { "Out of index" }

        mContainerLayout = holder.itemView.findViewById(R.id.horizontal_radio_layout)

        for ((mIndex, mValue) in mEntryValues.withIndex()) {

            val itemLayout = mContainerLayout!!.findViewById<ViewGroup>(getResId(mIndex))
            when (mType) {
                IMAGE -> {
                    itemLayout.findViewById<ImageView>(R.id.icon).setImageResource(mEntriesImage[mIndex])
                    itemLayout.findViewById<TextView>(R.id.icon_title).apply {
                        text = mEntries[mIndex]
                    }
                    itemLayout.findViewById<View>(R.id.image_frame).visibility = View.VISIBLE
                }
                NO_IMAGE -> {
                    itemLayout.findViewById<TextView>(R.id.title).apply {
                        if (titleSize != -1){
                            textSize = titleSize.toFloat()
                        }
                        text = mEntries[mIndex]
                    }
                    val subTitleView = itemLayout.findViewById<TextView>(R.id.sub_title)
                    if (mEntriesSubTitle == null){
                        subTitleView.visibility = View.GONE
                    }else{
                        subTitleView.text = mEntriesSubTitle!![mIndex]
                    }
                    itemLayout.findViewById<View>(R.id.text_frame).visibility = View.VISIBLE
                }
            }

            itemLayout.visibility = View.VISIBLE

            if (!mIsTouchEffectEnabled) {
                itemLayout.background = null
            }

            itemLayout.setOnTouchListener { v: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!mIsTouchEffectEnabled) {
                            v.alpha = 0.6f
                        }
                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!mIsTouchEffectEnabled) {
                            v.alpha = 1.0f
                        }
                        v.callOnClick()
                        return@setOnTouchListener false
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (!mIsTouchEffectEnabled) {
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
                            if (!mIsTouchEffectEnabled) {
                                v.alpha = 0.6f
                            }
                            return@setOnKeyListener true
                        }

                        KeyEvent.ACTION_UP -> {
                            if (!mIsTouchEffectEnabled) {
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
                this.value = mValue as String
                callChangeListener(mValue)
            }

            var itemPadding = paddingStartEnd

            if (!mIsDividerEnabled) {
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


    @Deprecated("Deprecated in Java",
        ReplaceWith("value = if (restoreValue) getPersistedString(value) else defaultValue?.toString()")
    )
    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        value = if (restoreValue) getPersistedString(value) else defaultValue?.toString()
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
        mType = viewType
    }

    val entry: CharSequence?
        get() {
            val index = valueIndex
            return if (index >= 0) mEntries[index] else null
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
        mIsItemEnabledMap[entry] = enabled
        invalidate()
    }

    fun setEntryHidden(entry: String, enabled: Boolean) {
        mIsItemHiddenMap[entry] = enabled
        invalidate()
    }

    fun setDividerEnabled(enabled: Boolean) {
        mIsDividerEnabled = enabled
    }

    fun setColorFilterEnabled(enabled: Boolean) {
        mIsColorFilterEnabled = enabled
    }

    fun setTouchEffectEnabled(enabled: Boolean) {
        mIsTouchEffectEnabled = enabled
    }

    fun findIndexOfValue(value: String?): Int {
        if (value != null) {
            return mEntryValues.apply { reverse() }.indexOf(value)
        }
        return -1
    }

    private fun invalidate() {
        if (mContainerLayout == null) return
        for ((mIndex, mValue) in mEntryValues.withIndex()) {
            val itemLayout = mContainerLayout!!.findViewById<ViewGroup>(getResId(mIndex))
            val selected = mValue == this@HorizontalRadioPreference.value
            when (mType) {
                IMAGE -> {
                    with(itemLayout.findViewById<ImageView>(R.id.icon)) {
                        if (mIsColorFilterEnabled ) {
                            setColorFilter(if (selected) { selectedColor } else { unselectedColor })
                        }
                    }
                    with(itemLayout.findViewById<TextView>(R.id.icon_title)){
                        isSelected = selected
                        typeface = if (selected) getBoldFont() else getNormalFont()
                    }

                    itemLayout.findViewById<View>(R.id.image_frame).visibility = View.VISIBLE
                }
                NO_IMAGE -> {
                    itemLayout.findViewById<TextView>(R.id.title).apply {
                        isSelected = selected
                        typeface = if (selected) getBoldFont() else getNormalFont()
                    }
                    itemLayout.findViewById<TextView>(R.id.sub_title).isSelected = selected
                    itemLayout.findViewById<View>(R.id.text_frame).visibility = View.VISIBLE
                }
            }

            itemLayout.findViewById<RadioButton>(R.id.radio_button).apply {
                isChecked = selected
                if (!mIsTouchEffectEnabled) jumpDrawablesToCurrentState()
            }

            with( itemLayout) {
                mIsItemHiddenMap[mValue]?.let { visibility = if (it) View.GONE else View.VISIBLE }
                isEnabled = (mIsItemEnabledMap[mValue] != false && isEnabled)
                alpha = if (isEnabled) 1.0f else .6f
            }
            mContainerLayout!!.setDividerEnabled(mIsDividerEnabled)
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

    companion object {
        private const val IMAGE = 0
        private const val NO_IMAGE = 1
    }
}