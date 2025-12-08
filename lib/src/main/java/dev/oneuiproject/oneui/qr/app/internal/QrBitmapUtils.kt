@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.qr.app.internal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.camera.core.internal.utils.ImageUtil.rotateBitmap


/**
 * Takes a frame bitmap and the detected Barcode and returns
 * an upright, cropped QR code bitmap, or null on failure.
 */
internal fun Bitmap.cropToUprightSquare(cornerPoints: Array<Point>?): Bitmap? {
    val points = cornerPoints ?: return null
    if (points.size < 4) return null  // need 4 corners

    // Use first 4 points
    val p0 = points[0]  // top-left-ish
    val p1 = points[1]  // top-right-ish
    val p2 = points[2]  // bottom-right-ish
    val p3 = points[3]  // bottom-left-ish

    // Find side length of output square (min edge length)
    val side = listOf(
        distance(p0.x.toFloat(), p0.y.toFloat(), p1.x.toFloat(), p1.y.toFloat()),
        distance(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat()),
        distance(p2.x.toFloat(), p2.y.toFloat(), p3.x.toFloat(), p3.y.toFloat()),
        distance(p3.x.toFloat(), p3.y.toFloat(), p0.x.toFloat(), p0.y.toFloat())
    ).maxOrNull()?.toInt()?.coerceAtLeast(1) ?: return null

    val src = floatArrayOf(
        p0.x.toFloat(), p0.y.toFloat(),
        p1.x.toFloat(), p1.y.toFloat(),
        p2.x.toFloat(), p2.y.toFloat(),
        p3.x.toFloat(), p3.y.toFloat()
    )

    // Desired upright square (0,0) → (side,side)
    val dst = floatArrayOf(
        0f, 0f,                      // top-left
        side.toFloat(), 0f,          // top-right
        side.toFloat(), side.toFloat(), // bottom-right
        0f, side.toFloat()           // bottom-left
    )

    val warpMatrix = Matrix().apply {
        // This takes the quad (src) and maps it into a perfect square (dst)
        setPolyToPoly(src, 0, dst, 0, 4)
    }

    val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    canvas.drawBitmap(this, warpMatrix, paint)
    return out
}


/**
 * Converts a CameraX ImageProxy into a correctly oriented Bitmap that
 * visually matches what the user sees in the PreviewView
 */
@SuppressLint("RestrictedApi")
internal fun ImageProxy.toFrameBitmap() = rotateBitmap(toBitmap(), imageInfo.rotationDegrees)

internal fun getTargetRect(
    cornerPoints: Array<Point>?,
    boundingBox: Rect?,
    previewWidth: Float,
    previewHeight: Float,
    frameBitmap: Bitmap,
    minSize: Float
): RectF {

    val scaleX = previewWidth / frameBitmap.width.toFloat()
    val scaleY = previewHeight / frameBitmap.height.toFloat()

    return if (cornerPoints != null && cornerPoints.size >= 4) {
        // Map cornerPoints into PreviewView coordinates
        val mappedPoints = cornerPoints.map { p ->
            PointF(
                p.x.toFloat() * scaleX,
                p.y.toFloat() * scaleY
            )
        }

        // Compute center from all 4 corners
        val cx = mappedPoints.map { it.x }.average().toFloat()
        val cy = mappedPoints.map { it.y }.average().toFloat()

        //Use actual side lengths (top and left edges) to estimate QR size
        val sideTop = distance(mappedPoints[0], mappedPoints[1]) // p0→p1
        val sideLeft = distance(mappedPoints[0], mappedPoints[3]) // p0→p3
        val halfSide = maxOf(sideTop, sideLeft, minSize) / 2f

        RectF(
            cx - halfSide,
            cy - halfSide,
            cx + halfSide,
            cy + halfSide
        )
    } else {
        if (boundingBox != null) {
            val mapped = RectF(
                boundingBox.left * scaleX,
                boundingBox.top * scaleY,
                boundingBox.right * scaleX,
                boundingBox.bottom * scaleY
            )

            val halfSide = maxOf(mapped.width(), mapped.height() * .95f, minSize) / 2
            val cx = mapped.centerX()
            val cy = mapped.centerY()

            RectF(
                cx - halfSide,
                cy - halfSide,
                cx + halfSide,
                cy + halfSide
            )
        } else {
            RectF()
        }
    }
}
