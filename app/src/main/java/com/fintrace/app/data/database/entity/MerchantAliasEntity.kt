package com.fintrace.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entity representing a merchant alias mapping.
 * When an SMS is processed, if the original merchant name has an alias,
 * the alias name is used throughout the app instead.
 */
@Entity(tableName = "merchant_aliases")
data class MerchantAliasEntity(
    @PrimaryKey
    @ColumnInfo(name = "original_name")
    val originalName: String,

    @ColumnInfo(name = "alias_name")
    val aliasName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
