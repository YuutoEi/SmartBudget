package com.dhandy.smartbudget.data.repository

import com.dhandy.smartbudget.data.local.dao.BudgetDao
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.TargetEntity
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    // Mengambil semua transaksi beserta detailnya sebagai Flow
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>> {
        return budgetDao.getAllTransactionsWithDetails()
    }

    // Mengambil semua dompet
    fun getAllWallets(): Flow<List<WalletEntity>> {
        return budgetDao.getAllWallets()
    }

    // Mengambil semua kategori
    fun getAllCategories(): Flow<List<com.dhandy.smartbudget.data.local.entity.CategoryEntity>> {
        return budgetDao.getAllCategories()
    }

    // Menambah transaksi baru dan secara otomatis memperbarui saldo dompet
    suspend fun addTransactionAndUpdateWallet(transaction: TransactionEntity) {
        // 1. Dapatkan informasi dompet dan kategori
        val wallet = budgetDao.getWalletById(transaction.walletId)
        val category = budgetDao.getCategoryById(transaction.categoryId)

        if (wallet != null && category != null) {
            // 2. Hitung saldo baru berdasarkan tipe kategori (INCOME atau EXPENSE)
            val newBalance = if (category.type == "INCOME") {
                wallet.currentBalance + transaction.amount
            } else {
                wallet.currentBalance - transaction.amount
            }

            // 3. Update saldo di dompet
            budgetDao.updateWallet(wallet.copy(currentBalance = newBalance))
        }

        // 4. Masukkan transaksi ke database
        budgetDao.insertTransaction(transaction)
    }

    // Menambah dompet baru
    suspend fun addWallet(wallet: WalletEntity) {
        budgetDao.insertWallet(wallet)
    }

    // Memperbarui dompet
    suspend fun updateWallet(wallet: WalletEntity) {
        budgetDao.updateWallet(wallet)
    }

    // Menghapus dompet
    suspend fun deleteWallet(wallet: WalletEntity) {
        budgetDao.deleteWallet(wallet)
    }

    // Memproses transfer saldo antar dompet
    suspend fun transferFunds(fromWalletId: Int, toWalletId: Int, amount: Double, note: String?) {
        // 1. Ambil atau buat kategori transfer
        var categoryOut = budgetDao.getCategoryByNameAndType("Transfer Keluar", "EXPENSE")
        if (categoryOut == null) {
            val newId = budgetDao.insertCategory(com.dhandy.smartbudget.data.local.entity.CategoryEntity(name = "Transfer Keluar", type = "EXPENSE"))
            categoryOut = com.dhandy.smartbudget.data.local.entity.CategoryEntity(id = newId.toInt(), name = "Transfer Keluar", type = "EXPENSE")
        }

        var categoryIn = budgetDao.getCategoryByNameAndType("Transfer Masuk", "INCOME")
        if (categoryIn == null) {
            val newId = budgetDao.insertCategory(com.dhandy.smartbudget.data.local.entity.CategoryEntity(name = "Transfer Masuk", type = "INCOME"))
            categoryIn = com.dhandy.smartbudget.data.local.entity.CategoryEntity(id = newId.toInt(), name = "Transfer Masuk", type = "INCOME")
        }

        // 2. Transaksi keluar dari dompet asal
        val transactionOut = TransactionEntity(
            walletId = fromWalletId,
            categoryId = categoryOut.id,
            amount = amount,
            date = System.currentTimeMillis(),
            note = note ?: "Transfer ke dompet lain"
        )
        addTransactionAndUpdateWallet(transactionOut)

        // 3. Transaksi masuk ke dompet tujuan
        val transactionIn = TransactionEntity(
            walletId = toWalletId,
            categoryId = categoryIn.id,
            amount = amount,
            date = System.currentTimeMillis(),
            note = note ?: "Transfer dari dompet lain"
        )
        addTransactionAndUpdateWallet(transactionIn)
    }

    // Mengambil semua target
    fun getAllTargets(): Flow<List<TargetEntity>> {
        return budgetDao.getAllTargets()
    }

    // Menambah target baru
    suspend fun addTarget(target: TargetEntity) {
        budgetDao.insertTarget(target)
    }

    // Memperbarui target
    suspend fun updateTarget(target: TargetEntity) {
        budgetDao.updateTarget(target)
    }

    // Menghapus target
    suspend fun deleteTarget(target: TargetEntity) {
        budgetDao.deleteTarget(target)
    }

    // Memproses menabung/transfer dari dompet ke Target
    suspend fun savingToTarget(walletId: Int, targetId: Int, amount: Double, note: String?) {
        val wallet = budgetDao.getWalletById(walletId)
        val target = budgetDao.getTargetById(targetId)

        if (wallet != null && target != null) {
            // 1. Ambil atau buat kategori tabungan target
            var category = budgetDao.getCategoryByNameAndType("Tabungan Target", "EXPENSE")
            if (category == null) {
                val newId = budgetDao.insertCategory(com.dhandy.smartbudget.data.local.entity.CategoryEntity(name = "Tabungan Target", type = "EXPENSE"))
                category = com.dhandy.smartbudget.data.local.entity.CategoryEntity(id = newId.toInt(), name = "Tabungan Target", type = "EXPENSE")
            }

            // 2. Kurangi saldo di dompet asal
            val newWalletBalance = wallet.currentBalance - amount
            budgetDao.updateWallet(wallet.copy(currentBalance = newWalletBalance))

            // 3. Tambah saldo terkumpul di Target
            val newTargetAmount = target.currentAmount + amount
            budgetDao.updateTarget(target.copy(currentAmount = newTargetAmount))

            // 4. Catat transaksi pengeluaran dari dompet asal
            val transaction = TransactionEntity(
                walletId = walletId,
                categoryId = category.id,
                amount = amount,
                date = System.currentTimeMillis(),
                note = note ?: "Tabung untuk target: ${target.name}"
            )
            budgetDao.insertTransaction(transaction)
        }
    }

    // Delegasi fungsi lain jika diperlukan (contoh: insert kategori, dompet, dll)
}
