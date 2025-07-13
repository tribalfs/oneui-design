package dev.oneuiproject.oneui.ktx

import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
@RequiresApi(Build.VERSION_CODES.N)
inline fun LocaleList.ifEmpty(block: () -> LocaleList): LocaleList {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (isEmpty) block() else this
}