package com.fintrace.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fintrace.app.MainActivity
import com.fintrace.app.FintraceApplication
import com.fintrace.app.R
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.manager.PendingTransactionManager
import com.fintrace.app.data.manager.SmsTransactionProcessor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that intercepts incoming SMS messages in real-time
 * and processes them for transaction data using the shared SmsTransactionProcessor.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsBroadcastReceiverEntryPoint {
        fun smsTransactionProcessor(): SmsTransactionProcessor
        fun transactionRepository(): com.fintrace.app.data.repository.TransactionRepository
        fun pendingTransactionManager(): PendingTransactionManager
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
        const val ACTION_EDIT_TRANSACTION = "com.fintrace.app.ACTION_EDIT_TRANSACTION"
        const val ACTION_REVIEW_PENDING = "com.fintrace.app.ACTION_REVIEW_PENDING"
        const val ACTION_QUICK_CONFIRM = "com.fintrace.app.ACTION_QUICK_CONFIRM"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_PENDING_ID = "pending_id"
        const val CHANNEL_ID = "transaction_notifications"
        const val CHANNEL_NAME = "Transaction Notifications"
        const val PENDING_CHANNEL_ID = "pending_transaction_notifications"
        const val PENDING_CHANNEL_NAME = "Pending Transaction Notifications"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Combine multi-part SMS messages with their timestamps
        data class SmsData(val body: StringBuilder, var timestamp: Long)
        val smsMap = mutableMapOf<String, SmsData>()
        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue
            val timestamp = message.timestampMillis

            val existing = smsMap.getOrPut(sender) { SmsData(StringBuilder(), timestamp) }
            existing.body.append(body)
            // Use the earliest timestamp for multi-part messages
            if (timestamp < existing.timestamp) {
                existing.timestamp = timestamp
            }
        }

        // Get the processor via Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsBroadcastReceiverEntryPoint::class.java
        )
        val processor = entryPoint.smsTransactionProcessor()

        // Process each unique SMS
        for ((sender, smsData) in smsMap) {
            val body = smsData.body.toString()
            val timestamp = smsData.timestamp
            Log.d(TAG, "Received SMS from: $sender at timestamp: $timestamp")

            processIncomingSms(context, processor, sender, body, timestamp)
        }
    }

    private fun processIncomingSms(
        context: Context,
        processor: SmsTransactionProcessor,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        receiverScope.launch {
            try {
                // Use the shared processor to parse and save the transaction
                val result = processor.processAndSaveTransaction(sender, body, timestamp)

                if (result.success) {
                    // Check if this requires confirmation (pending transaction flow)
                    if (result.requiresConfirmation && result.pendingTransaction != null) {
                        Log.d(TAG, "Transaction pending confirmation: ${result.pendingTransaction.id}")

                        // Show notification for pending transaction (only if app is in background)
                        if (!isAppInForeground(context)) {
                            showPendingTransactionNotification(
                                context = context,
                                pending = result.pendingTransaction
                            )
                        }
                    } else if (result.transactionId != null) {
                        // Direct save flow (confirmation disabled)
                        Log.d(TAG, "Transaction saved with ID: ${result.transactionId}")

                        // Show notification if app is not in foreground
                        if (!isAppInForeground(context)) {
                            // Get transaction details for notification
                            val parser = com.fintrace.parser.core.bank.BankParserFactory.getParser(sender)
                            val parsedTransaction = parser?.parse(body, sender, timestamp)

                            if (parsedTransaction != null) {
                                // Get entry point to access repository
                                val entryPoint = EntryPointAccessors.fromApplication(
                                    context.applicationContext,
                                    SmsBroadcastReceiverEntryPoint::class.java
                                )
                                val repository = entryPoint.transactionRepository()

                                // Fetch the saved transaction to get its category
                                val savedTransaction = repository.getTransactionById(result.transactionId)

                                showTransactionNotification(
                                    context = context,
                                    transactionId = result.transactionId,
                                    amount = parsedTransaction.amount.toString(),
                                    merchant = parsedTransaction.merchant ?: "Unknown",
                                    type = parsedTransaction.type.name,
                                    bankName = parsedTransaction.bankName ?: "Bank",
                                    category = savedTransaction?.category ?: "Others",
                                    repository = repository
                                )
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Transaction not saved: ${result.reason}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val application = context.applicationContext as? FintraceApplication
            application?.isAppInForeground ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun showTransactionNotification(
        context: Context,
        transactionId: Long,
        amount: String,
        merchant: String,
        type: String,
        bankName: String,
        category: String,
        repository: com.fintrace.app.data.repository.TransactionRepository
    ) {
        receiverScope.launch {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Create notification channel
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for new transactions"
                }
                notificationManager.createNotificationChannel(channel)

                // Create intent to open transaction detail
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_EDIT_TRANSACTION
                    putExtra(EXTRA_TRANSACTION_ID, transactionId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    transactionId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Format notification content
                val typeEmoji = when (type) {
                    "EXPENSE" -> "💸"
                    "INCOME" -> "💰"
                    "CREDIT" -> "💳"
                    "TRANSFER" -> "🔄"
                    "INVESTMENT" -> "📈"
                    else -> "💵"
                }

                val title = "$typeEmoji $amount - $merchant"
                val content = "$category • $bankName"

                // Get top 3 categories by usage (personalized for user)
                val topCategories = try {
                    repository.getTopCategoriesByUsage(3)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching top categories", e)
                    // Fallback to common categories
                    listOf("Food & Dining", "Shopping", "Transportation")
                }

                // Build notification with category quick actions
                val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                // Add quick action buttons for top categories (only if different from current)
                val notificationId = transactionId.toInt()
                topCategories.filter { it != category }.take(3).forEachIndexed { index, topCategory ->
                    val categoryIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_CHANGE_CATEGORY
                        putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transactionId)
                        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                        putExtra(NotificationActionReceiver.EXTRA_NEW_CATEGORY, topCategory)
                    }

                    val categoryPendingIntent = PendingIntent.getBroadcast(
                        context,
                        transactionId.toInt() + index + 1, // Unique request code
                        categoryIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    notificationBuilder.addAction(
                        0, // No icon for actions
                        topCategory,
                        categoryPendingIntent
                    )
                }

                val notification = notificationBuilder.build()
                notificationManager.notify(notificationId, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification", e)
            }
        }
    }

    /**
     * Shows a notification for a pending transaction with Review and Quick Confirm actions.
     */
    private fun showPendingTransactionNotification(
        context: Context,
        pending: PendingTransactionEntity
    ) {
        receiverScope.launch {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Create notification channel for pending transactions
                val channel = NotificationChannel(
                    PENDING_CHANNEL_ID,
                    PENDING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for pending transactions awaiting confirmation"
                }
                notificationManager.createNotificationChannel(channel)

                // Create intent to review pending transaction
                val reviewIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_REVIEW_PENDING
                    putExtra(EXTRA_PENDING_ID, pending.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val reviewPendingIntent = PendingIntent.getActivity(
                    context,
                    pending.id.toInt(),
                    reviewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Create intent for Quick Confirm action
                val quickConfirmIntent = Intent(context, PendingTransactionActionReceiver::class.java).apply {
                    action = ACTION_QUICK_CONFIRM
                    putExtra(EXTRA_PENDING_ID, pending.id)
                }

                val quickConfirmPendingIntent = PendingIntent.getBroadcast(
                    context,
                    pending.id.toInt() + 1000, // Unique request code
                    quickConfirmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Format notification content
                val typeEmoji = when (pending.transactionType.name) {
                    "EXPENSE" -> "💸"
                    "INCOME" -> "💰"
                    "CREDIT" -> "💳"
                    "TRANSFER" -> "🔄"
                    "INVESTMENT" -> "📈"
                    else -> "💵"
                }

                val currencySymbol = com.fintrace.app.utils.CurrencyFormatter.getCurrencySymbol(pending.currency)
                val title = "$typeEmoji $currencySymbol${pending.amount.toPlainString()} - ${pending.merchantName}"
                val content = "${pending.category} • ${pending.bankName ?: "Bank"} • Tap to review"

                // Build notification with actions
                val notification = NotificationCompat.Builder(context, PENDING_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(reviewPendingIntent)
                    .setAutoCancel(true)
                    .addAction(
                        0,
                        "Review",
                        reviewPendingIntent
                    )
                    .addAction(
                        0,
                        "Quick Confirm",
                        quickConfirmPendingIntent
                    )
                    .build()

                notificationManager.notify(pending.id.toInt() + 10000, notification)
                Log.d(TAG, "Shown pending transaction notification for ID: ${pending.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing pending notification", e)
            }
        }
    }
}
