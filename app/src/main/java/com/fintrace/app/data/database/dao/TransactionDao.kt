package com.fintrace.app.data.database.dao

import androidx.room.*
import com.fintrace.app.data.database.dao.result.AccountCashbackResult
import com.fintrace.app.data.database.dao.result.CategorySpendingResult
import com.fintrace.app.data.database.dao.result.DailySpendingResult
import com.fintrace.app.data.database.dao.result.DayOfWeekSpendingResult
import com.fintrace.app.data.database.dao.result.TopMerchantResult
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date_time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?
    
    @Query("""
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND date_time BETWEEN :startDate AND :endDate
        ORDER BY date_time DESC
    """)
    fun getTransactionsBetweenDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    /**
     * Optimized query that filters transactions at the database level.
     * Combines date range, currency, and transaction type filters to reduce memory usage.
     *
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @param currency Currency code to filter by (e.g., "INR", "USD")
     * @param transactionType Optional transaction type filter (null means all types)
     * @return Flow of filtered transactions ordered by date descending
     */
    @Query("""
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND date_time BETWEEN :startDate AND :endDate
        AND currency = :currency
        AND (:transactionType IS NULL OR transaction_type = :transactionType)
        ORDER BY date_time DESC
    """)
    fun getTransactionsFiltered(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String,
        transactionType: TransactionType?
    ): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND transaction_type = :type 
        ORDER BY date_time DESC
    """)
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND category = :category 
        ORDER BY date_time DESC
    """)
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (merchant_name LIKE '%' || :searchQuery || '%' 
        OR description LIKE '%' || :searchQuery || '%'
        OR sms_body LIKE '%' || :searchQuery || '%') 
        ORDER BY date_time DESC
    """)
    fun searchTransactions(searchQuery: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT DISTINCT category FROM transactions WHERE is_deleted = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT category FROM transactions
        WHERE is_deleted = 0
        GROUP BY category
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """)
    suspend fun getTopCategoriesByUsage(limit: Int = 3): List<String>

    @Query("SELECT DISTINCT merchant_name FROM transactions WHERE is_deleted = 0 ORDER BY merchant_name ASC")
    fun getAllMerchants(): Flow<List<String>>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE is_deleted = 0 
        AND transaction_type = :type 
        AND date_time BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalAmountByTypeAndPeriod(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    @Query("UPDATE transactions SET category = :newCategory WHERE merchant_name = :merchantName")
    suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE merchant_name = :merchantName AND id != :excludeId")
    suspend fun getTransactionCountForMerchant(merchantName: String, excludeId: Long): Int

    @Query("SELECT DISTINCT currency FROM transactions WHERE is_deleted = 0 ORDER BY currency")
    fun getAllCurrencies(): Flow<List<String>>

    @Query("SELECT DISTINCT currency FROM transactions WHERE is_deleted = 0 AND date_time BETWEEN :startDate AND :endDate ORDER BY currency")
    fun getCurrenciesForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<String>>

    // Soft delete methods
    @Query("UPDATE transactions SET is_deleted = 1 WHERE id = :transactionId")
    suspend fun softDeleteTransaction(transactionId: Long)

    @Query("UPDATE transactions SET is_deleted = 1 WHERE transaction_hash = :transactionHash")
    suspend fun softDeleteByHash(transactionHash: String)

    // Method to check if transaction exists by hash (including deleted)
    @Query("SELECT * FROM transactions WHERE transaction_hash = :transactionHash LIMIT 1")
    suspend fun getTransactionByHash(transactionHash: String): TransactionEntity?
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND date_time BETWEEN :startDate AND :endDate 
        ORDER BY date_time DESC
    """)
    suspend fun getTransactionsBetweenDatesList(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>
    
    @Query("""
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND bank_name = :bankName
        AND (account_number = :accountLast4 OR account_number IS NULL)
        ORDER BY date_time DESC
    """)
    fun getTransactionsByAccount(
        bankName: String,
        accountLast4: String
    ): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND bank_name = :bankName
        AND account_number = :accountLast4
        AND date_time BETWEEN :startDate AND :endDate
        ORDER BY date_time DESC
    """)
    fun getTransactionsByAccountAndDateRange(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    /**
     * Updates cashback for transactions that don't already have cashback set.
     * Only affects EXPENSE and CREDIT transactions for a specific account.
     * Updates transactions where cashback_amount is NULL or 0.
     * Matches transactions where:
     * - bank_name matches (case-insensitive) OR bank_name is NULL
     * - account_number equals the given value OR is NULL (for same bank)
     */
    @Query("""
        UPDATE transactions
        SET cashback_percent = :cashbackPercent,
            cashback_amount = (amount * :cashbackPercent / 100)
        WHERE is_deleted = 0
        AND (LOWER(bank_name) = LOWER(:bankName) OR bank_name IS NULL)
        AND (account_number = :accountLast4 OR account_number IS NULL)
        AND (transaction_type = 'EXPENSE' OR transaction_type = 'CREDIT')
        AND (cashback_amount IS NULL OR cashback_amount = 0)
    """)
    suspend fun applyRetroactiveCashback(
        bankName: String,
        accountLast4: String,
        cashbackPercent: Double
    ): Int

    // ========== Analytics Aggregation Queries ==========

    /**
     * Get daily spending totals for a given period and currency.
     * Used for spending trend charts.
     */
    @Query("""
        SELECT DATE(date_time) as date, SUM(amount) as total
        FROM transactions
        WHERE is_deleted = 0
        AND transaction_type = 'EXPENSE'
        AND currency = :currency
        AND date_time >= :startDate
        GROUP BY DATE(date_time)
        ORDER BY date ASC
    """)
    suspend fun getDailySpending(startDate: LocalDateTime, currency: String): List<DailySpendingResult>

    /**
     * Get spending by day of week.
     * dayOfWeek: 0 = Sunday, 6 = Saturday (SQLite strftime('%w') format)
     */
    @Query("""
        SELECT CAST(strftime('%w', date_time) AS INTEGER) as dayOfWeek, SUM(amount) as total
        FROM transactions
        WHERE is_deleted = 0
        AND transaction_type = 'EXPENSE'
        AND currency = :currency
        AND date_time BETWEEN :startDate AND :endDate
        GROUP BY dayOfWeek
    """)
    suspend fun getSpendingByDayOfWeek(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String
    ): List<DayOfWeekSpendingResult>

    /**
     * Get top merchants by spending amount.
     */
    @Query("""
        SELECT merchant_name as merchantName, SUM(amount) as total, COUNT(*) as transactionCount
        FROM transactions
        WHERE is_deleted = 0
        AND transaction_type = 'EXPENSE'
        AND currency = :currency
        AND date_time BETWEEN :startDate AND :endDate
        AND merchant_name IS NOT NULL
        GROUP BY merchant_name
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopMerchants(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String,
        limit: Int
    ): List<TopMerchantResult>

    /**
     * Get spending by category for a period.
     */
    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as transactionCount
        FROM transactions
        WHERE is_deleted = 0
        AND transaction_type = 'EXPENSE'
        AND currency = :currency
        AND date_time BETWEEN :startDate AND :endDate
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getSpendingByCategory(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String
    ): List<CategorySpendingResult>

    /**
     * Get total cashback earned in a period.
     */
    @Query("""
        SELECT COALESCE(SUM(cashback_amount), 0)
        FROM transactions
        WHERE is_deleted = 0
        AND cashback_amount > 0
        AND date_time BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalCashback(startDate: LocalDateTime, endDate: LocalDateTime): Double

    /**
     * Get cashback broken down by account.
     */
    @Query("""
        SELECT bank_name as bankName, account_number as accountNumber, SUM(cashback_amount) as total
        FROM transactions
        WHERE is_deleted = 0
        AND cashback_amount > 0
        AND date_time BETWEEN :startDate AND :endDate
        GROUP BY bank_name, account_number
    """)
    suspend fun getCashbackByAccount(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<AccountCashbackResult>

    /**
     * Get total expense amount for a period and currency.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE is_deleted = 0
        AND transaction_type = 'EXPENSE'
        AND currency = :currency
        AND date_time BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalExpenseForPeriod(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String
    ): Double

    /**
     * Get total income amount for a period and currency.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE is_deleted = 0
        AND transaction_type = 'INCOME'
        AND currency = :currency
        AND date_time BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalIncomeForPeriod(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        currency: String
    ): Double
}