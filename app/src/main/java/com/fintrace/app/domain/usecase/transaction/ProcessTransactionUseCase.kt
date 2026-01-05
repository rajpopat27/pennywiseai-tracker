package com.fintrace.app.domain.usecase.transaction

import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.processor.ProcessConfig
import com.fintrace.app.data.processor.ProcessResult
import com.fintrace.app.data.processor.TransactionProcessor
import javax.inject.Inject

/**
 * Use case for processing a transaction through the unified pipeline.
 *
 * This encapsulates the TransactionProcessor to provide a clean interface
 * for ViewModels and other components.
 */
class ProcessTransactionUseCase @Inject constructor(
    private val transactionProcessor: TransactionProcessor
) {
    /**
     * Process and save a transaction.
     *
     * @param entity The transaction entity to process
     * @param smsBody Original SMS body (for rule matching)
     * @param pendingEntity If converting from pending (for balance updates)
     * @param config Processing configuration options
     * @return ProcessResult indicating success, blocked, duplicate, or error
     */
    suspend operator fun invoke(
        entity: TransactionEntity,
        smsBody: String?,
        pendingEntity: PendingTransactionEntity? = null,
        config: ProcessConfig = ProcessConfig()
    ): ProcessResult {
        return transactionProcessor.processAndSave(
            entity = entity,
            smsBody = smsBody,
            pendingEntity = pendingEntity,
            config = config
        )
    }
}
