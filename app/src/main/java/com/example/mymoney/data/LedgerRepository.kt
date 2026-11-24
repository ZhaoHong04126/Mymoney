package com.example.mymoney.data

import android.content.Context
import androidx.core.content.edit
import com.example.mymoney.model.Account
import com.example.mymoney.model.AccountType
import com.example.mymoney.model.AppData
import com.example.mymoney.model.AppDefaults
import com.example.mymoney.model.Category
import com.example.mymoney.model.RecurringItem
import com.example.mymoney.model.TransactionItem
import com.example.mymoney.model.TxType
import org.json.JSONArray
import org.json.JSONObject

sealed class JsonImportResult {
    data class FullBackup(val data: AppData) : JsonImportResult()
    data class TransactionList(val items: List<TransactionItem>) : JsonImportResult()
    data class Invalid(val message: String) : JsonImportResult()
}

class LedgerRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var data: AppData = load()
        private set

    companion object {
        private const val PREFS_NAME = "mymoney_warm_ledger_prefs"
        private const val KEY_DATA_JSON = "mymoney_warm_ledger_v1"
        private val UTF8_BOM = Char(0xFEFF).toString()
    }

    private fun load(): AppData {
        val raw = prefs.getString(KEY_DATA_JSON, null)
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = fromJsonObject(JSONObject(raw))
                if (parsed.accounts.isNotEmpty()) {
                    return parsed
                }
            } catch (_: Exception) {
                // Fall back to default data if stored payload is corrupted
            }
        }
        val def = AppDefaults.createDefaultData()
        save(def)
        return def
    }

    fun save(target: AppData = data) {
        data = target
        prefs.edit { putString(KEY_DATA_JSON, toJsonObject(target).toString()) }
    }

    fun resetToDefaultData() {
        val def = AppDefaults.createDefaultData()
        save(def)
    }

    fun clearAllData() {
        data.transactions.clear()
        data.recurring.clear()
        data.accounts.forEach { it.initialBalance = 0L }
        save()
    }

    fun getCategories(type: TxType): MutableList<Category> = when (type) {
        TxType.INCOME -> data.incomeCategories
        else -> data.expenseCategories
    }

    fun getCategoryById(categoryId: String, typeHint: TxType = TxType.EXPENSE): Category {
        if (categoryId == "transfer") {
            return Category("transfer", "帳戶轉帳", "⇄")
        }
        return data.expenseCategories.find { it.id == categoryId }
            ?: data.incomeCategories.find { it.id == categoryId }
            ?: if (typeHint == TxType.INCOME) {
                Category("inc_other", "其他收入", "🌟")
            } else {
                Category("exp_other", "其他支出", "✨")
            }
    }

    fun getAccountById(accountId: String): Account {
        return data.accounts.find { it.id == accountId }
            ?: Account("unknown", "預設帳戶", AccountType.CASH, "👛", 0L)
    }

    fun computeAccountBalances(upToYm: String): Map<String, Long> {
        val map = LinkedHashMap<String, Long>()
        data.accounts.forEach { map[it.id] = it.initialBalance }

        val cutoff = "$upToYm-31"
        data.transactions.forEach { tx ->
            if (tx.date.isNotBlank() && tx.date > cutoff) return@forEach
            when (tx.type) {
                TxType.EXPENSE -> map[tx.accountId] = (map[tx.accountId] ?: 0L) - tx.amount
                TxType.INCOME -> map[tx.accountId] = (map[tx.accountId] ?: 0L) + tx.amount
                TxType.TRANSFER -> {
                    map[tx.accountId] = (map[tx.accountId] ?: 0L) - tx.amount
                    val dest = tx.toAccountId
                    if (dest != null && map.containsKey(dest)) {
                        map[dest] = (map[dest] ?: 0L) + tx.amount
                    }
                }
            }
        }
        return map
    }

    fun isMonthLocked(ym: String): Boolean {
        val firstYm = data.firstLoginMonth.ifBlank { AppDefaults.currentYearMonthString() }
        return ym < firstYm && ym !in data.unlockedMonths
    }

    fun unlockMonth(ym: String) {
        if (ym !in data.unlockedMonths) {
            data.unlockedMonths.add(ym)
            save()
        }
    }

    fun applyPendingRecurring(ym: String): Int {
        val pending = data.recurring.filter { it.lastAppliedMonth != ym }
        if (pending.isEmpty()) return 0

        pending.forEachIndexed { idx, r ->
            val dd = r.day.coerceIn(1, 28).toString().padStart(2, '0')
            data.transactions.add(
                TransactionItem(
                    id = "tx_rec_${System.currentTimeMillis()}_$idx",
                    type = r.type,
                    categoryId = r.categoryId,
                    accountId = r.accountId,
                    toAccountId = null,
                    amount = r.amount,
                    date = "$ym-$dd",
                    note = "[固定週期] ${r.note}"
                )
            )
            r.lastAppliedMonth = ym
        }
        save()
        return pending.size
    }

    fun addCategory(type: TxType, name: String, icon: String): Category {
        val list = getCategories(type)
        val newCat = Category("cat_${System.currentTimeMillis()}", name, icon)
        val otherIdx = list.indexOfFirst { AppDefaults.isProtectedCategory(it) }
        if (otherIdx != -1) {
            list.add(otherIdx, newCat)
        } else {
            list.add(newCat)
        }
        save()
        return newCat
    }

    fun resetDefaultCategories() {
        data.expenseCategories.clear()
        data.expenseCategories.addAll(AppDefaults.defaultExpenseCategories())
        data.incomeCategories.clear()
        data.incomeCategories.addAll(AppDefaults.defaultIncomeCategories())
        save()
    }

    fun exportToCsv(): String {
        val sb = StringBuilder(UTF8_BOM)
        sb.append("日期,類型,分類,金額,帳戶,轉入帳戶,備註\r\n")
        val sorted = data.transactions.sortedByDescending { it.date }
        sorted.forEach { tx ->
            val cat = getCategoryById(tx.categoryId, tx.type)
            val acc = getAccountById(tx.accountId)
            val toAccName = tx.toAccountId?.let { getAccountById(it).name } ?: ""
            val escapedNote = tx.note.replace("\"", "\"\"")
            sb.append(
                "${tx.date},${tx.type.label},${cat.name},${tx.amount},${acc.name},$toAccName,\"$escapedNote\"\r\n"
            )
        }
        return sb.toString()
    }

    fun parseCsvTransactions(rawContent: String): List<TransactionItem> {
        val cleaned = rawContent.removePrefix(UTF8_BOM)
        val lines = cleaned.split(Regex("\\r?\\n")).filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val imported = mutableListOf<TransactionItem>()
        val baseTime = System.currentTimeMillis()

        for (i in 1..<lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.size >= 4) {
                val date = cols[0].trim()
                val typeRaw = cols[1].trim()
                val catName = cols[2].trim()
                val amount = cols[3].trim().toLongOrNull() ?: 0L
                val accName = cols.getOrNull(4)?.trim().orEmpty()
                val toAccName = cols.getOrNull(5)?.trim().orEmpty()
                val note = cols.getOrNull(6)?.trim().orEmpty()

                if (date.isNotBlank() && amount > 0L) {
                    val txType = when (typeRaw) {
                        "收入" -> TxType.INCOME
                        "轉帳" -> TxType.TRANSFER
                        else -> TxType.EXPENSE
                    }
                    val cats = getCategories(if (txType == TxType.INCOME) TxType.INCOME else TxType.EXPENSE)
                    val matchedCat = cats.find { it.name == catName } ?: cats.first()
                    val matchedAcc = data.accounts.find { it.name == accName } ?: data.accounts.first()
                    val matchedToAcc = if (toAccName.isNotEmpty()) {
                        data.accounts.find { it.name == toAccName } ?: data.accounts.first()
                    } else null

                    imported.add(
                        TransactionItem(
                            id = "tx_imp_${baseTime}_$i",
                            type = txType,
                            categoryId = if (txType == TxType.TRANSFER) "transfer" else matchedCat.id,
                            accountId = matchedAcc.id,
                            toAccountId = if (txType == TxType.TRANSFER) matchedToAcc?.id else null,
                            amount = amount,
                            date = date,
                            note = note
                        )
                    )
                }
            }
        }
        return imported
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(ch)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    fun parseJsonBackup(rawContent: String): JsonImportResult {
        return try {
            val trimmed = rawContent.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                val items = parseTransactionsArray(arr)
                if (items.isEmpty()) JsonImportResult.Invalid("JSON 檔案中沒有找到有效的記帳資料")
                else JsonImportResult.TransactionList(items)
            } else {
                val obj = JSONObject(trimmed)
                if (!obj.has("transactions") || !obj.has("accounts")) {
                    return JsonImportResult.Invalid("JSON 檔案內容格式不符合記帳資料結構")
                }
                JsonImportResult.FullBackup(fromJsonObject(obj))
            }
        } catch (_: Exception) {
            JsonImportResult.Invalid("檔案讀取失敗，僅支援本帳本格式的 CSV 或 JSON 檔案")
        }
    }

    private fun toJsonObject(appData: AppData): JSONObject {
        val root = JSONObject()
        root.put("monthlyBudget", appData.monthlyBudget)
        root.put("firstLoginMonth", appData.firstLoginMonth)
        root.put("unlockedMonths", JSONArray(appData.unlockedMonths))

        val accArr = JSONArray()
        appData.accounts.forEach { acc ->
            accArr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("type", acc.type.key)
                    put("icon", acc.icon)
                    put("initialBalance", acc.initialBalance)
                }
            )
        }
        root.put("accounts", accArr)

        val catObj = JSONObject().apply {
            put("expense", categoriesToJson(appData.expenseCategories))
            put("income", categoriesToJson(appData.incomeCategories))
        }
        root.put("categories", catObj)

        val recArr = JSONArray()
        appData.recurring.forEach { r ->
            recArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("type", r.type.key)
                    put("day", r.day)
                    put("categoryId", r.categoryId)
                    put("accountId", r.accountId)
                    put("amount", r.amount)
                    put("note", r.note)
                    put("lastAppliedMonth", r.lastAppliedMonth)
                }
            )
        }
        root.put("recurring", recArr)

        val txArr = JSONArray()
        appData.transactions.forEach { tx ->
            txArr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type.key)
                    put("categoryId", tx.categoryId)
                    put("accountId", tx.accountId)
                    if (tx.toAccountId != null) put("toAccountId", tx.toAccountId)
                    put("amount", tx.amount)
                    put("date", tx.date)
                    put("note", tx.note)
                }
            )
        }
        root.put("transactions", txArr)

        return root
    }

    private fun categoriesToJson(list: List<Category>): JSONArray {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("icon", c.icon)
                }
            )
        }
        return arr
    }

    private fun fromJsonObject(obj: JSONObject): AppData {
        val firstLogin = obj.optString("firstLoginMonth").ifBlank { AppDefaults.currentYearMonthString() }
        val budget = obj.optLong("monthlyBudget", 10000L).coerceAtLeast(0L)

        val unlocked = mutableListOf<String>()
        obj.optJSONArray("unlockedMonths")?.let { arr ->
            for (i in 0..<arr.length()) {
                val ym = arr.optString(i)
                if (ym.isNotBlank()) unlocked.add(ym)
            }
        }

        val accounts = mutableListOf<Account>()
        obj.optJSONArray("accounts")?.let { arr ->
            for (i in 0..<arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                accounts.add(
                    Account(
                        id = a.optString("id", "acc_$i"),
                        name = a.optString("name", "帳戶"),
                        type = AccountType.fromKey(a.optString("type")),
                        icon = a.optString("icon", "💵"),
                        initialBalance = a.optLong("initialBalance", 0L)
                    )
                )
            }
        }
        if (accounts.isEmpty()) {
            accounts.add(Account("acc_cash", "隨身現金", AccountType.CASH, "💵", 0L))
        }

        val expCats = mutableListOf<Category>()
        val incCats = mutableListOf<Category>()
        val catsObj = obj.optJSONObject("categories")
        if (catsObj != null) {
            parseCategoryArray(catsObj.optJSONArray("expense"), expCats)
            parseCategoryArray(catsObj.optJSONArray("income"), incCats)
        }
        if (expCats.isEmpty()) expCats.addAll(AppDefaults.defaultExpenseCategories())
        if (incCats.isEmpty()) incCats.addAll(AppDefaults.defaultIncomeCategories())

        // 兼容舊網頁版 customCategories 結構
        obj.optJSONArray("customCategories")?.let { arr ->
            for (i in 0..<arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val id = c.optString("id")
                val name = c.optString("name")
                val icon = c.optString("icon", "✨")
                val targetList = if (c.optString("type") == "income") incCats else expCats
                if (id.isNotBlank() && targetList.none { it.id == id }) {
                    val otherIdx = targetList.indexOfFirst { AppDefaults.isProtectedCategory(it) }
                    val item = Category(id, name, icon)
                    if (otherIdx != -1) targetList.add(otherIdx, item) else targetList.add(item)
                }
            }
        }

        val recurring = mutableListOf<RecurringItem>()
        obj.optJSONArray("recurring")?.let { arr ->
            for (i in 0..<arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                recurring.add(
                    RecurringItem(
                        id = r.optString("id", "rec_$i"),
                        type = TxType.fromKey(r.optString("type")),
                        day = r.optInt("day", 1).coerceIn(1, 31),
                        categoryId = r.optString("categoryId", "exp_other"),
                        accountId = r.optString("accountId", accounts.first().id),
                        amount = r.optLong("amount", 0L),
                        note = r.optString("note", ""),
                        lastAppliedMonth = r.optString("lastAppliedMonth", "")
                    )
                )
            }
        }

        val transactions = parseTransactionsArray(obj.optJSONArray("transactions"))

        return AppData(
            monthlyBudget = budget,
            firstLoginMonth = firstLogin,
            unlockedMonths = unlocked,
            accounts = accounts,
            expenseCategories = expCats,
            incomeCategories = incCats,
            recurring = recurring,
            transactions = transactions
        )
    }

    private fun parseCategoryArray(arr: JSONArray?, target: MutableList<Category>) {
        if (arr == null) return
        for (i in 0..<arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            val id = c.optString("id")
            val name = c.optString("name")
            val icon = c.optString("icon", "✨")
            if (id.isNotBlank() && name.isNotBlank()) {
                target.add(Category(id, name, icon))
            }
        }
    }

    private fun parseTransactionsArray(arr: JSONArray?): MutableList<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        if (arr == null) return list
        for (i in 0..<arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            val toId = t.optString("toAccountId").takeIf { it.isNotBlank() && it != "null" }
            list.add(
                TransactionItem(
                    id = t.optString("id", "tx_imp_${System.currentTimeMillis()}_$i"),
                    type = TxType.fromKey(t.optString("type")),
                    categoryId = t.optString("categoryId", "exp_other"),
                    accountId = t.optString("accountId", "acc_cash"),
                    toAccountId = toId,
                    amount = t.optLong("amount", 0L),
                    date = t.optString("date", ""),
                    note = t.optString("note", "")
                )
            )
        }
        return list
    }
}
