package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.PendingTransactionDao
import com.pennywiseai.tracker.data.database.entity.PendingTransactionEntity
import com.pennywiseai.tracker.data.database.entity.PendingTransactionStatus
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingTransactionRepository @Inject constructor(
    private val pendingTransactionDao: PendingTransactionDao,
    private val transactionRepository: TransactionRepository
) {
    fun getAllPending(): Flow<List<PendingTransactionEntity>> =
        pendingTransactionDao.getAllPending()

    suspend fun getAllPendingList(): List<PendingTransactionEntity> =
        pendingTransactionDao.getAllPendingList()

    fun getPendingCount(): Flow<Int> =
        pendingTransactionDao.getPendingCount()

    suspend fun getById(id: Long): PendingTransactionEntity? =
        pendingTransactionDao.getById(id)

    suspend fun getByHash(hash: String): PendingTransactionEntity? =
        pendingTransactionDao.getByHash(hash)

    suspend fun insert(transaction: PendingTransactionEntity): Long =
        pendingTransactionDao.insert(transaction)

    suspend fun update(transaction: PendingTransactionEntity) =
        pendingTransactionDao.update(transaction)

    suspend fun delete(transaction: PendingTransactionEntity) =
        pendingTransactionDao.delete(transaction)

    suspend fun deleteById(id: Long) =
        pendingTransactionDao.deleteById(id)

    /**
     * Confirms a pending transaction by saving it to the main transactions table.
     *
     * @param pending The pending transaction to confirm
     * @param finalEntity The final transaction entity (may have user edits)
     * @return The ID of the saved transaction, or -1 if failed
     */
    suspend fun confirm(pending: PendingTransactionEntity, finalEntity: TransactionEntity): Long {
        return try {
            val transactionId = transactionRepository.insertTransaction(finalEntity)
            if (transactionId != -1L) {
                pendingTransactionDao.updateStatus(pending.id, PendingTransactionStatus.CONFIRMED)
            }
            transactionId
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Rejects a pending transaction (user dismissed it).
     */
    suspend fun reject(pendingId: Long) {
        pendingTransactionDao.updateStatus(pendingId, PendingTransactionStatus.REJECTED)
    }

    /**
     * Gets all pending transactions that have expired (past 24 hours).
     */
    suspend fun getExpiredPending(): List<PendingTransactionEntity> =
        pendingTransactionDao.getExpiredPending(LocalDateTime.now())

    /**
     * Marks a pending transaction as auto-saved.
     */
    suspend fun markAutoSaved(pendingId: Long) {
        pendingTransactionDao.updateStatus(pendingId, PendingTransactionStatus.AUTO_SAVED)
    }

    /**
     * Deletes old processed (non-pending) transactions to clean up the table.
     * Keeps them for 7 days after processing for audit trail.
     */
    suspend fun cleanupOldProcessed() {
        val cutoff = LocalDateTime.now().minusDays(7)
        pendingTransactionDao.deleteOldProcessed(cutoff)
    }

    /**
     * Confirms all pending transactions at once.
     *
     * @param pendingList List of pending transactions to confirm
     * @param entityMapper Function to convert pending to final entity
     * @return Number of successfully confirmed transactions
     */
    suspend fun confirmAll(
        pendingList: List<PendingTransactionEntity>,
        entityMapper: (PendingTransactionEntity) -> TransactionEntity
    ): Int {
        var confirmedCount = 0
        pendingList.forEach { pending ->
            val finalEntity = entityMapper(pending)
            val id = confirm(pending, finalEntity)
            if (id != -1L) {
                confirmedCount++
            }
        }
        return confirmedCount
    }
}
