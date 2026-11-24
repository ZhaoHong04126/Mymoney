package com.example.mymoney.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

object UiColors {
    val BG_SCREEN = "#FAF5EC".toColorInt()
    val BG_CARD = "#FFFDF8".toColorInt()
    val BG_INPUT = "#F6F0E4".toColorInt()
    val BG_DAY_HEADER = "#F9F2E6".toColorInt()
    val DIVIDER = "#F3ECE0".toColorInt()

    val AMBER_PRIMARY = "#EE9B27".toColorInt()
    val AMBER_DARK = "#D68111".toColorInt()
    val AMBER_LIGHT = "#FCE8C3".toColorInt()
    val AMBER_TEXT_DARK = "#9A5B06".toColorInt()

    val HERO_GRAD_START = "#FFF7E5".toColorInt()
    val HERO_GRAD_END = "#FCE5BA".toColorInt()
    val HERO_BORDER = "#F3D59B".toColorInt()

    val EXPENSE = "#DF6A4E".toColorInt()
    val EXPENSE_BG = "#FCECE8".toColorInt()
    val EXPENSE_BORDER = "#F2C6BC".toColorInt()
    val INCOME = "#46937E".toColorInt()
    val INCOME_BG = "#E5F4EF".toColorInt()
    val TRANSFER = "#8E6E53".toColorInt()
    val TRANSFER_BG = "#F2EBE3".toColorInt()

    val WARN_BG = "#FFF1D6".toColorInt()
    val WARN_TEXT = "#C87609".toColorInt()
    val BANNER_BG = "#FFF9EB".toColorInt()
    val BANNER_BORDER = "#F4D89E".toColorInt()

    val DARK_NW_START = "#4E3E30".toColorInt()
    val DARK_NW_END = "#382B20".toColorInt()
    val DARK_NW_GOLD = "#F9C971".toColorInt()

    val TEXT_MAIN = "#3D3127".toColorInt()
    val TEXT_SECONDARY = "#7B6B5D".toColorInt()
    val TEXT_MUTED = "#A69688".toColorInt()
    val BORDER_WARM = "#EAE0D1".toColorInt()
}

fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

fun Context.dpF(value: Float): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        resources.displayMetrics
    )

fun formatCurrency(amount: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale.TAIWAN)
    val absStr = nf.format(abs(amount))
    return if (amount < 0) "-$$absStr" else "$$absStr"
}

fun formatYmLabel(ym: String): String {
    val parts = ym.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull()
    val m = parts.getOrNull(1)?.toIntOrNull()
    return if (y != null && m != null) "${y}年 ${m}月" else ym
}

fun formatDayTitle(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val weekZh = when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> "日"
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.TUESDAY -> "二"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.THURSDAY -> "四"
            DayOfWeek.FRIDAY -> "五"
            DayOfWeek.SATURDAY -> "六"
        }
        "${date.monthValue}月${date.dayOfMonth}日 (週$weekZh)"
    } catch (_: Exception) {
        dateStr
    }
}

fun createRoundedRect(
    context: Context,
    fillColor: Int,
    radiusDp: Float = 20f,
    strokeColor: Int? = null,
    strokeWidthDp: Float = 1f
): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor)
        cornerRadius = context.dpF(radiusDp)
        if (strokeColor != null) {
            setStroke(max(1, context.dpF(strokeWidthDp).toInt()), strokeColor)
        }
    }
}

fun createGradientRect(
    context: Context,
    startColor: Int,
    endColor: Int,
    radiusDp: Float = 20f,
    strokeColor: Int? = null,
    strokeWidthDp: Float = 1f
): GradientDrawable {
    return GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(startColor, endColor)
    ).apply {
        cornerRadius = context.dpF(radiusDp)
        if (strokeColor != null) {
            setStroke(max(1, context.dpF(strokeWidthDp).toInt()), strokeColor)
        }
    }
}

fun createTopRoundedRect(
    context: Context,
    fillColor: Int,
    topRadiusDp: Float = 26f
): GradientDrawable {
    val r = context.dpF(topRadiusDp)
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor)
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
    }
}

fun createCircleDrawable(fillColor: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
    }
}

/**
 * 評估計算機四則運算算式 (支援 +, -, ×, ÷ 與小數)
 */
fun evaluateCalcExpression(rawExpr: String): Long {
    val expr = rawExpr
        .replace('×', '*')
        .replace('÷', '/')
        .replace('−', '-')
        .trimEnd('+', '-', '*', '/', '.')
    if (expr.isBlank()) return 0L

    return try {
        var idx = 0
        fun skipSpaces() {
            while (idx < expr.length && expr[idx] == ' ') idx++
        }

        fun parseNumber(): Double {
            skipSpaces()
            val start = idx
            while (idx < expr.length && (expr[idx].isDigit() || expr[idx] == '.')) {
                idx++
            }
            if (start == idx) return 0.0
            return expr.substring(start, idx).toDoubleOrNull() ?: 0.0
        }

        fun parseTerm(): Double {
            var value = parseNumber()
            while (true) {
                skipSpaces()
                if (idx >= expr.length) break
                when (expr[idx]) {
                    '*' -> {
                        idx++
                        value *= parseNumber()
                    }
                    '/' -> {
                        idx++
                        val divisor = parseNumber()
                        value = if (divisor == 0.0) 0.0 else value / divisor
                    }
                    else -> return value
                }
            }
            return value
        }

        fun parseExpr(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                if (idx >= expr.length) break
                when (expr[idx]) {
                    '+' -> {
                        idx++
                        value += parseTerm()
                    }
                    '-' -> {
                        idx++
                        value -= parseTerm()
                    }
                    else -> return value
                }
            }
            return value
        }

        val result = parseExpr()
        if (!result.isFinite()) 0L else max(0L, result.roundToLong())
    } catch (_: Exception) {
        0L
    }
}

/**
 * 自動換行標籤排版容器 (用於分類標籤與圖示選擇器)
 */
class WrapLayout(context: Context) : ViewGroup(context) {
    var horizontalGapPx: Int = context.dp(6)
    var verticalGapPx: Int = context.dp(6)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            Int.MAX_VALUE
        } else {
            MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        }

        var rowWidth = 0
        var rowHeight = 0
        var totalHeight = paddingTop + paddingBottom
        var maxUsedWidth = 0

        for (i in 0..<childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val cw = child.measuredWidth
            val ch = child.measuredHeight

            if (rowWidth > 0 && rowWidth + horizontalGapPx + cw > maxWidth) {
                maxUsedWidth = max(maxUsedWidth, rowWidth)
                totalHeight += rowHeight + verticalGapPx
                rowWidth = cw
                rowHeight = ch
            } else {
                rowWidth = if (rowWidth == 0) cw else rowWidth + horizontalGapPx + cw
                rowHeight = max(rowHeight, ch)
            }
        }
        maxUsedWidth = max(maxUsedWidth, rowWidth)
        totalHeight += rowHeight

        val resolvedWidth = resolveSize(maxUsedWidth + paddingLeft + paddingRight, widthMeasureSpec)
        val resolvedHeight = resolveSize(totalHeight, heightMeasureSpec)
        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val availWidth = r - l - paddingLeft - paddingRight
        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0

        for (i in 0..<childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            val cw = child.measuredWidth
            val ch = child.measuredHeight

            if (x > paddingLeft && x - paddingLeft + cw > availWidth) {
                x = paddingLeft
                y += rowHeight + verticalGapPx
                rowHeight = 0
            }

            child.layout(x, y, x + cw, y + ch)
            x += cw + horizontalGapPx
            rowHeight = max(rowHeight, ch)
        }
    }
}

/**
 * 通用刪除 / 清空 / 覆蓋 / 解鎖 確認詢問對話框
 */
fun showWarmConfirmDialog(
    context: Context,
    icon: String = "🗑️",
    title: String = "確認刪除？",
    message: String = "刪除後將無法復原這筆資料喔！",
    confirmText: String = "確定刪除",
    cancelText: String = "先不要",
    warmMode: Boolean = false,
    onConfirm: () -> Unit
) {
    val dialog = Dialog(context, com.example.mymoney.R.style.WarmCenterDialog)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

    val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = createRoundedRect(context, UiColors.BG_CARD, 24f, UiColors.BORDER_WARM, 1f)
        setPadding(context.dp(18), context.dp(20), context.dp(18), context.dp(16))
        elevation = context.dpF(12f)
    }

    val iconTv = TextView(context).apply {
        text = icon
        textSize = 24f
        gravity = Gravity.CENTER
        background = createRoundedRect(
            context,
            if (warmMode) UiColors.AMBER_LIGHT else UiColors.EXPENSE_BG,
            16f
        )
    }
    card.addView(iconTv, LinearLayout.LayoutParams(context.dp(48), context.dp(48)).apply {
        bottomMargin = context.dp(10)
    })

    val titleTv = TextView(context).apply {
        text = title
        textSize = 16f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(UiColors.TEXT_MAIN)
        gravity = Gravity.CENTER
    }
    card.addView(titleTv, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = context.dp(6) })

    val msgTv = TextView(context).apply {
        text = message
        textSize = 13f
        setTextColor(UiColors.TEXT_SECONDARY)
        gravity = Gravity.CENTER
        setLineSpacing(context.dpF(2f), 1.15f)
    }
    card.addView(msgTv, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = context.dp(16) })

    val btnRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    val cancelBtn = TextView(context).apply {
        text = cancelText
        textSize = 13f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(UiColors.TEXT_SECONDARY)
        gravity = Gravity.CENTER
        background = createRoundedRect(context, UiColors.BG_INPUT, 12f, UiColors.BORDER_WARM, 1f)
        setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        setOnClickListener { dialog.dismiss() }
    }

    val okBtn = TextView(context).apply {
        text = confirmText
        textSize = 13f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = createRoundedRect(
            context,
            if (warmMode) UiColors.AMBER_PRIMARY else UiColors.EXPENSE,
            12f
        )
        setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
    }

    btnRow.addView(cancelBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginEnd = context.dp(8)
    })
    btnRow.addView(okBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    card.addView(btnRow, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ))

    val container = LinearLayout(context).apply {
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
        gravity = Gravity.CENTER
        addView(card, LinearLayout.LayoutParams(context.dp(310), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    dialog.setContentView(container)
    dialog.window?.setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    dialog.show()
}
