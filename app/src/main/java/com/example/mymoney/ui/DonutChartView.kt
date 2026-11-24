package com.example.mymoney.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

data class DonutSlice(
    val categoryId: String,
    val amount: Long,
    val color: Int
)

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var slices: List<DonutSlice> = emptyList()
    private var selectedCategoryId: String? = null
    private var totalAmount: Long = 0L
    private var onSliceClickListener: ((String) -> Unit)? = null

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val emptyRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = "#EFE6D8".toColorInt()
    }

    private val arcRect = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var ringRadius = 0f

    fun setData(
        newSlices: List<DonutSlice>,
        total: Long,
        selectedCatId: String?,
        onSliceClick: (String) -> Unit
    ) {
        this.slices = newSlices
        this.totalAmount = total
        this.selectedCategoryId = selectedCatId
        this.onSliceClickListener = onSliceClick
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val baseStroke = context.dpF(26f)
        val activeStroke = context.dpF(31f)
        val padding = activeStroke / 2f + context.dpF(4f)

        centerX = width / 2f
        centerY = height / 2f
        ringRadius = size / 2f - padding
        if (ringRadius <= 0f) return

        arcRect.set(
            centerX - ringRadius,
            centerY - ringRadius,
            centerX + ringRadius,
            centerY + ringRadius
        )

        if (slices.isEmpty() || totalAmount <= 0L) {
            emptyRingPaint.strokeWidth = baseStroke
            canvas.drawCircle(centerX, centerY, ringRadius, emptyRingPaint)
            return
        }

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.amount.toFloat() / totalAmount.toFloat()) * 360f
            val isSelected = slice.categoryId == selectedCategoryId
            arcPaint.color = slice.color
            arcPaint.strokeWidth = if (isSelected) activeStroke else baseStroke
            canvas.drawArc(arcRect, startAngle, sweep.coerceAtLeast(1.5f), false, arcPaint)
            startAngle += sweep
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && slices.isNotEmpty() && totalAmount > 0L) {
            val dx = event.x - centerX
            val dy = event.y - centerY
            val dist = sqrt(dx * dx + dy * dy)
            val hitThickness = context.dpF(24f)

            if (dist >= ringRadius - hitThickness && dist <= ringRadius + hitThickness) {
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                if (angle < 0f) angle += 360f

                var accAngle = 0f
                for (slice in slices) {
                    val sweep = (slice.amount.toFloat() / totalAmount.toFloat()) * 360f
                    if (angle >= accAngle && angle <= accAngle + sweep) {
                        onSliceClickListener?.invoke(slice.categoryId)
                        performClick()
                        return true
                    }
                    accAngle += sweep
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
