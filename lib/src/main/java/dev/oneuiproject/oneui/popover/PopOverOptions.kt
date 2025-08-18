package dev.oneuiproject.oneui.popover

import android.content.Context
import android.graphics.Point
import android.view.View.LAYOUT_DIRECTION_RTL
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletLayout

/**
 * Represents the configuration options for a PopOver.
 *
 * Prebuilt options for common pop-over configurations are available as static methods:
 * - [centerAnchored]: Returns options for a centered pop-over.
 * - [topCenterAnchored]: Returns options for a top-centered pop-over.
 * - [bottomCenterAnchored]: Returns options for a bottom-centered pop-over.
 * - [topLeftAnchored]: Returns options for a top-left anchored pop-over.
 * - [centerLeftAnchored]: Returns options for a center-left anchored pop-over.
 * - [bottomLeftAnchored]: Returns options for a bottom-left anchored pop-over.
 * - [topRightAnchored]: Returns options for a top-right anchored pop-over.
 * - [centerRightAnchored]: Returns options for a center-right anchored pop-over.
 * - [bottomRightAnchored]: Returns options for a bottom-right anchored pop-over.
 *
 * @property anchor The anchor points for the PopOver in landscape and portrait orientations.
 *                  The first value in the pair is for landscape, and the second is for portrait.
 *                  Defaults to a PopOverAnchor with both points at (0,0).
 * @property popOverSize The size of the PopOver. This is a required parameter.
 * @property anchorPositions The desired positions of the PopOver relative to its anchor. This is a required parameter.
 *
 * @see dev.oneuiproject.oneui.ktx.startPopOverActivityForResult
 * @see dev.oneuiproject.oneui.ktx.startPopOverActivity
 */
data class PopOverOptions @JvmOverloads constructor(
    @JvmField
    val anchor: PopOverAnchor = PopOverAnchor(Point(), Point()),
    @JvmField
    val popOverSize: PopOverSize,
    @JvmField
    val anchorPositions: PopOverPositions
) {
    companion object {
        /**
         * Returns a default [PopOverOptions] instance for centered pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.CENTER] for both orientations.
         *
         * @param context The context used to determine the device layout.
         * @return A [PopOverOptions] instance with default centered settings.
         */
        fun centerAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = PopOverPositions(PopOverPosition.CENTER, PopOverPosition.CENTER)
        )

        /**
         * Returns a default [PopOverOptions] instance for top-centered pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.TOP_CENTER] for both orientations.
         *
         * @param context The context used to determine the device layout.
         * @return A [PopOverOptions] instance with default top-centered settings.
         */
        fun topCenterAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = PopOverPositions(PopOverPosition.TOP_CENTER, PopOverPosition.TOP_CENTER)
        )

        /**
         * Returns a default [PopOverOptions] instance for bottom-center anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.BOTTOM_CENTER] for both orientations.
         *
         * @param context The context to use for determining layout direction and device type.
         * @return A [PopOverOptions] object for bottom-center anchored pop-over activity.
         */
        fun bottomCenterAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = PopOverPositions(PopOverPosition.BOTTOM_CENTER, PopOverPosition.BOTTOM_CENTER)
        )

        /**
         * Returns a default [PopOverOptions] instance for top-left anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.TOP_LEFT] for both orientations
         *  in LTR layouts, and to [PopOverPosition.TOP_RIGHT] in RTL layouts.
         *
         * @param context The context to use for determining layout direction and device type.
         * @return A [PopOverOptions] object for top-left anchored pop-over activity.
         */
        fun topLeftAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                PopOverPositions(PopOverPosition.TOP_RIGHT, PopOverPosition.TOP_RIGHT)
            } else {
                PopOverPositions(PopOverPosition.TOP_LEFT, PopOverPosition.TOP_LEFT)
            }
        )

        /**
         * Returns a default [PopOverOptions] instance for center-left anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.CENTER_LEFT] in LTR layouts,
         * and to [PopOverPosition.CENTER_RIGHT] in RTL layouts for both orientations.
         *
         * @param context The context to use for determining layout direction and device type.
         * @return A [PopOverOptions] object for center-left anchored pop-over activity.
         */
        fun centerLeftAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                PopOverPositions(PopOverPosition.CENTER_RIGHT, PopOverPosition.CENTER_RIGHT)
            } else {
                PopOverPositions(PopOverPosition.CENTER_LEFT, PopOverPosition.CENTER_LEFT)
            }
        )

        /**
         * Returns a default [PopOverOptions] instance for bottom-left anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.BOTTOM_LEFT] in LTR layouts,
         * and to [PopOverPosition.BOTTOM_RIGHT] in RTL layouts for both orientations.
         *
         * @param context The context to use for determining layout direction and device type.
         * @return A [PopOverOptions] object for bottom-left anchored pop-over activity.
         */
        fun bottomLeftAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                PopOverPositions(PopOverPosition.BOTTOM_RIGHT, PopOverPosition.BOTTOM_RIGHT)
            } else {
                PopOverPositions(PopOverPosition.BOTTOM_LEFT, PopOverPosition.BOTTOM_LEFT)
            }
        )

        /**
         * Returns a default [PopOverOptions] instance for top-right anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.TOP_RIGHT] for both orientations
         * in LTR layouts, and to [PopOverPosition.TOP_LEFT] in RTL layouts.
         *
         * @param context The current context, used to determine layout direction and device type.
         * @return A PopOverOptions object for top-right anchored pop-over activity.
         */
        fun topRightAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                PopOverPositions(PopOverPosition.TOP_LEFT, PopOverPosition.TOP_LEFT)
            } else {
                PopOverPositions(PopOverPosition.TOP_RIGHT, PopOverPosition.TOP_RIGHT)
            }
        )

        /**
         * Returns a default [PopOverOptions] instance for center-right anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.CENTER_RIGHT] for both orientations
         * in LTR layouts, and to [PopOverPosition.CENTER_LEFT] in RTL layouts.
         *
         * @param context The current context, used to determine layout direction and device type.
         * @return A PopOverOptions object for center-right anchored pop-over activity.
         */
        fun centerRightAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                PopOverPositions(PopOverPosition.CENTER_LEFT, PopOverPosition.CENTER_LEFT)
            } else {
                PopOverPositions(PopOverPosition.CENTER_RIGHT, PopOverPosition.CENTER_RIGHT)
            }
        )

        /**
         * Returns a default [PopOverOptions] instance for bottom-right anchored pop-over activity.
         * This uses the default pop-over size from [PopOverSize.defaultSize].
         * The anchor positions are set to [PopOverPosition.BOTTOM_RIGHT] in LTR layouts,
         * and to [PopOverPosition.BOTTOM_LEFT] in RTL layouts for both orientations.
         *
         * @param context The context to use for determining layout direction and device type.
         * @return A [PopOverOptions] object for bottom-right anchored pop-over activity.
         */
        fun bottomRightAnchored(context: Context) = PopOverOptions(
            popOverSize = PopOverSize.defaultSize(context),
            anchorPositions = if (context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                PopOverPositions(PopOverPosition.BOTTOM_LEFT, PopOverPosition.BOTTOM_RIGHT)
            } else {
                PopOverPositions(PopOverPosition.BOTTOM_RIGHT, PopOverPosition.BOTTOM_RIGHT)
            }
        )
    }
}

/**
 * Represents the size of a pop-over in both portrait and landscape orientations.
 *
 * @property heightPortrait The height of the pop-over in portrait orientation.
 * @property widthPortrait The width of the pop-over in portrait orientation.
 * @property heightLandscape The height of the pop-over in landscape orientation.
 * @property widthLandscape The width of the pop-over in landscape orientation.
 */
data class PopOverSize(
    @JvmField
    val heightPortrait: Int,
    @JvmField
    val widthPortrait: Int,
    @JvmField
    val heightLandscape: Int,
    @JvmField
    val widthLandscape: Int
) {

    /**
     * Returns an array containing the height of the pop-over in landscape and portrait orientations.
     * The first element is the landscape height, and the second is the portrait height.
     *
     * @return An IntArray with two elements: [heightLandscape, heightPortrait].
     */
    fun getHeightArray()= intArrayOf(heightLandscape, heightPortrait)

    /**
     * Returns an array containing the width of the pop-over in landscape and portrait orientations.
     * The first element is the landscape width, and the second element is the portrait width.
     *
     * @return An IntArray containing the landscape and portrait widths.
     */
    fun getWidthArray() = intArrayOf(widthLandscape, widthPortrait)

    companion object {
        const val DEFAULT_HEIGHT_PORTRAIT = 731
        const val DEFAULT_WIDTH_PORTRAIT = 360
        const val DEFAULT_HEIGHT_LANDSCAPE = 574
        const val DEFAULT_HEIGHT_LANDSCAPE_TABLET = 731
        const val DEFAULT_WIDTH_LANDSCAPE = 360

        /**
         * Creates and returns a default [PopOverSize] object with dimensions suitable for most use cases.
         *
         * The dimensions are:
         * - Portrait: 731dp height, 360dp width
         * - Landscape (Tablet): 731dp height, 360dp width
         * - Landscape (Phone): 574dp height, 360dp width
         *
         * @param context The context used to determine if the device is a tablet.
         * @return A [PopOverSize] object with default dimensions.
         */
        fun defaultSize(context: Context) = PopOverSize(
            DEFAULT_HEIGHT_PORTRAIT,
            DEFAULT_WIDTH_PORTRAIT,
            if (isTabletLayout(context.resources)) {
                DEFAULT_HEIGHT_LANDSCAPE_TABLET
            } else DEFAULT_HEIGHT_LANDSCAPE,
            DEFAULT_WIDTH_LANDSCAPE
        )
    }
}


/**
 * A data class that holds the anchor points for a popover in portrait and landscape orientations.
 *
 * @property portrait The anchor point for portrait orientation.
 * @property landscape The anchor point for landscape orientation.
 */
data class PopOverAnchor(
    @JvmField
    val portrait: Point,
    @JvmField
    val landscape: Point,
) {

    fun getPointArray(): Array<Point> {
        return arrayOf(landscape, portrait)
    }
}

/**
 * Represents the anchor positions for a PopOver in both portrait and landscape orientations.
 *
 * @property portrait The anchor position for portrait orientation.
 * @property landscape The anchor position for landscape orientation.
 */
data class PopOverPositions(
    @JvmField
    val portrait: PopOverPosition,
    @JvmField
    val landscape: PopOverPosition,
) {
    fun getFlagArray(): IntArray {
        return intArrayOf(landscape.flag, portrait.flag)

    }
}