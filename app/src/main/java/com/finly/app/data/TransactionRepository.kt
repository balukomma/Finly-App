package com.finly.app.data

import androidx.lifecycle.LiveData

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): LiveData<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: String): LiveData<List<Transaction>> = transactionDao.getTransactionsByType(type)

    fun getTransactionsByMonth(month: String): LiveData<List<Transaction>> = transactionDao.getTransactionsByMonth(month)

    fun searchTransactions(query: String): LiveData<List<Transaction>> = transactionDao.searchTransactions(query)

    val totalIncome: LiveData<Double?> = transactionDao.getTotalIncome()
    val totalExpense: LiveData<Double?> = transactionDao.getTotalExpense()
    val categoryWiseExpenses: LiveData<List<CategorySum>> = transactionDao.getCategoryWiseExpenses()

    suspend fun insert(transaction: Transaction) = transactionDao.insertTransaction(transaction)

    suspend fun update(transaction: Transaction) = transactionDao.updateTransaction(transaction)

    suspend fun delete(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    
    suspend fun deleteAll() = transactionDao.deleteAllTransactions()
    suspend fun deleteSampleTransactions() = transactionDao.deleteSampleTransactions()

    // ─── Insights ─────────────────────────────────────────────────────────
    suspend fun getCurrentMonthExpense(start: String, end: String): Double? =
        transactionDao.getTotalExpenseForPeriod(start, end)

    suspend fun getLastMonthExpense(start: String, end: String): Double? =
        transactionDao.getTotalExpenseForPeriod(start, end)

    suspend fun getCategoryBreakdown(start: String, end: String): List<CategorySum> =
        transactionDao.getCategoryExpenseForPeriod(start, end)

    suspend fun getMonthlyTrend(): List<CategorySum> =
        transactionDao.getMonthlyTrend()

    suspend fun getWeeklyComparison(start: String, end: String): List<CategorySum> =
        transactionDao.getDailyExpenseForPeriod(start, end)

    suspend fun getFrequentTransactions(): List<CategorySum> =
        transactionDao.getFrequentCategories()

    suspend fun getSpendingPattern(start: String, end: String): List<CategorySum> =
        transactionDao.getSpendingByDayOfWeek(start, end)
}

