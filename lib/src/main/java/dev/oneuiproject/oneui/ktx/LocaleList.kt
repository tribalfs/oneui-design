package dev.oneuiproject.oneui.ktx

import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
inline fun LocaleList.ifEmpty(block: () -> LocaleList): LocaleList {
    return if (isEmpty) block() else this
}