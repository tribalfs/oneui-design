package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import dev.oneuiproject.oneui.design.R

class StrokedTextView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : TextView(context, attributeSet) {

    private var stroke: Boolean = false
    private var strokeWidth: Float = 0f
    private var strokeColor: Int = 0
    private var isDrawingStroke: Boolean = false

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StrokedTextView) {
            stroke = getBoolean(R.styleable.StrokedTextView_textViewStroke, false)
            strokeWidth = getFloat(
                R.styleable.StrokedTextView_textViewStrokeWidth,
                0.0f
            )
            strokeColor = getColor(
                R.styleable.StrokedTextView_textViewStrokeColor,
                Color.TRANSPARENT
            )
        }
    }

    override fun invalidate() {
        if (isDrawingStroke) {
            return
        }
        super.invalidate()
    }


    public override fun onDraw(canvas: Canvas) {
        if (stroke) {
            isDrawingStroke = true
            val textColors = getTextColors()
            paint.apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidth
            }
            setTextColor(strokeColor)
            super.onDraw(canvas)

            paint.setStyle(Paint.Style.FILL)
            setTextColor(textColors)
            this.isDrawingStroke = false
        }
        super.onDraw(canvas)
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!stroke || text.isEmpty()) return

        setMeasuredDimension(
            measuredWidth + (strokeWidth.toInt()),
            measuredHeight + (strokeWidth.toInt())
        )
    }

    fun setStroke(enable: Boolean) {
        this.stroke = enable
    }

    fun setStrokeColor(color: Int) {
        this.strokeColor = color
    }

    fun setStrokeWidth(width: Int) {
        this.strokeWidth = width.toFloat()
    }
}