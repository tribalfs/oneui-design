package dev.oneuiproject.oneui.qr.app.internal

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION
import android.util.Size
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import dev.oneuiproject.oneui.ktx.activity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Helper for choosing CameraX resolutions/aspect ratios that:
 * - roughly match the device screen;
 * - still use camera-friendly ratios (4:3, 16:9);
 * - provide enough detail for ML Kit QR scanning.
 */
internal object CameraResolutionHelper {

    /**
     * Retrieves the size of the current application window.
     *
     * @param context The context used to access the WindowManager.
     * @return A [Size] object representing the width(x) and height(y) of the window bounds.
     */
    fun getWindowSize(context: Context): Point {
        val activity = context.activity
        activity?.let {
            if (VERSION.SDK_INT >= 30) {
                val metrics = it.windowManager.currentWindowMetrics
                return with (metrics.bounds) {Point(width(), height()) }
            }
        }

        return Point().apply {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(this)
        }
    }

    /**
     * Decide whether 4:3 or 16:9 is closer to the actual device screen ratio.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun chooseDeviceFriendlyAspectRatio(context: Context): Int {
        val (w, h) = getWindowSize(context)

        val longer = max(w, h).toFloat()
        val shorter = min(w, h).toFloat()
        val deviceRatio = longer / shorter

        val ratio4by3 = 4f / 3f
        val ratio16by9 = 16f / 9f

        val diff4by3 = abs(deviceRatio - ratio4by3)
        val diff16by9 = abs(deviceRatio - ratio16by9)

        return if (diff4by3 < diff16by9) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    /**
     * Build a ResolutionSelector tuned for QR scanning:
     * - prefers a high resolution (e.g. 1920x1080);
     * - respects a device-friendly aspect ratio.
     */
    fun createQrResolutionSelector(
        context: Context,
        preferredSize: Size = Size(2400, 1350)
    ): ResolutionSelector {
        val aspectRatio = chooseDeviceFriendlyAspectRatio(context)

        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    aspectRatio,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )
            .setResolutionStrategy(
                ResolutionStrategy(
                    preferredSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }
}
