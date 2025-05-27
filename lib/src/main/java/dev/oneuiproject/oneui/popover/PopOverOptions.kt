package dev.oneuiproject.oneui.popover

import android.graphics.Point

/**
 * Represents the configuration options for a PopOver.
 *
 * @property allowOutsideTouch Whether to allow touches outside the PopOver to dismiss it. Defaults to false.
 * @property removeOutline Whether to remove the default outline of the PopOver. Defaults to false.
 * @property removeDefaultMargin Whether to remove the default margin of the PopOver. Defaults to false.
 * @property inheritOptions Whether the PopOver should inherit options from its parent. Defaults to true.
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
    val allowOutsideTouch: Boolean = false,
    @JvmField
    val removeOutline: Boolean = false,
    @JvmField
    val removeDefaultMargin: Boolean = false,
    @JvmField
    val inheritOptions: Boolean = true,
    /**
     * First value: landscapeConfig
     * Second value: portrait
     */
    @JvmField
    val anchor: PopOverAnchor = PopOverAnchor(Point(), Point()),
    @JvmField
    val popOverSize: PopOverSize,
    @JvmField
    val anchorPositions: PopOverPositions
)

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
){
    fun getHeightArray(): IntArray{
        return intArrayOf(heightLandscape, heightPortrait)
    }

    fun getWidthArray(): IntArray{
        return intArrayOf(widthLandscape, widthPortrait)
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
){

    fun getPointArray(): Array<Point>{
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
){
    fun getFlagArray(): IntArray{
        return intArrayOf(landscape.flag, portrait.flag)

    }
}