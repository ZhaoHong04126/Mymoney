package com.example.mymoney.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Interpolator
import android.widget.LinearLayout
import android.widget.OverScroller
import android.widget.TextView
import androidx.core.view.isGone
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object UiColors {
    var isDark: Boolean = false

    val BG_SCREEN: Int get() = if (isDark) 0xFF181410.toInt() else 0xFFFAF5EC.toInt()
    val BG_CARD: Int get() = if (isDark) 0xFF241E18.toInt() else 0xFFFFFDF8.toInt()
    val BG_INPUT: Int get() = if (isDark) 0xFF2F2720.toInt() else 0xFFF6F0E4.toInt()
    val BG_DAY_HEADER: Int get() = if (isDark) 0xFF2B231C.toInt() else 0xFFF9F2E6.toInt()
    val DIVIDER: Int get() = if (isDark) 0xFF332B23.toInt() else 0xFFF3ECE0.toInt()

    val AMBER_PRIMARY: Int get() = if (isDark) 0xFFF0A230.toInt() else 0xFFEE9B27.toInt()
    val AMBER_DARK: Int get() = if (isDark) 0xFFF5B349.toInt() else 0xFFD68111.toInt()
    val AMBER_LIGHT: Int get() = if (isDark) 0xFF3D2E1A.toInt() else 0xFFFCE8C3.toInt()
    val AMBER_TEXT_DARK: Int get() = if (isDark) 0xFFFAC873.toInt() else 0xFF9A5B06.toInt()

    val HERO_GRAD_START: Int get() = if (isDark) 0xFF36291B.toInt() else 0xFFFFF7E5.toInt()
    val HERO_GRAD_END: Int get() = if (isDark) 0xFF281E14.toInt() else 0xFFFCE5BA.toInt()
    val HERO_BORDER: Int get() = if (isDark) 0xFF574126.toInt() else 0xFFF3D59B.toInt()
    val HERO_LABEL: Int get() = if (isDark) 0xFFD8BA92.toInt() else 0xFF84633B.toInt()
    val HERO_SUB_LABEL: Int get() = if (isDark) 0xFFCDB496.toInt() else 0xFF7D6242.toInt()
    val HERO_PILL_BG: Int get() = if (isDark) 0x991A1510.toInt() else 0xC7FFFDF8.toInt()
    val HERO_PILL_BORDER: Int get() = if (isDark) 0x66A87E3E.toInt() else 0xB3EBC686.toInt()
    val HERO_DIVIDER: Int get() = if (isDark) 0x4DA8824C.toInt() else 0x4DBA9256.toInt()
    val HERO_TRACK_BG: Int get() = if (isDark) 0xB3181410.toInt() else 0xD9FFFDF8.toInt()
    val HERO_DOT_INACTIVE: Int get() = if (isDark) 0x59D8BA92.toInt() else 0x4D84633B.toInt()

    val EXPENSE: Int get() = if (isDark) 0xFFF08065.toInt() else 0xFFDF6A4E.toInt()
    val EXPENSE_BG: Int get() = if (isDark) 0xFF3A221D.toInt() else 0xFFFCECE8.toInt()
    val EXPENSE_BORDER: Int get() = if (isDark) 0xFF664037.toInt() else 0xFFF2C6BC.toInt()
    val INCOME: Int get() = if (isDark) 0xFF63B8A0.toInt() else 0xFF46937E.toInt()
    val INCOME_BG: Int get() = if (isDark) 0xFF1D332D.toInt() else 0xFFE5F4EF.toInt()
    val INCOME_BORDER: Int get() = if (isDark) 0x6663B8A0.toInt() else 0x6646937E.toInt()
    val TRANSFER: Int get() = if (isDark) 0xFFC4A080.toInt() else 0xFF8E6E53.toInt()
    val TRANSFER_BG: Int get() = if (isDark) 0xFF302720.toInt() else 0xFFF2EBE3.toInt()

    val WARN_BG: Int get() = if (isDark) 0xFF3B2B17.toInt() else 0xEBFFFDF8.toInt()
    val WARN_TEXT: Int get() = if (isDark) 0xFFF7B955.toInt() else 0xFFC87609.toInt()
    val WARN_BORDER: Int get() = if (isDark) 0x80D48C2A.toInt() else 0xCCA86B14.toInt()
    val BANNER_BG: Int get() = if (isDark) 0xFF302619.toInt() else 0xFFFFF9EB.toInt()
    val BANNER_BORDER: Int get() = if (isDark) 0xFF594426.toInt() else 0xFFF4D89E.toInt()

    val DARK_NW_START: Int get() = if (isDark) 0xFF3B2E22.toInt() else 0xFF4E3E30.toInt()
    val DARK_NW_END: Int get() = if (isDark) 0xFF281F17.toInt() else 0xFF382B20.toInt()
    val DARK_NW_GOLD: Int get() = 0xFFF9C971.toInt()

    val TEXT_MAIN: Int get() = if (isDark) 0xFFF5ECE0.toInt() else 0xFF3D3127.toInt()
    val TEXT_SECONDARY: Int get() = if (isDark) 0xFFB8A796.toInt() else 0xFF7B6B5D.toInt()
    val TEXT_MUTED: Int get() = if (isDark) 0xFF877768.toInt() else 0xFFA69688.toInt()
    val BORDER_WARM: Int get() = if (isDark) 0xFF3B3128.toInt() else 0xFFEAE0D1.toInt()

    val SCRIM_OVERLAY: Int get() = if (isDark) 0x73000000.toInt() else 0x333D3127.toInt()
    val LOCKED_OVERLAY_BG: Int get() = if (isDark) 0xBF181410.toInt() else 0xB3FAF5EC.toInt()
    val LOCKED_CARD_BG: Int get() = if (isDark) 0xF526201A.toInt() else 0xF5FFFDF8.toInt()
    val LOCKED_CARD_BORDER: Int get() = if (isDark) 0xFF4A3C2E.toInt() else 0xFFEADBC5.toInt()
    val SELECTED_CARD_BORDER: Int get() = if (isDark) 0xFF8C6836.toInt() else 0xFFE5C899.toInt()
    val DONUT_EMPTY_RING: Int get() = if (isDark) 0xFF332B23.toInt() else 0xFFEFE6D8.toInt()
    val SHEET_HANDLE: Int get() = if (isDark) 0xFF52463B.toInt() else 0xFFD5C8B8.toInt()
    val AMOUNT_BOX_BORDER: Int get() = if (isDark) 0xFF5E472A.toInt() else 0xFFEDD9B4.toInt()
    val CALC_OP_BG: Int get() = if (isDark) 0xFF362B20.toInt() else 0xFFF5EAD8.toInt()
    val TOAST_BG: Int get() = if (isDark) 0xF23A2F25.toInt() else 0xEB3D3127.toInt()
    val TOAST_BORDER: Int? get() = if (isDark) 0xFF5C4936.toInt() else null
    val TOAST_TEXT: Int get() = 0xFFFFFDF8.toInt()
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
 * 雙頁／多頁等高水平滑動容器 (支援手指拖曳、慣性滑動、邊界回彈與外部 ScrollView 防衝突)
 */
class HorizontalCardPager(context: Context) : ViewGroup(context) {

    var currentPage: Int = 0
        private set

    var onScrollProgress: ((Float) -> Unit)? = null

    private val smoothInterpolator = Interpolator { t ->
        val f = t - 1f
        f * f * f * f * f + 1f
    }
    private val scroller = OverScroller(context, smoothInterpolator)
    private val viewConfig = ViewConfiguration.get(context)
    private val touchSlop = viewConfig.scaledTouchSlop
    private val minFlingVelocity = max(viewConfig.scaledMinimumFlingVelocity * 3, context.dp(240))
    private val maxFlingVelocity = viewConfig.scaledMaximumFlingVelocity

    private var velocityTracker: VelocityTracker? = null
    private var initialX = 0f
    private var initialY = 0f
    private var lastX = 0f
    private var isDragging = false
    private var disallowReleasedForVertical = false

    init {
        clipChildren = true
        clipToPadding = true
    }

    private fun visiblePages(): List<View> {
        val list = ArrayList<View>(childCount)
        for (i in 0..<childCount) {
            val c = getChildAt(i)
            if (!c.isGone) list.add(c)
        }
        return list
    }

    private fun pageWidth(): Int = max(0, width - paddingLeft - paddingRight)

    fun setCurrentPage(page: Int, smooth: Boolean = true) {
        val pages = visiblePages()
        if (pages.isEmpty()) return
        val clamped = page.coerceIn(0, pages.lastIndex)
        currentPage = clamped

        val pw = pageWidth()
        if (pw <= 0) {
            dispatchProgress(clamped.toFloat())
            return
        }

        val targetX = clamped * pw
        val dx = targetX - scrollX
        if (!smooth || dx == 0) {
            if (!scroller.isFinished) scroller.abortAnimation()
            scrollTo(targetX, 0)
            dispatchProgress(clamped.toFloat())
        } else {
            val distanceRatio = min(1f, abs(dx).toFloat() / pw.toFloat())
            val duration = (220 + (140 * distanceRatio).roundToInt()).coerceIn(200, 360)
            scroller.startScroll(scrollX, 0, dx, 0, duration)
            postInvalidateOnAnimation()
        }
    }

    private fun dispatchProgress(overrideProgress: Float? = null) {
        val pages = visiblePages()
        if (pages.size <= 1) {
            onScrollProgress?.invoke(0f)
            return
        }
        val pw = pageWidth()
        val p = overrideProgress ?: if (pw > 0) {
            (scrollX.toFloat() / pw.toFloat()).coerceIn(0f, pages.lastIndex.toFloat())
        } else {
            currentPage.toFloat()
        }
        onScrollProgress?.invoke(p)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val pw = max(0, totalWidth - paddingLeft - paddingRight)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(pw, MeasureSpec.EXACTLY)
        val unconstrainedHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        val pages = visiblePages()
        var maxChildHeight = 0
        for (child in pages) {
            child.measure(childWidthSpec, unconstrainedHeightSpec)
            maxChildHeight = max(maxChildHeight, child.measuredHeight)
        }

        val exactHeightSpec = MeasureSpec.makeMeasureSpec(maxChildHeight, MeasureSpec.EXACTLY)
        for (child in pages) {
            child.measure(childWidthSpec, exactHeightSpec)
        }

        val resolvedW = resolveSize(pw + paddingLeft + paddingRight, widthMeasureSpec)
        val resolvedH = resolveSize(maxChildHeight + paddingTop + paddingBottom, heightMeasureSpec)
        setMeasuredDimension(resolvedW, resolvedH)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val pw = max(0, (r - l) - paddingLeft - paddingRight)
        val ph = max(0, (b - t) - paddingTop - paddingBottom)
        val pages = visiblePages()

        pages.forEachIndexed { index, child ->
            val left = paddingLeft + index * pw
            child.layout(left, paddingTop, left + pw, paddingTop + ph)
        }

        if (pages.isNotEmpty()) {
            currentPage = currentPage.coerceIn(0, pages.lastIndex)
            if (scroller.isFinished && !isDragging && pw > 0) {
                val expectedX = currentPage * pw
                if (scrollX != expectedX) {
                    scrollTo(expectedX, 0)
                }
                dispatchProgress()
            }
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            dispatchProgress()
            postInvalidateOnAnimation()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (visiblePages().size > 1) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    disallowReleasedForVertical = false
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging && !disallowReleasedForVertical) {
                        val dx = abs(ev.x - initialX)
                        val dy = abs(ev.y - initialY)
                        if (dy > touchSlop && dy > dx * 1.15f) {
                            disallowReleasedForVertical = true
                            parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val pages = visiblePages()
        if (pages.size <= 1) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                lastX = ev.x
                obtainOrResetVelocityTracker().addMovement(ev)

                if (!scroller.isFinished) {
                    val remaining = abs(scroller.finalX - scroller.currX)
                    scroller.abortAnimation()
                    if (remaining > context.dp(4)) {
                        isDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        isDragging = false
                        scrollTo(currentPage * pageWidth(), 0)
                        dispatchProgress()
                    }
                } else {
                    isDragging = false
                }
                return isDragging
            }
            MotionEvent.ACTION_MOVE -> {
                if (disallowReleasedForVertical) return false
                if (isDragging) return true

                val dx = abs(ev.x - initialX)
                val dy = abs(ev.y - initialY)
                if (dx > touchSlop && dx >= dy) {
                    isDragging = true
                    lastX = ev.x
                    obtainVelocityTracker().addMovement(ev)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                recycleVelocityTracker()
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val pages = visiblePages()
        if (pages.size <= 1) return super.onTouchEvent(ev)

        obtainVelocityTracker().addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                lastX = ev.x
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dxTotal = abs(ev.x - initialX)
                val dyTotal = abs(ev.y - initialY)
                if (!isDragging && !disallowReleasedForVertical && dxTotal > touchSlop && dxTotal >= dyTotal) {
                    isDragging = true
                    lastX = ev.x
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isDragging) {
                    val deltaX = lastX - ev.x
                    lastX = ev.x
                    val pw = pageWidth()
                    val maxScroll = max(0, (pages.size - 1) * pw).toFloat()
                    val curX = scrollX.toFloat()
                    val factor = if ((curX <= 0f && deltaX < 0f) || (curX >= maxScroll && deltaX > 0f)) {
                        0.24f
                    } else {
                        1f
                    }
                    val maxOverscroll = context.dpF(22f)
                    val nextX = (curX + deltaX * factor).coerceIn(-maxOverscroll, maxScroll + maxOverscroll)
                    scrollTo(nextX.roundToInt(), 0)
                    dispatchProgress()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val pw = pageWidth()
                var targetPage = currentPage
                if (pw > 0 && isDragging) {
                    val vt = obtainVelocityTracker()
                    vt.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val vx = vt.xVelocity
                    val totalDx = ev.x - initialX
                    val dragOffset = scrollX - currentPage * pw

                    targetPage = when {
                        vx < -minFlingVelocity && totalDx < -context.dp(12) ->
                            (currentPage + 1).coerceAtMost(pages.lastIndex)
                        vx > minFlingVelocity && totalDx > context.dp(12) ->
                            (currentPage - 1).coerceAtLeast(0)
                        dragOffset > pw * 0.20f ->
                            (currentPage + 1).coerceAtMost(pages.lastIndex)
                        dragOffset < -pw * 0.20f ->
                            (currentPage - 1).coerceAtLeast(0)
                        else -> currentPage
                    }
                } else if (!isDragging && abs(ev.x - initialX) < touchSlop && abs(ev.y - initialY) < touchSlop) {
                    performClick()
                }
                isDragging = false
                recycleVelocityTracker()
                setCurrentPage(targetPage, smooth = true)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                recycleVelocityTracker()
                setCurrentPage(currentPage, smooth = true)
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun obtainOrResetVelocityTracker(): VelocityTracker {
        val vt = velocityTracker
        return if (vt == null) {
            VelocityTracker.obtain().also { velocityTracker = it }
        } else {
            vt.clear()
            vt
        }
    }

    private fun obtainVelocityTracker(): VelocityTracker {
        return velocityTracker ?: VelocityTracker.obtain().also { velocityTracker = it }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
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
    dialog.window?.apply {
        setDimAmount(if (UiColors.isDark) 0.65f else 0.45f)
        setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    dialog.show()
}
