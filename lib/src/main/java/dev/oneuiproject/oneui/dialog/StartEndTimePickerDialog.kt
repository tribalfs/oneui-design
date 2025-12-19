@file:Suppress("MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.IntRange
import androidx.appcompat.app.AlertDialog
import androidx.picker.widget.SeslTimePicker
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.dialog.internal.StartEndTabLayout
import dev.oneuiproject.oneui.dialog.internal.getCustomCalendarInstance
import dev.oneuiproject.oneui.dialog.internal.getTimeText
import dev.oneuiproject.oneui.ktx.addTab
import dev.oneuiproject.oneui.utils.internal.updateWidth

/**
 * A dialog that allows users to pick a start and end time using a tabbed interface.
 *
 * This dialog presents two tabs (Start and End), each with a time picker. The user can switch between
 * the tabs to set the start and end times independently. The dialog supports both 24-hour and 12-hour formats.
 *
 * @constructor Creates a new [StartEndTimePickerDialog].
 * @param context The context to use for building the dialog.
 * @param startTime The initial start time in minutes from midnight (0-1439).
 * @param endTime The initial end time in minutes from midnight (0-1439).
 * @param is24HourFormat Whether to use 24-hour time format. Defaults to the system setting.
 * @param timePickerChangeListener Listener to receive the selected start and end times when the user confirms.
 *
 * @see TimePickerChangeListener
 */
class StartEndTimePickerDialog(
    context: Context,
    startTime: Int,
    endTime: Int,
    private val is24HourFormat: Boolean = DateFormat.is24HourFormat(context),
    private val timePickerChangeListener: TimePickerChangeListener?,
) : AlertDialog(context, R.style.OneUI_StartEndTimePickerDialog),
    SeslTimePicker.OnTimeChangedListener,
    SeslTimePicker.OnEditTextModeChangedListener {

    private var tabLayout: StartEndTabLayout? = null
    private var timePicker: SeslTimePicker? = null
    private var timePickerDialog: ViewGroup? = null

    fun interface TimePickerChangeListener {
        fun onTimeSet(startTime: Int, endTime: Int)
    }


    override fun onStart() {
        super.onStart()
        updateWidth()
    }

    override fun onEditTextModeChanged(view: SeslTimePicker, isEditTextMode: Boolean) {}

    init {
        initMainView()
        initDialogButton()
        onUpdateHourFormat()

        tabLayout!!.init(
            startTime,
            endTime,
            onTabSelectedListener = object: StartEndTabLayout.OnTabSelectedListener {
                override fun onPreTabSelected() {
                    timePicker!!.isEditTextMode = false
                }

                override fun onTabSelected(index: Int, time: Int) {
                    updatePicker(time)
                }

            },
            timeFormatter = {
                timeFormatter(it)
            }
        )
        tabLayout!!.select(0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            onUpdateHourFormat()
            timePickerDialog?.requestLayout()
        }
    }

    private fun initMainView() {
        @SuppressLint("InflateParams")
        timePickerDialog = inflate(context, R.layout.oui_des_dialog_start_end_time_picker, null) as LinearLayout
        tabLayout = StartEndTabLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_tab_height)
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_tab_margin_top)
                marginStart = resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_tab_margin)
                marginEnd = resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_tab_margin)
            }
            addTab(R.string.oui_des_time_dialog_start).apply {
                tag = R.string.oui_des_time_dialog_start
            }
            addTab(R.string.oui_des_time_dialog_end).apply {
                tag = R.string.oui_des_time_dialog_end
            }
        }
        timePickerDialog!!.findViewById<FrameLayout>(R.id.tabFrame).addView(tabLayout, 0)

        timePicker = SeslTimePicker(
            ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_height)
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_margin_top)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.oui_des_dialog_start_end_time_picker_margin_bottom)
            }
            setOnEditTextModeChangedListener(this@StartEndTimePickerDialog)
            setOnTimeChangedListener(this@StartEndTimePickerDialog)
        }
        timePickerDialog!!.findViewById<LinearLayout>(R.id.timePickerLayout).addView(timePicker, 0)

        setView(timePickerDialog)
        seslSetBackgroundBlurEnabled()
    }

    private fun initDialogButton() {
        val resources = context.resources
        setButton(
            BUTTON_POSITIVE,
            resources.getString(R.string.oui_des_common_done)
        ) { _, _ ->
            timePicker!!.clearFocus()
            tabLayout!!.times.let{
                timePickerChangeListener?.onTimeSet(it[0], it[1])
            }
        }
        setButton(
            BUTTON_NEGATIVE,
            resources.getString(R.string.oui_des_common_cancel)
        ) { _, _ -> timePicker!!.clearFocus() }
    }

    override fun onTimeChanged(view: SeslTimePicker, hourOfDay: Int, minute: Int) {
        tabLayout!!.updateTime(getTimeInt(hourOfDay, minute))
    }

    private fun updatePicker(time: Int) {
        timePicker!!.apply {
            hour = time / 60
            minute = time % 60
        }
    }

    private fun onUpdateHourFormat() {
        timePicker!!.setIs24HourView(is24HourFormat)
        tabLayout!!.reload()
    }

    private fun timeFormatter(i: Int): String = getTimeText(
        context,
        getCustomCalendarInstance(i / 60, i % 60, is24HourFormat),
        is24HourFormat
    )

    private fun getTimeInt(hourOfDay: Int, minute: Int): Int {
        return hourOfDay * 60 + minute
    }

    /**
     * Sets the title for the start time tab.
     *
     * @param title The title to be set for the start time tab.
     */
    fun setStartTimeTitle(title: String) {
        tabLayout!!.getTabAt(0)!!.setText(title)
    }

    /**
     * Sets the title for the "End" time tab.
     *
     * @param title The title to display for the end time tab.
     */
    fun setEndTimeTitle(title: String) {
        tabLayout!!.getTabAt(1)!!.setText(title)
    }

    /**
     * Sets whether to show the sub-text (time) below the tab titles.
     *
     * @param show `true` to show the sub-text, `false` to hide it.
     */
    fun setShowSubText(show: Boolean) {
        tabLayout!!.showSubText = show
    }

    /**
     * Selects the tab at the specified index.
     *
     * @param index The index of the tab to select (0 for start time, 1 for end time).
     */
    fun selectTabAtIndex(@IntRange(0,1) index: Int){
        tabLayout!!.select(index)
    }

}