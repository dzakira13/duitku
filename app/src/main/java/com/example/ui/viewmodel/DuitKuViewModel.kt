package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryBudget
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.ThemePreference
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WalletColor
import com.example.data.model.WalletType
import com.example.data.repository.DuitKuRepository
import com.example.util.CurrencyFormatter
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class DuitKuViewModel(
    private val repository: DuitKuRepository,
    private val currentUserId: String,
    private val googleDisplayName: String,
    private val googleEmail: String,
    private val googlePhotoUrl: String?
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val walletsState: StateFlow<UiState<List<Wallet>>> =
        repository.observeWallets(currentUserId)
            .map<List<Wallet>, UiState<List<Wallet>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing wallets", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load wallets"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val transactionsState: StateFlow<UiState<List<MoneyTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<MoneyTransaction>, UiState<List<MoneyTransaction>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing transactions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load transactions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val budgetsState: StateFlow<UiState<List<CategoryBudget>>> =
        repository.observeBudgets(currentUserId)
            .map<List<CategoryBudget>, UiState<List<CategoryBudget>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing budgets", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load budgets"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            repository.ensureInitialUserProfile(
                userId = currentUserId,
                defaultName = googleDisplayName,
                defaultEmail = googleEmail,
                defaultAvatarUrl = googlePhotoUrl
            )
        }
    }

    fun consumeMessage() {
        _snackbarMessage.value = null
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun saveUserProfile(
        displayName: String,
        bio: String,
        avatarData: String,
        email: String,
        phone: String,
        location: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveUserProfile(
                userId = currentUserId,
                displayName = displayName,
                bio = bio,
                avatarData = avatarData,
                email = email,
                phone = phone,
                location = location
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _snackbarMessage.value = "Profile updated"
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save profile"
                }
            )
        }
    }

    fun updatePreferences(
        currency: CurrencyOption,
        theme: ThemePreference,
        budgetCycleDay: Int,
        budgetAlertPercent: Int,
        privacyBalanceHidden: Boolean,
        budgetAlertsEnabled: Boolean,
        weeklyDigestEnabled: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.updatePreferences(
                userId = currentUserId,
                currencyCode = currency,
                themeMode = theme,
                budgetCycleDay = budgetCycleDay,
                budgetAlertPercent = budgetAlertPercent,
                privacyBalanceHidden = privacyBalanceHidden,
                budgetAlertsEnabled = budgetAlertsEnabled,
                weeklyDigestEnabled = weeklyDigestEnabled
            )
            result.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to update preference"
            }
        }
    }

    fun togglePrivacyBalance(currentProfile: UserProfile) {
        updatePreferences(
            currency = CurrencyOption.fromCode(currentProfile.currencyCode),
            theme = ThemePreference.fromRaw(currentProfile.themeMode),
            budgetCycleDay = currentProfile.budgetCycleDay.coerceIn(1, 28),
            budgetAlertPercent = currentProfile.budgetAlertPercent.coerceIn(50, 100),
            privacyBalanceHidden = !currentProfile.privacyBalanceHidden,
            budgetAlertsEnabled = currentProfile.budgetAlertsEnabled,
            weeklyDigestEnabled = currentProfile.weeklyDigestEnabled
        )
    }

    fun saveWallet(
        walletId: String?,
        name: String,
        walletType: WalletType,
        balance: Double,
        colorKey: WalletColor,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.upsertWallet(
                userId = currentUserId,
                walletId = walletId,
                name = name,
                walletType = walletType,
                balance = balance,
                colorKey = colorKey
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _snackbarMessage.value = if (walletId == null) "Wallet created" else "Wallet updated"
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save wallet"
                }
            )
        }
    }

    fun deleteWallet(walletId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteWallet(currentUserId, walletId)
            _isBusy.value = false
            result.fold(
                onSuccess = { _snackbarMessage.value = "Wallet removed" },
                onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Could not delete wallet" }
            )
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        wallet: Wallet,
        note: String,
        occurredAtMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addTransaction(
                userId = currentUserId,
                title = title,
                amount = amount,
                transactionType = type,
                category = category,
                wallet = wallet,
                note = note,
                occurredAtMillis = occurredAtMillis
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _snackbarMessage.value = "Transaction saved"
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save transaction"
                }
            )
        }
    }

    fun updateTransaction(
        oldTransaction: MoneyTransaction,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        newWallet: Wallet,
        note: String,
        occurredAtMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateTransaction(
                userId = currentUserId,
                oldTransaction = oldTransaction,
                title = title,
                amount = amount,
                transactionType = type,
                category = category,
                newWallet = newWallet,
                note = note,
                occurredAtMillis = occurredAtMillis
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _snackbarMessage.value = "Transaction updated"
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not update transaction"
                }
            )
        }
    }

    fun deleteTransaction(transaction: MoneyTransaction) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteTransaction(currentUserId, transaction)
            _isBusy.value = false
            result.fold(
                onSuccess = { _snackbarMessage.value = "Transaction deleted" },
                onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Could not delete transaction" }
            )
        }
    }

    fun saveBudget(
        budgetId: String?,
        category: String,
        monthlyLimit: Double,
        monthKey: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.upsertBudget(
                userId = currentUserId,
                budgetId = budgetId,
                category = category,
                monthlyLimit = monthlyLimit,
                monthKey = monthKey
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _snackbarMessage.value = "Budget saved"
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save budget"
                }
            )
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteBudget(currentUserId, budgetId)
            _isBusy.value = false
            result.fold(
                onSuccess = { _snackbarMessage.value = "Budget removed" },
                onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Could not remove budget" }
            )
        }
    }

    fun clearFinancialData() {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.clearFinancialData(currentUserId)
            _isBusy.value = false
            result.fold(
                onSuccess = { _snackbarMessage.value = "All wallets, transactions, and budgets cleared" },
                onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Failed to reset data" }
            )
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteAccountData(currentUserId)
            _isBusy.value = false
            result.fold(
                onSuccess = { onDeleted() },
                onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete profile data" }
            )
        }
    }

    fun buildExportSummaryText(
        profile: UserProfile?,
        wallets: List<Wallet>,
        transactions: List<MoneyTransaction>
    ): String {
        val currency = CurrencyOption.fromCode(profile?.currencyCode)
        val totalBalance = wallets.sumOf { it.balance }
        val sb = StringBuilder()
        sb.appendLine("DuitKu Financial Summary")
        sb.appendLine("Owner: ${profile?.displayName ?: googleDisplayName}")
        sb.appendLine("Total Net Balance: ${CurrencyFormatter.format(totalBalance, currency)}")
        sb.appendLine()
        sb.appendLine("Wallets (${wallets.size}):")
        wallets.forEach { w ->
            sb.appendLine("- ${w.name} (${WalletType.fromRaw(w.walletType).label}): ${CurrencyFormatter.format(w.balance, currency)}")
        }
        sb.appendLine()
        sb.appendLine("Recent Transactions (${transactions.take(25).size}):")
        transactions.take(25).forEach { tx ->
            val sign = if (TransactionType.fromRaw(tx.transactionType) == TransactionType.INCOME) "+" else "-"
            sb.appendLine("- ${CurrencyFormatter.formatShortDate(tx.occurredAtMillis)} | ${tx.title} [${tx.category}] $sign${CurrencyFormatter.format(tx.amount, currency)} (${tx.walletName})")
        }
        return sb.toString()
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "DuitKuVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
