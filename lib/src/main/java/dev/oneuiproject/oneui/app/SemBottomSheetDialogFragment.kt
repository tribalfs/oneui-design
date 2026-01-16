@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.pxToDp
import dev.oneuiproject.oneui.ktx.semSetBackgroundBlurEnabled
import dev.oneuiproject.oneui.ktx.setRoundedCorners
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletLayoutOrDesktop
import dev.oneuiproject.oneui.utils.internal.updateWidth
import com.google.android.material.R as materialR
import androidx.appcompat.R as appcompatR

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
                forceExpandOnNestedScrollStop(true)
                setRetainOriginalPaddings(true)
            }
        }
    }


    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.rootView.apply {
            resources.getDimensionPixelSize(appcompatR.dimen.sesl_dialog_background_inset_horizontal).let {
                updatePadding(left = it, right = it)
            }
            findViewById<ViewGroup>(materialR.id.design_bottom_sheet).apply {
                if (!semSetBackgroundBlurEnabled() || isReduceTransparencyOn()) {
                    setRoundedCorners(resources.getDimension(appcompatR.dimen.sesl_dialog_background_corner_radius))
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
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
            if (!isTabletLayoutOrDesktop(context)) {
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isNavigationBarContrastEnforced = false
            }
        }
    }

    private fun updateWidth() {
        (dialog as? BottomSheetDialog)?.apply {
            if (isTabletLayoutOrDesktop(context)) {
                updateWidth()
            } else {
                //https://m3.material.io/components/bottom-sheets/specs
                val widthInDp = resources.displayMetrics.widthPixels.pxToDp(resources)
                val width = if (widthInDp > 640.0f) {
                    (widthInDp - (56 * 2)).dpToPx(resources)
                } else {
                    widthInDp.dpToPx(resources)
                }
                behavior.maxWidth = width
            }
        }
    }

    private fun isReduceTransparencyOn(): Boolean =
        Settings.System.getInt(
            requireContext().getContentResolver(),
            "accessibility_reduce_transparency",
            0
        ) != 0
}