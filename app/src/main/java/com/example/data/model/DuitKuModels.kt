package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val label: String,
    val decimals: Int
) {
    IDR("IDR", "Rp", "Indonesian Rupiah (Rp)", 0),
    USD("USD", "$", "US Dollar ($)", 2),
    SGD("SGD", "S$", "Singapore Dollar (S$)", 2),
    MYR("MYR", "RM", "Malaysian Ringgit (RM)", 2),
    EUR("EUR", "€", "Euro (€)", 2);

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: IDR
    }
}

enum class ThemePreference(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromRaw(raw: String?): ThemePreference =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class WalletType(val label: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    EWALLET("E-Wallet"),
    SAVINGS("Savings"),
    INVESTMENT("Investment");

    companion object {
        fun fromRaw(raw: String?): WalletType =
            entries.firstOrNull { it.name == raw } ?: BANK
    }
}

enum class WalletColor(val label: String) {
    EMERALD("Emerald"),
    GOLD("Gold"),
    OCEAN("Ocean"),
    SLATE("Slate"),
    CORAL("Coral");

    companion object {
        fun fromRaw(raw: String?): WalletColor =
            entries.firstOrNull { it.name == raw } ?: EMERALD
    }
}

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

object TransactionCategories {
    val expenseCategories = listOf(
        "Food & Dining",
        "Transport",
        "Groceries",
        "Bills & Utilities",
        "Shopping",
        "Health",
        "Entertainment",
        "Education",
        "Other"
    )

    val incomeCategories = listOf(
        "Salary",
        "Freelance",
        "Business",
        "Investment",
        "Gift",
        "Refund",
        "Other"
    )
}

object AvatarPresets {
    const val EMERALD = "preset:emerald"
    const val GOLD = "preset:gold"
    const val OCEAN = "preset:ocean"
    const val CORAL = "preset:coral"
    const val SLATE = "preset:slate"

    val all = listOf(EMERALD, GOLD, OCEAN, CORAL, SLATE)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarData: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val currencyCode: String = CurrencyOption.IDR.code,
    val themeMode: String = ThemePreference.SYSTEM.name,
    val budgetCycleDay: Int = 1,
    val budgetAlertPercent: Int = 80,
    val privacyBalanceHidden: Boolean = false,
    val budgetAlertsEnabled: Boolean = true,
    val weeklyDigestEnabled: Boolean = true,
    val profileSetupDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val walletType: String = WalletType.BANK.name,
    val balance: Double = 0.0,
    val colorKey: String = WalletColor.EMERALD.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MoneyTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val transactionType: String = TransactionType.EXPENSE.name,
    val category: String = "Other",
    val walletId: String = "",
    val walletName: String = "",
    val note: String = "",
    val occurredAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "",
    val monthlyLimit: Double = 0.0,
    val monthKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
