package com.fintrace.app.data.repository

import com.fintrace.app.data.database.dao.MerchantAliasDao
import com.fintrace.app.data.database.entity.MerchantAliasEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantAliasRepository @Inject constructor(
    private val merchantAliasDao: MerchantAliasDao
) {

    /**
     * Gets the alias for a merchant name.
     * Returns null if no alias is set.
     */
    suspend fun getAliasForMerchant(originalName: String): String? {
        return merchantAliasDao.getAlias(originalName)
    }

    /**
     * Sets or updates an alias for a merchant.
     */
    suspend fun setAlias(originalName: String, aliasName: String) {
        merchantAliasDao.insertOrUpdate(
            MerchantAliasEntity(
                originalName = originalName,
                aliasName = aliasName,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    /**
     * Removes an alias for a merchant.
     */
    suspend fun removeAlias(originalName: String) {
        merchantAliasDao.delete(originalName)
    }

    /**
     * Gets all merchant aliases as a Flow for UI.
     */
    fun getAllAliases(): Flow<List<MerchantAliasEntity>> {
        return merchantAliasDao.getAllAliases()
    }

    /**
     * Gets all merchant aliases as a list.
     */
    suspend fun getAllAliasesList(): List<MerchantAliasEntity> {
        return merchantAliasDao.getAllAliasesList()
    }

    /**
     * Checks if a merchant has an alias.
     */
    suspend fun hasAlias(originalName: String): Boolean {
        return merchantAliasDao.hasAlias(originalName)
    }

    /**
     * Gets the count of merchant aliases.
     */
    suspend fun getAliasCount(): Int {
        return merchantAliasDao.getAliasCount()
    }

    /**
     * Searches for aliases by original name or alias name.
     */
    fun searchAliases(query: String): Flow<List<MerchantAliasEntity>> {
        return merchantAliasDao.searchAliases(query)
    }
}
