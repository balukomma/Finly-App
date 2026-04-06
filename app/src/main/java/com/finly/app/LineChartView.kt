package com.finly.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F1F5F9")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 28f
        textAlign = Paint.Align.LEFT
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var dataPoints = listOf<Float>()
    private var labels = listOf<String>()
    
    fun setData(points: List<Float>, labelList: List<String>) {
        dataPoints = points
        labels = labelList
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val paddingLeft = 100f
        val paddingBottom = 60f
        val paddingTop = 40f
        val paddingRight = 40f
        
        val width = width.toFloat() - paddingLeft - paddingRight
        val height = height.toFloat() - paddingTop - paddingBottom

        val maxData = 12000f // Hardcoded or dynamic based on data
        
        // Draw Grid Lines and Y-Labels
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = paddingTop + height - (i.toFloat() / gridSteps * height)
            canvas.drawLine(paddingLeft, y, paddingLeft + width, y, gridPaint)
            
            val labelValue = (i * 3).toString() + "k"
            canvas.drawText("₹$labelValue", 20f, y + 10f, textPaint)
        }

        val stepX = width / (dataPoints.size.coerceAtLeast(2) - 1).coerceAtLeast(1).toFloat()
        val path = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = paddingLeft + index * stepX
            val y = paddingTop + height - (value / maxData * height).coerceAtMost(height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint)
        
        // Draw Points and X-Labels
        dataPoints.forEachIndexed { index, value ->
            val x = paddingLeft + index * stepX
            val y = paddingTop + height - (value / maxData * height).coerceAtMost(height)
            
            canvas.drawCircle(x, y, 12f, pointPaint)
            
            if (index < labels.size) {
                canvas.drawText(labels[index], x, paddingTop + height + 45f, labelPaint)
            }
        }
    }
}
