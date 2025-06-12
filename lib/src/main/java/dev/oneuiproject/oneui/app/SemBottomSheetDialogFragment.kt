@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.pxToDp
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil

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
            behavior.apply {
                skipCollapsed = true
                significantVelocityThreshold = 2_000
                setReleaseLowOffset(200)
                setRetainOriginalPaddings(true)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateWidth()
        super.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        fullyExpand()
        super.onStart()
        updateWidth()
        setupSystemUI()
    }

    private fun fullyExpand() {
        (dialog as? BottomSheetDialog)?.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setupSystemUI() {
        @Suppress("DEPRECATION")
        dialog?.window?.apply {
            if (!DeviceLayoutUtil.isTabletLayoutOrDesktop(context)) {
                    decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isNavigationBarContrastEnforced = false
            }
        }
    }

    //https://m3.material.io/components/bottom-sheets/specs
    private fun updateWidth() {
        (dialog as? BottomSheetDialog)?.apply {
            val displayMetrics: DisplayMetrics = resources.displayMetrics
            val widthInDp = displayMetrics.widthPixels.pxToDp(resources)
            val maxWidth = if (widthInDp > 640.0f) {
                (widthInDp - (56 * 2)).dpToPx(resources)
            } else {
                (widthInDp).dpToPx(resources)
            }
            behavior.maxWidth = maxWidth
        }
    }
}