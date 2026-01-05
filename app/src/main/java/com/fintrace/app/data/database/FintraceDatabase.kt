package com.fintrace.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fintrace.app.data.database.converter.Converters
import com.fintrace.app.data.database.dao.AccountBalanceDao
import com.fintrace.app.data.database.dao.BudgetDao
import com.fintrace.app.data.database.dao.CardDao
import com.fintrace.app.data.database.dao.CategoryDao
import com.fintrace.app.data.database.dao.ExchangeRateDao
import com.fintrace.app.data.database.dao.MerchantAliasDao
import com.fintrace.app.data.database.dao.MerchantMappingDao
import com.fintrace.app.data.database.dao.RuleApplicationDao
import com.fintrace.app.data.database.dao.RuleDao
import com.fintrace.app.data.database.dao.SubscriptionDao
import com.fintrace.app.data.database.dao.TransactionDao
import com.fintrace.app.data.database.dao.PendingTransactionDao
import com.fintrace.app.data.database.dao.UnrecognizedSmsDao
import com.fintrace.app.data.database.entity.AccountBalanceEntity
import com.fintrace.app.data.database.entity.BudgetEntity
import com.fintrace.app.data.database.entity.BudgetHistoryEntity
import com.fintrace.app.data.database.entity.CardEntity
import com.fintrace.app.data.database.entity.CategoryEntity
import com.fintrace.app.data.database.entity.ExchangeRateEntity
import com.fintrace.app.data.database.entity.MerchantAliasEntity
import com.fintrace.app.data.database.entity.MerchantMappingEntity
import com.fintrace.app.data.database.entity.RuleApplicationEntity
import com.fintrace.app.data.database.entity.RuleEntity
import com.fintrace.app.data.database.entity.SubscriptionEntity
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.UnrecognizedSmsEntity
import com.fintrace.app.data.database.view.LatestAccountBalanceView

/**
 * The Fintrace Room database.
 *
 * This database stores all financial transaction data locally on the device.
 *
 * ALPHA MODE: Using fallbackToDestructiveMigration() - database will be wiped on schema changes.
 * See docs/database-migrations.md for production migration setup.
 *
 * @property version Current database version. Increment this when making schema changes.
 * @property entities List of all entities (tables) in the database.
 * @property exportSchema Set to true to export schema for version control.
 */
@Database(
    entities = [
        TransactionEntity::class,
        SubscriptionEntity::class,
        MerchantMappingEntity::class,
        MerchantAliasEntity::class,
        CategoryEntity::class,
        AccountBalanceEntity::class,
        UnrecognizedSmsEntity::class,
        CardEntity::class,
        RuleEntity::class,
        RuleApplicationEntity::class,
        ExchangeRateEntity::class,
        PendingTransactionEntity::class,
        BudgetEntity::class,
        BudgetHistoryEntity::class
    ],
    views = [LatestAccountBalanceView::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FintraceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun merchantAliasDao(): MerchantAliasDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountBalanceDao(): AccountBalanceDao
    abstract fun unrecognizedSmsDao(): UnrecognizedSmsDao
    abstract fun cardDao(): CardDao
    abstract fun ruleDao(): RuleDao
    abstract fun ruleApplicationDao(): RuleApplicationDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "pennywise_database"

        @Volatile
        private var INSTANCE: FintraceDatabase? = null

        /**
         * Returns a singleton instance of the database.
         * This is used by components that don't have access to Hilt injection
         * (like BroadcastReceivers).
         */
        fun getInstance(context: android.content.Context): FintraceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    FintraceDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Sets the singleton instance. Called by Hilt module to ensure
         * the same instance is used throughout the app.
         */
        fun setInstance(database: FintraceDatabase) {
            INSTANCE = database
        }
    }
}
