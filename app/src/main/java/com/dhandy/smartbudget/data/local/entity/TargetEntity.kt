package com.dhandy.smartbudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "targets")
data class TargetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // Contoh: iPhone 17 Pro
    val targetAmount: Double, // Target harga barang
    val currentAmount: Double = 0.0 // Saldo terkumpul
)
