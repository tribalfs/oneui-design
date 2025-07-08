package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.util

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.picker.helper.SeslAppInfoDataHelper
import androidx.picker.model.AppInfoData
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model.ListTypes


fun getAppList(context: Context, listType: ListTypes): List<AppInfoData> {
    val actionIcon by lazy { ContextCompat.getDrawable(context,  dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline) }

    val appInfoDataHelper = SeslAppInfoDataHelper(context, listType.builder)
    return appInfoDataHelper.getPackages().onEach {
        it.subLabel = it.packageName
        if (listType == ListTypes.TYPE_LIST_ACTION_BUTTON) {
            it.actionIcon = actionIcon
        }
    }
}