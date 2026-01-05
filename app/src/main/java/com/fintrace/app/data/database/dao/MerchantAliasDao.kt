package com.fintrace.app.data.database.dao

import androidx.room.*
import com.fintrace.app.data.database.entity.MerchantAliasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantAliasDao {

    /**
     * Gets the alias for a merchant name (case-insensitive lookup).
     */
    @Query("SELECT alias_name FROM merchant_aliases WHERE LOWER(original_name) = LOWER(:originalName)")
    suspend fun getAlias(originalName: String): String?

    /**
     * Gets all merchant aliases, ordered by original name.
     */
    @Query("SELECT * FROM merchant_aliases ORDER BY original_name ASC")
    fun getAllAliases(): Flow<List<MerchantAliasEntity>>

    /**
     * Gets all merchant aliases as a list (for batch operations).
     */
    @Query("SELECT * FROM merchant_aliases ORDER BY original_name ASC")
    suspend fun getAllAliasesList(): List<MerchantAliasEntity>

    /**
     * Inserts or updates a merchant alias.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(alias: MerchantAliasEntity)

    /**
     * Deletes a merchant alias by original name.
     */
    @Query("DELETE FROM merchant_aliases WHERE original_name = :originalName")
    suspend fun delete(originalName: String)

    /**
     * Checks if an alias exists for a merchant.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM merchant_aliases WHERE LOWER(original_name) = LOWER(:originalName))")
    suspend fun hasAlias(originalName: String): Boolean

    /**
     * Gets the count of merchant aliases.
     */
    @Query("SELECT COUNT(*) FROM merchant_aliases")
    suspend fun getAliasCount(): Int

    /**
     * Deletes all merchant aliases.
     */
    @Query("DELETE FROM merchant_aliases")
    suspend fun deleteAll()

    /**
     * Searches for aliases by original name or alias name.
     */
    @Query("""
        SELECT * FROM merchant_aliases
        WHERE LOWER(original_name) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(alias_name) LIKE '%' || LOWER(:query) || '%'
        ORDER BY original_name ASC
    """)
    fun searchAliases(query: String): Flow<List<MerchantAliasEntity>>
}
