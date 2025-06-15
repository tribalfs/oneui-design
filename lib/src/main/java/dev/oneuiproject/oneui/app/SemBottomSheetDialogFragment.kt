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

/**
 * A [BottomSheetDialogFragment] that provides the Samsung One UI bottom sheet design.
 *
 * Features:
 * - **Skip Collapsed State:** The bottom sheet directly expands to the fully expanded state,
 *   skipping the collapsed state.
 * - **Max Width Calculation:** Dynamically calculates and sets the maximum width of the bottom sheet
 *   based on the screen size, adhering to One UI design guidelines.
 * - **Configuration Change Handling:** Adjusts the maximum width when the device configuration (e.g., orientation) changes.
 *
 * This class is designed to be extended by specific bottom sheet dialog implementations.
 */
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