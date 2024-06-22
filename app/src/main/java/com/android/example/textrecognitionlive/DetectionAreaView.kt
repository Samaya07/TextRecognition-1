package com.android.example.textrecognitionlive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DetectionAreaView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint: Paint = Paint().apply {
        color = 0x55FF0000 // Semi-transparent red color
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val detectionRect = RectF()

    fun setDetectionArea(left: Float, top: Float, right: Float, bottom: Float) {
        detectionRect.set(left, top, right, bottom)
        invalidate()
    }
    fun getDetectionArea(): RectF{
        return detectionRect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(detectionRect, paint)
    }
}


