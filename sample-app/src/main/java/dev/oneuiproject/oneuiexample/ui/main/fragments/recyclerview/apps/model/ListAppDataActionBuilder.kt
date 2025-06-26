package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model

import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.picker.model.AppData.AppDataBuilder
import androidx.picker.model.AppData.AppDataBuilderInfo
import androidx.picker.model.AppData.Companion.TYPE_ITEM_ACTION_BUTTON
import androidx.picker.model.AppInfo
import androidx.picker.model.AppInfoData
import androidx.picker.model.AppInfoDataImpl

@AppDataBuilderInfo(itemType = TYPE_ITEM_ACTION_BUTTON)
@Keep
class ListAppDataActionBuilder(val appInfo: AppInfo) : AppDataBuilder<AppInfoData> {

    override fun build(): AppInfoData =
        AppInfoDataImpl(
            appInfo,
            TYPE_ITEM_ACTION_BUTTON,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            false
        )
    
}
