package dev.oneuiproject.oneuiexample.ui.main.fragment.apppicker.model

import androidx.annotation.StringRes
import androidx.apppickerview.widget.AppPickerView
import com.sec.sesl.tester.R

enum class ListTypes(@AppPickerView.AppPickerType val type: Int, @StringRes val description: Int ){

    LIST_TYPE(AppPickerView.TYPE_LIST, R.string.list),

    TYPE_LIST_ACTION_BUTTON(AppPickerView.TYPE_LIST_ACTION_BUTTON, R.string.list_action_button),

    TYPE_LIST_CHECKBOX(AppPickerView.TYPE_LIST_CHECKBOX, R.string.list_checkbox),

    TYPE_LIST_CHECKBOX_WITH_ALL_APPS(AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS, R.string.list_checkbox_all_apps),

    TYPE_LIST_RADIOBUTTON(AppPickerView.TYPE_LIST_RADIOBUTTON, R.string.list_radiobutton),

    TYPE_LIST_SWITCH(AppPickerView.TYPE_LIST_SWITCH, R.string.list_switch),

    TYPE_LIST_SWITCH_WITH_ALL_APPS(AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS, R.string.list_switch_all_apps),

    TYPE_GRID(AppPickerView.TYPE_GRID, R.string.grid),

    TYPE_GRID_CHECKBOX(AppPickerView.TYPE_GRID_CHECKBOX, R.string.grid_checkbox)

}