package com.example.mymoney

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.mymoney.data.JsonImportResult
import com.example.mymoney.data.LedgerRepository
import com.example.mymoney.model.AccountType
import com.example.mymoney.model.AppDefaults
import com.example.mymoney.model.TxType
import com.example.mymoney.ui.BottomSheetDialogs
import com.example.mymoney.ui.DonutChartView
import com.example.mymoney.ui.DonutSlice
import com.example.mymoney.ui.UiColors
import com.example.mymoney.ui.createCircleDrawable
import com.example.mymoney.ui.createGradientRect
import com.example.mymoney.ui.createRoundedRect
import com.example.mymoney.ui.dp
import com.example.mymoney.ui.dpF
import com.example.mymoney.ui.formatCurrency
import com.example.mymoney.ui.formatDayTitle
import com.example.mymoney.ui.formatYmLabel
import com.example.mymoney.ui.showWarmConfirmDialog
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private enum class Tab { LEDGER, STATS, ACCOUNTS, MORE }

    private lateinit var repo: LedgerRepository

    private var currentYearMonth: YearMonth = YearMonth.now()
    private var activeTab: Tab = Tab.LEDGER
    private var filterTypeKey: String = "all" // all, expense, income, transfer
    private var filterAccountId: String = "all"
    private var statsType: TxType = TxType.EXPENSE
    private var selectedStatCategoryId: String? = null
    private var expandedRankCategoryId: String? = null

    private var pendingExportCsv: String? = null

    // 頂部元件
    private lateinit var monthSwitcherContainer: LinearLayout
    private lateinit var monthLabelTv: TextView

    // 主內容區與鎖定霧面遮罩
    private lateinit var mainScrollView: ScrollView
    private lateinit var pageLedger: LinearLayout
    private lateinit var pageStats: LinearLayout
    private lateinit var pageAccounts: LinearLayout
    private lateinit var pageMore: LinearLayout
    private lateinit var monthLockedOverlay: FrameLayout
    private lateinit var lockedTitleTv: TextView
    private lateinit var lockedDescTv: TextView
    private lateinit var unlockMonthBtn: TextView

    // 底部導覽與吐司
    private val navTabs = mutableMapOf<Tab, Pair<TextView, TextView>>()
    private lateinit var toastTv: TextView
    private val toastHandler = Handler(Looper.getMainLooper())
    private var toastHideRunnable: Runnable? = null

    // 首頁明細視圖元件
    private lateinit var monthExpenseTv: TextView
    private lateinit var monthNetBalanceTv: TextView
    private lateinit var monthIncomeTv: TextView
    private lateinit var heroNetWorthTv: TextView
    private lateinit var budgetStatusTagTv: TextView
    private lateinit var budgetProgressFill: View
    private lateinit var budgetUsedTv: TextView
    private lateinit var budgetRemainTv: TextView
    private lateinit var recurringBanner: LinearLayout
    private lateinit var recurringBannerTitleTv: TextView
    private lateinit var recurringBannerDescTv: TextView
    private val filterChipViews = mutableMapOf<String, TextView>()
    private lateinit var accountFilterSpinner: Spinner
    private var suppressAccountSpinnerEvent = false
    private lateinit var ledgerListContainer: LinearLayout

    // 統計分析視圖元件
    private lateinit var statsExpenseToggleBtn: TextView
    private lateinit var statsIncomeToggleBtn: TextView
    private lateinit var donutChartView: DonutChartView
    private lateinit var donutCenterLabelTv: TextView
    private lateinit var donutCenterAmountTv: TextView
    private lateinit var donutCenterPctTv: TextView
    private lateinit var categoryRankContainer: LinearLayout

    // 多帳戶視圖元件
    private lateinit var accNetWorthTv: TextView
    private lateinit var accAssetsTv: TextView
    private lateinit var accLiabilitiesTv: TextView
    private lateinit var accountsListContainer: LinearLayout

    // 更多 / 設定視圖元件
    private lateinit var moreRecurringSummaryTv: TextView
    private lateinit var moreCategorySummaryTv: TextView

    // Android 系統檔案匯出器 (SAF)
    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val content = pendingExportCsv
        pendingExportCsv = null
        if (uri == null || content == null) return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(StandardCharsets.UTF_8))
            }
            showToast("已匯出記帳資料 📊")
        } catch (_: Exception) {
            showToast("檔案匯出失敗，請確認儲存空間")
        }
    }

    // Android 系統檔案匯入器 (SAF)
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        handleImportedUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = LedgerRepository(this)
        buildRootUi()
        setupBackNavigation()
        renderAll()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeTab != Tab.LEDGER) {
                    switchTab(Tab.LEDGER)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun currentYmString(): String = currentYearMonth.format(AppDefaults.YM_FORMATTER)

    private fun showToast(message: String) {
        toastHideRunnable?.let { toastHandler.removeCallbacks(it) }
        toastTv.text = message
        toastTv.visibility = View.VISIBLE
        val r = Runnable { toastTv.visibility = View.GONE }
        toastHideRunnable = r
        toastHandler.postDelayed(r, 2300L)
    }

    // =========================================================================
    // 建置原生 Android 介面結構
    // =========================================================================
    private fun buildRootUi() {
        val rootFrame = FrameLayout(this).apply {
            setBackgroundColor(UiColors.BG_SCREEN)
            fitsSystemWindows = true
        }

        val rootColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. 頂部標題列與月份切換器
        rootColumn.addView(buildHeaderBar())

        // 2. 中央內容區 (ScrollView + 早於首次登入月份之霧面鎖定遮罩)
        val contentHost = FrameLayout(this)
        mainScrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        val scrollInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(22))
        }

        pageLedger = buildLedgerPage()
        pageStats = buildStatsPage()
        pageAccounts = buildAccountsPage()
        pageMore = buildMorePage()

        scrollInner.addView(pageLedger)
        scrollInner.addView(pageStats)
        scrollInner.addView(pageAccounts)
        scrollInner.addView(pageMore)
        mainScrollView.addView(scrollInner)

        monthLockedOverlay = buildMonthLockedOverlay()

        contentHost.addView(mainScrollView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        contentHost.addView(monthLockedOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        rootColumn.addView(contentHost, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        // 3. 底部導覽列 (5 格 + 中央凸起「＋ 記一筆」按鈕)
        rootColumn.addView(buildBottomNavBar())

        rootFrame.addView(rootColumn, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // 4. 輕量暖心吐司通知
        toastTv = TextView(this).apply {
            visibility = View.GONE
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor("#FFFDF8".toColorInt())
            background = createRoundedRect(this@MainActivity, "#EB3D3127".toColorInt(), 99f)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            elevation = dpF(8f)
        }
        rootFrame.addView(toastTv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            bottomMargin = dp(78)
        })

        setContentView(rootFrame)
    }

    private fun buildHeaderBar(): LinearLayout {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(UiColors.BG_SCREEN)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        val logo = TextView(this).apply {
            text = "🍯"
            textSize = 20f
            gravity = Gravity.CENTER
            background = createGradientRect(
                this@MainActivity,
                "#FCE4B4".toColorInt(),
                "#F5B851".toColorInt(),
                12f
            )
        }
        val brandTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), 0, dp(6), 0)
            addView(TextView(this@MainActivity).apply {
                text = "暖暖記帳"
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_MAIN)
            })
            addView(TextView(this@MainActivity).apply {
                text = "今天也要溫柔地打理生活 ✨"
                textSize = 11f
                setTextColor(UiColors.TEXT_SECONDARY)
            })
        }

        // 月份切換器
        monthSwitcherContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 99f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(4), dp(3), dp(4), dp(3))
        }
        val prevBtn = TextView(this).apply {
            text = "‹"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
            setOnClickListener {
                currentYearMonth = currentYearMonth.minusMonths(1)
                selectedStatCategoryId = null
                expandedRankCategoryId = null
                renderAll()
            }
        }
        monthLabelTv = TextView(this).apply {
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            setOnClickListener {
                currentYearMonth = YearMonth.now()
                selectedStatCategoryId = null
                expandedRankCategoryId = null
                renderAll()
                showToast("已回到本月")
            }
        }
        val nextBtn = TextView(this).apply {
            text = "›"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
            setOnClickListener {
                currentYearMonth = currentYearMonth.plusMonths(1)
                selectedStatCategoryId = null
                expandedRankCategoryId = null
                renderAll()
            }
        }

        monthSwitcherContainer.addView(prevBtn, LinearLayout.LayoutParams(dp(26), dp(26)))
        monthSwitcherContainer.addView(monthLabelTv)
        monthSwitcherContainer.addView(nextBtn, LinearLayout.LayoutParams(dp(26), dp(26)))

        header.addView(logo, LinearLayout.LayoutParams(dp(36), dp(36)))
        header.addView(brandTexts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(monthSwitcherContainer)
        return header
    }

    private fun buildMonthLockedOverlay(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            setBackgroundColor("#B3FAF5EC".toColorInt())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = createRoundedRect(
                this@MainActivity,
                "#F5FFFDF8".toColorInt(),
                24f,
                "#EADBC5".toColorInt(),
                1.5f
            )
            setPadding(dp(20), dp(22), dp(20), dp(18))
            elevation = dpF(10f)
        }

        card.addView(TextView(this).apply {
            text = "🔒"
            textSize = 25f
            gravity = Gravity.CENTER
            background = createRoundedRect(this@MainActivity, UiColors.AMBER_LIGHT, 18f)
        }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { bottomMargin = dp(10) })

        card.addView(TextView(this).apply {
            text = "未開啟月份"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_TEXT_DARK)
            background = createRoundedRect(this@MainActivity, UiColors.AMBER_LIGHT, 99f)
            setPadding(dp(11), dp(3), dp(11), dp(3))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        lockedTitleTv = TextView(this).apply {
            textSize = 16.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
            gravity = Gravity.CENTER
        }
        card.addView(lockedTitleTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) })

        lockedDescTv = TextView(this).apply {
            textSize = 12.5f
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
            setLineSpacing(dpF(2f), 1.15f)
        }
        card.addView(lockedDescTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        unlockMonthBtn = TextView(this).apply {
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = createGradientRect(
                this@MainActivity,
                "#F6B94D".toColorInt(),
                "#EA901A".toColorInt(),
                14f
            )
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { promptUnlockCurrentMonth() }
        }
        card.addView(unlockMonthBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        overlay.addView(card, FrameLayout.LayoutParams(
            dp(296),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
        return overlay
    }

    // =========================================================================
    // TAB 1: 首頁明細
    // =========================================================================
    private fun buildLedgerPage(): LinearLayout {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // 1. 本月收支總覽卡片
        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createGradientRect(
                this@MainActivity,
                UiColors.HERO_GRAD_START,
                UiColors.HERO_GRAD_END,
                20f,
                UiColors.HERO_BORDER,
                1f
            )
            setPadding(dp(16), dp(15), dp(16), dp(14))
        }

        val heroTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val heroLeft = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "本月總支出"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor("#84633B".toColorInt())
            })
            monthExpenseTv = TextView(this@MainActivity).apply {
                textSize = 27f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.EXPENSE)
            }
            addView(monthExpenseTv)
        }
        val balancePill = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            background = createRoundedRect(
                this@MainActivity,
                "#C7FFFDF8".toColorInt(),
                14f,
                "#B3EBC686".toColorInt(),
                1f
            )
            setPadding(dp(11), dp(6), dp(11), dp(6))
            addView(TextView(this@MainActivity).apply {
                text = "本月結餘"
                textSize = 10.5f
                setTextColor(UiColors.TEXT_SECONDARY)
                gravity = Gravity.END
            })
            monthNetBalanceTv = TextView(this@MainActivity).apply {
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_MAIN)
                gravity = Gravity.END
            }
            addView(monthNetBalanceTv)
        }
        heroTop.addView(heroLeft, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heroTop.addView(balancePill)
        heroCard.addView(heroTop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })

        heroCard.addView(View(this).apply {
            setBackgroundColor("#4DBA9256".toColorInt())
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            bottomMargin = dp(10)
        })

        val heroSub = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val subLeft = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(View(this@MainActivity).apply {
                background = createCircleDrawable(UiColors.INCOME)
            }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(5) })
            addView(TextView(this@MainActivity).apply {
                text = "本月收入 "
                textSize = 12f
                setTextColor("#7D6242".toColorInt())
            })
            monthIncomeTv = TextView(this@MainActivity).apply {
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_MAIN)
            }
            addView(monthIncomeTv)
        }
        val subRight = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setOnClickListener { switchTab(Tab.ACCOUNTS) }
            addView(View(this@MainActivity).apply {
                background = createCircleDrawable(UiColors.AMBER_PRIMARY)
            }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(5) })
            addView(TextView(this@MainActivity).apply {
                text = "總淨資產 "
                textSize = 12f
                setTextColor("#7D6242".toColorInt())
            })
            heroNetWorthTv = TextView(this@MainActivity).apply {
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_MAIN)
            }
            addView(heroNetWorthTv)
        }
        heroSub.addView(subLeft, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heroSub.addView(View(this).apply {
            setBackgroundColor("#59BA9256".toColorInt())
        }, LinearLayout.LayoutParams(dp(1), dp(14)).apply {
            marginStart = dp(8)
            marginEnd = dp(8)
        })
        heroSub.addView(subRight, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heroCard.addView(heroSub)

        page.addView(heroCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 2. 月度預算進度卡片
        val budgetCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 20f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
        val budgetHead = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        budgetHead.addView(TextView(this).apply {
            text = "🎯 本月預算進度"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        })
        budgetStatusTagTv = TextView(this).apply {
            textSize = 10.5f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        budgetHead.addView(budgetStatusTagTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(6) })
        budgetHead.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        budgetHead.addView(TextView(this).apply {
            text = "設定預算 ✎"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            setPadding(dp(6), dp(2), dp(4), dp(2))
            setOnClickListener {
                BottomSheetDialogs.showBudgetSheet(
                    context = this@MainActivity,
                    repo = repo,
                    onToast = ::showToast,
                    onSaved = ::renderAll
                )
            }
        })
        budgetCard.addView(budgetHead, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(9) })

        val track = FrameLayout(this).apply {
            background = createRoundedRect(this@MainActivity, UiColors.BG_INPUT, 99f)
        }
        budgetProgressFill = View(this)
        track.addView(budgetProgressFill, FrameLayout.LayoutParams(0, dp(10)))
        budgetCard.addView(track, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(10)
        ).apply { bottomMargin = dp(7) })

        val budgetMeta = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        budgetUsedTv = TextView(this).apply {
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
        }
        budgetRemainTv = TextView(this).apply {
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.END
        }
        budgetMeta.addView(budgetUsedTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        budgetMeta.addView(budgetRemainTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        budgetCard.addView(budgetMeta)

        page.addView(budgetCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 3. 固定週期收支待入帳橫幅
        recurringBanner = LinearLayout(this).apply {
            visibility = View.GONE
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedRect(this@MainActivity, UiColors.BANNER_BG, 20f, UiColors.BANNER_BORDER, 1f)
            setPadding(dp(13), dp(10), dp(13), dp(10))
        }
        recurringBanner.addView(TextView(this).apply {
            text = "🗓️"
            textSize = 20f
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(9) })

        val recBannerTexts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        recurringBannerTitleTv = TextView(this).apply {
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        }
        recurringBannerDescTv = TextView(this).apply {
            textSize = 11f
            setTextColor(UiColors.TEXT_SECONDARY)
        }
        recBannerTexts.addView(recurringBannerTitleTv)
        recBannerTexts.addView(recurringBannerDescTv)
        recurringBanner.addView(recBannerTexts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        recurringBanner.addView(TextView(this).apply {
            text = "一鍵入帳"
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createRoundedRect(this@MainActivity, UiColors.AMBER_PRIMARY, 99f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                val count = repo.applyPendingRecurring(currentYmString())
                if (count > 0) {
                    renderAll()
                    showToast("已一鍵寫入 $count 筆本月固定收支 ✨")
                }
            }
        })
        page.addView(recurringBanner, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 4. 篩選列 (僅保留 全部/支出/收入/轉帳 + 帳戶篩選，不顯示搜尋輸入框)
        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val chipsScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("all" to "全部", "expense" to "支出", "income" to "收入", "transfer" to "轉帳").forEach { (key, label) ->
            val chip = TextView(this).apply {
                text = label
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(11), dp(5), dp(11), dp(5))
                setOnClickListener {
                    filterTypeKey = key
                    renderLedgerTab()
                }
            }
            filterChipViews[key] = chip
            chipsRow.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(5) })
        }
        chipsScroll.addView(chipsRow)

        accountFilterSpinner = Spinner(this).apply {
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 10f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(4), dp(2), dp(4), dp(2))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (suppressAccountSpinnerEvent) return
                    val nextId = if (position <= 0) "all" else repo.data.accounts.getOrNull(position - 1)?.id ?: "all"
                    if (nextId != filterAccountId) {
                        filterAccountId = nextId
                        renderLedgerTab()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        filterRow.addView(chipsScroll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(6)
        })
        filterRow.addView(accountFilterSpinner, LinearLayout.LayoutParams(dp(118), LinearLayout.LayoutParams.WRAP_CONTENT))
        page.addView(filterRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 5. 每日明細清單
        ledgerListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(ledgerListContainer)

        return page
    }

    // =========================================================================
    // TAB 2: 統計分析
    // =========================================================================
    private fun buildStatsPage(): LinearLayout {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // 支出分類佔比 / 收入分類佔比 切換器
        val toggleBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 99f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        statsExpenseToggleBtn = TextView(this).apply {
            text = "支出分類佔比"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(7), dp(10), dp(7))
            setOnClickListener {
                statsType = TxType.EXPENSE
                selectedStatCategoryId = null
                expandedRankCategoryId = null
                renderStatsTab()
            }
        }
        statsIncomeToggleBtn = TextView(this).apply {
            text = "收入分類佔比"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(7), dp(10), dp(7))
            setOnClickListener {
                statsType = TxType.INCOME
                selectedStatCategoryId = null
                expandedRankCategoryId = null
                renderStatsTab()
            }
        }
        toggleBox.addView(statsExpenseToggleBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        toggleBox.addView(statsIncomeToggleBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(toggleBox, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 圓餅圖卡片 (精簡呈現，無下方重複分類圖例)
        val chartCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 20f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(14), dp(16), dp(14), dp(16))
        }
        val chartFrame = FrameLayout(this)
        donutChartView = DonutChartView(this)
        val centerInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        donutCenterLabelTv = TextView(this).apply {
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
        }
        donutCenterAmountTv = TextView(this).apply {
            textSize = 18.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
            gravity = Gravity.CENTER
        }
        donutCenterPctTv = TextView(this).apply {
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            background = createRoundedRect(this@MainActivity, UiColors.AMBER_LIGHT, 99f)
            setPadding(dp(9), dp(2), dp(9), dp(2))
            gravity = Gravity.CENTER
        }
        centerInfo.addView(donutCenterLabelTv)
        centerInfo.addView(donutCenterAmountTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(2); bottomMargin = dp(3) })
        centerInfo.addView(donutCenterPctTv)

        chartFrame.addView(donutChartView, FrameLayout.LayoutParams(dp(196), dp(196), Gravity.CENTER))
        chartFrame.addView(centerInfo, FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER))
        chartCard.addView(chartFrame, LinearLayout.LayoutParams(dp(196), dp(196)))
        page.addView(chartCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 分類排行標題列 + 🏷️ 分類管理快捷按鈕
        val rankHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        rankHeader.addView(TextView(this).apply {
            text = "📊 分類排行明細"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rankHeader.addView(TextView(this).apply {
            text = "🏷️ 分類管理"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            setPadding(dp(8), dp(4), dp(6), dp(4))
            setOnClickListener {
                BottomSheetDialogs.showCategorySheet(
                    context = this@MainActivity,
                    repo = repo,
                    preferredType = statsType,
                    onToast = ::showToast,
                    onChanged = ::renderAll
                )
            }
        })
        page.addView(rankHeader, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        categoryRankContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(categoryRankContainer)
        return page
    }

    // =========================================================================
    // TAB 3: 多帳戶管理
    // =========================================================================
    private fun buildAccountsPage(): LinearLayout {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val nwCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createGradientRect(
                this@MainActivity,
                UiColors.DARK_NW_START,
                UiColors.DARK_NW_END,
                20f
            )
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        nwCard.addView(TextView(this).apply {
            text = getString(R.string.net_worth_title)
            textSize = 11.5f
            setTextColor("#D8C7B5".toColorInt())
        })
        accNetWorthTv = TextView(this).apply {
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.DARK_NW_GOLD)
        }
        nwCard.addView(accNetWorthTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(2); bottomMargin = dp(12) })

        val subGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createRoundedRect(this@MainActivity, "#14FFFFFF".toColorInt(), 13f)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        val box1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "總資產"
                textSize = 10.5f
                setTextColor("#CBB9A6".toColorInt())
            })
            accAssetsTv = TextView(this@MainActivity).apply {
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }
            addView(accAssetsTv)
        }
        val box2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "信用卡/負債"
                textSize = 10.5f
                setTextColor("#CBB9A6".toColorInt())
            })
            accLiabilitiesTv = TextView(this@MainActivity).apply {
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }
            addView(accLiabilitiesTv)
        }
        subGrid.addView(box1, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        subGrid.addView(box2, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nwCard.addView(subGrid, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun makeDarkActionBtn(label: String, onClick: () -> Unit): TextView {
            return TextView(this).apply {
                text = label
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor("#FCE2B1".toColorInt())
                gravity = Gravity.CENTER
                background = createRoundedRect(
                    this@MainActivity,
                    "#2EF9C971".toColorInt(),
                    12f,
                    "#66F9C971".toColorInt(),
                    1f
                )
                setPadding(dp(10), dp(9), dp(10), dp(9))
                setOnClickListener { onClick() }
            }
        }
        val transferBtn = makeDarkActionBtn("⇄ 帳戶間轉帳") {
            if (repo.isMonthLocked(currentYmString())) {
                promptUnlockCurrentMonth()
            } else {
                BottomSheetDialogs.showTransactionSheet(
                    context = this@MainActivity,
                    repo = repo,
                    defaultType = TxType.TRANSFER,
                    currentViewedYm = currentYmString(),
                    onToast = ::showToast
                ) { switchYm ->
                    if (switchYm != null) currentYearMonth = YearMonth.parse(switchYm)
                    renderAll()
                }
            }
        }
        val addAccBtn = makeDarkActionBtn("＋ 新增帳戶") {
            BottomSheetDialogs.showAccountSheet(
                context = this@MainActivity,
                repo = repo,
                onToast = ::showToast,
                onSaved = ::renderAll
            )
        }
        actionRow.addView(transferBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })
        actionRow.addView(addAccBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nwCard.addView(actionRow)

        page.addView(nwCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        accountsListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(accountsListContainer)
        return page
    }

    // =========================================================================
    // TAB 4: 更多 / 設定
    // =========================================================================
    private fun buildMorePage(): LinearLayout {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        fun makeSingleCardButton(
            icon: String,
            title: String,
            onClick: () -> Unit
        ): Pair<LinearLayout, TextView> {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 20f, UiColors.BORDER_WARM, 1f)
                setPadding(dp(15), dp(13), dp(15), dp(13))
                setOnClickListener { onClick() }
            }
            card.addView(TextView(this).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                background = createRoundedRect(this@MainActivity, UiColors.AMBER_LIGHT, 12f)
            }, LinearLayout.LayoutParams(dp(40), dp(40)))

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), 0, dp(8), 0)
                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 13.5f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(UiColors.TEXT_MAIN)
                })
            }
            val subTv = TextView(this).apply {
                textSize = 11f
                setTextColor(UiColors.TEXT_SECONDARY)
            }
            col.addView(subTv)
            card.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(TextView(this).apply {
                text = "›"
                textSize = 21f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.AMBER_DARK)
            })
            return Pair(card, subTv)
        }

        // 1. 固定週期收支
        val (recCard, recSummary) = makeSingleCardButton("🗓️", "固定週期收支") {
            BottomSheetDialogs.showRecurringSheet(
                context = this@MainActivity,
                repo = repo,
                currentYm = currentYmString(),
                onToast = ::showToast,
                onChanged = ::renderAll
            )
        }
        moreRecurringSummaryTv = recSummary
        page.addView(recCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 2. 收支分類管理
        val (catCard, catSummary) = makeSingleCardButton("🏷️", "收支分類管理") {
            BottomSheetDialogs.showCategorySheet(
                context = this@MainActivity,
                repo = repo,
                onToast = ::showToast,
                onChanged = ::renderAll
            )
        }
        moreCategorySummaryTv = catSummary
        page.addView(catCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 3. 資料維護
        val maintenanceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 20f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
        maintenanceCard.addView(TextView(this).apply {
            text = "🧹 資料維護"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_MAIN)
        })
        maintenanceCard.addView(TextView(this).apply {
            text = "重置為預設範例資料或清空所有紀錄"
            textSize = 11f
            setTextColor(UiColors.TEXT_SECONDARY)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(2); bottomMargin = dp(10) })

        val maintenanceButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val reloadBtn = TextView(this).apply {
            text = "載入範例資料"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.TEXT_SECONDARY)
            gravity = Gravity.CENTER
            background = createRoundedRect(this@MainActivity, UiColors.BG_INPUT, 12f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                showWarmConfirmDialog(
                    context = this@MainActivity,
                    icon = "🔄",
                    title = "確定重新載入範例資料？",
                    message = "載入預設範例資料將會覆蓋並清除您目前已記錄的所有帳目與設定，確定要繼續嗎？",
                    confirmText = "確定載入",
                    cancelText = "取消"
                ) {
                    repo.resetToDefaultData()
                    renderAll()
                    showToast("已重新載入預設範例資料 ✨")
                }
            }
        }
        val clearBtn = TextView(this).apply {
            text = "清空全部資料"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.EXPENSE)
            gravity = Gravity.CENTER
            background = createRoundedRect(this@MainActivity, UiColors.EXPENSE_BG, 12f, UiColors.EXPENSE_BORDER, 1f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                val txCount = repo.data.transactions.size
                val recCount = repo.data.recurring.size
                showWarmConfirmDialog(
                    context = this@MainActivity,
                    icon = "🧹",
                    title = "確定清空全部資料？",
                    message = "即將永久清空目前共 $txCount 筆記帳明細、$recCount 筆固定週期設定，並將所有帳戶初始餘額歸零（建議清空前先匯出備份），此操作無法復原！",
                    confirmText = "確定清空",
                    cancelText = "先不要"
                ) {
                    repo.clearAllData()
                    renderAll()
                    showToast("已清空所有帳目紀錄並將帳戶歸零")
                }
            }
        }
        maintenanceButtons.addView(reloadBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })
        maintenanceButtons.addView(clearBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        maintenanceCard.addView(maintenanceButtons)
        page.addView(maintenanceCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        // 4. 一鈕式：資料匯出 / 匯入 (置於最下方)
        val (ioCard, ioSub) = makeSingleCardButton("📦", "資料匯出 / 匯入") {
            BottomSheetDialogs.showDataIoSheet(
                context = this@MainActivity,
                onExportClick = {
                    pendingExportCsv = repo.exportToCsv()
                    exportCsvLauncher.launch("暖暖記帳_備份資料_${currentYmString()}.csv")
                },
                onImportClick = {
                    importFileLauncher.launch(
                        arrayOf("text/csv", "text/comma-separated-values", "application/json", "text/plain", "*/*")
                    )
                }
            )
        }
        ioSub.text = "將記帳紀錄存成檔案備份，或讀回以前的紀錄"
        page.addView(ioCard)

        return page
    }

    // =========================================================================
    // 底部導覽列
    // =========================================================================
    private fun buildBottomNavBar(): FrameLayout {
        val wrapper = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(UiColors.BG_CARD)
        }

        fun makeNavSlot(tab: Tab, icon: String, label: String): LinearLayout {
            val slot = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(6))
                setOnClickListener { switchTab(tab) }
            }
            val iconTv = TextView(this).apply {
                text = icon
                textSize = 18.5f
                gravity = Gravity.CENTER
            }
            val labelTv = TextView(this).apply {
                text = label
                textSize = 10.5f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            navTabs[tab] = Pair(iconTv, labelTv)
            slot.addView(iconTv)
            slot.addView(labelTv)
            return slot
        }

        bar.addView(makeNavSlot(Tab.LEDGER, "📒", "明細"), LinearLayout.LayoutParams(0, dp(62), 1f))
        bar.addView(makeNavSlot(Tab.STATS, "🍩", "統計"), LinearLayout.LayoutParams(0, dp(62), 1f))

        // 中央空白佔位，上方疊加凸起「＋ 記一筆」
        val fabHolder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { openAddTransactionAction() }
        }
        fabHolder.addView(View(this), LinearLayout.LayoutParams(dp(46), dp(34)))
        fabHolder.addView(TextView(this).apply {
            text = "記一筆"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(UiColors.AMBER_DARK)
            gravity = Gravity.CENTER
        })
        bar.addView(fabHolder, LinearLayout.LayoutParams(0, dp(62), 1f))

        bar.addView(makeNavSlot(Tab.ACCOUNTS, "👛", "帳戶"), LinearLayout.LayoutParams(0, dp(62), 1f))
        bar.addView(makeNavSlot(Tab.MORE, "⚙️", "更多"), LinearLayout.LayoutParams(0, dp(62), 1f))

        val topBorder = View(this).apply { setBackgroundColor(UiColors.BORDER_WARM) }

        val fabBtn = TextView(this).apply {
            text = "＋"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = createGradientRect(
                this@MainActivity,
                "#F6B94D".toColorInt(),
                "#EA901A".toColorInt(),
                16f,
                UiColors.BG_CARD,
                3f
            )
            elevation = dpF(8f)
            setOnClickListener { openAddTransactionAction() }
        }

        wrapper.addView(bar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(62),
            Gravity.BOTTOM
        ))
        wrapper.addView(topBorder, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(1),
            Gravity.BOTTOM
        ).apply { bottomMargin = dp(62) })
        wrapper.addView(fabBtn, FrameLayout.LayoutParams(
            dp(48),
            dp(48),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = dp(24) })

        return wrapper
    }

    private fun openAddTransactionAction() {
        if (activeTab != Tab.MORE && repo.isMonthLocked(currentYmString())) {
            promptUnlockCurrentMonth()
            return
        }
        BottomSheetDialogs.showTransactionSheet(
            context = this,
            repo = repo,
            currentViewedYm = currentYmString(),
            onToast = ::showToast
        ) { switchYm ->
            if (switchYm != null) {
                runCatching { currentYearMonth = YearMonth.parse(switchYm) }
            }
            renderAll()
        }
    }

    private fun switchTab(tab: Tab) {
        activeTab = tab
        pageLedger.visibility = if (tab == Tab.LEDGER) View.VISIBLE else View.GONE
        pageStats.visibility = if (tab == Tab.STATS) View.VISIBLE else View.GONE
        pageAccounts.visibility = if (tab == Tab.ACCOUNTS) View.VISIBLE else View.GONE
        pageMore.visibility = if (tab == Tab.MORE) View.VISIBLE else View.GONE

        // 設定頁中不顯示月份切換器
        monthSwitcherContainer.visibility = if (tab == Tab.MORE) View.GONE else View.VISIBLE

        mainScrollView.scrollTo(0, 0)
        renderAll()
    }

    private fun promptUnlockCurrentMonth() {
        val ym = currentYmString()
        val label = "${currentYearMonth.year}年 ${currentYearMonth.monthValue}月"
        val firstLabel = formatYmLabel(repo.data.firstLoginMonth)
        showWarmConfirmDialog(
            context = this,
            icon = "🔓",
            title = "確定開啟 $label 帳本？",
            message = "此月份早於您的首次登入月份（$firstLabel）。開啟後即可解除霧面鎖定，並在此月份查閱或補記帳目，確定要開啟嗎？",
            confirmText = "確定開啟",
            cancelText = "先不要",
            warmMode = true
        ) {
            repo.unlockMonth(ym)
            renderAll()
            showToast("已開啟 $label 帳本 ✨")
        }
    }

    // =========================================================================
    // 總畫面渲染與更新
    // =========================================================================
    private fun renderAll() {
        val locked = repo.isMonthLocked(currentYmString())
        monthLabelTv.text = getString(
            if (locked) R.string.month_label_locked_format else R.string.month_label_format,
            currentYearMonth.year,
            currentYearMonth.monthValue
        )

        navTabs.forEach { (tab, pair) ->
            val active = tab == activeTab
            pair.first.scaleX = if (active) 1.08f else 1f
            pair.first.scaleY = if (active) 1.08f else 1f
            pair.second.setTextColor(if (active) UiColors.AMBER_DARK else UiColors.TEXT_MUTED)
        }

        refreshAccountFilterSpinner()
        renderLedgerTab()
        renderStatsTab()
        renderAccountsTab()
        renderMoreTab()
        renderMonthLockOverlay()
    }

    private fun renderMonthLockOverlay() {
        val ym = currentYmString()
        val locked = activeTab != Tab.MORE && repo.isMonthLocked(ym)
        if (locked) {
            val label = getString(
                R.string.month_label_format,
                currentYearMonth.year,
                currentYearMonth.monthValue
            )
            val firstLabel = formatYmLabel(repo.data.firstLoginMonth)
            lockedTitleTv.text = getString(R.string.locked_month_title, label)
            lockedDescTv.text = getString(R.string.locked_month_desc, firstLabel)
            unlockMonthBtn.text = getString(R.string.unlock_month_btn, label)

            mainScrollView.scrollTo(0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mainScrollView.setRenderEffect(RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP))
            } else {
                mainScrollView.alpha = 0.35f
            }
            monthLockedOverlay.visibility = View.VISIBLE
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mainScrollView.setRenderEffect(null)
            } else {
                mainScrollView.alpha = 1f
            }
            monthLockedOverlay.visibility = View.GONE
        }
    }

    private fun refreshAccountFilterSpinner() {
        val accounts = repo.data.accounts
        if (filterAccountId != "all" && accounts.none { it.id == filterAccountId }) {
            filterAccountId = "all"
        }
        val labels = listOf("所有帳戶") + accounts.map { "${it.icon} ${it.name}" }
        val selectedIndex = if (filterAccountId == "all") 0 else (accounts.indexOfFirst { it.id == filterAccountId } + 1).coerceAtLeast(0)

        suppressAccountSpinnerEvent = true
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.textSize = 11.5f
                v.setTypeface(null, Typeface.BOLD)
                v.setTextColor(UiColors.TEXT_SECONDARY)
                v.isSingleLine = true
                v.ellipsize = TextUtils.TruncateAt.END
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.textSize = 12.5f
                v.setTextColor(UiColors.TEXT_MAIN)
                v.setPadding(dp(12), dp(10), dp(12), dp(10))
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountFilterSpinner.adapter = adapter
        accountFilterSpinner.setSelection(selectedIndex, false)
        suppressAccountSpinnerEvent = false
    }

    private fun renderLedgerTab() {
        val ym = currentYmString()
        val monthTxs = repo.data.transactions.filter { it.date.startsWith(ym) }

        var monthExpense = 0L
        var monthIncome = 0L
        monthTxs.forEach { tx ->
            if (tx.type == TxType.EXPENSE) monthExpense += tx.amount
            if (tx.type == TxType.INCOME) monthIncome += tx.amount
        }

        val netBalance = monthIncome - monthExpense
        val balances = repo.computeAccountBalances(ym)
        val totalNetWorth = balances.values.sum()

        monthExpenseTv.text = formatCurrency(monthExpense)
        monthIncomeTv.text = formatCurrency(monthIncome)
        val formattedBalance = formatCurrency(netBalance)
        monthNetBalanceTv.text = if (netBalance >= 0L) {
            getString(R.string.signed_amount_plus, formattedBalance)
        } else {
            formattedBalance
        }
        heroNetWorthTv.text = getString(R.string.net_worth_arrow, formatCurrency(totalNetWorth))

        // 預算進度條
        val budget = repo.data.monthlyBudget
        val pctClamped = if (budget > 0L) ((monthExpense.toDouble() / budget.toDouble()) * 100).roundToInt().coerceIn(0, 100) else 0
        val rawPct = if (budget > 0L) ((monthExpense.toDouble() / budget.toDouble()) * 100).roundToInt() else 0
        val remain = budget - monthExpense

        when {
            budget <= 0L -> {
                budgetStatusTagTv.text = "尚未設定預算"
                budgetStatusTagTv.setTextColor(UiColors.INCOME)
                budgetStatusTagTv.background = createRoundedRect(this, UiColors.INCOME_BG, 99f)
                budgetProgressFill.background = createGradientRect(this, "#F6C36B".toColorInt(), UiColors.AMBER_PRIMARY, 99f)
            }
            rawPct >= 100 -> {
                budgetStatusTagTv.text = "已超支提醒 🐾"
                budgetStatusTagTv.setTextColor(UiColors.EXPENSE)
                budgetStatusTagTv.background = createRoundedRect(this, UiColors.EXPENSE_BG, 99f)
                budgetProgressFill.background = createGradientRect(this, "#E8836B".toColorInt(), "#D9534F".toColorInt(), 99f)
            }
            rawPct >= 80 -> {
                budgetStatusTagTv.text = "接近預算上限"
                budgetStatusTagTv.setTextColor(UiColors.WARN_TEXT)
                budgetStatusTagTv.background = createRoundedRect(this, UiColors.WARN_BG, 99f)
                budgetProgressFill.background = createGradientRect(this, "#F5B041".toColorInt(), "#E67E22".toColorInt(), 99f)
            }
            else -> {
                budgetStatusTagTv.text = "安心花費中 ✨"
                budgetStatusTagTv.setTextColor(UiColors.INCOME)
                budgetStatusTagTv.background = createRoundedRect(this, UiColors.INCOME_BG, 99f)
                budgetProgressFill.background = createGradientRect(this, "#F6C36B".toColorInt(), UiColors.AMBER_PRIMARY, 99f)
            }
        }

        budgetProgressFill.post {
            val parentWidth = (budgetProgressFill.parent as? View)?.width ?: 0
            if (parentWidth > 0) {
                val lp = budgetProgressFill.layoutParams
                lp.width = (parentWidth * pctClamped / 100f).toInt()
                budgetProgressFill.layoutParams = lp
            }
        }
        budgetUsedTv.text = getString(R.string.budget_used_format, formatCurrency(monthExpense), rawPct)
        budgetRemainTv.text = if (remain >= 0L) {
            getString(R.string.budget_remain_format, formatCurrency(remain))
        } else {
            getString(R.string.budget_over_format, formatCurrency(abs(remain)))
        }

        // 固定週期收支待入帳橫幅
        val pendingRecs = repo.data.recurring.filter { it.lastAppliedMonth != ym }
        if (pendingRecs.isNotEmpty()) {
            recurringBanner.visibility = View.VISIBLE
            recurringBannerTitleTv.text = getString(R.string.pending_recurring_title, pendingRecs.size.toString())
            recurringBannerDescTv.text = pendingRecs.take(2).joinToString("、") { it.note }
        } else {
            recurringBanner.visibility = View.GONE
        }

        // 篩選按鈕高亮
        filterChipViews.forEach { (key, chip) ->
            val active = key == filterTypeKey
            chip.background = createRoundedRect(
                this,
                if (active) UiColors.AMBER_PRIMARY else UiColors.BG_CARD,
                99f,
                if (active) UiColors.AMBER_PRIMARY else UiColors.BORDER_WARM,
                1f
            )
            chip.setTextColor(if (active) Color.WHITE else UiColors.TEXT_SECONDARY)
        }

        // 篩選交易並依日期新到舊分組
        val filtered = monthTxs.filter { tx ->
            if (filterTypeKey != "all" && tx.type.key != filterTypeKey) return@filter false
            if (filterAccountId != "all" && tx.accountId != filterAccountId && tx.toAccountId != filterAccountId) {
                return@filter false
            }
            true
        }.sortedByDescending { it.date }

        ledgerListContainer.removeAllViews()
        if (filtered.isEmpty()) {
            ledgerListContainer.addView(buildEmptyCard("🍯", "這個月份還沒有符合的帳目喔", "點擊下方中央的「＋記一筆」開始記錄溫暖生活吧！"))
            return
        }

        val grouped = filtered.groupBy { it.date }
        grouped.keys.sortedDescending().forEach { dateStr ->
            val items = grouped[dateStr].orEmpty()
            val dayInc = items.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            val dayExp = items.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 20f, UiColors.BORDER_WARM, 1f)
                setPadding(dp(1), dp(1), dp(1), dp(1))
            }

            // 日期群組頂部條
            val dayHead = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = createRoundedRect(this@MainActivity, UiColors.BG_DAY_HEADER, 0f).apply {
                    val r = dpF(19f)
                    cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                }
                setPadding(dp(13), dp(9), dp(13), dp(9))
            }
            dayHead.addView(TextView(this).apply {
                text = formatDayTitle(dateStr)
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_SECONDARY)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val sumParts = mutableListOf<String>()
            if (dayInc > 0L) sumParts.add("收 ${formatCurrency(dayInc)}")
            if (dayExp > 0L) sumParts.add("支 ${formatCurrency(dayExp)}")
            dayHead.addView(TextView(this).apply {
                text = sumParts.joinToString("  ")
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_SECONDARY)
            })
            card.addView(dayHead)
            card.addView(View(this).apply { setBackgroundColor(UiColors.BORDER_WARM) }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ))

            items.forEachIndexed { idx, tx ->
                val cat = repo.getCategoryById(tx.categoryId, tx.type)
                val acc = repo.getAccountById(tx.accountId)
                val badgeText = if (tx.type == TxType.TRANSFER) {
                    val toAcc = tx.toAccountId?.let { repo.getAccountById(it) }
                    "${acc.name} ➔ ${toAcc?.name ?: acc.name}"
                } else {
                    "${acc.icon} ${acc.name}"
                }

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(13), dp(10), dp(13), dp(10))
                    setOnClickListener {
                        BottomSheetDialogs.showTransactionSheet(
                            context = this@MainActivity,
                            repo = repo,
                            editingTx = tx,
                            currentViewedYm = ym,
                            onToast = ::showToast
                        ) { switchYm ->
                            if (switchYm != null) {
                                runCatching { currentYearMonth = YearMonth.parse(switchYm) }
                            }
                            renderAll()
                        }
                    }
                }

                val iconBg = when (tx.type) {
                    TxType.EXPENSE -> UiColors.EXPENSE_BG
                    TxType.INCOME -> UiColors.INCOME_BG
                    TxType.TRANSFER -> UiColors.TRANSFER_BG
                }
                row.addView(TextView(this).apply {
                    text = cat.icon
                    textSize = 19f
                    gravity = Gravity.CENTER
                    background = createRoundedRect(this@MainActivity, iconBg, 13f)
                }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })

                val infoCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                val titleLine = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                titleLine.addView(TextView(this).apply {
                    text = cat.name
                    textSize = 13.5f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(UiColors.TEXT_MAIN)
                })
                titleLine.addView(TextView(this).apply {
                    text = badgeText
                    textSize = 10f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(UiColors.TEXT_SECONDARY)
                    background = createRoundedRect(this@MainActivity, UiColors.BG_INPUT, 6f)
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(6) })

                infoCol.addView(titleLine)
                infoCol.addView(TextView(this).apply {
                    text = tx.note.ifBlank { "無備註" }
                    textSize = 11.5f
                    setTextColor(UiColors.TEXT_SECONDARY)
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) })
                row.addView(infoCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                val sign = when (tx.type) {
                    TxType.INCOME -> "+"
                    TxType.EXPENSE -> "-"
                    TxType.TRANSFER -> ""
                }
                val amtColor = when (tx.type) {
                    TxType.EXPENSE -> UiColors.EXPENSE
                    TxType.INCOME -> UiColors.INCOME
                    TxType.TRANSFER -> UiColors.TRANSFER
                }
                row.addView(TextView(this).apply {
                    text = getString(R.string.signed_amount_pair, sign, formatCurrency(tx.amount))
                    textSize = 14.5f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(amtColor)
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) })

                card.addView(row)
                if (idx < items.lastIndex) {
                    card.addView(View(this).apply { setBackgroundColor(UiColors.DIVIDER) }, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ))
                }
            }

            ledgerListContainer.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(11) })
        }
    }

    private fun renderStatsTab() {
        val isExp = statsType == TxType.EXPENSE
        statsExpenseToggleBtn.background = if (isExp) createRoundedRect(this, UiColors.AMBER_PRIMARY, 99f) else null
        statsExpenseToggleBtn.setTextColor(if (isExp) Color.WHITE else UiColors.TEXT_SECONDARY)
        statsIncomeToggleBtn.background = if (!isExp) createRoundedRect(this, UiColors.AMBER_PRIMARY, 99f) else null
        statsIncomeToggleBtn.setTextColor(if (!isExp) Color.WHITE else UiColors.TEXT_SECONDARY)

        val ym = currentYmString()
        val txs = repo.data.transactions.filter { it.date.startsWith(ym) && it.type == statsType }
        val totalAmount = txs.sumOf { it.amount }

        val grouped = txs.groupBy { it.categoryId }.entries
            .map { (catId, list) -> Triple(catId, list.sumOf { it.amount }, list) }
            .sortedByDescending { it.second }

        categoryRankContainer.removeAllViews()

        if (grouped.isEmpty() || totalAmount <= 0L) {
            donutChartView.setData(emptyList(), 0L, null) {}
            donutCenterLabelTv.text = if (isExp) "本月總支出" else "本月總收入"
            donutCenterAmountTv.text = getString(R.string.zero_currency)
            donutCenterPctTv.text = "尚無資料"
            categoryRankContainer.addView(
                buildEmptyCard(
                    "🍩",
                    "本月尚無${if (isExp) "支出" else "收入"}統計資料",
                    "新增幾筆帳目後，這裡就會自動繪製療癒圓餅圖囉！"
                )
            )
            return
        }

        val slices = grouped.mapIndexed { idx, (catId, amt, _) ->
            DonutSlice(catId, amt, AppDefaults.CHART_COLORS[idx % AppDefaults.CHART_COLORS.size])
        }

        donutChartView.setData(slices, totalAmount, selectedStatCategoryId) { tappedCatId ->
            selectedStatCategoryId = if (selectedStatCategoryId == tappedCatId) null else tappedCatId
            renderStatsTab()
        }

        val activeEntry = grouped.find { it.first == selectedStatCategoryId }
        if (activeEntry != null) {
            val cat = repo.getCategoryById(activeEntry.first, statsType)
            val pct = ((activeEntry.second.toDouble() / totalAmount.toDouble()) * 1000).roundToInt() / 10.0
            donutCenterLabelTv.text = getString(R.string.icon_name_pair, cat.icon, cat.name)
            donutCenterAmountTv.text = formatCurrency(activeEntry.second)
            donutCenterPctTv.text = getString(R.string.stats_pct_label, pct.toString())
        } else {
            donutCenterLabelTv.text = if (isExp) "本月總支出" else "本月總收入"
            donutCenterAmountTv.text = formatCurrency(totalAmount)
            donutCenterPctTv.text = getString(R.string.stats_total_count, txs.size)
        }

        grouped.forEachIndexed { idx, (catId, amt, items) ->
            val cat = repo.getCategoryById(catId, statsType)
            val color = AppDefaults.CHART_COLORS[idx % AppDefaults.CHART_COLORS.size]
            val pct = ((amt.toDouble() / totalAmount.toDouble()) * 1000).roundToInt() / 10.0
            val isExpanded = expandedRankCategoryId == catId

            val rankCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = createRoundedRect(
                    this@MainActivity,
                    UiColors.BG_CARD,
                    16f,
                    if (selectedStatCategoryId == catId) "#E5C899".toColorInt() else UiColors.BORDER_WARM,
                    1f
                )
                setPadding(dp(13), dp(11), dp(13), dp(11))
                setOnClickListener {
                    expandedRankCategoryId = if (expandedRankCategoryId == catId) null else catId
                    selectedStatCategoryId = catId
                    renderStatsTab()
                }
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(this).apply {
                text = cat.icon
                textSize = 16f
                gravity = Gravity.CENTER
                background = createRoundedRect(this@MainActivity, UiColors.BG_INPUT, 10f)
            }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(8) })

            val nameCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@MainActivity).apply {
                    text = cat.name
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(UiColors.TEXT_MAIN)
                })
                addView(TextView(this@MainActivity).apply {
                    text = getString(R.string.rank_record_count, items.size)
                    textSize = 11f
                    setTextColor(UiColors.TEXT_MUTED)
                })
            }
            topRow.addView(nameCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            topRow.addView(TextView(this).apply {
                text = getString(R.string.rank_amount_pct, formatCurrency(amt), pct.toString())
                textSize = 13.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_MAIN)
            })
            rankCard.addView(topRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(7) })

            val barTrack = FrameLayout(this).apply {
                background = createRoundedRect(this@MainActivity, UiColors.BG_INPUT, 99f)
            }
            val barFill = View(this).apply {
                background = createRoundedRect(this@MainActivity, color, 99f)
            }
            barTrack.addView(barFill, FrameLayout.LayoutParams(0, dp(7)))
            barFill.post {
                val pw = barTrack.width
                if (pw > 0) {
                    val lp = barFill.layoutParams
                    lp.width = (pw * pct.coerceAtLeast(4.0) / 100.0).toInt()
                    barFill.layoutParams = lp
                }
            }
            rankCard.addView(barTrack, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(7)
            ))

            if (isExpanded) {
                rankCard.addView(View(this).apply { setBackgroundColor(UiColors.BORDER_WARM) }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply { topMargin = dp(9); bottomMargin = dp(7) })

                items.forEach { t ->
                    val subRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    subRow.addView(TextView(this).apply {
                        text = getString(R.string.rank_sub_item, t.date.drop(5), t.note.ifBlank { cat.name })
                        textSize = 11.5f
                        setTextColor(UiColors.TEXT_SECONDARY)
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    subRow.addView(TextView(this).apply {
                        text = formatCurrency(t.amount)
                        textSize = 11.5f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(UiColors.TEXT_MAIN)
                    })
                    rankCard.addView(subRow, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(4) })
                }
            }

            categoryRankContainer.addView(rankCard, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
        }
    }

    private fun renderAccountsTab() {
        val balances = repo.computeAccountBalances(currentYmString())
        var totalAssets = 0L
        var totalLiabilities = 0L

        repo.data.accounts.forEach { acc ->
            val bal = balances[acc.id] ?: 0L
            if (bal >= 0L) totalAssets += bal else totalLiabilities += abs(bal)
        }
        val netWorth = totalAssets - totalLiabilities

        accNetWorthTv.text = formatCurrency(netWorth)
        accAssetsTv.text = formatCurrency(totalAssets)
        accLiabilitiesTv.text = formatCurrency(totalLiabilities)

        accountsListContainer.removeAllViews()
        val grouped = repo.data.accounts.groupBy { it.type }

        AccountType.entries.forEach { accType ->
            val list = grouped[accType].orEmpty()
            if (list.isEmpty()) return@forEach

            accountsListContainer.addView(TextView(this).apply {
                text = accType.groupTitle
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_SECONDARY)
                setPadding(dp(4), dp(2), dp(4), dp(6))
            })

            list.forEach { acc ->
                val bal = balances[acc.id] ?: 0L
                val itemCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 16f, UiColors.BORDER_WARM, 1f)
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    setOnClickListener {
                        BottomSheetDialogs.showAccountSheet(
                            context = this@MainActivity,
                            repo = repo,
                            editingAccount = acc,
                            onToast = ::showToast,
                            onSaved = ::renderAll
                        )
                    }
                }
                itemCard.addView(TextView(this).apply {
                    text = acc.icon
                    textSize = 19f
                    gravity = Gravity.CENTER
                    background = createRoundedRect(this@MainActivity, UiColors.AMBER_LIGHT, 12f)
                }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })

                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(this@MainActivity).apply {
                        text = acc.name
                        textSize = 13.5f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(UiColors.TEXT_MAIN)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = getString(R.string.account_initial_meta, formatCurrency(acc.initialBalance))
                        textSize = 11f
                        setTextColor(UiColors.TEXT_MUTED)
                    })
                }
                itemCard.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                itemCard.addView(TextView(this).apply {
                    text = formatCurrency(bal)
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(if (bal < 0L) UiColors.EXPENSE else UiColors.TEXT_MAIN)
                })

                accountsListContainer.addView(itemCard, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) })
            }
        }
    }

    private fun renderMoreTab() {
        val recCount = repo.data.recurring.size
        moreRecurringSummaryTv.text = if (recCount > 0) {
            "目前已設定 $recCount 筆固定週期項目"
        } else {
            "管理每月房租、訂閱、固定薪資"
        }

        val expCount = repo.data.expenseCategories.size
        val incCount = repo.data.incomeCategories.size
        moreCategorySummaryTv.text = getString(R.string.more_category_summary, expCount, incCount)
    }

    private fun buildEmptyCard(icon: String, title: String, desc: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = createRoundedRect(this@MainActivity, UiColors.BG_CARD, 20f, UiColors.BORDER_WARM, 1f)
            setPadding(dp(18), dp(28), dp(18), dp(28))
            addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 34f
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(UiColors.TEXT_MAIN)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(4) })
            addView(TextView(this@MainActivity).apply {
                text = desc
                textSize = 12f
                setTextColor(UiColors.TEXT_SECONDARY)
                gravity = Gravity.CENTER
            })
        }
    }

    // =========================================================================
    // 檔案匯入解析 (僅支援 CSV 與 JSON)
    // =========================================================================
    private fun handleImportedUri(uri: Uri) {
        val filename = queryDisplayName(uri)
        val lower = filename.lowercase()
        if (!lower.endsWith(".csv") && !lower.endsWith(".json")) {
            showToast("僅支援 CSV 與 JSON 格式的檔案喔！")
            return
        }

        val text = try {
            contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
        if (text.isNullOrBlank()) {
            showToast("檔案內容為空或無法讀取")
            return
        }

        if (lower.endsWith(".json")) {
            when (val res = repo.parseJsonBackup(text)) {
                is JsonImportResult.FullBackup -> {
                    showWarmConfirmDialog(
                        context = this,
                        icon = "📥",
                        title = "確認匯入記帳資料？",
                        message = "從「$filename」找到 ${res.data.transactions.size} 筆記帳紀錄與帳戶設定，匯入將會覆蓋目前的帳本資料，確定要繼續嗎？",
                        confirmText = "確定匯入",
                        cancelText = "取消"
                    ) {
                        repo.save(res.data)
                        renderAll()
                        showToast("已成功匯入記帳資料 🎉")
                    }
                }
                is JsonImportResult.TransactionList -> {
                    showWarmConfirmDialog(
                        context = this,
                        icon = "📥",
                        title = "確認匯入記帳資料？",
                        message = "從「$filename」找到 ${res.items.size} 筆記帳紀錄，確定要讀進目前的帳本中嗎？",
                        confirmText = "確定匯入",
                        cancelText = "取消"
                    ) {
                        repo.data.transactions.addAll(res.items)
                        repo.save()
                        renderAll()
                        showToast("已成功匯入 ${res.items.size} 筆記帳紀錄 📥")
                    }
                }
                is JsonImportResult.Invalid -> showToast(res.message)
            }
        } else {
            val newTxs = repo.parseCsvTransactions(text)
            if (newTxs.isEmpty()) {
                showToast("檔案中沒有找到有效的記帳資料")
            } else {
                showWarmConfirmDialog(
                    context = this,
                    icon = "📥",
                    title = "確認匯入記帳資料？",
                    message = "從「$filename」找到 ${newTxs.size} 筆記帳紀錄，確定要讀進目前的帳本中嗎？",
                    confirmText = "確定匯入",
                    cancelText = "取消"
                ) {
                    repo.data.transactions.addAll(newTxs)
                    repo.save()
                    renderAll()
                    showToast("已成功匯入 ${newTxs.size} 筆記帳紀錄 📥")
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx).orEmpty()
            }
        }
        return uri.lastPathSegment.orEmpty()
    }
}
