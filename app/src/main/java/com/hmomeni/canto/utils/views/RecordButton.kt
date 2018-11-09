package com.hmomeni.canto.utils.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.hmomeni.canto.utils.dpToPx

class RecordButton : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val outerPaint = Paint().apply {
        color = Color.parseColor("#99FFFFFF")
        isAntiAlias = true
        strokeWidth = radiusInnerOuterDiff.toFloat()
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val innerPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val recordPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = dpToPx(14).toFloat()
    }

    var mode: Mode = Mode.Idle
        set(value) {
            field = value
            requestLayout()
        }

    private var cx: Float = 0f
    private var cy: Float = 0f
    private var radius: Float = 0f
    private val recordRect = RectF()
    private val arcRect = RectF()
    private val textBounds = Rect()
    var progress = 20
        set(value) {
            field = value
            requestLayout()
        }

    private val radiusInnerOuterDiff = dpToPx(10)
    private val recordRadius = dpToPx(5).toFloat()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredHeight > 0 && measuredWidth > 0) {
            cx = measuredWidth / 2f
            cy = measuredHeight / 2f
            radius = Math.min(measuredHeight, measuredWidth) / 2 * 0.95f
            recordRect.set(cx - recordRadius, cy - recordRadius, cx + recordRadius, cy + recordRadius)
            arcRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        when (mode) {
            Mode.Loading -> {
                canvas.drawArc(arcRect, 0f, 360f * (progress.toFloat() / 100f), true, outerPaint)
                canvas.drawCircle(cx, cy, radius - radiusInnerOuterDiff, innerPaint)
                val text = "$progress%"
                textPaint.getTextBounds(text, 0, text.length, textBounds)
                canvas.drawText(text, cx, cy - textBounds.exactCenterY(), textPaint)
            }
            Mode.Ready -> {
                canvas.drawCircle(cx, cy, radius, outerPaint)
                canvas.drawCircle(cx, cy, radius - radiusInnerOuterDiff, innerPaint)
                canvas.drawCircle(cx, cy, recordRadius, recordPaint)
            }
            Mode.Recording -> {
                canvas.drawCircle(cx, cy, radius, outerPaint)
                canvas.drawCircle(cx, cy, radius - radiusInnerOuterDiff, innerPaint)
                canvas.drawRect(recordRect, recordPaint)
            }
            Mode.Idle -> {
                canvas.drawCircle(cx, cy, radius, outerPaint)
                canvas.drawCircle(cx, cy, radius - radiusInnerOuterDiff, innerPaint)
            }
        }
    }

    enum class Mode {
        Loading, Ready, Recording, Idle
    }
}