package com.finly.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981") // current week green
        style = Paint.Style.FILL
    }

    private val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0") // last week light gray
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F1F5F9")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var thisWeekData = listOf<Float>()
    private var lastWeekData = listOf<Float>()
    private var labels = listOf<String>()
    
    private val rectF = RectF()

    fun setData(thisWeek: List<Float>, lastWeek: List<Float>, labelList: List<String>) {
        thisWeekData = thisWeek
        lastWeekData = lastWeek
        labels = labelList
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (thisWeekData.isEmpty() || lastWeekData.isEmpty()) return

        val paddingBottom = 60f
        val paddingTop = 40f
        val width = width.toFloat()
        val height = height.toFloat() - paddingTop - paddingBottom

        val maxData = 1600f // Hardcoded specifically for Image 2 parity
        val itemWidth = width / labels.size
        val barWidth = itemWidth * 0.25f
        val spacing = 6f

        // Draw Axis Lines (mocking the Grid in Image 2)
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = paddingTop + height - (i.toFloat() / gridSteps * height)
            canvas.drawLine(0f, y, width, y, axisPaint)
        }

        labels.forEachIndexed { index, label ->
            val centerX = index * itemWidth + itemWidth / 2
            
            // Last Week Bar (left of center)
            val lastVal = lastWeekData.getOrNull(index) ?: 0f
            val lastHeight = (lastVal / maxData * height).coerceAtMost(height)
            rectF.set(centerX - barWidth - spacing, paddingTop + height - lastHeight, centerX - spacing, paddingTop + height)
            canvas.drawRoundRect(rectF, 8f, 8f, secondaryPaint)

            // This Week Bar (right of center)
            val thisVal = thisWeekData.getOrNull(index) ?: 0f
            val thisHeight = (thisVal / maxData * height).coerceAtMost(height)
            rectF.set(centerX + spacing, paddingTop + height - thisHeight, centerX + barWidth + spacing, paddingTop + height)
            canvas.drawRoundRect(rectF, 8f, 8f, primaryPaint)

            // Label
            canvas.drawText(label, centerX, height + paddingTop + 45f, labelPaint)
        }
    }
}
