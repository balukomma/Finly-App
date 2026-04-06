package com.finly.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date LIKE :month || '%' ORDER BY date DESC")
    fun getTransactionsByMonth(month: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): LiveData<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income'")
    fun getTotalIncome(): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense'")
    fun getTotalExpense(): LiveData<Double?>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'expense' GROUP BY category")
    fun getCategoryWiseExpenses(): LiveData<List<CategorySum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE note = 'SAMPLE_DATA'")
    suspend fun deleteSampleTransactions()

    // ─── Insights Queries ─────────────────────────────────────────────────

    /** Total expense for a period (e.g. current/last month, week) */
    @Query("SELECT SUM(amount) FROM transactions WHERE type='expense' AND date >= :start AND date <= :end")
    suspend fun getTotalExpenseForPeriod(start: String, end: String): Double?

    /** Per-category expense for a period */
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type='expense' AND date >= :start AND date <= :end GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryExpenseForPeriod(start: String, end: String): List<CategorySum>

    /** Monthly totals (for trend chart) */
    @Query("SELECT strftime('%Y-%m', date) as category, SUM(amount) as total FROM transactions WHERE type='expense' GROUP BY strftime('%Y-%m', date) ORDER BY date ASC LIMIT 6")
    suspend fun getMonthlyTrend(): List<CategorySum>

    /** Daily totals for a specific period (to build weekly bars) */
    @Query("SELECT date as category, SUM(amount) as total FROM transactions WHERE type='expense' AND date >= :start AND date <= :end GROUP BY date")
    suspend fun getDailyExpenseForPeriod(start: String, end: String): List<CategorySum>

    /** Per-category transaction count (frequent categories) */
    @Query("SELECT category, COUNT(*) as total FROM transactions WHERE type='expense' GROUP BY category ORDER BY total DESC LIMIT 5")
    suspend fun getFrequentCategories(): List<CategorySum>

    /** Spending by day of week (0=Sun … 6=Sat) */
    @Query("SELECT strftime('%w', date) as category, SUM(amount) as total FROM transactions WHERE type='expense' AND date >= :start AND date <= :end GROUP BY strftime('%w', date)")
    suspend fun getSpendingByDayOfWeek(start: String, end: String): List<CategorySum>
}

data class CategorySum(
    val category: String,
    val total: Double
)
