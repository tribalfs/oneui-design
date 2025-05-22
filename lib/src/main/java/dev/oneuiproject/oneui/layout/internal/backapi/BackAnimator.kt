package dev.oneuiproject.oneui.layout.internal.backapi

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface BackAnimator: BackHandler{
    fun isBackProgressStarted(): Boolean
}