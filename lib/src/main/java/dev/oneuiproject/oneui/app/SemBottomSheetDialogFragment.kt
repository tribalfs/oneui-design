package dev.oneuiproject.oneui.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowInsets
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class SemBottomSheetDialogFragment : BottomSheetDialogFragment {

    @SuppressLint("ValidFragment")
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.skipCollapsed = true
            setOnShowListener { behavior.state = BottomSheetBehavior.STATE_EXPANDED }
            behavior.maxWidth = getMaxWidth()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (dialog as BottomSheetDialog).behavior.maxWidth = getMaxWidth()
    }

    private fun getMaxWidth(): Int {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        var maxWidthInPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        val widthInDp = maxWidthInPixels / density
        return if (widthInDp >= 600.0f && widthInDp < 960.0f) {
            (maxWidthInPixels * 0.86).toInt()
        } else if (widthInDp >= 960.0f) {
            (density * 840.0f).toInt()
        } else maxWidthInPixels
    }

}