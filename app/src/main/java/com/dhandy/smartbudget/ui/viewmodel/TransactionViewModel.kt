package com.dhandy.smartbudget.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import com.dhandy.smartbudget.data.local.entity.TargetEntity
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import com.dhandy.smartbudget.data.repository.BudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(private val repository: BudgetRepository) : ViewModel() {

    // State untuk mengontrol tema gelap/terang secara manual (Default ke Dark Mode)
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    // Mengekspos daftar transaksi secara real-time sebagai StateFlow untuk Jetpack Compose
    val transactions: StateFlow<List<TransactionWithDetails>> = repository.getAllTransactionsWithDetails()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Berhenti mengumpulkan jika UI tidak aktif selama 5 detik
            initialValue = emptyList()
        )

    // Mengekspos daftar dompet untuk menghitung total saldo
    val wallets: StateFlow<List<WalletEntity>> = repository.getAllWallets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Mengekspos daftar kategori untuk dropdown
    val categories: StateFlow<List<com.dhandy.smartbudget.data.local.entity.CategoryEntity>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Mengekspos daftar target
    val targets: StateFlow<List<TargetEntity>> = repository.getAllTargets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Menambah transaksi baru dan secara otomatis memperbarui saldo dompet terkait
     * Operasi database dijalankan di viewModelScope (background thread via Coroutines)
     */
    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransactionAndUpdateWallet(transaction)
        }
    }

    /**
     * Menambah dompet baru kustom oleh pengguna
     */
    fun addWallet(name: String, initialBalance: Double) {
        viewModelScope.launch {
            val wallet = WalletEntity(
                name = name,
                initialBalance = initialBalance,
                currentBalance = initialBalance
            )
            repository.addWallet(wallet)
        }
    }

    /**
     * Memperbarui data dompet kustom
     */
    fun updateWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            repository.updateWallet(wallet)
        }
    }

    /**
     * Menghapus dompet kustom
     */
    fun deleteWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }

    /**
     * Memproses transfer dana antar dompet
     */
    fun transferFunds(fromWalletId: Int, toWalletId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.transferFunds(fromWalletId, toWalletId, amount, note)
        }
    }

    /**
     * Menambah target baru kustom oleh pengguna
     */
    fun addTarget(name: String, targetAmount: Double) {
        viewModelScope.launch {
            val target = TargetEntity(name = name, targetAmount = targetAmount, currentAmount = 0.0)
            repository.addTarget(target)
        }
    }

    /**
     * Memperbarui target
     */
    fun updateTarget(target: TargetEntity) {
        viewModelScope.launch {
            repository.updateTarget(target)
        }
    }

    /**
     * Menghapus target
     */
    fun deleteTarget(target: TargetEntity) {
        viewModelScope.launch {
            repository.deleteTarget(target)
        }
    }

    /**
     * Memproses transfer menabung ke target dari dompet
     */
    fun savingToTarget(walletId: Int, targetId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.savingToTarget(walletId, targetId, amount, note)
        }
    }
}
