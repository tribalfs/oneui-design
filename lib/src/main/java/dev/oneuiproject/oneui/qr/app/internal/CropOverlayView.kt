package dev.oneuiproject.oneui.qr.app.internal

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ------------------------------------------------------------------------------------
    // Config
    // ------------------------------------------------------------------------------------

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val frameStrokeWidth = dp(0.3f)
    private val handleStrokeWidth = dp(2f)
    private val handleLength = dp(26f)
    private val touchSlop = dp(24f)
    private val minCropSize = dp(80f)

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = frameStrokeWidth
        strokeCap = Paint.Cap.BUTT
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = handleStrokeWidth
        strokeCap = Paint.Cap.SQUARE
    }

    private val clipPath = Path()

    // ------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------

    private val cropRect = RectF()
    private var limitBounds: RectF? = null

    /**
     * If true, we have a cropRect that is ready to draw.
     * We do not draw anything until initialized = true => no flicker.
     */
    private var initialized = false

    /**
     * If non-null, we will apply this as soon as we have valid limitBounds.
     * This lets CropOverlayView handle restoration internally with no intermediate reset state.
     */
    private var pendingRestore: NormRect? = null

    // ------------------------------------------------------------------------------------
    // Normalized rect (persistent)
    // ------------------------------------------------------------------------------------

    data class NormRect(val l: Float, val t: Float, val r: Float, val b: Float) {
        fun isValid() = l < r && t < b
        fun clamp() = NormRect(
            l.coerceIn(0f, 1f),
            t.coerceIn(0f, 1f),
            r.coerceIn(0f, 1f),
            b.coerceIn(0f, 1f)
        )
    }

    /**
     * Optional convenience: returns the normalized crop relative to current bounds.
     */
    fun getCropRectNormalized(): NormRect? {
        val b = limitBounds ?: return null

        val r = RectF(cropRect)
        if (!r.intersect(b)) return null

        val w = b.width()
        val h = b.height()
        if (w <= 0f || h <= 0f) return null

        return NormRect(
            (r.left - b.left) / w,
            (r.top - b.top) / h,
            (r.right - b.left) / w,
            (r.bottom - b.top) / h
        ).clamp()
    }

    // ------------------------------------------------------------------------------------
    // Public API (unchanged signatures where possible)
    // ------------------------------------------------------------------------------------

    /**
     * Sets the allowed crop area. Crop will never extend outside this region.
     * This is your decodedImageBounds.
     *
     * IMPORTANT: This method does NOT force a reset immediately if a restore is pending.
     * It will apply pendingRestore first (if any). Otherwise it will default-initialize once.
     */
    fun setImageBounds(bounds: RectF, relativeCropBounds: NormRect? = null) {
        pendingRestore = relativeCropBounds ?: getCropRectNormalized()
        limitBounds = bounds
        tryApplyPendingOrInit()
        invalidate()
    }

    fun getCropRect(): RectF = RectF(cropRect)

    fun reset() {
        initialized = false
        pendingRestore = null
        limitBounds = null
        invalidate()
    }
    // ------------------------------------------------------------------------------------
    // Internal init / restore (the key part)
    // ------------------------------------------------------------------------------------

    private fun tryApplyPendingOrInit() {
        val b = limitBounds ?: return
        if (b.isEmpty) return

        // 1) Apply pending restore first (no flicker path)
        val pending = pendingRestore
        if (pending != null) {
            applyNormalizedInternal(pending, b)
            pendingRestore = null
            initialized = true
            return
        }

        // 2) If already initialized, just clamp to new bounds (e.g., rotation/layout change)
        if (initialized) {
            clampToBounds(cropRect, b)
            return
        }

        // 3) Default init (only if nothing to restore)
        resetInternal(b)
        initialized = true
    }

    private fun applyNormalizedInternal(norm: NormRect, b: RectF) {
        val n = norm.clamp()
        val w = b.width()
        val h = b.height()
        cropRect.set(
            b.left + n.l * w,
            b.top + n.t * h,
            b.left + n.r * w,
            b.top + n.b * h
        )
        enforceMinSize(cropRect)
        clampToBounds(cropRect, b)
    }

    private fun resetInternal(b: RectF) {
        val size = min(b.width(), b.height()) * 0.9f
        val cx = b.centerX()
        val cy = b.centerY()
        cropRect.set(
            cx - size / 2f,
            cy - size / 2f,
            cx + size / 2f,
            cy + size / 2f
        )
        enforceMinSize(cropRect)
        clampToBounds(cropRect, b)
    }

    // ------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!initialized || cropRect.isEmpty) return

        clipPath.reset()
        clipPath.fillType = Path.FillType.WINDING
        clipPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        clipPath.addRect(cropRect, Path.Direction.CCW)
        canvas.drawPath(clipPath, dimPaint)

        val inset = max(0f, (handleStrokeWidth - frameStrokeWidth) / 2f)
        val frameRect = RectF(cropRect).apply { inset(inset, inset) }
        canvas.drawRect(frameRect, framePaint)

        drawCornerHandles(canvas, cropRect)
    }

    private fun drawCornerHandles(canvas: Canvas, r: RectF) {
        val l = r.left
        val t = r.top
        val rr = r.right
        val b = r.bottom
        val len = handleLength

        canvas.drawLine(l, t, l + len, t, handlePaint)
        canvas.drawLine(l, t, l, t + len, handlePaint)

        canvas.drawLine(rr - len, t, rr, t, handlePaint)
        canvas.drawLine(rr, t, rr, t + len, handlePaint)

        canvas.drawLine(l, b - len, l, b, handlePaint)
        canvas.drawLine(l, b, l + len, b, handlePaint)

        canvas.drawLine(rr, b - len, rr, b, handlePaint)
        canvas.drawLine(rr - len, b, rr, b, handlePaint)
    }

    // ------------------------------------------------------------------------------------
    // Touch handling
    // ------------------------------------------------------------------------------------

    private enum class DragMode {
        NONE, MOVE,
        LEFT, TOP, RIGHT, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private var dragMode = DragMode.NONE
    private var lastX = 0f
    private var lastY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!initialized) return false

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = hitTest(x, y)
                if (dragMode == DragMode.NONE) return false
                lastX = x
                lastY = y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX
                val dy = y - lastY

                when (dragMode) {
                    DragMode.MOVE -> moveRect(dx, dy)
                    DragMode.LEFT, DragMode.RIGHT,
                    DragMode.TOP, DragMode.BOTTOM,
                    DragMode.TOP_LEFT, DragMode.TOP_RIGHT,
                    DragMode.BOTTOM_LEFT, DragMode.BOTTOM_RIGHT -> resizeRect(dx, dy)
                    else -> Unit
                }

                lastX = x
                lastY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragMode = DragMode.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTest(x: Float, y: Float): DragMode {
        val l = cropRect.left
        val t = cropRect.top
        val r = cropRect.right
        val b = cropRect.bottom

        val nearLeft = abs(x - l) <= touchSlop
        val nearRight = abs(x - r) <= touchSlop
        val nearTop = abs(y - t) <= touchSlop
        val nearBottom = abs(y - b) <= touchSlop

        if (nearLeft && nearTop) return DragMode.TOP_LEFT
        if (nearRight && nearTop) return DragMode.TOP_RIGHT
        if (nearLeft && nearBottom) return DragMode.BOTTOM_LEFT
        if (nearRight && nearBottom) return DragMode.BOTTOM_RIGHT

        if (nearLeft) return DragMode.LEFT
        if (nearRight) return DragMode.RIGHT
        if (nearTop) return DragMode.TOP
        if (nearBottom) return DragMode.BOTTOM

        return if (cropRect.contains(x, y)) DragMode.MOVE else DragMode.NONE
    }

    private fun moveRect(dx: Float, dy: Float) {
        cropRect.offset(dx, dy)
        limitBounds?.let { clampToBoundsAsWhole(cropRect, it) }
    }

    private fun resizeRect(dx: Float, dy: Float) {
        val b = limitBounds ?: return
        val r = cropRect

        when (dragMode) {
            DragMode.LEFT -> r.left = (r.left + dx).coerceIn(b.left, r.right - minCropSize)
            DragMode.TOP -> r.top = (r.top + dy).coerceIn(b.top, r.bottom - minCropSize)
            DragMode.RIGHT -> r.right = (r.right + dx).coerceIn(r.left + minCropSize, b.right)
            DragMode.BOTTOM -> r.bottom = (r.bottom + dy).coerceIn(r.top + minCropSize, b.bottom)

            DragMode.TOP_LEFT -> {
                r.top = (r.top + dy).coerceIn(b.top, r.bottom - minCropSize)
                r.left = (r.left + dx).coerceIn(b.left, r.right - minCropSize)
            }

            DragMode.BOTTOM_LEFT -> {
                r.bottom = (r.bottom + dy).coerceIn(r.top + minCropSize, b.bottom)
                r.left = (r.left + dx).coerceIn(b.left, r.right - minCropSize)
            }

            DragMode.TOP_RIGHT -> {
                r.top = (r.top + dy).coerceIn(b.top, r.bottom - minCropSize)
                r.right = (r.right + dx).coerceIn(r.left + minCropSize, b.right)
            }

            DragMode.BOTTOM_RIGHT -> {
                r.bottom = (r.bottom + dy).coerceIn(r.top + minCropSize, b.bottom)
                r.right = (r.right + dx).coerceIn(r.left + minCropSize, b.right)
            }
            else -> Unit
        }

        enforceMinSize(r)
        clampToBounds(r, b)
    }

    // ------------------------------------------------------------------------------------
    // Bounds / min size helpers
    // ------------------------------------------------------------------------------------

    private fun clampToBoundsAsWhole(r: RectF, b: RectF) {
        var dx = 0f
        var dy = 0f

        if (r.left < b.left) dx = b.left - r.left
        if (r.right > b.right) dx = b.right - r.right
        if (r.top < b.top) dy = b.top - r.top
        if (r.bottom > b.bottom) dy = b.bottom - r.bottom

        r.offset(dx, dy)
    }

    private fun clampToBounds(r: RectF, b: RectF) {
        // same as clampToBoundsAsWhole for this rect type
        clampToBoundsAsWhole(r, b)
    }

    private fun enforceMinSize(r: RectF) {
        val w = r.width()
        val h = r.height()

        if (w < minCropSize) {
            val cx = r.centerX()
            r.left = cx - minCropSize / 2f
            r.right = cx + minCropSize / 2f
        }
        if (h < minCropSize) {
            val cy = r.centerY()
            r.top = cy - minCropSize / 2f
            r.bottom = cy + minCropSize / 2f
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
