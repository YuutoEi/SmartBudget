package com.dhandy.smartbudget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // Contoh: Makanan, Gaji
    val type: String, // Tipe: "INCOME" atau "EXPENSE"
    val budgetLimit: Double? = null // Batas Anggaran (opsional)
)
