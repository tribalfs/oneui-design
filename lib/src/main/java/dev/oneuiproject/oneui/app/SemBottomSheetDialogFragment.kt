package dev.oneuiproject.oneui.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.oneuiproject.oneui.utils.internal.updateWidth

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
            updateWidth()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.updateWidth()
    }

    override fun onStart() {
        hideStatusBar()
        super.onStart()
    }

    private fun hideStatusBar(){
        @Suppress("DEPRECATION")
        dialog?.window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}