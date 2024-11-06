package dev.oneuiproject.oneuiexample.ui.fragment.apppicker.model

import androidx.apppickerview.widget.AppPickerView

enum class ListTypes(@AppPickerView.AppPickerType val type: Int, val description: String ){

    LIST_TYPE(AppPickerView.TYPE_LIST,    "List"),

    TYPE_LIST_ACTION_BUTTON(AppPickerView.TYPE_LIST_ACTION_BUTTON, "List, Action Button"),

    TYPE_LIST_CHECKBOX(AppPickerView.TYPE_LIST_CHECKBOX,"List, CheckBox"),

    TYPE_LIST_CHECKBOX_WITH_ALL_APPS(AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS,"List, CheckBox, All apps"),

    TYPE_LIST_RADIOBUTTON(AppPickerView.TYPE_LIST_RADIOBUTTON,"List, RadioButton"),

    TYPE_LIST_SWITCH(AppPickerView.TYPE_LIST_SWITCH,"List, Switch"),

    TYPE_LIST_SWITCH_WITH_ALL_APPS(AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS, "List, Switch, All apps"),

    TYPE_GRID(AppPickerView.TYPE_GRID,"Grid"),

    TYPE_GRID_CHECKBOX(AppPickerView.TYPE_GRID_CHECKBOX, "Grid, CheckBox")

}