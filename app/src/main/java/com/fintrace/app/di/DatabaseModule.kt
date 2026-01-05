package com.fintrace.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fintrace.app.data.database.FintraceDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the singleton instance of FintraceDatabase.
     * 
     * @param context Application context
     * @return Configured Room database instance
     */
    @Provides
    @Singleton
    fun provideFintraceDatabase(
        @ApplicationContext context: Context
    ): FintraceDatabase {
        val database = Room.databaseBuilder(
            context,
            FintraceDatabase::class.java,
            FintraceDatabase.DATABASE_NAME
        )
            // For alpha: wipe database on schema changes instead of migrating
            .fallbackToDestructiveMigration()

            // Add callback to seed default data on first creation
            .addCallback(DatabaseCallback())

            .build()

        // Set the singleton instance so BroadcastReceivers can access it
        FintraceDatabase.setInstance(database)

        return database
    }
    
    /**
     * Provides the TransactionDao from the database.
     * 
     * @param database The FintraceDatabase instance
     * @return TransactionDao for accessing transaction data
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: FintraceDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    /**
     * Provides the SubscriptionDao from the database.
     * 
     * @param database The FintraceDatabase instance
     * @return SubscriptionDao for accessing subscription data
     */
    @Provides
    @Singleton
    fun provideSubscriptionDao(database: FintraceDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }
    
    /**
     * Provides the MerchantMappingDao from the database.
     * 
     * @param database The FintraceDatabase instance
     * @return MerchantMappingDao for accessing merchant mapping data
     */
    @Provides
    @Singleton
    fun provideMerchantMappingDao(database: FintraceDatabase): MerchantMappingDao {
        return database.merchantMappingDao()
    }

    /**
     * Provides the MerchantAliasDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return MerchantAliasDao for accessing merchant alias data
     */
    @Provides
    @Singleton
    fun provideMerchantAliasDao(database: FintraceDatabase): MerchantAliasDao {
        return database.merchantAliasDao()
    }
    
    /**
     * Provides the CategoryDao from the database.
     * 
     * @param database The FintraceDatabase instance
     * @return CategoryDao for accessing category data
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: FintraceDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    /**
     * Provides the AccountBalanceDao from the database.
     * 
     * @param database The FintraceDatabase instance
     * @return AccountBalanceDao for accessing account balance data
     */
    @Provides
    @Singleton
    fun provideAccountBalanceDao(database: FintraceDatabase): AccountBalanceDao {
        return database.accountBalanceDao()
    }
    
    /**
     * Provides the UnrecognizedSmsDao from the database.
     * 
     * @param database The FintraceDatabase instance
     * @return UnrecognizedSmsDao for accessing unrecognized SMS data
     */
    @Provides
    @Singleton
    fun provideUnrecognizedSmsDao(database: FintraceDatabase): UnrecognizedSmsDao {
        return database.unrecognizedSmsDao()
    }
    
    /**
     * Provides the CardDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return CardDao for accessing card data
     */
    @Provides
    @Singleton
    fun provideCardDao(database: FintraceDatabase): CardDao {
        return database.cardDao()
    }

    /**
     * Provides the RuleDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return RuleDao for accessing rule data
     */
    @Provides
    @Singleton
    fun provideRuleDao(database: FintraceDatabase): RuleDao {
        return database.ruleDao()
    }

    /**
     * Provides the RuleApplicationDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return RuleApplicationDao for accessing rule application data
     */
    @Provides
    @Singleton
    fun provideRuleApplicationDao(database: FintraceDatabase): RuleApplicationDao {
        return database.ruleApplicationDao()
    }

    /**
     * Provides the ExchangeRateDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return ExchangeRateDao for accessing exchange rate data
     */
    @Provides
    @Singleton
    fun provideExchangeRateDao(database: FintraceDatabase): ExchangeRateDao {
        return database.exchangeRateDao()
    }

    /**
     * Provides the PendingTransactionDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return PendingTransactionDao for accessing pending transaction data
     */
    @Provides
    @Singleton
    fun providePendingTransactionDao(database: FintraceDatabase): PendingTransactionDao {
        return database.pendingTransactionDao()
    }

    /**
     * Provides the BudgetDao from the database.
     *
     * @param database The FintraceDatabase instance
     * @return BudgetDao for accessing budget data
     */
    @Provides
    @Singleton
    fun provideBudgetDao(database: FintraceDatabase): BudgetDao {
        return database.budgetDao()
    }
}

/**
 * Database callback to seed initial data when database is first created
 */
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Seed default categories for new installations
        CoroutineScope(Dispatchers.IO).launch {
            seedCategories(db)
        }
    }
    
    private fun seedCategories(db: SupportSQLiteDatabase) {
        val categories = listOf(
            Triple("Food & Dining", "#FC8019", false),
            Triple("Groceries", "#5AC85A", false),
            Triple("Transportation", "#000000", false),
            Triple("Shopping", "#FF9900", false),
            Triple("Bills & Utilities", "#4CAF50", false),
            Triple("Entertainment", "#E50914", false),
            Triple("Healthcare", "#10847E", false),
            Triple("Investments", "#00D09C", false),
            Triple("Banking", "#004C8F", false),
            Triple("Personal Care", "#6A4C93", false),
            Triple("Education", "#673AB7", false),
            Triple("Mobile", "#2A3890", false),
            Triple("Fitness", "#FF3278", false),
            Triple("Insurance", "#0066CC", false),
            Triple("Travel", "#00BCD4", false),
            Triple("Salary", "#4CAF50", true),
            Triple("Income", "#4CAF50", true),
            Triple("Others", "#757575", false)
        )
        
        categories.forEachIndexed { index, (name, color, isIncome) ->
            db.execSQL("""
                INSERT OR IGNORE INTO categories (name, color, is_system, is_income, display_order, created_at, updated_at)
                VALUES (?, ?, 1, ?, ?, datetime('now'), datetime('now'))
            """.trimIndent(), arrayOf<Any>(name, color, if (isIncome) 1 else 0, index + 1))
        }
    }
}
