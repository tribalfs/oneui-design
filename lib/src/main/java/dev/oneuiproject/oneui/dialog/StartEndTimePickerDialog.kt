@file:Suppress("MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IntRange
import androidx.appcompat.app.AlertDialog
import androidx.picker.widget.SeslTimePicker
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.dialog.internal.StartEndTabLayout
import dev.oneuiproject.oneui.dialog.internal.getCustomCalendarInstance
import dev.oneuiproject.oneui.dialog.internal.getTimeText
import dev.oneuiproject.oneui.utils.internal.updateWidth

class StartEndTimePickerDialog(
    context: Context,
    /**Start time in minutes*/
    startTime: Int,
    /**End time in minutes*/
    endTime: Int,
    private val is24HourFormat: Boolean = DateFormat.is24HourFormat(context),
    private val timePickerChangeListener: TimePickerChangeListener?,
) : AlertDialog(context, R.style.OneUI_StartEndTimePickerDialog),
    SeslTimePicker.OnTimeChangedListener,
    SeslTimePicker.OnEditTextModeChangedListener {

    private var tabLayout: StartEndTabLayout? = null
    private var timePicker: SeslTimePicker? = null
    private var timePickerDialog: View? = null

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
        LayoutInflater.from(context).inflate(R.layout.oui_des_dialog_start_end_time_picker, null).also {
            setView(it)
            timePickerDialog = it
            timePicker = it.findViewById<SeslTimePicker>(R.id.time_picker).apply {
                setOnEditTextModeChangedListener(this@StartEndTimePickerDialog)
                setOnTimeChangedListener(this@StartEndTimePickerDialog)
            }
            tabLayout = it.findViewById(R.id.time_picker_tab)
        }
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

    fun setStartTimeTitle(title: String) {
        tabLayout!!.getTabAt(0)!!.setText(title)
    }

    fun setEndTimeTitle(title: String) {
        tabLayout!!.getTabAt(1)!!.setText(title)
    }

    fun setShowSubText(show: Boolean) {
        tabLayout!!.showSubText = show
    }

    fun selectTabAtIndex(@IntRange(0,1) index: Int){
        tabLayout!!.select(index)
    }

}