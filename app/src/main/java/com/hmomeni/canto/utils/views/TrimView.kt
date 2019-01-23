package com.hmomeni.canto.utils.views

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.getBitmapFromVectorDrawable
import timber.log.Timber

class TrimView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#99ffffff")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val whitePaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
//        color = Color.WHITE
        isAntiAlias = true
    }
    private val progressPaint = Paint().apply {
        color = Color.parseColor("#007AFF")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val anchorPaint = Paint().apply {
        color = Color.parseColor("#FB861F")
        isAntiAlias = true
    }

    private val leftBracket = getBitmapFromVectorDrawable(context, R.drawable.ic_bracket_left)
    private val rightBracket = getBitmapFromVectorDrawable(context, R.drawable.ic_bracket_right)
    private val bgLine = RectF()
    private val mainLine = RectF()
    private val progressLineActual = RectF()
    private val progressLine = RectF()
    private val leftAnchor = RectF()
    private val rightAnchor = RectF()
    private val mainLineHeight = dpToPx(8)
    private var anchorWidth: Float = 0f
    private val radius = dpToPx(5).toFloat()

    var onTrimChangeListener: TrimChangeListener? = null

    var max: Int = 100

    var progress = 0
        set(value) {
            field = value
            calculateProgress(true)
        }

    var trim: Int = max / 3
        set(value) {
            field = value
            calculateLeftandRight()
        }
    var trimStart: Int = 0
    var minTrim: Int = 0
    var maxTrim: Int = max

    private var maxPx = 0

    private val anchorCompensate = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (measuredWidth > 0 && measuredHeight > 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            if (anchorWidth == 0f) {
                anchorWidth = MeasureSpec.getSize(heightMeasureSpec).toFloat()
            }
            maxPx = (MeasureSpec.getSize(widthMeasureSpec) - (2 * anchorWidth)).toInt()
            bgLine.set(
                    anchorWidth,
                    (MeasureSpec.getSize(heightMeasureSpec) / 2 - mainLineHeight).toFloat(),
                    MeasureSpec.getSize(widthMeasureSpec) - anchorWidth,
                    (MeasureSpec.getSize(heightMeasureSpec) / 2 + mainLineHeight).toFloat()
            )

            mainLine.set(
                    -anchorCompensate,
                    (MeasureSpec.getSize(heightMeasureSpec) / 2 - mainLineHeight).toFloat(),
                    -anchorCompensate,
                    (MeasureSpec.getSize(heightMeasureSpec) / 2 + mainLineHeight).toFloat()
            )
            progressLineActual.set(
                    -anchorCompensate,
                    (MeasureSpec.getSize(heightMeasureSpec) / 2 - mainLineHeight).toFloat(),
                    -anchorCompensate,
                    (MeasureSpec.getSize(heightMeasureSpec) / 2 + mainLineHeight).toFloat()
            )
            progressLine.set(progressLineActual)
            leftAnchor.set(0f, 0f, 0f, MeasureSpec.getSize(heightMeasureSpec).toFloat())
            rightAnchor.set(
                    0f,
                    0f,
                    0f,
                    MeasureSpec.getSize(heightMeasureSpec).toFloat()
            )

            calculateLeftandRight(false)
            calculateProgress(false)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(bgLine, radius, radius, bgPaint)
        canvas.drawRect(mainLine, whitePaint)
        canvas.drawRoundRect(progressLine, radius, radius, progressPaint)
        canvas.drawRoundRect(leftAnchor, radius, radius, anchorPaint)
        canvas.drawRoundRect(rightAnchor, radius, radius, anchorPaint)
        canvas.drawBitmap(leftBracket, null, leftAnchor, null)
        canvas.drawBitmap(rightBracket, null, rightAnchor, null)
    }

    private var captured: Captured = Captured.WHOLE

    private var initTrim = 0
    private var initTrimStart = 0
    private var initx = 0f
    private var initrx = 0f
    private var initlx = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initTrim = trim
                initTrimStart = trimStart
                initx = event.x
                captured = when {
                    rightAnchor.contains(event.x, event.y) -> {
                        initrx = rightAnchor.left
                        Captured.RIGHT
                    }
                    leftAnchor.contains(event.x, event.y) -> {
                        initlx = leftAnchor.left
                        Captured.LEFT
                    }
                    else -> {
                        initrx = rightAnchor.left
                        initlx = leftAnchor.left
                        Captured.WHOLE
                    }
                }
                onTrimChangeListener?.onDragStarted(trimStart, trim)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initx
                when (captured) {
                    Captured.LEFT -> {
                        val newx = initlx + dx
                        val newTrimStart = initTrimStart + (dx * max / (maxPx)).toInt()
                        val newTrim = initTrim - newTrimStart + initTrimStart
                        if (
                                newTrim in minTrim..maxTrim
                                && newx >= 0
                        ) {
                            trimStart = newTrimStart
                            trim = newTrim
                            calculateLeftandRight()
                            onTrimChangeListener?.onLeftEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.RIGHT -> {
                        val newx = initrx + dx
                        val newTrim = initTrim + (dx * max / (maxPx)).toInt()
                        if (
                                newTrim in minTrim..maxTrim
                                && newx + anchorWidth <= measuredWidth
                        ) {
                            trim = newTrim
                            if (progress > trim) {
                                progress = trim
                            }
                            calculateLeftandRight()
                            onTrimChangeListener?.onRightEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.WHOLE -> {
                        if (initrx + dx + anchorWidth <= measuredWidth && initlx + dx >= 0) {
                            trimStart = initTrimStart + (dx * max / (measuredWidth - 2 * anchorWidth)).toInt()
                            calculateLeftandRight()
                            onTrimChangeListener?.onRangeChanged(trimStart, trim)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onTrimChangeListener?.onDragStopped(trimStart, trim)
            }
        }

        return true
    }

    private fun calculateLeftandRight(invalidate: Boolean = true) {
        val trimStartPx = trimStart * maxPx / max
        val trimPx = trim * maxPx / max

        leftAnchor.left = trimStartPx.toFloat()
        leftAnchor.right = leftAnchor.left + anchorWidth
        mainLine.left = leftAnchor.right - anchorCompensate

        rightAnchor.left = (trimStartPx + trimPx).toFloat() + anchorWidth
        rightAnchor.right = rightAnchor.left + anchorWidth
        mainLine.right = rightAnchor.left + anchorCompensate

        calculateProgress(false)

//        report()

        if (invalidate)
            invalidate()
    }

    private fun calculateProgress(invalidate: Boolean) {
        val progressPx = progress * maxPx / max
        progressLineActual.left = mainLine.left
        progressLine.left = mainLine.left - 10
        progressLineActual.right = progressLineActual.left + progressPx + anchorCompensate
        progressLine.right = progressLineActual.right + 10

        if (invalidate)
            invalidate()
    }

    private fun report() {
        Timber.d("trimStart=%d, trim=%d, progress=%d", trimStart, trim, progress)
    }

    enum class Captured {
        LEFT, RIGHT, WHOLE
    }

    abstract class TrimChangeListener {
        open fun onDragStarted(trimStart: Int, trim: Int) {}
        open fun onLeftEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRightEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRangeChanged(trimStart: Int, trim: Int) {}
        open fun onDragStopped(trimStart: Int, trim: Int) {}
    }

    private fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()

}