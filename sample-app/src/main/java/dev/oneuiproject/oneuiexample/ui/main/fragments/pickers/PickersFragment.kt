package dev.oneuiproject.oneuiexample.ui.main.fragments.pickers

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.picker.app.SeslDatePickerDialog
import androidx.picker.widget.SeslTimePicker
import androidx.picker3.app.SeslColorPickerDialog
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentPickersBinding
import dev.oneuiproject.oneui.dialog.StartEndTimePickerDialog
import dev.oneuiproject.oneui.ktx.init
import dev.oneuiproject.oneui.ktx.setEntries
import dev.oneuiproject.oneui.ktx.showTimePickerDialog
import dev.oneuiproject.oneui.utils.getRegularFont
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast
import java.util.Calendar

class PickersFragment : AbsBaseFragment(R.layout.fragment_pickers) {

    private val binding by autoCleared { FragmentPickersBinding.bind(requireView()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNumberPicker(view)
        initTimePickers(view)
        initDatePickers(view)
        initSpinner(view)
        initDialogBtns(view)
    }


    private fun initNumberPicker(view: View) {
        binding.pickerNumber3.apply {
            setTextTypeface(
                ResourcesCompat.getFont(
                    requireContext(),
                    dev.oneuiproject.oneui.design.R.font.samsungsharpsans_bold
                )
            )
            minValue = 0
            maxValue = 2
            setTextSize(40f)
            displayedValues = arrayOf("A", "B", "C")
            editText.apply {
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        isEditTextMode = false
                    }
                    false
                }
            }
        }

        binding.pickerNumber2.apply {
            setTextTypeface(getRegularFont())
            minValue = 0
            maxValue = 10
            value = 8
            setTextSize(50f)
            editText.apply {
                imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NEXT
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        isEditTextMode = false
                        binding.pickerNumber3.isEditTextMode = true
                        binding.pickerNumber3.requestFocus()
                    }
                    false
                }
            }
        }

        binding.pickerNumber1.apply {
            minValue = 1
            maxValue = 100
            value = 50
            setTextSize(40f)
            editText.apply {
                imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NEXT
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        isEditTextMode = false
                        binding.pickerNumber2.isEditTextMode = true
                        binding.pickerNumber2.requestFocus()
                    }
                    false
                }
            }
        }
    }

    private fun initTimePickers(view: View) {
        binding.pickerTime.apply {
            setIs24HourView(DateFormat.is24HourFormat(requireContext()))
            setNumberPickerTextSize(SeslTimePicker.PICKER_HOUR, 42f)
            setNumberPickerTextSize(SeslTimePicker.PICKER_MINUTE, 42f)
            setNumberPickerTextSize(SeslTimePicker.PICKER_DIVIDER, 42f)
            setNumberPickerTextSize(SeslTimePicker.PICKER_AMPM, 42f)
        }

        fun showSleepTimePickerDialog(focusStart: Boolean = true) {
            StartEndTimePickerDialog(
                requireContext(),
                binding.pickerSleepTime.bedTimeInMinute.toInt(),
                binding.pickerSleepTime.wakeUpTimeInMinute.toInt(),
                DateFormat.is24HourFormat(requireContext())
            ) { startTime: Int, endTime: Int ->
                binding.pickerSleepTime.bedTimeInMinute = startTime.toFloat()
                binding.pickerSleepTime.wakeUpTimeInMinute = endTime.toFloat()
            }.apply {
                setShowSubText(false)
                setStartTimeTitle("BedTime")
                setEndTimeTitle("Wake-up time")
                selectTabAtIndex(if (focusStart) 0 else 1)
                show()
            }
        }

        binding.pickerSleepTime.apply {
            bedTimeView.setOnClickListener { showSleepTimePickerDialog() }
            wakeUpTimeView.setOnClickListener { showSleepTimePickerDialog(false) }
        }
    }

    private fun initDatePickers(view: View) {
        binding.pickerDate.init(Calendar.getInstance())
        binding.pickerSpinningDate.showMarginRight(true)
    }

    private fun initSpinner(view: View) {
        fun onItemSelected(position: Int) {
            when (position) {
                0 -> {
                    binding.pickersNumber.visibility = View.VISIBLE

                    binding.pickerTime.visibility = View.GONE
                    binding.pickerDate.visibility = View.GONE
                    binding.pickerSpinningDate.visibility = View.GONE
                    binding.pickerSleepTime.visibility = View.GONE
                }

                1 -> {
                    binding.pickersNumber.visibility = View.GONE
                    binding.pickerTime.visibility = View.VISIBLE
                    binding.pickerTime.startAnimation(200, null)
                    binding.pickerDate.visibility = View.GONE
                    binding.pickerSpinningDate.visibility = View.GONE
                    binding.pickerSleepTime.visibility = View.GONE
                }

                2 -> {
                    binding.pickersNumber.visibility = View.GONE
                    binding.pickerTime.visibility = View.GONE
                    binding.pickerDate.visibility = View.VISIBLE
                    binding.pickerSpinningDate.visibility = View.GONE
                    binding.pickerSleepTime.visibility = View.GONE
                }

                3 -> {
                    binding.pickersNumber.visibility = View.GONE
                    binding.pickerTime.visibility = View.GONE
                    binding.pickerDate.visibility = View.GONE
                    binding.pickerSpinningDate.visibility = View.VISIBLE
                    binding.pickerSleepTime.visibility = View.GONE
                }

                4 -> {
                    binding.pickersNumber.visibility = View.GONE
                    binding.pickerTime.visibility = View.GONE
                    binding.pickerDate.visibility = View.GONE
                    binding.pickerSpinningDate.visibility = View.GONE
                    binding.pickerSleepTime.visibility = View.VISIBLE
                }
            }
        }

        val categories = listOf(
            "NumberPicker",
            "TimePicker",
            "DatePicker",
            "SpinningDatePicker",
            "SleepTimePicker"
        )

        view.findViewById<AppCompatSpinner>(R.id.pickers_spinner).apply {
            setEntries(categories) { p, _ -> p?.let { onItemSelected(it) } }
        }
    }

    private fun initDialogBtns(view: View) {
        binding.pickersDialogDate.setOnClickListener { openDatePickerDialog() }
        binding.pickersDialogTime.setOnClickListener { openTimePickerDialog() }
        binding.pickersDialogStartEndTime.setOnClickListener { openStartEndTimePickerDialog() }
        binding.pickersDialogColor.setOnClickListener { openColorPickerDialog() }
    }

    private fun openDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val dialog = SeslDatePickerDialog(
            requireContext(),
            { view, year, monthOfYear, dayOfMonth ->
                semToast("Year: $year\nMonth: $monthOfYear\nDay: $dayOfMonth")
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
        dialog.show()
    }

    private fun openTimePickerDialog() {
        val calendar = Calendar.getInstance()

        requireContext().showTimePickerDialog(calendar) { _, hourOfDay, minute ->
            semToast("Hour: $hourOfDay\nMinute: $minute")
        }
    }

    private fun openStartEndTimePickerDialog() {
        val dialog = StartEndTimePickerDialog(
            requireContext(), 0, 600,
            DateFormat.is24HourFormat(requireContext())
        ) { startTime, endTime ->
            semToast(
                "Start: H:${startTime / 60} M:${startTime % 60}" +
                        "\nEnd: H:${endTime / 60} M:${endTime % 60}"
            )
        }
        dialog.show()
    }

    private var colorPickerDialog: SeslColorPickerDialog? = null
    private var currentColor: Int = Color.RED
    private val recentColors = mutableListOf<Int>(Color.RED, Color.GREEN, Color.BLUE)

    private fun openColorPickerDialog() {
        colorPickerDialog = SeslColorPickerDialog(
            requireContext(),
            { color ->
                currentColor = color
                if (recentColors.size == 6) {
                    recentColors.removeAt(5)
                }
                recentColors.add(0, color)
            },
            currentColor, recentColors.toIntArray(), true
        ).apply {
            setTransparencyControlEnabled(true)
            colorPicker.setOnColorChangedListener { currentColor = it }
            show()
            requireView().post {
                setOnBitmapSetListener(object: SeslColorPickerDialog.OnBitmapSetListener{
                    override fun onBitmapSet(): Bitmap {
                        val rootView = requireActivity().window.decorView.rootView
                        val bitmap = createBitmap(rootView.width, rootView.height)
                        val canvas = Canvas(bitmap)
                        rootView.draw(canvas)
                        return bitmap
                    }
                })
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        colorPickerDialog?.apply {
            if (isShowing == true){
                dismiss()
                openColorPickerDialog()
            }
        }
    }
}