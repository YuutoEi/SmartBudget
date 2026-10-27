package com.dhandy.smartbudget.data.local.dao

import androidx.room.*
import com.dhandy.smartbudget.data.local.entity.CategoryEntity
import com.dhandy.smartbudget.data.local.entity.TargetEntity
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // ========================
    // Wallet Operations
    // ========================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity): Int

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity): Int

    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    suspend fun getWalletById(walletId: Int): WalletEntity?

    // ========================
    // Category Operations
    // ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity): Int

    @Delete
    suspend fun deleteCategory(category: CategoryEntity): Int

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByNameAndType(name: String, type: String): CategoryEntity?

    // ========================
    // Transaction Operations
    // ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity): Int

    @Query("""
        SELECT t.*, 
               w.id AS wallet_id, w.name AS wallet_name, w.initialBalance AS wallet_initialBalance, w.currentBalance AS wallet_currentBalance,
               c.id AS category_id, c.name AS category_name, c.type AS category_type, c.budgetLimit AS category_budgetLimit
        FROM transactions t
        INNER JOIN wallets w ON t.walletId = w.id
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    // ========================
    // Target Operations
    // ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetEntity): Long

    @Update
    suspend fun updateTarget(target: TargetEntity): Int

    @Delete
    suspend fun deleteTarget(target: TargetEntity): Int

    @Query("SELECT * FROM targets")
    fun getAllTargets(): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE id = :targetId LIMIT 1")
    suspend fun getTargetById(targetId: Int): TargetEntity?
}
