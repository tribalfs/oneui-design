package dev.oneuiproject.oneui.layout.internal.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.slidingpanelayout.widget.SlidingPaneLayout


@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class NavSlidingPaneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): SlidingPaneLayout(context, attrs, defStyle){

    init{
        setOverhangSize(DEFAULT_OVERHANG_SIZE)
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        seslSetPendingAction(if (isOpen) PENDING_ACTION_EXPANDED else PENDING_ACTION_COLLAPSED)
        super.onConfigurationChanged(configuration)
    }

    companion object{
        const val DEFAULT_OVERHANG_SIZE = 32 // dp;
    }
}