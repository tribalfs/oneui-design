@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.oneuiproject.oneui.utils.internal.updateWidth

/**
 * A [BottomSheetDialogFragment] that provides the Samsung One UI bottom sheet design.
 *
 * Features:
 * - **Skip Half Expanded State:** The bottom sheet directly expands to the fully expanded state.
 * - **Max Width Calculation:** Dynamically calculates and sets the maximum width of the bottom sheet
 *   based on the screen size, adhering to One UI design guidelines.
 * - **Configuration Change Handling:** Adjusts the maximum width when the device configuration (e.g., orientation) changes.
 *
 * This class is designed to be extended by specific bottom sheet dialog implementations.
 */
open class SemBottomSheetDialogFragment : BottomSheetDialogFragment {

    constructor() : super()

    @SuppressLint("ValidFragment")
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    @CallSuper
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.skipCollapsed = true
            updateWidth()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.updateWidth()
    }

    override fun onStart() {
        fullExpandAndHideStatusBar()
        super.onStart()
    }

    private inline fun fullExpandAndHideStatusBar(){
        (dialog as? BottomSheetDialog)?.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            @Suppress("DEPRECATION")
            window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }
}