package com.dhandy.smartbudget.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.dhandy.smartbudget.data.local.entity.CategoryEntity
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.WalletEntity

data class TransactionWithDetails(
    @Embedded 
    val transaction: TransactionEntity,
    
    @Embedded(prefix = "wallet_")
    val wallet: WalletEntity,
    
    @Embedded(prefix = "category_")
    val category: CategoryEntity
)
