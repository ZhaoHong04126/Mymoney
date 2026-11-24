package com.example.mymoney.model

import androidx.core.graphics.toColorInt
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class TxType(val key: String, val label: String) {
    EXPENSE("expense", "支出"),
    INCOME("income", "收入"),
    TRANSFER("transfer", "轉帳");

    companion object {
        fun fromKey(key: String?): TxType = when (key) {
            "income" -> INCOME
            "transfer" -> TRANSFER
            else -> EXPENSE
        }
    }
}

enum class AccountType(val key: String, val groupTitle: String, val spinnerLabel: String) {
    CASH("cash", "💵 現金錢包", "💵 現金錢包"),
    BANK("bank", "🏦 銀行存款", "🏦 銀行存款"),
    DIGITAL_PAY("digital_pay", "📱 電子支付 / 票卡", "📱 電子支付 / 票卡"),
    CREDIT("credit", "💳 信用卡 / 負債", "💳 信用卡");

    companion object {
        fun fromKey(key: String?): AccountType = when (key) {
            "bank" -> BANK
            "digital_pay", "e_pay" -> DIGITAL_PAY
            "credit" -> CREDIT
            else -> if (key != null && key.startsWith("e") && key.endsWith("pay")) DIGITAL_PAY else CASH
        }
    }
}

data class Category(
    val id: String,
    var name: String,
    var icon: String
)

data class Account(
    val id: String,
    var name: String,
    var type: AccountType,
    var icon: String,
    var initialBalance: Long
)

data class RecurringItem(
    val id: String,
    val type: TxType,
    val day: Int,
    val categoryId: String,
    val accountId: String,
    val amount: Long,
    val note: String,
    var lastAppliedMonth: String
)

data class TransactionItem(
    val id: String,
    var type: TxType,
    var categoryId: String,
    var accountId: String,
    var toAccountId: String?,
    var amount: Long,
    var date: String,
    var note: String
)

data class AppData(
    var monthlyBudget: Long,
    var firstLoginMonth: String,
    val unlockedMonths: MutableList<String>,
    val accounts: MutableList<Account>,
    val expenseCategories: MutableList<Category>,
    val incomeCategories: MutableList<Category>,
    val recurring: MutableList<RecurringItem>,
    val transactions: MutableList<TransactionItem>
)

object AppDefaults {
    val YM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    val CHART_COLORS: IntArray = intArrayOf(
        "#EE9B27".toColorInt(), // 暖陽琥珀
        "#DF6A4E".toColorInt(), // 柔和赤陶
        "#529985".toColorInt(), // 鼠尾草綠
        "#E6B84F".toColorInt(), // 蜂蜜奶油
        "#9C7A5B".toColorInt(), // 焦糖拿鐵
        "#D98A6C".toColorInt(), // 蜜桃奶茶
        "#6BA392".toColorInt(), // 薄荷青
        "#C48B71".toColorInt(), // 肉桂棕
        "#B59268".toColorInt(), // 燕麥奶
        "#7D8C75".toColorInt()  // 橄欖綠
    )

    val PROTECTED_CATEGORY_IDS: Set<String> = setOf("exp_other", "inc_other")

    fun isProtectedCategory(category: Category): Boolean {
        return category.id in PROTECTED_CATEGORY_IDS ||
            category.name == "其他支出" ||
            category.name == "其他收入"
    }

    fun defaultExpenseCategories(): MutableList<Category> = mutableListOf(
        Category("exp_food", "餐飲美食", "🍱"),
        Category("exp_drink", "飲料咖啡", "☕"),
        Category("exp_transport", "交通通勤", "🚌"),
        Category("exp_shopping", "生活購物", "🛍️"),
        Category("exp_grocery", "超市買菜", "🥬"),
        Category("exp_fun", "休閒娛樂", "🎬"),
        Category("exp_home", "居家房租", "🏠"),
        Category("exp_bills", "水電通訊", "💡"),
        Category("exp_medical", "醫療保健", "💊"),
        Category("exp_other", "其他支出", "✨")
    )

    fun defaultIncomeCategories(): MutableList<Category> = mutableListOf(
        Category("inc_salary", "薪資收入", "💰"),
        Category("inc_bonus", "獎金回饋", "🧧"),
        Category("inc_invest", "投資理財", "📈"),
        Category("inc_part_time", "兼職接案", "💻"),
        Category("inc_other", "其他收入", "🌟")
    )

    fun currentYearMonthString(): String = YearMonth.now().format(YM_FORMATTER)

    fun createDefaultData(): AppData {
        val ym = currentYearMonthString()
        return AppData(
            monthlyBudget = 10000L,
            firstLoginMonth = ym,
            unlockedMonths = mutableListOf(),
            accounts = mutableListOf(
                Account("acc_cash", "隨身現金", AccountType.CASH, "💵", 0L),
                Account("acc_bank", "台新薪轉戶", AccountType.BANK, "🏦", 0L),
                Account("acc_digital_pay", "LINE Pay", AccountType.DIGITAL_PAY, "📱", 0L),
                Account("acc_credit", "日常信用卡", AccountType.CREDIT, "💳", 0L)
            ),
            expenseCategories = defaultExpenseCategories(),
            incomeCategories = defaultIncomeCategories(),
            recurring = mutableListOf(
                RecurringItem(
                    id = "rec_1",
                    type = TxType.EXPENSE,
                    day = 5,
                    categoryId = "exp_home",
                    accountId = "acc_bank",
                    amount = 8500L,
                    note = "每月溫馨小套房租金",
                    lastAppliedMonth = ym
                ),
                RecurringItem(
                    id = "rec_2",
                    type = TxType.EXPENSE,
                    day = 15,
                    categoryId = "exp_fun",
                    accountId = "acc_credit",
                    amount = 390L,
                    note = "Netflix 家庭方案訂閱",
                    lastAppliedMonth = ym
                ),
                RecurringItem(
                    id = "rec_3",
                    type = TxType.INCOME,
                    day = 5,
                    categoryId = "inc_salary",
                    accountId = "acc_bank",
                    amount = 46000L,
                    note = "本月薪資入帳",
                    lastAppliedMonth = ym
                )
            ),
            transactions = mutableListOf(
                TransactionItem("tx_1", TxType.INCOME, "inc_salary", "acc_bank", null, 46000L, "$ym-05", "本月薪資入帳 💼"),
                TransactionItem("tx_2", TxType.EXPENSE, "exp_home", "acc_bank", null, 8500L, "$ym-05", "每月溫馨小套房租金"),
                TransactionItem("tx_3", TxType.TRANSFER, "transfer", "acc_bank", "acc_digital_pay", 3000L, "$ym-08", "儲值 LINE Pay 生活費"),
                TransactionItem("tx_4", TxType.EXPENSE, "exp_grocery", "acc_digital_pay", null, 680L, "$ym-12", "全聯買牛奶、雞蛋與蔬菜"),
                TransactionItem("tx_5", TxType.EXPENSE, "exp_fun", "acc_credit", null, 390L, "$ym-15", "Netflix 家庭方案訂閱"),
                TransactionItem("tx_6", TxType.EXPENSE, "exp_shopping", "acc_credit", null, 1450L, "$ym-18", "MUJI 無印良品收納盒與香氛"),
                TransactionItem("tx_7", TxType.INCOME, "inc_bonus", "acc_bank", null, 1200L, "$ym-20", "信用卡現金回饋與發票中獎"),
                TransactionItem("tx_8", TxType.EXPENSE, "exp_transport", "acc_digital_pay", null, 1200L, "$ym-21", "通勤月票續購"),
                TransactionItem("tx_9", TxType.EXPENSE, "exp_food", "acc_cash", null, 245L, "$ym-22", "暖呼呼日式拉麵晚餐"),
                TransactionItem("tx_10", TxType.EXPENSE, "exp_drink", "acc_digital_pay", null, 135L, "$ym-23", "燕麥奶拿鐵 + 烤布蕾點心")
            )
        )
    }
}
