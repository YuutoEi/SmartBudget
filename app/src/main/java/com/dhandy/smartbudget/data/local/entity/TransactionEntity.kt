package com.dhandy.smartbudget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE // Jika dompet dihapus, semua transaksi di dompet ini ikut terhapus
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT // Mencegah penghapusan kategori jika masih ada transaksi terkait
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(index = true)
    val walletId: Int,
    @ColumnInfo(index = true)
    val categoryId: Int,
    val amount: Double,
    val date: Long, // Disimpan dalam bentuk Unix Timestamp (milidetik)
    val note: String? = null
)
