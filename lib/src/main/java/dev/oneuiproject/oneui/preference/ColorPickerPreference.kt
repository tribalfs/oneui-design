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
import androidx.core.content.withStyledAttributes
import androidx.picker3.app.SeslColorPickerDialog
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.preference.internal.PreferenceImageView
import dev.oneuiproject.oneui.design.R
import androidx.core.content.edit
import androidx.core.graphics.toColorInt


/**
 * A Preference that allows the user to pick a color.
 * The color is stored as an integer.
 *
 * @param context The Context this preference is associated with.
 * @param attrs The attributes of the XML tag that is inflating the preference.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 * that supplies default values for the view. Can be 0 to not look for defaults.
 */
class ColorPickerPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr), Preference.OnPreferenceClickListener,
    SeslColorPickerDialog.OnColorSetListener {

    private var colorPickerDialog: SeslColorPickerDialog? = null

    @SuppressLint("RestrictedApi")
    private var preview: PreferenceImageView? = null
    private var _value = Color.BLACK
    private val usedColors = ArrayList<Int>(5)
    private var lastClickTime: Long = 0
    private var alphaSliderEnabled = false
    private var persistRecentColors = true

    init {
        widgetLayoutResource = R.layout.oui_des_preference_color_picker_widget
        onPreferenceClickListener = this

        context.withStyledAttributes(attrs, R.styleable.ColorPickerPreference) {
            alphaSliderEnabled =
                getBoolean(R.styleable.ColorPickerPreference_showAlphaSlider, false)
            persistRecentColors =
                getBoolean(R.styleable.ColorPickerPreference_persistRecentColors, true)
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
        onColorSet(getPersistedInt(defaultValue as? Int ?: _value))
        if (persistRecentColors) {
            loadRecentColors()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        preview = holder.findViewById(R.id.imageview_widget) as PreferenceImageView
        setPreviewColor()
    }

    private fun setPreviewColor() {
        if (preview == null) return
        preview!!.background =
            (ContextCompat.getDrawable(context, R.drawable.oui_des_preference_color_picker_preview)!!
                .mutate() as GradientDrawable)
                .apply { setColor(_value) }
    }

    override fun onColorSet(color: Int) {
        if (!callChangeListener(color)) {
            return
        }

        if (isPersistent) {
            persistInt(color)
        }
        _value = color

        addRecentColor(color)
        notifyChanged()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val uptimeMillis = SystemClock.uptimeMillis()
        if (uptimeMillis - lastClickTime > 600L) {
            showDialog(null)
        }
        lastClickTime = uptimeMillis
        return false
    }

    private fun showDialog(state: Bundle?) {
        colorPickerDialog = SeslColorPickerDialog(
            context, this, _value, recentColors, alphaSliderEnabled
        ).apply {
            setNewColor(_value)
            setTransparencyControlEnabled(alphaSliderEnabled)
            if (state != null) onRestoreInstanceState(state)
            show()
        }
    }

    fun setAlphaSliderEnabled(enable: Boolean) {
        alphaSliderEnabled = enable
    }

    private fun addRecentColor(color: Int) {
        usedColors.removeAll { it == color }
        if (usedColors.size > 6) {
            usedColors.removeAt(0)
        }
        usedColors.add(color)
    }

    private val recentColors: IntArray
        get() = ArrayList(usedColors).apply { reverse() }.toIntArray()

    val value: Int get() = _value

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (colorPickerDialog == null || !colorPickerDialog!!.isShowing) {
            return superState
        }
        val myState = SavedState(superState)
        myState.dialogBundle = colorPickerDialog!!.onSaveInstanceState()
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
            dialogBundle = source.readBundle(Bundle::class.java.getClassLoader())
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

        usedColors.apply {
            clear()
            addAll(recentColorsString.split(":").map { convertToColorInt(it) })
        }
    }

    override fun onDetached() {
        super.onDetached()
        if (persistRecentColors) {
            saveRecentColors()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun saveRecentColors() {
        //Saving it as String instead of String set to preserve its order
        val recentColorsStringSet = usedColors.joinToString(":") { "#${it.toHexString()}" }
        preferenceDataStore
            ?.putString(KEY_RECENT_COLORS, recentColorsStringSet)
            ?: PreferenceManager.getDefaultSharedPreferences(context)!!.edit {
                putString(KEY_RECENT_COLORS, recentColorsStringSet)
            }
    }

    companion object {
        private const val KEY_RECENT_COLORS: String = "oneui:color_picker:recent5_colors"

        @ColorInt
        fun convertToColorInt(argb: String) =
            (if (argb.startsWith("#")) argb else "#$argb").toColorInt()
    }
}