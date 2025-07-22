package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.picker.helper.SeslAppInfoDataHelper
import androidx.picker.model.AppInfoData
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model.ListTypes
import kotlin.collections.addAll


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


inline fun <reified T: View> ViewGroup.firstChildOfType(): T? {
    val queue = ArrayDeque<View>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val view = queue.removeFirst()
        if (view is T) {
            return view
        }
        if (view is ViewGroup) {
            queue.addAll(view.children)
        }
    }
    return null
}