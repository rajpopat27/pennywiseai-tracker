package com.fintrace.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.PendingTransactionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface PendingTransactionDao {

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY created_at DESC")
    fun getAllPending(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY created_at DESC")
    suspend fun getAllPendingList(): List<PendingTransactionEntity>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun getById(id: Long): PendingTransactionEntity?

    @Query("SELECT * FROM pending_transactions WHERE transaction_hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): PendingTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: PendingTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<PendingTransactionEntity>)

    @Update
    suspend fun update(transaction: PendingTransactionEntity)

    @Delete
    suspend fun delete(transaction: PendingTransactionEntity)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PendingTransactionStatus)

    @Query("""
        SELECT * FROM pending_transactions
        WHERE status = 'PENDING' AND expires_at < :currentTime
        ORDER BY created_at ASC
    """)
    suspend fun getExpiredPending(currentTime: LocalDateTime): List<PendingTransactionEntity>

    @Query("""
        DELETE FROM pending_transactions
        WHERE status != 'PENDING' AND created_at < :cutoffTime
    """)
    suspend fun deleteOldProcessed(cutoffTime: LocalDateTime)

    @Query("DELETE FROM pending_transactions WHERE status = 'PENDING'")
    suspend fun deleteAllPending()
}
