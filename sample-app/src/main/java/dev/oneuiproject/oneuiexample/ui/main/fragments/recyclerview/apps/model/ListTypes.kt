package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model

import androidx.annotation.StringRes
import androidx.picker.model.AppData
import androidx.picker.model.AppData.GridAppDataBuilder
import androidx.picker.model.AppData.GridCheckBoxAppDataBuilder
import androidx.picker.model.AppData.ListAppDataBuilder
import androidx.picker.model.AppData.ListCheckBoxAppDataBuilder
import androidx.picker.model.AppData.ListRadioButtonAppDataBuilder
import androidx.picker.model.AppData.ListSwitchAppDataBuilder
import androidx.picker.model.AppInfoData
import com.sec.sesl.tester.R

enum class ListTypes(
    val builder: Class<out AppData.AppDataBuilder<AppInfoData>>,
    @field:StringRes val description: Int
) {

    LIST_TYPE(ListAppDataBuilder::class.java, R.string.list),

    TYPE_LIST_ACTION_BUTTON(ListAppDataActionBuilder::class.java, R.string.list_action_button),

    TYPE_LIST_CHECKBOX(ListCheckBoxAppDataBuilder::class.java, R.string.list_checkbox),

    TYPE_LIST_RADIOBUTTON(ListRadioButtonAppDataBuilder::class.java, R.string.list_radiobutton),

    TYPE_LIST_SWITCH(ListSwitchAppDataBuilder::class.java, R.string.list_switch),

    TYPE_GRID(GridAppDataBuilder::class.java, R.string.grid),

    TYPE_GRID_CHECKBOX(GridCheckBoxAppDataBuilder::class.java, R.string.grid_checkbox)

}