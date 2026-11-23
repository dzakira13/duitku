package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.CategoryBudget
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.ThemePreference
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.data.repository.DuitKuRepository
import com.example.ui.components.BudgetEditorDialog
import com.example.ui.components.EditProfileSheet
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.components.WalletEditorDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DuitKuTheme
import com.example.ui.viewmodel.DuitKuViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.CurrencyFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class DuitKuRoute(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    ACTIVITY("activity", R.string.nav_transactions, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BUDGETS("budgets", R.string.nav_analytics, Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    PROFILE("profile", R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person)
}

@Composable
fun DuitKuApp() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    val user = currentUser
    if (user == null) {
        DuitKuTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedDuitKuShell(
                currentUserId = user.uid,
                googleDisplayName = user.displayName ?: "DuitKu User",
                googleEmail = user.email ?: "",
                googlePhotoUrl = user.photoUrl?.toString()
            )
        }
    }
}

@Composable
private fun AuthenticatedDuitKuShell(
    currentUserId: String,
    googleDisplayName: String,
    googleEmail: String,
    googlePhotoUrl: String?
) {
    val viewModel: DuitKuViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DuitKuViewModel(
                    repository = DuitKuRepository(db),
                    currentUserId = currentUserId,
                    googleDisplayName = googleDisplayName,
                    googleEmail = googleEmail,
                    googlePhotoUrl = googlePhotoUrl
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val walletsState by viewModel.walletsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val budgetsState by viewModel.budgetsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val themePreference = ThemePreference.fromRaw(profile?.themeMode)
    val currency = CurrencyOption.fromCode(profile?.currencyCode)
    val wallets = (walletsState as? UiState.Success)?.data.orEmpty()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: DuitKuRoute.HOME.route

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    var showEditProfileSheet by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var showCreateWalletDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var showCreateTransactionType by remember { mutableStateOf<TransactionType?>(null) }
    var editingBudget by remember { mutableStateOf<CategoryBudget?>(null) }
    var showCreateBudgetDialog by remember { mutableStateOf(false) }

    val navigateToTab: (DuitKuRoute) -> Unit = { target ->
        navController.navigate(target.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openQuickTransaction: (TransactionType) -> Unit = { type ->
        if (wallets.isEmpty()) {
            viewModel.postMessage("Create a wallet first to record transactions.")
            showCreateWalletDialog = true
        } else {
            showCreateTransactionType = type
        }
    }

    DuitKuTheme(themePreference = themePreference) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            DuitKuRoute.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag("nav_tab_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentRoute == DuitKuRoute.HOME.route || currentRoute == DuitKuRoute.ACTIVITY.route) {
                        FloatingActionButton(
                            onClick = { openQuickTransaction(TransactionType.EXPENSE) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_transaction")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            DuitKuRoute.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag("rail_tab_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = DuitKuRoute.HOME.route
                        ) {
                            composable(DuitKuRoute.HOME.route) {
                                HomeScreen(
                                    profileState = profileState,
                                    walletsState = walletsState,
                                    transactionsState = transactionsState,
                                    fallbackDisplayName = googleDisplayName,
                                    fallbackAvatarUrl = googlePhotoUrl,
                                    onOpenProfileTab = { navigateToTab(DuitKuRoute.PROFILE) },
                                    onOpenEditProfile = { showEditProfileSheet = true },
                                    onTogglePrivacy = { viewModel.togglePrivacyBalance(it) },
                                    onAddWalletClick = { showCreateWalletDialog = true },
                                    onEditWalletClick = { editingWallet = it },
                                    onQuickAddTransaction = openQuickTransaction,
                                    onTransactionClick = { editingTransaction = it },
                                    onSeeAllTransactions = { navigateToTab(DuitKuRoute.ACTIVITY) }
                                )
                            }

                            composable(DuitKuRoute.ACTIVITY.route) {
                                TransactionsScreen(
                                    profileState = profileState,
                                    walletsState = walletsState,
                                    transactionsState = transactionsState,
                                    onTransactionClick = { editingTransaction = it }
                                )
                            }

                            composable(DuitKuRoute.BUDGETS.route) {
                                AnalyticsScreen(
                                    profileState = profileState,
                                    transactionsState = transactionsState,
                                    budgetsState = budgetsState,
                                    onAddBudgetClick = { showCreateBudgetDialog = true },
                                    onEditBudgetClick = { editingBudget = it }
                                )
                            }

                            composable(DuitKuRoute.PROFILE.route) {
                                ProfileScreen(
                                    profileState = profileState,
                                    walletsState = walletsState,
                                    transactionsState = transactionsState,
                                    currentUserId = currentUserId,
                                    googleEmail = googleEmail,
                                    googleDisplayName = googleDisplayName,
                                    googleAvatarUrl = googlePhotoUrl,
                                    onEditProfileClick = { showEditProfileSheet = true },
                                    onUpdatePreferences = { cur, theme, cycleDay, alertPct, privacy, alerts, digest ->
                                        viewModel.updatePreferences(
                                            currency = cur,
                                            theme = theme,
                                            budgetCycleDay = cycleDay,
                                            budgetAlertPercent = alertPct,
                                            privacyBalanceHidden = privacy,
                                            budgetAlertsEnabled = alerts,
                                            weeklyDigestEnabled = digest
                                        )
                                    },
                                    onBuildExportText = { p, w, t ->
                                        viewModel.buildExportSummaryText(p, w, t)
                                    },
                                    onResetFinancialData = { viewModel.clearFinancialData() },
                                    onSignOutClick = {
                                        signOut(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    },
                                    onDeleteAccountData = {
                                        viewModel.deleteAccountData {
                                            signOut(
                                                credentialManager = credentialManager,
                                                scope = scope
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showEditProfileSheet) {
                EditProfileSheet(
                    profile = profile,
                    googlePhotoUrl = googlePhotoUrl,
                    googleDisplayName = googleDisplayName,
                    googleEmail = googleEmail,
                    isBusy = isBusy,
                    onDismiss = { showEditProfileSheet = false },
                    onSave = { name, bio, avatar, email, phone, location ->
                        viewModel.saveUserProfile(
                            displayName = name,
                            bio = bio,
                            avatarData = avatar,
                            email = email,
                            phone = phone,
                            location = location,
                            onSuccess = { showEditProfileSheet = false }
                        )
                    }
                )
            }

            if (showCreateWalletDialog || editingWallet != null) {
                val targetWallet = editingWallet
                WalletEditorDialog(
                    existingWallet = targetWallet,
                    currency = currency,
                    isBusy = isBusy,
                    onDismiss = {
                        showCreateWalletDialog = false
                        editingWallet = null
                    },
                    onSave = { name, type, balance, color ->
                        viewModel.saveWallet(
                            walletId = targetWallet?.id,
                            name = name,
                            walletType = type,
                            balance = balance,
                            colorKey = color,
                            onSuccess = {
                                showCreateWalletDialog = false
                                editingWallet = null
                            }
                        )
                    },
                    onDelete = targetWallet?.let { w ->
                        {
                            viewModel.deleteWallet(w.id)
                            editingWallet = null
                        }
                    }
                )
            }

            if (showCreateTransactionType != null || editingTransaction != null) {
                val targetTx = editingTransaction
                TransactionEditorSheet(
                    existingTransaction = targetTx,
                    initialType = showCreateTransactionType ?: TransactionType.EXPENSE,
                    wallets = wallets,
                    currency = currency,
                    isBusy = isBusy,
                    onDismiss = {
                        showCreateTransactionType = null
                        editingTransaction = null
                    },
                    onSave = { title, amount, type, category, wallet, note, occurredAtMillis ->
                        if (targetTx == null) {
                            viewModel.addTransaction(
                                title = title,
                                amount = amount,
                                type = type,
                                category = category,
                                wallet = wallet,
                                note = note,
                                occurredAtMillis = occurredAtMillis,
                                onSuccess = { showCreateTransactionType = null }
                            )
                        } else {
                            viewModel.updateTransaction(
                                oldTransaction = targetTx,
                                title = title,
                                amount = amount,
                                type = type,
                                category = category,
                                newWallet = wallet,
                                note = note,
                                occurredAtMillis = occurredAtMillis,
                                onSuccess = { editingTransaction = null }
                            )
                        }
                    },
                    onDelete = targetTx?.let { tx ->
                        {
                            viewModel.deleteTransaction(tx)
                            editingTransaction = null
                        }
                    }
                )
            }

            if (showCreateBudgetDialog || editingBudget != null) {
                val targetBudget = editingBudget
                val monthKey = remember { CurrencyFormatter.currentMonthKey() }
                BudgetEditorDialog(
                    existingBudget = targetBudget,
                    currency = currency,
                    monthKey = targetBudget?.monthKey ?: monthKey,
                    isBusy = isBusy,
                    onDismiss = {
                        showCreateBudgetDialog = false
                        editingBudget = null
                    },
                    onSave = { category, limit ->
                        viewModel.saveBudget(
                            budgetId = targetBudget?.id,
                            category = category,
                            monthlyLimit = limit,
                            monthKey = targetBudget?.monthKey ?: monthKey,
                            onSuccess = {
                                showCreateBudgetDialog = false
                                editingBudget = null
                            }
                        )
                    },
                    onDelete = targetBudget?.let { b ->
                        {
                            viewModel.deleteBudget(b.id)
                            editingBudget = null
                        }
                    }
                )
            }
        }
    }
}
