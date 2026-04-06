package com.finly.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class HeatmapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()
    private val colors = listOf(
        Color.parseColor("#F1F5F9"), // Low
        Color.parseColor("#DCFCE7"),
        Color.parseColor("#86EFAC"),
        Color.parseColor("#22C55E"),
        Color.parseColor("#15803D")  // High
    )

    private var intensityData = Array(4) { IntArray(7) { (0..4).random() } }
    
    fun setData(data: Array<IntArray>) {
        intensityData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val margin = 8f
        val labelWidth = 100f
        val labelHeight = 40f
        val cellCountX = 7
        val cellCountY = 4
        
        val cellWidth = (width - labelWidth - (cellCountX * margin)) / cellCountX
        val cellHeight = (height - labelHeight - (cellCountY * margin)) / cellCountY
        
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        // Draw Day Labels
        for (i in 0 until cellCountX) {
            val x = labelWidth + i * (cellWidth + margin) + cellWidth / 2
            canvas.drawText(days[i], x, 30f, headerPaint)
        }

        // Draw Rows
        for (row in 0 until cellCountY) {
            val yLabel = 30f + labelHeight + row * (cellHeight + margin) + cellHeight / 2
            canvas.drawText("Week ${row + 1}", 0f, yLabel + 10f, textPaint)
            
            for (col in 0 until cellCountX) {
                val left = labelWidth + col * (cellWidth + margin)
                val top = 30f + labelHeight + row * (cellHeight + margin)
                val right = left + cellWidth
                val bottom = top + cellHeight
                
                val intensity = intensityData[row][col].coerceIn(0, colors.size - 1)
                cellPaint.color = colors[intensity]
                
                rectF.set(left, top, right, bottom)
                canvas.drawRoundRect(rectF, 8f, 8f, cellPaint)
            }
        }
    }
}
