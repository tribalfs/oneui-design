@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.qr.app.internal

import android.graphics.Point
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.hypot


internal fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = bx - ax
    val dy = by - ay
    return hypot(dx, dy)
}

internal inline fun distance(a: PointF, b: PointF): Float =
    distance(a.x, a.y, b.x, b.y)


/**
 * Returns the QR angle in degrees, based on the first edge (p0 → p1).
 * 0° means "pointing right", positive angles are counterclockwise.
 */
internal fun getAngleDegrees(cornerPoints: Array<Point>?): Float? {
    val points = cornerPoints ?: return null
    if (points.size < 2) return null

    val p0 = points[0] // top-left-ish
    val p1 = points[1] // top-right-ish

    val dx = (p1.x - p0.x).toFloat()
    val dy = (p1.y - p0.y).toFloat()

    return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
}
