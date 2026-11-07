package com.dhandy.smartbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dhandy.smartbudget.data.local.AppDatabase
import com.dhandy.smartbudget.data.repository.BudgetRepository
import com.dhandy.smartbudget.ui.components.AppBottomNav
import com.dhandy.smartbudget.ui.screens.AddTransactionScreen
import com.dhandy.smartbudget.ui.screens.DashboardScreen
import com.dhandy.smartbudget.ui.screens.AllTransactionsScreen
import com.dhandy.smartbudget.ui.screens.AnalyticsScreen
import com.dhandy.smartbudget.ui.screens.WalletCalculatorScreen
import com.dhandy.smartbudget.ui.theme.SmartBudgetTheme
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mengoptimalkan tampilan layar agar berjalan di 120Hz pada perangkat yang mendukung (via refleksi agar aman dari error kompilasi)
        try {
            val layoutParams = window.attributes
            val minField = layoutParams.javaClass.getField("preferredMinDisplayRefreshRate")
            val maxField = layoutParams.javaClass.getField("preferredMaxDisplayRefreshRate")
            minField.set(layoutParams, 120f)
            maxField.set(layoutParams, 120f)
            window.attributes = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 1. Inisialisasi Database dan Repository (Dependency Injection Manual untuk MVP)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BudgetRepository(database.budgetDao())

        // 2. Membuat Factory untuk ViewModel agar bisa menerima parameter Repository
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TransactionViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            // 4. Inisialisasi ViewModel menggunakan Factory
            val viewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            SmartBudgetTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Inisialisasi NavController untuk Navigasi antar layar
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Box(modifier = Modifier.fillMaxSize()) {
                        // 5. Setup NavHost dengan transisi slide halus antar layar (navbar auto slide out)
                        NavHost(
                            navController = navController, 
                            startDestination = "dashboard",
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            }
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onAddTransactionClick = { navController.navigate("add_transaction") },
                                    onAllTransactionsClick = { navController.navigate("all_transactions") },
                                    onAnalyticsClick = { navController.navigate("analytics") },
                                    onCalculatorClick = { navController.navigate("wallet_calculator") }
                                )
                            }
                            // Pop up bottom sheet ke atas dengan animasi slide in vertikal dari bawah ke atas
                            composable(
                                "add_transaction",
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(380)
                                    ) + fadeIn(animationSpec = tween(380))
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(320)
                                    ) + fadeOut(animationSpec = tween(320))
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(380)
                                    ) + fadeIn(animationSpec = tween(380))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(320)
                                    ) + fadeOut(animationSpec = tween(320))
                                }
                            ) {
                                AddTransactionScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("all_transactions") {
                                AllTransactionsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("analytics") {
                                AnalyticsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            // Pop up bottom sheet kalkulator ke atas dengan animasi slide in vertikal
                            composable(
                                "wallet_calculator",
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(380)
                                    ) + fadeIn(animationSpec = tween(380))
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(320)
                                    ) + fadeOut(animationSpec = tween(320))
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(380)
                                    ) + fadeIn(animationSpec = tween(380))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(320)
                                    ) + fadeOut(animationSpec = tween(320))
                                }
                            ) {
                                WalletCalculatorScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Bottom Navigation Bar ditampilkan di seluruh tab kecuali kalkulator
                        AppBottomNav(
                            currentRoute = currentRoute,
                            onNavigateToHome = {
                                if (currentRoute != "dashboard") {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onNavigateToCalculator = {
                                if (currentRoute != "wallet_calculator") {
                                    navController.navigate("wallet_calculator") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onNavigateToAddTransaction = {
                                if (currentRoute != "add_transaction") {
                                    navController.navigate("add_transaction") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onNavigateToAnalytics = {
                                if (currentRoute != "analytics") {
                                    navController.navigate("analytics") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onNavigateToAllTransactions = {
                                if (currentRoute != "all_transactions") {
                                    navController.navigate("all_transactions") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}