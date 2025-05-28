@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.os.SystemClock
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.picker3.app.SeslColorPickerDialog
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.preference.internal.PreferenceImageView
import dev.oneuiproject.oneui.design.R


class ColorPickerPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr), Preference.OnPreferenceClickListener,
    SeslColorPickerDialog.OnColorSetListener {

    private var mColorPickerDialog: SeslColorPickerDialog? = null

    @SuppressLint("RestrictedApi")
    private var mPreview: PreferenceImageView? = null
    private var mValue = Color.BLACK
    private val mUsedColors = ArrayList<Int>(5)
    private var mLastClickTime: Long = 0
    private var mAlphaSliderEnabled = false
    private var mPersistRecentColors = true

    init {
        widgetLayoutResource = R.layout.oui_preference_color_picker_widget
        onPreferenceClickListener = this

        context.obtainStyledAttributes(attrs, R.styleable.ColorPickerPreference).use {
            mAlphaSliderEnabled =
                it.getBoolean(R.styleable.ColorPickerPreference_showAlphaSlider, false)
            mPersistRecentColors =
                it.getBoolean(R.styleable.ColorPickerPreference_persistRecentColors, true)
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        val mHexDefaultValue = a.getString(index)
        return if (mHexDefaultValue != null && mHexDefaultValue.startsWith("#")) {
            convertToColorInt(mHexDefaultValue)
        } else {
            a.getColor(index, Color.BLACK)
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        onColorSet(getPersistedInt(defaultValue as? Int ?: mValue))
        if (mPersistRecentColors) {
            loadRecentColors()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mPreview = holder.findViewById(R.id.imageview_widget) as PreferenceImageView
        setPreviewColor()
    }

    private fun setPreviewColor() {
        if (mPreview == null) return
        mPreview!!.background =
            (ContextCompat.getDrawable(context, R.drawable.oui_preference_color_picker_preview)!!
                .mutate() as GradientDrawable)
                .apply { setColor(mValue) }
    }

    override fun onColorSet(color: Int) {
        if (!callChangeListener(color)) {
            return
        }

        if (isPersistent) {
            persistInt(color)
        }
        mValue = color

        addRecentColor(color)
        notifyChanged()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val uptimeMillis = SystemClock.uptimeMillis()
        if (uptimeMillis - mLastClickTime > 600L) {
            showDialog(null)
        }
        mLastClickTime = uptimeMillis
        return false
    }

    private fun showDialog(state: Bundle?) {
        mColorPickerDialog = SeslColorPickerDialog(
            context, this, mValue, recentColors, mAlphaSliderEnabled
        ).apply {
            setNewColor(mValue)
            setTransparencyControlEnabled(mAlphaSliderEnabled)
            if (state != null) onRestoreInstanceState(state)
            show()
        }
    }

    fun setAlphaSliderEnabled(enable: Boolean) {
        mAlphaSliderEnabled = enable
    }

    private fun addRecentColor(color: Int) {
        mUsedColors.removeAll { it == color }
        if (mUsedColors.size > 5) {
            mUsedColors.removeAt(0)
        }
        mUsedColors.add(color)
    }

    private val recentColors: IntArray
        get() = ArrayList(mUsedColors).apply { reverse() }.toIntArray()

    val value: Int get() = mValue

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (mColorPickerDialog == null || !mColorPickerDialog!!.isShowing) {
            return superState
        }
        val myState = SavedState(superState)
        myState.dialogBundle = mColorPickerDialog!!.onSaveInstanceState()
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            showDialog(state.dialogBundle)
        } else {
            super.onRestoreInstanceState(state)
        }
    }


    private class SavedState : BaseSavedState {
        var dialogBundle: Bundle?

        constructor(source: Parcel) : super(source) {
            dialogBundle = source.readBundle()
        }

        constructor(superState: Parcelable?) : super(superState) {
            dialogBundle = null

        }

        constructor(superState: Parcelable?, dialogBundle: Bundle?) : super(superState) {
            this.dialogBundle = dialogBundle
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeBundle(dialogBundle)
        }

        companion object CREATOR : Creator<SavedState?> {
            override fun createFromParcel(source: Parcel): SavedState {
                return SavedState(source)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    private fun loadRecentColors() {
        val recentColorsString = preferenceDataStore
            ?.getString(KEY_RECENT_COLORS, "")
            ?: PreferenceManager.getDefaultSharedPreferences(context)!!
                .getString(KEY_RECENT_COLORS, "") ?: return

        if (recentColorsString.isEmpty()) return

        mUsedColors.apply {
            clear()
            addAll(recentColorsString.split(":").map { convertToColorInt(it) })
        }
    }

    override fun onDetached() {
        super.onDetached()
        if (mPersistRecentColors) {
            saveRecentColors()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun saveRecentColors() {
        //Saving it as String instead of String set to preserve its order
        val recentColorsStringSet = mUsedColors.joinToString(":") { "#${it.toHexString()}" }
        preferenceDataStore
            ?.putString(KEY_RECENT_COLORS, recentColorsStringSet)
            ?: PreferenceManager.getDefaultSharedPreferences(context)!!.edit()
                .putString(KEY_RECENT_COLORS, recentColorsStringSet).apply()
    }

    companion object {
        private const val KEY_RECENT_COLORS: String = "oneui:color_picker:recent5_colors"

        @Throws(IllegalArgumentException::class)
        @ColorInt
        fun convertToColorInt(argb: String) =
            Color.parseColor(if (argb.startsWith("#")) argb else "#$argb")
    }
}