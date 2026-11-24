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
        Category("exp_fun", "休閒娛樂", "🎬"),
        Category("exp_home", "居家房租", "🏠"),
        Category("exp_bills", "水電通訊", "💡"),
        Category("exp_other", "其他支出", "✨")
    )

    fun defaultIncomeCategories(): MutableList<Category> = mutableListOf(
        Category("inc_salary", "薪資收入", "💰"),
        Category("inc_invest", "投資理財", "📈"),
        Category("inc_bonus", "獎金", "🧧"),
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
                Account("acc_bank", "郵局帳戶", AccountType.BANK, "🏦", 0L),
                Account("acc_digital_pay", "LINE Pay", AccountType.DIGITAL_PAY, "📱", 0L),
                Account("acc_credit", "日常信用卡", AccountType.CREDIT, "💳", 0L)
            ),
            expenseCategories = defaultExpenseCategories(),
            incomeCategories = defaultIncomeCategories(),
            recurring = mutableListOf(),
            transactions = mutableListOf()
        )
    }
}
