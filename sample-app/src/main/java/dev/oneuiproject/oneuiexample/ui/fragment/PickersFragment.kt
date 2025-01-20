package dev.oneuiproject.oneuiexample.ui.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.res.ResourcesCompat
import androidx.picker.app.SeslDatePickerDialog
import androidx.picker.app.SeslTimePickerDialog
import androidx.picker.widget.SeslDatePicker
import androidx.picker.widget.SeslNumberPicker
import androidx.picker.widget.SeslSleepTimePicker
import androidx.picker.widget.SeslSpinningDatePicker
import androidx.picker.widget.SeslTimePicker
import androidx.picker3.app.SeslColorPickerDialog
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.dialog.StartEndTimePickerDialog
import dev.oneuiproject.oneui.ktx.showTimePickerDialog
import dev.oneuiproject.oneui.widget.Toast
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment
import java.util.Calendar

class PickersFragment : BaseFragment(), AdapterView.OnItemSelectedListener,
    SeslColorPickerDialog.OnColorSetListener {
    private var mCurrentColor = 0
    private val mRecentColors: MutableList<Int> = ArrayList()

    private lateinit var mNumberPickers: LinearLayout
    private lateinit var mTimePicker: SeslTimePicker
    private lateinit var mDatePicker: SeslDatePicker
    private lateinit var mSpinningDatePicker: SeslSpinningDatePicker
    private lateinit var mSleepTimePicker: SeslSleepTimePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentColor = -16547330 // #0381fe
        mRecentColors.add(mCurrentColor)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNumberPicker(view)
        initTimePicker(view)
        initDatePickers(view)
        initSpinner(view)
        initDialogBtns(view)
    }

    override fun getLayoutResId(): Int {
        return R.layout.sample3_fragment_pickers
    }

    override fun getIconResId(): Int {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_calendar
    }

    override fun getTitle(): CharSequence {
        return "Pickers"
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> {
                mNumberPickers.visibility = View.VISIBLE
                mTimePicker.visibility = View.GONE
                mDatePicker.visibility = View.GONE
                mSpinningDatePicker.visibility = View.GONE
                mSleepTimePicker.visibility = View.GONE
            }

            1 -> {
                mNumberPickers.visibility = View.GONE
                mTimePicker.visibility = View.VISIBLE
                mTimePicker.startAnimation(200, null)
                mDatePicker.visibility = View.GONE
                mSpinningDatePicker.visibility = View.GONE
                mSleepTimePicker.visibility = View.GONE
            }

            2 -> {
                mNumberPickers.visibility = View.GONE
                mTimePicker.visibility = View.GONE
                mDatePicker.visibility = View.VISIBLE
                mSpinningDatePicker.visibility = View.GONE
                mSleepTimePicker.visibility = View.GONE
            }

            3 -> {
                mNumberPickers.visibility = View.GONE
                mTimePicker.visibility = View.GONE
                mDatePicker.visibility = View.GONE
                mSpinningDatePicker.visibility = View.VISIBLE
                mSleepTimePicker.visibility = View.GONE
            }

            4 -> {
                mNumberPickers.visibility = View.GONE
                mTimePicker.visibility = View.GONE
                mDatePicker.visibility = View.GONE
                mSpinningDatePicker.visibility = View.GONE
                mSleepTimePicker.visibility = View.VISIBLE
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun initNumberPicker(view: View) {
        mNumberPickers = view.findViewById(R.id.pickers_number)

        val numberPickerThree = mNumberPickers.findViewById<SeslNumberPicker>(R.id.picker_number_3)
        numberPickerThree.setTextTypeface(
            ResourcesCompat.getFont(
                mContext,
                R.font.samsungsharpsans_bold
            )
        )
        numberPickerThree.minValue = 0
        numberPickerThree.maxValue = 2
        numberPickerThree.setTextSize(40f)
        numberPickerThree.displayedValues = arrayOf("A", "B", "C")
        val et3 = numberPickerThree.editText
        et3.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                numberPickerThree.isEditTextMode = false
            }
            false
        }

        val numberPickerTwo = mNumberPickers.findViewById<SeslNumberPicker>(R.id.picker_number_2)
        numberPickerTwo.setTextTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL))
        numberPickerTwo.minValue = 0
        numberPickerTwo.maxValue = 10
        numberPickerTwo.value = 8
        numberPickerTwo.setTextSize(50f)
        val et2 = numberPickerTwo.editText
        et2.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NEXT
        et2.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                numberPickerTwo.isEditTextMode = false
                numberPickerThree.isEditTextMode = true
                numberPickerThree.requestFocus()
            }
            false
        }

        val numberPickerOne = mNumberPickers.findViewById<SeslNumberPicker>(R.id.picker_number_1)
        numberPickerOne.minValue = 1
        numberPickerOne.maxValue = 100
        numberPickerOne.value = 50
        numberPickerOne.setTextSize(40f)
        val et1 = numberPickerOne.editText
        et1.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NEXT
        et1.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                numberPickerOne.isEditTextMode = false
                numberPickerTwo.isEditTextMode = true
                numberPickerTwo.requestFocus()
            }
            false
        }
    }

    private fun initTimePicker(view: View) {
        mTimePicker = view.findViewById(R.id.picker_time)
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(mContext))

        mSleepTimePicker = view.findViewById(R.id.picker_sleep_time)
        mSleepTimePicker.bedTimeView.setOnClickListener { v: View? ->
            requireContext().showTimePickerDialog(
                (mSleepTimePicker.bedTimeInMinute/60).toInt(),
                (mSleepTimePicker.bedTimeInMinute%60).toInt(),
            ){ _, hourOfDay, minute ->
                mSleepTimePicker.bedTimeInMinute = (hourOfDay * 60 + minute).toFloat()
            }
        }
        mSleepTimePicker.wakeUpTimeView.setOnClickListener { v: View? ->
            requireContext().showTimePickerDialog(
                (mSleepTimePicker.wakeUpTimeInMinute/60).toInt(),
                (mSleepTimePicker.wakeUpTimeInMinute%60).toInt(),
            ){ _, hourOfDay, minute ->
                mSleepTimePicker.wakeUpTimeInMinute = (hourOfDay * 60 + minute).toFloat()
            }
        }
    }

    private fun initDatePickers(view: View) {
        mDatePicker = view.findViewById(R.id.picker_date)
        mSpinningDatePicker = view.findViewById(R.id.picker_spinning_date)

        val calendar = Calendar.getInstance()
        mDatePicker.init(
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH], null
        )
        mSpinningDatePicker.init(
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH], null
        )
        mSpinningDatePicker.showMarginRight(true)
    }

    private fun initSpinner(view: View) {
        val spinner = view.findViewById<AppCompatSpinner>(R.id.pickers_spinner)

        val categories: MutableList<String> = ArrayList()
        categories.add("NumberPicker")
        categories.add("TimePicker")
        categories.add("DatePicker")
        categories.add("SpinningDatePicker")
        categories.add("SleepTimePicker")

        val adapter = ArrayAdapter(mContext, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }

    private fun initDialogBtns(view: View) {
        val dateBtn = view.findViewById<AppCompatButton>(R.id.pickers_dialog_date)
        dateBtn.setOnClickListener { v: View? -> openDatePickerDialog() }
        val timeBtn = view.findViewById<AppCompatButton>(R.id.pickers_dialog_time)
        timeBtn.setOnClickListener { v: View? -> openTimePickerDialog() }
        val startEndTimeBtn = view.findViewById<AppCompatButton>(R.id.pickers_dialog_start_end_time)
        startEndTimeBtn.setOnClickListener { v: View? -> openStartEndTimePickerDialog() }
        val colorBtn = view.findViewById<AppCompatButton>(R.id.pickers_dialog_color)
        colorBtn.setOnClickListener { v: View? -> openColorPickerDialog() }
    }

    private fun openDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val dialog = SeslDatePickerDialog(
            mContext,
            { view: SeslDatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                Toast.makeText(
                    mContext,
                    "Year: $year\nMonth: $monthOfYear\nDay: $dayOfMonth",
                    Toast.LENGTH_SHORT
                ).show()
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
        dialog.show()
    }

    private fun openTimePickerDialog() {
        val calendar = Calendar.getInstance()
        requireContext().showTimePickerDialog(calendar){
            _, hourOfDay, minute ->
            Toast.makeText(
                mContext,
                "Hour: $hourOfDay\nMinute: $minute",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openStartEndTimePickerDialog() {
        val dialog = StartEndTimePickerDialog(
            mContext,
            0, 600,
            DateFormat.is24HourFormat(mContext)
        ) { startTime: Int, endTime: Int ->
            android.widget.Toast.makeText(
                mContext,
                """Start: H:${startTime / 60} M:${startTime % 60}
End: H:${endTime / 60} M:${endTime % 60}""", Toast.LENGTH_SHORT
            ).show()
        }
        dialog.show()
    }

    private fun openColorPickerDialog() {
        val dialog = SeslColorPickerDialog(
            mContext, this,
            mCurrentColor, buildIntArray(mRecentColors), true
        )
        dialog.setTransparencyControlEnabled(true)
        dialog.show()
    }

    override fun onColorSet(color: Int) {
        mCurrentColor = color
        if (mRecentColors.size == 6) {
            mRecentColors.removeAt(5)
        }
        mRecentColors.add(0, color)
    }

    private fun buildIntArray(integers: List<Int>): IntArray {
        val ints = IntArray(integers.size)
        var i = 0
        for (n in integers) {
            ints[i++] = n
        }
        return ints
    }
}
