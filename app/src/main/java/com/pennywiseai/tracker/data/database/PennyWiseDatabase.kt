package com.pennywiseai.tracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pennywiseai.tracker.data.database.converter.Converters
import com.pennywiseai.tracker.data.database.dao.AccountBalanceDao
import com.pennywiseai.tracker.data.database.dao.CardDao
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.dao.ExchangeRateDao
import com.pennywiseai.tracker.data.database.dao.MerchantMappingDao
import com.pennywiseai.tracker.data.database.dao.RuleApplicationDao
import com.pennywiseai.tracker.data.database.dao.RuleDao
import com.pennywiseai.tracker.data.database.dao.SubscriptionDao
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.dao.PendingTransactionDao
import com.pennywiseai.tracker.data.database.dao.UnrecognizedSmsDao
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CardEntity
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.ExchangeRateEntity
import com.pennywiseai.tracker.data.database.entity.MerchantMappingEntity
import com.pennywiseai.tracker.data.database.entity.RuleApplicationEntity
import com.pennywiseai.tracker.data.database.entity.RuleEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.PendingTransactionEntity
import com.pennywiseai.tracker.data.database.entity.UnrecognizedSmsEntity

/**
 * The PennyWise Room database.
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
        CategoryEntity::class,
        AccountBalanceEntity::class,
        UnrecognizedSmsEntity::class,
        CardEntity::class,
        RuleEntity::class,
        RuleApplicationEntity::class,
        ExchangeRateEntity::class,
        PendingTransactionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PennyWiseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountBalanceDao(): AccountBalanceDao
    abstract fun unrecognizedSmsDao(): UnrecognizedSmsDao
    abstract fun cardDao(): CardDao
    abstract fun ruleDao(): RuleDao
    abstract fun ruleApplicationDao(): RuleApplicationDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun pendingTransactionDao(): PendingTransactionDao

    companion object {
        const val DATABASE_NAME = "pennywise_database"

        @Volatile
        private var INSTANCE: PennyWiseDatabase? = null

        /**
         * Returns a singleton instance of the database.
         * This is used by components that don't have access to Hilt injection
         * (like BroadcastReceivers).
         */
        fun getInstance(context: android.content.Context): PennyWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    PennyWiseDatabase::class.java,
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
        fun setInstance(database: PennyWiseDatabase) {
            INSTANCE = database
        }
    }
}
