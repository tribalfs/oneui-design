package dev.oneuiproject.oneui.qr.app.internal

/**
 * Provides hysteresis-based orientation snapping for UI components that should behave
 * as if the device supports portrait and both landscape directions — even when the
 * hosting Activity itself is locked to portrait.
 *
 * This class:
 * - Accepts raw 0–359° sensor orientation values.
 * - Applies *different thresholds* when entering or exiting landscape modes.
 * - Tracks internal state so the UI does not jitter around threshold boundaries.
 * - Returns ONLY 3 stable orientations:
 *    - 0°   → Portrait
 *    - 90°  → Landscape Right
 *    - -90° → Landscape Left
 *
 * Usage:
 * ```
 * val hysteresis = OrientationHysteresis()
 *
 * orientationListener = object : OrientationEventListener(context) {
 *     override fun onOrientationChanged(deg: Int) {
 *         val rotation = hysteresis.update(deg)
 *         if (rotation != null) {
 *             qrCodeScannerView.updateOrientation(rotation)
 *         }
 *     }
 * }
 * ```
 */
internal class OrientationHysteresis {

    /**
     * Logical UI orientation state (not tied to Activity/system orientation).
     */
    private enum class UiOrientation { PORTRAIT, LANDSCAPE_RIGHT, LANDSCAPE_LEFT }

    private var state: UiOrientation = UiOrientation.PORTRAIT

    /**
     * Process a raw orientation reading (0–359 degrees), apply hysteresis,
     * and return FINAL snapped UI rotation (0°, +90°, -90°).
     *
     * Returns:
     * - Float rotation in degrees → when UI orientation changes
     * - null → when no change
     */
    fun update(orientation: Int): Float? {
        if (orientation == ORIENTATION_UNKNOWN) return null

        return when (state) {
            UiOrientation.PORTRAIT -> handlePortrait(orientation)
            UiOrientation.LANDSCAPE_RIGHT -> handleLandscapeRight(orientation)
            UiOrientation.LANDSCAPE_LEFT -> handleLandscapeLeft(orientation)
        }
    }

    private fun handlePortrait(o: Int): Float? {
        // PORTRAIT → LANDSCAPE RIGHT (near +90°)
        if (o in 65..120) {
            state = UiOrientation.LANDSCAPE_RIGHT
            return -90f
        }

        // PORTRAIT → LANDSCAPE LEFT (near -90° → 270°)
        if (o in 240..295) {
            state = UiOrientation.LANDSCAPE_LEFT
            return 90f
        }

        return null
    }

    private fun handleLandscapeRight(o: Int): Float? {
        // LANDSCAPE RIGHT → PORTRAIT (must be clearly upright)
        val nearPortrait = o in 0..30
        if (nearPortrait) {
            state = UiOrientation.PORTRAIT
            return 0f
        }

        // LANDSCAPE RIGHT → LANDSCAPE LEFT (user flips to other side)
        if (o in 240..295) {
            state = UiOrientation.LANDSCAPE_LEFT
            return 90f
        }

        return null
    }

    private fun handleLandscapeLeft(o: Int): Float? {
        // LANDSCAPE LEFT → PORTRAIT
        val nearPortrait = o in 0..30
        if (nearPortrait) {
            state = UiOrientation.PORTRAIT
            return 0f
        }

        // LANDSCAPE LEFT → LANDSCAPE RIGHT
        if (o in 65..120) {
            state = UiOrientation.LANDSCAPE_RIGHT
            return -90f
        }

        return null
    }

    companion object {
        const val ORIENTATION_UNKNOWN = -1
    }
}
