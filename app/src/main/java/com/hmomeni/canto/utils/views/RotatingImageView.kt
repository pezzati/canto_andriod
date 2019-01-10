package com.hmomeni.canto.utils.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.ImageView

class RotatingImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var rotationDegs = 0f

    fun startRotation() {
        postDelayed({
            rotationDegs += .3f
            invalidate()
            startRotation()
        }, 50)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(rotationDegs, measuredWidth / 2f, measuredHeight / 2f)
        canvas.scale(1.5f, 1.5f, measuredWidth / 2f, measuredHeight / 2f)
        super.onDraw(canvas)
        canvas.restore()
    }
}