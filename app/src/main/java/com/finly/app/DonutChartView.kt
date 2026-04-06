package com.finly.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 40f
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()
    private var animationProgress = 0f
    
    var data: List<Float> = listOf(0.4f, 0.3f, 0.3f)
        set(value) {
            field = value
            animateChart()
        }
        
    var colors: List<Int> = listOf(Color.parseColor("#FF4D4D"), Color.parseColor("#9575CD"), Color.parseColor("#4DB6AC"), Color.parseColor("#FFD54F"))
        set(value) {
            field = value
            invalidate()
        }

    init {
        animateChart()
    }

    private fun animateChart() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { 
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val size = Math.min(width, height).toFloat()
        if (size <= 0f) return

        val margin = paint.strokeWidth / 2f
        rectF.set(margin, margin, size - margin, size - margin)
        
        var startAngle = -90f
        val total = data.sum()
        if (total <= 0f) return
        
        data.forEachIndexed { index, value ->
            paint.color = colors[index % colors.size]
            val sweepAngle = (value / total) * 360f * animationProgress
            canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
            startAngle += sweepAngle
        }
    }
}
