package com.pennywiseai.tracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pennywiseai.tracker.data.manager.PendingTransactionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that auto-saves expired pending transactions.
 * Runs periodically to check for pending transactions that have passed their expiry time
 * and automatically saves them to the main transactions table.
 */
@HiltWorker
class PendingTransactionAutoSaveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingTransactionManager: PendingTransactionManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "PendingTxAutoSaveWorker"
        const val WORK_NAME = "pending_transaction_auto_save_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting auto-save check for expired pending transactions...")

            val autoSavedCount = pendingTransactionManager.autoSaveExpiredTransactions()

            if (autoSavedCount > 0) {
                Log.d(TAG, "Auto-saved $autoSavedCount expired pending transactions")
            } else {
                Log.d(TAG, "No expired pending transactions to auto-save")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto-save check", e)
            Result.retry()
        }
    }
}
