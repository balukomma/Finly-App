package com.finly.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#41A582")
    }
    
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6C757D")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val rectF = RectF()
    private var animationProgress = 0f
    private var selectedIndex = -1
    
    var data: List<Float> = listOf(0.32f, 0.52f, 0.38f, 0.82f, 0.48f, 0.62f, 0.28f)
        set(value) {
            field = value
            animateBars()
        }
        
    var labels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        set(value) {
            field = value
            invalidate()
        }

    init {
        animateBars()
    }

    private fun animateBars() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val itemWidth = width.toFloat() / data.size
            val x = event.x
            val index = (x / itemWidth).toInt()
            if (index in data.indices) {
                if (selectedIndex != index) {
                    selectedIndex = index
                    invalidate()
                }
            }
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            // Uncomment if you want to clear selection on release
            // selectedIndex = -1
            // invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val barWidthRatio = 0.6f
        val itemWidth = width.toFloat() / data.size
        val barWidth = itemWidth * barWidthRatio
        val maxBarHeight = height.toFloat() - 100f // Adjusted for labels and tooltip
        
        data.forEachIndexed { index, value ->
            val left = index * itemWidth + (itemWidth - barWidth) / 2
            val currentHeight = value * maxBarHeight * animationProgress
            val top = height - 80f - currentHeight
            val right = left + barWidth
            val bottom = height - 80f
            
            // Draw bar
            paint.color = if (index == selectedIndex) Color.parseColor("#10B981") else Color.parseColor("#41A582")
            rectF.set(left, top, right, bottom)
            canvas.drawRoundRect(rectF, 20f, 20f, paint)
            
            // Draw label
            canvas.drawText(labels[index], left + barWidth / 2f, height - 20f, labelPaint)

            // Draw tooltip if selected
            if (index == selectedIndex) {
                val percentage = (value * 100).toInt()
                canvas.drawText("$percentage%", left + barWidth / 2f, top - 15f, tooltipPaint)
            }
        }
    }
}
