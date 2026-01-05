package com.fintrace.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.fintrace.app.data.manager.PendingTransactionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles quick actions for pending transactions from notifications.
 */
class PendingTransactionActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PendingTransactionActionReceiverEntryPoint {
        fun pendingTransactionManager(): PendingTransactionManager
    }

    companion object {
        private const val TAG = "PendingTxActionReceiver"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingId = intent.getLongExtra(SmsBroadcastReceiver.EXTRA_PENDING_ID, -1L)

        if (pendingId == -1L) {
            Log.e(TAG, "Invalid pending ID in intent")
            return
        }

        when (action) {
            SmsBroadcastReceiver.ACTION_QUICK_CONFIRM -> {
                handleQuickConfirm(context, pendingId)
            }
        }
    }

    private fun handleQuickConfirm(context: Context, pendingId: Long) {
        receiverScope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    PendingTransactionActionReceiverEntryPoint::class.java
                )
                val pendingTransactionManager = entryPoint.pendingTransactionManager()

                val transactionId = pendingTransactionManager.quickConfirmTransaction(pendingId)

                if (transactionId != -1L) {
                    Log.d(TAG, "Quick confirmed transaction: $transactionId")

                    // Dismiss the notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(pendingId.toInt() + 10000)

                    // Show toast on main thread
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Transaction saved!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Failed to quick confirm transaction")
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save transaction", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in quick confirm", e)
            }
        }
    }
}
