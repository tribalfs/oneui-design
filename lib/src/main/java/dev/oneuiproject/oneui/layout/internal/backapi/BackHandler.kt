package dev.oneuiproject.oneui.layout.internal.backapi

import androidx.activity.BackEventCompat
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface BackHandler{
    fun startBackProgress(backEvent: BackEventCompat)
    fun updateBackProgress(backEvent: BackEventCompat)
    fun handleBackInvoked()
    fun cancelBackProgress()
}