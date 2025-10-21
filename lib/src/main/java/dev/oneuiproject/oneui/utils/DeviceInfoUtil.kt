package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import androidx.reflect.DeviceInfo
import androidx.reflect.feature.SeslFloatingFeatureReflector

/**
 * A flag indicating whether the device supports 3D surface transitions.
 *
 * This is the primary determinant for whether the device supports the One UI blur engine.
 * The value is determined by checking the Samsung floating feature
 * `SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG`.
 *
 * @return `true` if the device is running One UI and supports 3D surface transitions (and thus the blur engine), `false` otherwise.
 */
val supports3DTransitionFlag by lazy(LazyThreadSafetyMode.NONE) {
    @SuppressLint("RestrictedApi")
    DeviceInfo.isOneUI() && SeslFloatingFeatureReflector.getString(
        "SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG",
        "FALSE"
    ) == "TRUE"
}