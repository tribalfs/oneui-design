package dev.oneuiproject.oneui.popover

import android.graphics.Point

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