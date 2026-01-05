package com.fintrace.app.navigation

import kotlinx.serialization.Serializable

// Define navigation destinations using Kotlin Serialization
@Serializable
object Permission

@Serializable
object Home

@Serializable
object Transactions

@Serializable
object Settings

@Serializable
object Categories

@Serializable
object Analytics

@Serializable
object Chat

@Serializable
data class TransactionDetail(val transactionId: Long)

@Serializable
object AddTransaction

@Serializable
data class AccountDetail(val bankName: String, val accountLast4: String)

@Serializable
object UnrecognizedSms

@Serializable
object Faq

@Serializable
object Rules

@Serializable
data class CreateRule(val ruleId: String? = null)

@Serializable
object PendingTransactions

@Serializable
data class PendingTransactionReview(val pendingId: Long)

@Serializable
object MerchantAliases