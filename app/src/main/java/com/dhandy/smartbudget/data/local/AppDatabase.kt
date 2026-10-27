package com.dhandy.smartbudget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhandy.smartbudget.data.local.dao.BudgetDao
import com.dhandy.smartbudget.data.local.entity.CategoryEntity
import com.dhandy.smartbudget.data.local.entity.TargetEntity
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WalletEntity::class, CategoryEntity::class, TransactionEntity::class, TargetEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrasi dari Versi 1 ke Versi 2 (Menambahkan tabel Target) untuk mencegah hilangnya data pengguna
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `targets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_budget_database"
                )
                .addMigrations(MIGRATION_1_2)
                // Menambahkan data awal (pre-populate) saat database pertama kali dibuat
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            populateDefaultDataIfEmpty(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            populateDefaultDataIfEmpty(db)
        }

        private fun populateDefaultDataIfEmpty(db: SupportSQLiteDatabase) {
            // 1. Inisialisasi Dompet Bawaan (hanya jika belum ada dompet sama sekali di database)
            val walletCursor = db.query("SELECT COUNT(*) FROM wallets")
            var walletCount = 0
            if (walletCursor.moveToFirst()) {
                walletCount = walletCursor.getInt(0)
            }
            walletCursor.close()

            if (walletCount == 0) {
                db.execSQL(
                    "INSERT INTO wallets (name, initialBalance, currentBalance) VALUES ('Cash', 0.0, 0.0)"
                )
            }

            // 2. Inisialisasi Kategori Bawaan (jika belum ada)
            val defaultCategories = listOf(
                // INCOME
                Pair("Gaji", "INCOME"),
                Pair("Investasi", "INCOME"),
                Pair("Bonus", "INCOME"),
                Pair("Penjualan", "INCOME"),
                Pair("Lain-lain (Pemasukan)", "INCOME"),
                // EXPENSE
                Pair("Makanan & Minuman", "EXPENSE"),
                Pair("Transportasi", "EXPENSE"),
                Pair("Hiburan & Rekreasi", "EXPENSE"),
                Pair("Belanja", "EXPENSE"),
                Pair("Tagihan & Utilitas", "EXPENSE"),
                Pair("Kesehatan", "EXPENSE"),
                Pair("Pendidikan", "EXPENSE"),
                Pair("Tempat Tinggal", "EXPENSE"),
                Pair("Asuransi", "EXPENSE"),
                Pair("Hobi & Olahraga", "EXPENSE"),
                Pair("Donasi & Hadiah", "EXPENSE"),
                Pair("Lain-lain (Pengeluaran)", "EXPENSE")
            )

            val budgetLimits = mapOf(
                "Makanan & Minuman" to 1500000.0,
                "Transportasi" to 500000.0,
                "Hiburan & Rekreasi" to 500000.0,
                "Belanja" to 1000000.0,
                "Tagihan & Utilitas" to 750000.0,
                "Kesehatan" to 500000.0,
                "Pendidikan" to 1000000.0,
                "Tempat Tinggal" to 2000000.0,
                "Asuransi" to 500000.0,
                "Hobi & Olahraga" to 400000.0,
                "Donasi & Hadiah" to 300000.0,
                "Lain-lain (Pengeluaran)" to 300000.0
            )

            for (cat in defaultCategories) {
                val cursor = db.query("SELECT COUNT(*) FROM categories WHERE name = ? AND type = ?", arrayOf(cat.first, cat.second))
                var exists = false
                if (cursor.moveToFirst()) {
                    exists = cursor.getInt(0) > 0
                }
                cursor.close()

                if (!exists) {
                    val limit = budgetLimits[cat.first]
                    val limitVal = if (limit != null) "$limit" else "NULL"
                    db.execSQL(
                        "INSERT INTO categories (name, type, budgetLimit) VALUES ('${cat.first}', '${cat.second}', $limitVal)"
                    )
                }
            }
        }
    }
}
