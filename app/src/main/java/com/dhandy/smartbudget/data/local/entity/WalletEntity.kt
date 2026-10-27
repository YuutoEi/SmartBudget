package com.dhandy.smartbudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // Contoh: Cash, BCA, GoPay
    val initialBalance: Double,
    val currentBalance: Double
)
