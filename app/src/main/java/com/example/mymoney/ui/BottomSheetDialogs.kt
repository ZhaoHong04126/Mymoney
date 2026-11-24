package com.example.mymoney.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.mymoney.R
import com.example.mymoney.data.LedgerRepository
import com.example.mymoney.model.Account
import com.example.mymoney.model.AccountType
import com.example.mymoney.model.AppDefaults
import com.example.mymoney.model.Category
import com.example.mymoney.model.RecurringItem
import com.example.mymoney.model.TransactionItem
import com.example.mymoney.model.TxType
import java.time.LocalDate
import java.util.Locale

object BottomSheetDialogs {

    private fun buildSheetShell(context: Context): Triple<Dialog, LinearLayout, ScrollView> {
        val dialog = Dialog(context, R.style.WarmBottomSheetDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val rootOverlay = FrameLayout(context).apply {
            setOnClickListener { dialog.dismiss() }
        }

        val scrollView = ScrollView(context).apply {
            background = createTopRoundedRect(context, UiColors.BG_SCREEN, 26f)
            isVerticalScrollBarEnabled = false
            isClickable = true
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(15), context.dp(8), context.dp(15), context.dp(24))
        }

        // 頂部拖曳橫條指示器
        val handle = View(context).apply {
            background = createRoundedRect(context, "#D5C8B8".toColorInt(), 99f)
        }
        container.addView(handle, LinearLayout.LayoutParams(context.dp(38), context.dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = context.dp(2)
            bottomMargin = context.dp(10)
        })

        scrollView.addView(container)
        rootOverlay.addView(
            scrollView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        ViewCompat.setOnApplyWindowInsetsListener(rootOverlay) { view, insets ->
            val sysBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val gestures = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomSafe = maxOf(sysBars.bottom, gestures.bottom, ime.bottom)

            view.setPadding(sysBars.left, sysBars.top + context.dp(16), sysBars.right, 0)
            container.setPadding(
                context.dp(15),
                context.dp(8),
                context.dp(15),
                bottomSafe + context.dp(20)
            )
            insets
        }

        dialog.setContentView(rootOverlay)
        dialog.window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                win.isNavigationBarContrastEnforced = false
            }
            WindowInsetsControllerCompat(win, win.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            win.setGravity(Gravity.BOTTOM)
        }
        dialog.setOnShowListener {
            ViewCompat.requestApplyInsets(rootOverlay)
        }
        return Triple(dialog, container, scrollView)
    }

    private fun makeFieldLabel(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 11.5f
        setTypeface(null, Typeface.BOLD)
        setTextColor(UiColors.TEXT_SECONDARY)
    }

    private fun makeWarmInput(
        context: Context,
        hint: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        maxLength: Int? = null
    ): EditText = EditText(context).apply {
        this.hint = hint
        this.inputType = inputType
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        setTextColor(UiColors.TEXT_MAIN)
        setHintTextColor(UiColors.TEXT_MUTED)
        background = createRoundedRect(context, UiColors.BG_CARD, 11f, UiColors.BORDER_WARM, 1f)
        setPadding(context.dp(11), context.dp(9), context.dp(11), context.dp(9))
        isSingleLine = true
        if (maxLength != null) {
            filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }
    }

    private fun makeWarmSpinner(context: Context, items: List<String>, selectedIndex: Int = 0): Spinner {
        return Spinner(context).apply {
            background = createRoundedRect(context, UiColors.BG_CARD, 11f, UiColors.BORDER_WARM, 1f)
            setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
            val adapter = object : ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_item,
                items
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.textSize = 12.5f
                    v.setTypeface(null, Typeface.BOLD)
                    v.setTextColor(UiColors.TEXT_MAIN)
                    return v
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.textSize = 13f
                    v.setTextColor(UiColors.TEXT_MAIN)
                    v.setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                    return v
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            if (selectedIndex in items.indices) setSelection(selectedIndex)
        }
    }

    private fun makeButtonPair(
        context: Context,
        cancelText: String,
        confirmText: String,
        onCancel: () -> Unit,
        onConfirm: () -> Unit
    ): Pair<LinearLayout, TextView> {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val cancelBtn = TextView(context).apply {
            text = cancelText
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
            background = createRoundedRect(context, UiColors.BG_INPUT, 12f, UiColors.BORDER_WARM, 1f)
            setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
            setOnClickListener { onCancel() }
        }
        val okBtn = TextView(context).apply {
            text = confirmText
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = createRoundedRect(context, UiColors.AMBER_PRIMARY, 12f)
            setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
            setOnClickListener { onConfirm() }
        }
        row.addView(cancelBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = context.dp(8)
        })
        row.addView(okBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return Pair(row, okBtn)
    }

    // =========================================================================
    // 1. 新增 / 編輯記帳抽屜 (含內建計算機鍵盤)
    // =========================================================================
    fun showTransactionSheet(
        context: Context,
        repo: LedgerRepository,
        editingTx: TransactionItem? = null,
        defaultType: TxType = TxType.EXPENSE,
        currentViewedYm: String,
        onToast: (String) -> Unit,
        onDataChanged: (switchToYm: String?) -> Unit
    ) {
        val (dialog, container, _) = buildSheetShell(context)
        val accounts = repo.data.accounts

        var txType = editingTx?.type ?: defaultType
        var selectedCatId = editingTx?.categoryId ?: repo.getCategories(txType).firstOrNull()?.id ?: "exp_other"
        var calcExpr = editingTx?.amount?.toString() ?: "0"
        var selectedDate = editingTx?.date ?: LocalDate.now().toString()

        // 頂部：取消 | 支出/收入/轉帳 | 刪除
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val closeBtn = TextView(context).apply {
            text = "取消"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        val segmentGroup = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createRoundedRect(context, UiColors.BG_INPUT, 99f)
            setPadding(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
        }
        val deleteBtn = TextView(context).apply {
            text = "🗑️"
            textSize = 17f
            setPadding(context.dp(8), context.dp(4), context.dp(6), context.dp(4))
            visibility = if (editingTx != null) View.VISIBLE else View.INVISIBLE
        }

        val segmentWrap = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            addView(segmentGroup)
        }
        header.addView(closeBtn)
        header.addView(segmentWrap, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(deleteBtn)
        container.addView(header, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(8) })

        // 刪除交易詢問框
        deleteBtn.setOnClickListener {
            val target = editingTx ?: return@setOnClickListener
            val cat = repo.getCategoryById(target.categoryId, target.type)
            val label = if (target.note.isNotBlank()) "${cat.name}（${target.note}）" else cat.name
            showWarmConfirmDialog(
                context = context,
                icon = "🗑️",
                title = "確定刪除這筆記帳？",
                message = "即將刪除 ${target.date} 的「${cat.icon} $label ${formatCurrency(target.amount)}」，刪除後無法復原喔！",
                confirmText = "確定刪除",
                cancelText = "先不要"
            ) {
                repo.data.transactions.removeAll { it.id == target.id }
                repo.save()
                dialog.dismiss()
                onDataChanged(null)
                onToast("已刪除該筆紀錄 🗑️")
            }
        }

        // 金額算式顯示列
        val amountBox = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedRect(context, UiColors.BG_CARD, 16f, "#EDD9B4".toColorInt(), 1.5f)
            setPadding(context.dp(12), context.dp(9), context.dp(12), context.dp(9))
        }
        val catBadgeIcon = TextView(context).apply { textSize = 20f }
        val catBadgeName = TextView(context).apply {
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
            setPadding(context.dp(6), 0, 0, 0)
        }
        val badgeLeft = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(catBadgeIcon)
            addView(catBadgeName)
        }
        val exprPreviewTv = TextView(context).apply {
            textSize = 11f
            setTextColor(UiColors.TEXT_MUTED)
            gravity = Gravity.END
        }
        val amountMainTv = TextView(context).apply {
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            gravity = Gravity.END
        }
        val amountRight = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(exprPreviewTv)
            addView(amountMainTv)
        }
        amountBox.addView(badgeLeft)
        amountBox.addView(amountRight, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(amountBox, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(8) })

        // 分類 5 欄網格 (包在固定高度 ScrollView 内)
        val catGridScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
        }
        val catGrid = GridLayout(context).apply {
            columnCount = 5
        }
        catGridScroll.addView(catGrid)
        container.addView(catGridScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.dp(130)
        ).apply { bottomMargin = context.dp(8) })

        // 轉帳帳戶選擇區
        val accLabels = accounts.map { "${it.icon} ${it.name}" }
        val fromIdxInit = accounts.indexOfFirst { it.id == editingTx?.accountId }.coerceAtLeast(0)
        val toIdxInit = editingTx?.toAccountId?.let { targetId ->
            accounts.indexOfFirst { it.id == targetId }.takeIf { it >= 0 }
        } ?: if (accounts.size > 1) 1 else 0

        val fromSpinner = makeWarmSpinner(context, accLabels, fromIdxInit)
        val toSpinner = makeWarmSpinner(context, accLabels, toIdxInit)
        val transferPanel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedRect(context, UiColors.BG_CARD, 14f, UiColors.BORDER_WARM, 1f)
            setPadding(context.dp(10), context.dp(8), context.dp(10), context.dp(8))
        }
        val fromBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeFieldLabel(context, "轉出帳戶"))
            addView(fromSpinner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(3) })
        }
        val arrowTv = TextView(context).apply {
            text = "➔"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            setPadding(context.dp(6), context.dp(14), context.dp(6), 0)
        }
        val toBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeFieldLabel(context, "轉入帳戶"))
            addView(toSpinner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(3) })
        }
        transferPanel.addView(fromBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        transferPanel.addView(arrowTv)
        transferPanel.addView(toBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(transferPanel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(8) })

        // 帳戶 & 日期 & 備註列
        val singleAccSpinner = makeWarmSpinner(context, accLabels, fromIdxInit)
        val singleAccBox = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedRect(context, UiColors.BG_CARD, 11f, UiColors.BORDER_WARM, 1f)
            setPadding(context.dp(8), context.dp(2), context.dp(4), context.dp(2))
            addView(TextView(context).apply { text = "👛"; textSize = 13f })
            addView(singleAccSpinner, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        singleAccSpinner.background = null

        val dateBtn = TextView(context).apply {
            text = context.getString(R.string.date_button_format, selectedDate)
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedRect(context, UiColors.BG_CARD, 11f, UiColors.BORDER_WARM, 1f)
            setPadding(context.dp(10), context.dp(9), context.dp(10), context.dp(9))
            setOnClickListener {
                val parsed = runCatching { LocalDate.parse(selectedDate) }.getOrElse { LocalDate.now() }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                        text = context.getString(R.string.date_button_format, selectedDate)
                    },
                    parsed.year,
                    parsed.monthValue - 1,
                    parsed.dayOfMonth
                ).show()
            }
        }

        val accDateRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        accDateRow.addView(singleAccBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            marginEnd = context.dp(6)
        })
        accDateRow.addView(dateBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(accDateRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(6) })

        val noteInput = makeWarmInput(context, "✏️ 寫點備註吧（例如：拿鐵、全聯買菜）...").apply {
            setText(editingTx?.note.orEmpty())
        }
        container.addView(noteInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(4) })

        val recurringCheck = CheckBox(context).apply {
            text = "同時加入「每月固定週期收支」清單"
            textSize = 12f
            setTextColor(UiColors.TEXT_SECONDARY)
            visibility = if (editingTx == null) View.VISIBLE else View.GONE
        }
        container.addView(recurringCheck, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(6) })

        lateinit var submitKeyView: TextView

        fun refreshAmountDisplay() {
            val hasOp = calcExpr.any { it == '+' || it == '-' || it == '×' || it == '÷' }
            val valLong = evaluateCalcExpression(calcExpr)
            exprPreviewTv.text = if (hasOp) calcExpr else ""
            exprPreviewTv.visibility = if (hasOp) View.VISIBLE else View.GONE
            amountMainTv.text = formatCurrency(valLong)
            submitKeyView.text = if (editingTx != null) "儲存修改 ✓" else "完成記帳 ✓"
        }

        fun refreshCategoryGrid() {
            catGrid.removeAllViews()
            if (txType == TxType.TRANSFER) {
                catGridScroll.visibility = View.GONE
                transferPanel.visibility = View.VISIBLE
                singleAccBox.visibility = View.GONE
                recurringCheck.visibility = View.GONE
                catBadgeIcon.text = "⇄"
                catBadgeName.text = "帳戶間轉帳"
                selectedCatId = "transfer"
                return
            }

            catGridScroll.visibility = View.VISIBLE
            transferPanel.visibility = View.GONE
            singleAccBox.visibility = View.VISIBLE
            recurringCheck.visibility = if (editingTx == null) View.VISIBLE else View.GONE

            val cats = repo.getCategories(txType)
            val chosen = cats.find { it.id == selectedCatId } ?: cats.first()
            selectedCatId = chosen.id
            catBadgeIcon.text = chosen.icon
            catBadgeName.text = chosen.name

            val totalTiles = cats.size + 1
            for (idx in 0..<totalTiles) {
                val isManageTile = idx == cats.size
                val cat = if (!isManageTile) cats[idx] else null
                val isActive = cat != null && cat.id == selectedCatId

                val tile = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(context.dp(2), context.dp(6), context.dp(2), context.dp(6))
                    background = createRoundedRect(
                        context,
                        if (isActive) UiColors.AMBER_LIGHT else UiColors.BG_CARD,
                        12f,
                        if (isActive) UiColors.AMBER_PRIMARY else Color.TRANSPARENT,
                        1f
                    )
                    addView(TextView(context).apply {
                        text = if (isManageTile) "⚙️" else cat!!.icon
                        textSize = 18f
                        gravity = Gravity.CENTER
                    })
                    addView(TextView(context).apply {
                        text = if (isManageTile) "管理分類" else cat!!.name
                        textSize = 10.5f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(UiColors.TEXT_MAIN)
                        gravity = Gravity.CENTER
                        isSingleLine = true
                    })
                    setOnClickListener {
                        if (isManageTile) {
                            showCategorySheet(
                                context = context,
                                repo = repo,
                                preferredType = txType,
                                onToast = onToast
                            ) {
                                refreshCategoryGrid()
                                onDataChanged(null)
                            }
                        } else {
                            selectedCatId = cat!!.id
                            refreshCategoryGrid()
                        }
                    }
                }

                val params = GridLayout.LayoutParams(
                    GridLayout.spec(idx / 5),
                    GridLayout.spec(idx % 5, 1f)
                ).apply {
                    width = 0
                    setMargins(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
                }
                catGrid.addView(tile, params)
            }
            if (totalTiles < 5) {
                for (emptyCol in totalTiles..<5) {
                    val spacer = View(context)
                    val params = GridLayout.LayoutParams(
                        GridLayout.spec(0),
                        GridLayout.spec(emptyCol, 1f)
                    ).apply { width = 0; height = 1 }
                    catGrid.addView(spacer, params)
                }
            }
        }

        // 建立 支出 / 收入 / 轉帳 切換按鈕
        val segmentButtons = mutableMapOf<TxType, TextView>()
        fun refreshSegmentedButtons() {
            TxType.entries.forEach { type ->
                val btn = segmentButtons[type] ?: return@forEach
                val active = txType == type
                btn.background = if (active) createRoundedRect(context, UiColors.AMBER_PRIMARY, 99f) else null
                btn.setTextColor(if (active) Color.WHITE else UiColors.TEXT_SECONDARY)
            }
        }
        TxType.entries.forEach { type ->
            val btn = TextView(context).apply {
                text = type.label
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(context.dp(14), context.dp(5), context.dp(14), context.dp(5))
                setOnClickListener {
                    txType = type
                    if (type != TxType.TRANSFER) {
                        val cats = repo.getCategories(type)
                        if (cats.none { it.id == selectedCatId }) {
                            selectedCatId = cats.firstOrNull()?.id ?: "exp_other"
                        }
                    }
                    refreshSegmentedButtons()
                    refreshCategoryGrid()
                }
            }
            segmentButtons[type] = btn
            segmentGroup.addView(btn)
        }
        refreshSegmentedButtons()

        // 內建記帳計算機鍵盤 (4 欄 x 5 列)
        val keypad = GridLayout(context).apply { columnCount = 4 }
        val keys = listOf(
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "-",
            ".", "0", "BACK", "+",
            "C", "OK"
        )

        fun submitTransaction() {
            val amount = evaluateCalcExpression(calcExpr)
            if (amount <= 0L) {
                onToast("請輸入大於 0 的金額喔 🍯")
                return
            }
            val note = noteInput.text.toString().trim()
            val accId: String
            val toAccId: String?

            if (txType == TxType.TRANSFER) {
                accId = accounts.getOrNull(fromSpinner.selectedItemPosition)?.id ?: accounts.first().id
                toAccId = accounts.getOrNull(toSpinner.selectedItemPosition)?.id ?: accounts.first().id
                if (accId == toAccId) {
                    onToast("轉出與轉入帳戶不能相同喔！")
                    return
                }
            } else {
                accId = accounts.getOrNull(singleAccSpinner.selectedItemPosition)?.id ?: accounts.first().id
                toAccId = null
            }

            val finalCatId = if (txType == TxType.TRANSFER) "transfer" else selectedCatId

            if (editingTx != null) {
                editingTx.type = txType
                editingTx.categoryId = finalCatId
                editingTx.accountId = accId
                editingTx.toAccountId = toAccId
                editingTx.amount = amount
                editingTx.date = selectedDate
                editingTx.note = note
                onToast("已更新記帳紀錄 ✨")
            } else {
                repo.data.transactions.add(
                    TransactionItem(
                        id = "tx_${System.currentTimeMillis()}",
                        type = txType,
                        categoryId = finalCatId,
                        accountId = accId,
                        toAccountId = toAccId,
                        amount = amount,
                        date = selectedDate,
                        note = note
                    )
                )
                if (recurringCheck.isChecked && txType != TxType.TRANSFER) {
                    val dayNum = selectedDate.split("-").getOrNull(2)?.toIntOrNull() ?: 1
                    repo.data.recurring.add(
                        RecurringItem(
                            id = "rec_${System.currentTimeMillis()}",
                            type = txType,
                            day = dayNum,
                            categoryId = finalCatId,
                            accountId = accId,
                            amount = amount,
                            note = note.ifBlank { repo.getCategoryById(finalCatId, txType).name },
                            lastAppliedMonth = selectedDate.take(7)
                        )
                    )
                }
                onToast("記帳成功！今天也辛苦了 🍯")
            }

            repo.save()
            dialog.dismiss()
            val targetYm = selectedDate.take(7).takeIf { it.length == 7 && it != currentViewedYm }
            onDataChanged(targetYm)
        }

        fun handleCalcKey(key: String) {
            when (key) {
                "C" -> calcExpr = "0"
                "BACK" -> calcExpr = if (calcExpr.length <= 1) "0" else calcExpr.dropLast(1)
                "OK" -> {
                    submitTransaction()
                    return
                }
                "+", "-", "×", "÷" -> {
                    val operators = setOf('+', '-', '×', '÷')
                    calcExpr = if (calcExpr.lastOrNull() in operators) {
                        calcExpr.dropLast(1) + key
                    } else {
                        calcExpr + key
                    }
                }
                "." -> {
                    val currentPart = calcExpr.split('+', '-', '×', '÷').lastOrNull().orEmpty()
                    if ('.' in currentPart) return
                    calcExpr += "."
                }
                else -> {
                    calcExpr = if (calcExpr == "0") key else if (calcExpr.length < 16) calcExpr + key else calcExpr
                }
            }
            refreshAmountDisplay()
        }

        keys.forEachIndexed { idx, key ->
            val isOk = key == "OK"
            val isClear = key == "C"
            val isOp = key in setOf("÷", "×", "-", "+")

            val label = when (key) {
                "BACK" -> "⌫"
                "-" -> "−"
                "+" -> "＋"
                "C" -> "AC"
                "OK" -> if (editingTx != null) "儲存修改 ✓" else "完成記帳 ✓"
                else -> key
            }

            val keyBtn = TextView(context).apply {
                text = label
                textSize = when {
                    isClear -> 13f
                    isOk -> 14.5f
                    else -> 15.5f
                }
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = when {
                    isOk -> createGradientRect(context, "#F4B247".toColorInt(), "#E68D18".toColorInt(), 12f)
                    isClear -> createRoundedRect(context, UiColors.EXPENSE_BG, 12f, UiColors.EXPENSE_BORDER, 1f)
                    isOp -> createRoundedRect(context, "#F5EAD8".toColorInt(), 12f, UiColors.BORDER_WARM, 1f)
                    else -> createRoundedRect(context, UiColors.BG_CARD, 12f, UiColors.BORDER_WARM, 1f)
                }
                setTextColor(
                    when {
                        isOk -> Color.WHITE
                        isClear -> UiColors.EXPENSE
                        isOp -> UiColors.AMBER_DARK
                        else -> UiColors.TEXT_MAIN
                    }
                )
                setOnClickListener { handleCalcKey(key) }
            }
            if (isOk) submitKeyView = keyBtn

            val row = idx / 4
            val col = idx % 4
            val colSpan = if (isOk) 3 else 1
            val params = GridLayout.LayoutParams(
                GridLayout.spec(row),
                GridLayout.spec(col, colSpan, colSpan.toFloat())
            ).apply {
                width = 0
                height = context.dp(40)
                setMargins(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
            }
            keypad.addView(keyBtn, params)
        }

        container.addView(keypad, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        refreshCategoryGrid()
        refreshAmountDisplay()
        dialog.show()
    }

    // =========================================================================
    // 2. 設定每月總預算抽屜 (預設 10000，含快速選額按鈕)
    // =========================================================================
    fun showBudgetSheet(
        context: Context,
        repo: LedgerRepository,
        onToast: (String) -> Unit,
        onSaved: () -> Unit
    ) {
        val (dialog, container, _) = buildSheetShell(context)

        container.addView(TextView(context).apply {
            text = "🎯 設定每月總預算"
            textSize = 15.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        })
        container.addView(TextView(context).apply {
            text = "當支出接近或超過預算時，系統會溫柔提醒您"
            textSize = 12f
            setTextColor(UiColors.TEXT_SECONDARY)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(3); bottomMargin = context.dp(12) })

        container.addView(makeFieldLabel(context, "每月預算金額 (NT$)"))
        val input = makeWarmInput(context, "例如 10000", InputType.TYPE_CLASS_NUMBER).apply {
            setText(context.getString(R.string.plain_number_format, repo.data.monthlyBudget))
            setSelection(text.length)
        }
        container.addView(input, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(5); bottomMargin = context.dp(10) })

        val presetValues = listOf(5000L, 10000L, 15000L, 20000L)
        val chipRow = WrapLayout(context)
        val chipViews = mutableMapOf<Long, TextView>()

        fun syncChipHighlight() {
            val currentVal = input.text.toString().trim().toLongOrNull()
            presetValues.forEach { v ->
                val chip = chipViews[v] ?: return@forEach
                val active = currentVal == v
                chip.background = createRoundedRect(
                    context,
                    if (active) UiColors.AMBER_PRIMARY else UiColors.AMBER_LIGHT,
                    99f
                )
                chip.setTextColor(if (active) Color.WHITE else UiColors.AMBER_TEXT_DARK)
            }
        }

        presetValues.forEach { v ->
            val chip = TextView(context).apply {
                text = formatCurrency(v)
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(context.dp(12), context.dp(6), context.dp(12), context.dp(6))
                setOnClickListener {
                    input.setText(context.getString(R.string.plain_number_format, v))
                    input.setSelection(input.text.length)
                    syncChipHighlight()
                }
            }
            chipViews[v] = chip
            chipRow.addView(chip)
        }
        syncChipHighlight()
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                syncChipHighlight()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        container.addView(chipRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(14) })

        val (btnRow, _) = makeButtonPair(
            context,
            "取消",
            "儲存預算",
            onCancel = { dialog.dismiss() },
            onConfirm = {
                val valLong = (input.text.toString().trim().toLongOrNull() ?: 0L).coerceAtLeast(0L)
                repo.data.monthlyBudget = valLong
                repo.save()
                dialog.dismiss()
                onSaved()
                onToast("已將每月預算設為 ${formatCurrency(valLong)} 🎯")
            }
        )
        container.addView(btnRow)
        dialog.show()
    }

    // =========================================================================
    // 3. 新增 / 編輯帳戶抽屜
    // =========================================================================
    fun showAccountSheet(
        context: Context,
        repo: LedgerRepository,
        editingAccount: Account? = null,
        onToast: (String) -> Unit,
        onSaved: () -> Unit
    ) {
        val (dialog, container, _) = buildSheetShell(context)
        var selectedEmoji = editingAccount?.icon ?: "💵"

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(context).apply {
            text = if (editingAccount != null) "👛 編輯帳戶" else "👛 新增帳戶"
            textSize = 15.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        if (editingAccount != null) {
            val delBtn = TextView(context).apply {
                text = "🗑️"
                textSize = 17f
                setPadding(context.dp(8), context.dp(4), context.dp(4), context.dp(4))
                setOnClickListener {
                    if (repo.data.accounts.size <= 1) {
                        onToast("至少需保留一個帳戶喔！")
                        return@setOnClickListener
                    }
                    val relatedCount = repo.data.transactions.count {
                        it.accountId == editingAccount.id || it.toAccountId == editingAccount.id
                    }
                    val extra = if (relatedCount > 0) "（此帳戶目前關聯 $relatedCount 筆交易紀錄）" else ""
                    showWarmConfirmDialog(
                        context = context,
                        icon = "👛",
                        title = "確定刪除此帳戶？",
                        message = "確定要刪除帳戶「${editingAccount.icon} ${editingAccount.name}」嗎？${extra}刪除後無法復原喔！",
                        confirmText = "確定刪除",
                        cancelText = "取消"
                    ) {
                        repo.data.accounts.removeAll { it.id == editingAccount.id }
                        repo.save()
                        dialog.dismiss()
                        onSaved()
                        onToast("已刪除帳戶")
                    }
                }
            }
            headerRow.addView(delBtn)
        }
        container.addView(headerRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(10) })

        container.addView(makeFieldLabel(context, "帳戶名稱"))
        val nameInput = makeWarmInput(context, "例如：生活現金、郵局帳戶、LINE Pay").apply {
            setText(editingAccount?.name.orEmpty())
        }
        container.addView(nameInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(4); bottomMargin = context.dp(10) })

        val types = AccountType.entries
        val typeSpinner = makeWarmSpinner(
            context,
            types.map { it.spinnerLabel },
            types.indexOfFirst { it == (editingAccount?.type ?: AccountType.CASH) }.coerceAtLeast(0)
        )
        val balanceInput = makeWarmInput(
            context,
            "0",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        ).apply {
            setText(context.getString(R.string.plain_number_format, editingAccount?.initialBalance ?: 0L))
        }

        val twoCol = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val col1 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeFieldLabel(context, "帳戶類型"))
            addView(typeSpinner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(4) })
        }
        val col2 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeFieldLabel(context, "初始餘額 (NT$)"))
            addView(balanceInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(4) })
        }
        twoCol.addView(col1, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = context.dp(8)
        })
        twoCol.addView(col2, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(twoCol, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(10) })

        container.addView(makeFieldLabel(context, "代表圖示"))
        val emojis = listOf("💵", "👛", "🏦", "💳", "📱", "🐷", "💎")
        val emojiRow = WrapLayout(context)
        val emojiButtons = mutableMapOf<String, TextView>()
        fun refreshEmojiPicker() {
            emojis.forEach { em ->
                val v = emojiButtons[em] ?: return@forEach
                val active = em == selectedEmoji
                v.background = createRoundedRect(
                    context,
                    if (active) UiColors.AMBER_LIGHT else UiColors.BG_CARD,
                    10f,
                    if (active) UiColors.AMBER_PRIMARY else UiColors.BORDER_WARM,
                    1f
                )
            }
        }
        emojis.forEach { em ->
            val tv = TextView(context).apply {
                text = em
                textSize = 18f
                gravity = Gravity.CENTER
                setOnClickListener {
                    selectedEmoji = em
                    refreshEmojiPicker()
                }
            }
            emojiButtons[em] = tv
            emojiRow.addView(tv, ViewGroup.LayoutParams(context.dp(38), context.dp(38)))
        }
        refreshEmojiPicker()
        container.addView(emojiRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(5); bottomMargin = context.dp(14) })

        val (btnRow, _) = makeButtonPair(
            context,
            "取消",
            "儲存帳戶",
            onCancel = { dialog.dismiss() },
            onConfirm = {
                val accName = nameInput.text.toString().trim()
                if (accName.isBlank()) {
                    onToast("請輸入帳戶名稱")
                    return@makeButtonPair
                }
                val accType = types.getOrElse(typeSpinner.selectedItemPosition) { AccountType.CASH }
                val initBal = balanceInput.text.toString().trim().toLongOrNull() ?: 0L

                if (editingAccount != null) {
                    editingAccount.name = accName
                    editingAccount.type = accType
                    editingAccount.initialBalance = initBal
                    editingAccount.icon = selectedEmoji
                    onToast("已更新帳戶資訊")
                } else {
                    repo.data.accounts.add(
                        Account(
                            id = "acc_${System.currentTimeMillis()}",
                            name = accName,
                            type = accType,
                            icon = selectedEmoji,
                            initialBalance = initBal
                        )
                    )
                    onToast("已新增帳戶 👛")
                }
                repo.save()
                dialog.dismiss()
                onSaved()
            }
        )
        container.addView(btnRow)
        dialog.show()
    }

    // =========================================================================
    // 4. 固定週期收支設定抽屜
    // =========================================================================
    fun showRecurringSheet(
        context: Context,
        repo: LedgerRepository,
        currentYm: String,
        onToast: (String) -> Unit,
        onChanged: () -> Unit
    ) {
        val (dialog, container, _) = buildSheetShell(context)

        container.addView(TextView(context).apply {
            text = "🗓️ 固定週期收支設定"
            textSize = 15.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        })
        container.addView(TextView(context).apply {
            text = "管理每月固定會發生的房租、訂閱服務或薪資"
            textSize = 12f
            setTextColor(UiColors.TEXT_SECONDARY)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(3); bottomMargin = context.dp(10) })

        val listBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        container.addView(listBox, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(10) })

        fun refreshRecList() {
            listBox.removeAllViews()
            val recs = repo.data.recurring
            if (recs.isEmpty()) {
                listBox.addView(TextView(context).apply {
                    text = "目前沒有設定固定週期收支項目"
                    textSize = 12f
                    setTextColor(UiColors.TEXT_SECONDARY)
                })
                return
            }

            recs.forEach { rec ->
                val cat = repo.getCategoryById(rec.categoryId, rec.type)
                val acc = repo.getAccountById(rec.accountId)
                val applied = rec.lastAppliedMonth == currentYm

                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = createRoundedRect(context, UiColors.BG_INPUT, 13f)
                    setPadding(context.dp(10), context.dp(9), context.dp(10), context.dp(9))
                }
                val dayBadge = TextView(context).apply {
                    text = context.getString(R.string.recurring_day_badge, rec.day)
                    textSize = 10.5f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(UiColors.AMBER_TEXT_DARK)
                    background = createRoundedRect(context, UiColors.AMBER_LIGHT, 8f)
                    setPadding(context.dp(7), context.dp(3), context.dp(7), context.dp(3))
                }
                val infoCol = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(context.dp(8), 0, context.dp(6), 0)
                    addView(TextView(context).apply {
                        text = context.getString(R.string.icon_name_pair, cat.icon, rec.note.ifBlank { cat.name })
                        textSize = 12.5f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(UiColors.TEXT_MAIN)
                    })
                    addView(TextView(context).apply {
                        text = context.getString(
                            if (applied) R.string.recurring_status_applied else R.string.recurring_status_pending,
                            acc.name
                        )
                        textSize = 10.5f
                        setTextColor(UiColors.TEXT_SECONDARY)
                    })
                }
                val amtTv = TextView(context).apply {
                    text = context.getString(
                        R.string.signed_amount_pair,
                        if (rec.type == TxType.INCOME) "+" else "-",
                        formatCurrency(rec.amount)
                    )
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(if (rec.type == TxType.INCOME) UiColors.INCOME else UiColors.EXPENSE)
                }
                val delBtn = TextView(context).apply {
                    text = "✕"
                    textSize = 13f
                    setTextColor(UiColors.TEXT_MUTED)
                    setPadding(context.dp(8), context.dp(4), context.dp(4), context.dp(4))
                    setOnClickListener {
                        val itemName = rec.note.ifBlank { cat.name }
                        showWarmConfirmDialog(
                            context = context,
                            icon = "🗓️",
                            title = "確定刪除固定週期項目？",
                            message = "確定要刪除「每月 ${rec.day} 日 · $itemName (${formatCurrency(rec.amount)})」嗎？刪除後將不再自動提醒入帳。",
                            confirmText = "確定刪除",
                            cancelText = "保留"
                        ) {
                            repo.data.recurring.removeAll { it.id == rec.id }
                            repo.save()
                            refreshRecList()
                            onChanged()
                            onToast("已移除週期收支項目")
                        }
                    }
                }
                row.addView(dayBadge)
                row.addView(infoCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(amtTv)
                row.addView(delBtn)
                listBox.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = context.dp(7) })
            }
        }
        refreshRecList()

        // 分隔線 + 新增表單
        container.addView(View(context).apply {
            background = createRoundedRect(context, UiColors.BORDER_WARM, 1f)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dp(1)).apply {
            bottomMargin = context.dp(8)
        })
        container.addView(TextView(context).apply {
            text = "＋ 新增固定週期項目"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(8) })

        val typeOptions = listOf("支出", "收入")
        val typeSpinner = makeWarmSpinner(context, typeOptions, 0)
        val dayInput = makeWarmInput(context, "1~31日", InputType.TYPE_CLASS_NUMBER).apply {
            setText("5")
        }

        val catSpinner = makeWarmSpinner(context, emptyList())
        var currentCatList: List<Category> = emptyList()
        fun syncRecCategories() {
            val t = if (typeSpinner.selectedItemPosition == 1) TxType.INCOME else TxType.EXPENSE
            currentCatList = repo.getCategories(t)
            val labels = currentCatList.map { "${it.icon} ${it.name}" }
            val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, labels) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.textSize = 12.5f
                    v.setTypeface(null, Typeface.BOLD)
                    v.setTextColor(UiColors.TEXT_MAIN)
                    return v
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.textSize = 13f
                    v.setTextColor(UiColors.TEXT_MAIN)
                    v.setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                    return v
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            catSpinner.adapter = adapter
        }
        syncRecCategories()
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                syncRecCategories()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val accounts = repo.data.accounts
        val accSpinner = makeWarmSpinner(context, accounts.map { "${it.icon} ${it.name}" })
        val amtInput = makeWarmInput(context, "例如 390", InputType.TYPE_CLASS_NUMBER)
        val noteInput = makeWarmInput(context, "例如：房租、月薪")

        fun addTwoColRow(l1: String, v1: View, l2: String, v2: View) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val c1 = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(makeFieldLabel(context, l1))
                addView(v1, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = context.dp(4) })
            }
            val c2 = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(makeFieldLabel(context, l2))
                addView(v2, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = context.dp(4) })
            }
            row.addView(c1, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = context.dp(8)
            })
            row.addView(c2, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            container.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = context.dp(8) })
        }

        addTwoColRow("收支類型", typeSpinner, "每月固定入帳日", dayInput)
        addTwoColRow("分類", catSpinner, "扣款/入帳帳戶", accSpinner)
        addTwoColRow("金額 (NT$)", amtInput, "項目名稱 / 備註", noteInput)

        val (btnRow, _) = makeButtonPair(
            context,
            "關閉",
            "＋ 新增週期項目",
            onCancel = { dialog.dismiss() },
            onConfirm = {
                val amount = amtInput.text.toString().trim().toLongOrNull() ?: 0L
                if (amount <= 0L) {
                    onToast("請輸入大於 0 的金額")
                    return@makeButtonPair
                }
                val t = if (typeSpinner.selectedItemPosition == 1) TxType.INCOME else TxType.EXPENSE
                val day = (dayInput.text.toString().trim().toIntOrNull() ?: 1).coerceIn(1, 31)
                val cat = currentCatList.getOrNull(catSpinner.selectedItemPosition) ?: currentCatList.first()
                val acc = accounts.getOrNull(accSpinner.selectedItemPosition) ?: accounts.first()
                val note = noteInput.text.toString().trim().ifBlank { cat.name }

                repo.data.recurring.add(
                    RecurringItem(
                        id = "rec_${System.currentTimeMillis()}",
                        type = t,
                        day = day,
                        categoryId = cat.id,
                        accountId = acc.id,
                        amount = amount,
                        note = note,
                        lastAppliedMonth = ""
                    )
                )
                amtInput.setText("")
                noteInput.setText("")
                repo.save()
                refreshRecList()
                onChanged()
                onToast("已新增固定週期收支 🗓️")
            }
        )
        container.addView(btnRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(4) })

        dialog.show()
    }

    // =========================================================================
    // 5. 收支分類管理抽屜 (含編輯、刪除、系統保留「其他」防誤刪、還原預設)
    // =========================================================================
    fun showCategorySheet(
        context: Context,
        repo: LedgerRepository,
        preferredType: TxType = TxType.EXPENSE,
        onToast: (String) -> Unit,
        onChanged: () -> Unit
    ) {
        val (dialog, container, _) = buildSheetShell(context)
        var editingCatId: String? = null
        var selectedEmoji = "🍱"

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(context).apply {
            text = "🏷️ 收支分類管理"
            textSize = 15.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val resetDefaultBtn = TextView(context).apply {
            text = "還原預設"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            setPadding(context.dp(8), context.dp(4), context.dp(6), context.dp(4))
        }
        headerRow.addView(resetDefaultBtn)
        container.addView(headerRow)

        container.addView(TextView(context).apply {
            text = "點擊現有分類可直接修改名稱與圖示，或點 ✕ 刪除"
            textSize = 12f
            setTextColor(UiColors.TEXT_SECONDARY)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(2); bottomMargin = context.dp(10) })

        val tagsScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
        }
        val tagsWrap = WrapLayout(context)
        tagsScroll.addView(tagsWrap)
        container.addView(tagsScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.dp(150)
        ).apply { bottomMargin = context.dp(10) })

        container.addView(View(context).apply {
            background = createRoundedRect(context, UiColors.BORDER_WARM, 1f)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dp(1)).apply {
            bottomMargin = context.dp(8)
        })

        val formTitleTv = TextView(context).apply {
            text = "＋ 新增分類"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
        }
        container.addView(formTitleTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(8) })

        val catTypeLabels = listOf("支出分類", "收入分類")
        val typeSpinner = makeWarmSpinner(
            context,
            catTypeLabels,
            if (preferredType == TxType.INCOME) 1 else 0
        )
        val nameInput = makeWarmInput(context, "例如：居家房租、寵物", maxLength = 8)

        val twoCol = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val c1 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeFieldLabel(context, "所屬類型"))
            addView(typeSpinner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(4) })
        }
        val c2 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeFieldLabel(context, "分類名稱"))
            addView(nameInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(4) })
        }
        twoCol.addView(c1, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = context.dp(8)
        })
        twoCol.addView(c2, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(twoCol, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = context.dp(10) })

        container.addView(makeFieldLabel(context, "選擇圖示"))
        val emojis = listOf(
            "🍱", "☕", "🚌", "🛍️", "🥬", "🎬", "🏠", "💡",
            "💊", "🐾", "🍰", "🧋", "🎮", "💄", "📚", "🏋️",
            "✈️", "🎁", "💰", "🧧", "📈", "💻", "✨"
        )
        val emojiWrap = WrapLayout(context)
        val emojiViews = mutableMapOf<String, TextView>()
        fun refreshEmojiPicker() {
            emojis.forEach { em ->
                val v = emojiViews[em] ?: return@forEach
                val active = em == selectedEmoji
                v.background = createRoundedRect(
                    context,
                    if (active) UiColors.AMBER_LIGHT else UiColors.BG_CARD,
                    10f,
                    if (active) UiColors.AMBER_PRIMARY else UiColors.BORDER_WARM,
                    1f
                )
            }
        }
        emojis.forEach { em ->
            val tv = TextView(context).apply {
                text = em
                textSize = 17f
                gravity = Gravity.CENTER
                setOnClickListener {
                    selectedEmoji = em
                    refreshEmojiPicker()
                }
            }
            emojiViews[em] = tv
            emojiWrap.addView(tv, ViewGroup.LayoutParams(context.dp(36), context.dp(36)))
        }
        refreshEmojiPicker()
        container.addView(emojiWrap, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(5); bottomMargin = context.dp(14) })

        lateinit var saveCatBtn: TextView
        lateinit var refreshCategoryTags: () -> Unit

        fun resetFormMode() {
            editingCatId = null
            nameInput.setText("")
            formTitleTv.text = "＋ 新增分類"
            saveCatBtn.text = "＋ 新增分類"
            refreshCategoryTags()
        }

        fun currentTxType(): TxType =
            if (typeSpinner.selectedItemPosition == 1) TxType.INCOME else TxType.EXPENSE

        refreshCategoryTags = {
            tagsWrap.removeAllViews()
            val activeType = currentTxType()
            val cats = repo.getCategories(activeType)

            cats.forEach { cat ->
                val isEditing = editingCatId == cat.id
                val isProtected = AppDefaults.isProtectedCategory(cat)

                val pill = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = createRoundedRect(
                        context,
                        if (isEditing) UiColors.AMBER_LIGHT else UiColors.BG_CARD,
                        99f,
                        if (isEditing) UiColors.AMBER_PRIMARY else UiColors.BORDER_WARM,
                        if (isEditing) 1.5f else 1f
                    )
                    setPadding(context.dp(10), context.dp(6), context.dp(10), context.dp(6))
                    setOnClickListener {
                        if (isProtected) {
                            onToast("「${cat.name}」為系統保留分類，無法修改或刪除喔 🔒")
                            return@setOnClickListener
                        }
                        if (editingCatId == cat.id) {
                            resetFormMode()
                        } else {
                            editingCatId = cat.id
                            selectedEmoji = cat.icon
                            nameInput.setText(cat.name)
                            formTitleTv.text = context.getString(R.string.category_edit_title, cat.icon, cat.name)
                            saveCatBtn.text = "儲存修改 ✓"
                            refreshEmojiPicker()
                            refreshCategoryTags()
                        }
                    }
                }

                pill.addView(TextView(context).apply {
                    text = context.getString(R.string.icon_name_pair, cat.icon, cat.name)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(UiColors.TEXT_MAIN)
                })

                if (!isProtected) {
                    val delBtn = TextView(context).apply {
                        text = "✕"
                        textSize = 11.5f
                        setTextColor(UiColors.TEXT_MUTED)
                        setPadding(context.dp(6), 0, 0, 0)
                        setOnClickListener {
                            if (cats.size <= 1) {
                                onToast("至少需保留一個分類喔！")
                                return@setOnClickListener
                            }
                            showWarmConfirmDialog(
                                context = context,
                                icon = "🏷️",
                                title = "確定刪除此分類？",
                                message = "確定要刪除「${cat.icon} ${cat.name}」分類嗎？",
                                confirmText = "確定刪除",
                                cancelText = "保留"
                            ) {
                                cats.removeAll { it.id == cat.id }
                                if (editingCatId == cat.id) {
                                    editingCatId = null
                                    nameInput.setText("")
                                    formTitleTv.text = "＋ 新增分類"
                                    saveCatBtn.text = "＋ 新增分類"
                                }
                                repo.save()
                                refreshCategoryTags()
                                onChanged()
                                onToast("已刪除「${cat.name}」分類")
                            }
                        }
                    }
                    pill.addView(delBtn)
                }

                tagsWrap.addView(pill)
            }
        }

        val (btnRow, okBtnRef) = makeButtonPair(
            context,
            "關閉",
            "＋ 新增分類",
            onCancel = { dialog.dismiss() },
            onConfirm = {
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    onToast("請輸入分類名稱")
                    return@makeButtonPair
                }
                val activeType = currentTxType()
                val cats = repo.getCategories(activeType)
                if (editingCatId != null) {
                    val target = cats.find { it.id == editingCatId }
                    if (target != null) {
                        target.name = name
                        target.icon = selectedEmoji
                    }
                    repo.save()
                    onToast("已更新「$selectedEmoji $name」分類 ✨")
                } else {
                    repo.addCategory(activeType, name, selectedEmoji)
                    onToast("已新增「$selectedEmoji $name」分類")
                }
                resetFormMode()
                onChanged()
            }
        )
        saveCatBtn = okBtnRef
        container.addView(btnRow)

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetFormMode()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        resetDefaultBtn.setOnClickListener {
            val defaultTotal = AppDefaults.defaultExpenseCategories().size + AppDefaults.defaultIncomeCategories().size
            showWarmConfirmDialog(
                context = context,
                icon = "🏷️",
                title = "確定還原預設分類？",
                message = "這會將支出與收入分類還原為系統預設的 $defaultTotal 種常用分類，確定要還原嗎？",
                confirmText = "確定還原",
                cancelText = "取消"
            ) {
                repo.resetDefaultCategories()
                resetFormMode()
                onChanged()
                onToast("已還原為預設收支分類 ✨")
            }
        }

        refreshCategoryTags()
        dialog.show()
    }

    // =========================================================================
    // 6. 資料匯出 / 匯入 選擇抽屜 (僅支援 CSV 與 JSON 檔案)
    // =========================================================================
    fun showDataIoSheet(
        context: Context,
        onExportClick: () -> Unit,
        onImportClick: () -> Unit
    ) {
        val (dialog, container, _) = buildSheetShell(context)

        container.addView(TextView(context).apply {
            text = "📦 資料匯出 / 匯入"
            textSize = 15.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        })
        container.addView(TextView(context).apply {
            text = "請選擇要把目前的記帳紀錄存起來，或把以前的紀錄讀進來"
            textSize = 12f
            setTextColor(UiColors.TEXT_SECONDARY)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(3) })

        container.addView(TextView(context).apply {
            text = context.getString(R.string.data_io_format_hint)
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(3); bottomMargin = context.dp(12) })

        fun makeChoiceCard(icon: String, title: String, desc: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = createRoundedRect(context, UiColors.BG_CARD, 16f, UiColors.BORDER_WARM, 1.5f)
                setPadding(context.dp(14), context.dp(13), context.dp(14), context.dp(13))
                setOnClickListener {
                    dialog.dismiss()
                    onClick()
                }

                addView(TextView(context).apply {
                    text = icon
                    textSize = 20f
                    gravity = Gravity.CENTER
                    background = createRoundedRect(context, UiColors.AMBER_LIGHT, 12f)
                }, LinearLayout.LayoutParams(context.dp(40), context.dp(40)))

                val textCol = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(context.dp(12), 0, 0, 0)
                    addView(TextView(context).apply {
                        text = title
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(UiColors.TEXT_MAIN)
                    })
                    addView(TextView(context).apply {
                        text = desc
                        textSize = 11.5f
                        setTextColor(UiColors.TEXT_SECONDARY)
                    })
                }
                addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
        }

        container.addView(
            makeChoiceCard(
                "📤",
                "匯出記帳資料",
                "把目前的記帳紀錄下載存檔（可用 Excel 開啟）",
                onExportClick
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = context.dp(9) }
        )

        container.addView(
            makeChoiceCard(
                "📥",
                "匯入記帳資料",
                "選擇手機或電腦裡以前存好的記帳檔案（僅支援 .csv / .json）",
                onImportClick
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = context.dp(14) }
        )

        val cancelBtn = TextView(context).apply {
            text = "取消"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
            background = createRoundedRect(context, UiColors.BG_INPUT, 12f, UiColors.BORDER_WARM, 1f)
            setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
            setOnClickListener { dialog.dismiss() }
        }
        container.addView(cancelBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        dialog.show()
    }
}
