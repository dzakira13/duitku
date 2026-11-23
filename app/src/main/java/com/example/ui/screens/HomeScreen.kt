package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WalletColor
import com.example.data.model.WalletType
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.TransactionRow
import com.example.ui.components.walletGradientBrush
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoralLight
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.viewmodel.UiState
import com.example.util.CurrencyFormatter

@Composable
fun HomeScreen(
    profileState: UiState<UserProfile?>,
    walletsState: UiState<List<Wallet>>,
    transactionsState: UiState<List<MoneyTransaction>>,
    fallbackDisplayName: String,
    fallbackAvatarUrl: String?,
    onOpenProfileTab: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onTogglePrivacy: (UserProfile) -> Unit,
    onAddWalletClick: () -> Unit,
    onEditWalletClick: (Wallet) -> Unit,
    onQuickAddTransaction: (TransactionType) -> Unit,
    onTransactionClick: (MoneyTransaction) -> Unit,
    onSeeAllTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val currency = CurrencyOption.fromCode(profile?.currencyCode)
    val hideAmounts = profile?.privacyBalanceHidden ?: false
    val displayName = profile?.displayName?.ifBlank { fallbackDisplayName } ?: fallbackDisplayName
    val avatarData = profile?.avatarData?.ifBlank { fallbackAvatarUrl.orEmpty() }
        ?: fallbackAvatarUrl.orEmpty()

    val wallets = (walletsState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    val currentMonthKey = CurrencyFormatter.currentMonthKey()
    val monthlyTransactions = transactions.filter {
        CurrencyFormatter.currentMonthKey(it.occurredAtMillis) == currentMonthKey
    }
    val monthlyIncome = monthlyTransactions
        .filter { TransactionType.fromRaw(it.transactionType) == TransactionType.INCOME }
        .sumOf { it.amount }
    val monthlyExpense = monthlyTransactions
        .filter { TransactionType.fromRaw(it.transactionType) == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val totalBalance = wallets.sumOf { it.balance }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header with Profile Avatar shortcut
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ProfileAvatar(
                    avatarData = avatarData,
                    displayName = displayName,
                    size = 48.dp,
                    onClick = onOpenProfileTab,
                    modifier = Modifier.testTag("home_profile_avatar")
                )
            }
        }

        // Onboarding callout when profile setup isn't completed yet
        item {
            AnimatedVisibility(visible = profile != null && !profile.profileSetupDone) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.complete_profile_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.complete_profile_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenEditProfile,
                            modifier = Modifier.testTag("complete_profile_cta")
                        ) {
                            Text(stringResource(R.string.setup_now))
                        }
                    }
                }
            }
        }

        // Net Balance Hero Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldDeep, EmeraldPrimary, EmeraldBright)
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.total_balance),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(totalBalance, currency, hidden = hideAmounts),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (profile != null) {
                            IconButton(
                                onClick = { onTogglePrivacy(profile) },
                                modifier = Modifier.testTag("toggle_privacy_button")
                            ) {
                                Icon(
                                    imageVector = if (hideAmounts) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (hideAmounts) "Show balances" else "Hide balances",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryStatPill(
                            label = stringResource(R.string.monthly_income),
                            value = CurrencyFormatter.format(monthlyIncome, currency, hidden = hideAmounts),
                            icon = Icons.Rounded.ArrowDownward,
                            accent = IncomeGreenLight,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = stringResource(R.string.monthly_expense),
                            value = CurrencyFormatter.format(monthlyExpense, currency, hidden = hideAmounts),
                            icon = Icons.Rounded.ArrowUpward,
                            accent = ExpenseCoralLight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onQuickAddTransaction(TransactionType.INCOME) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = EmeraldDeep
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_add_income_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_income), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onQuickAddTransaction(TransactionType.EXPENSE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x33FFFFFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_add_expense_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_expense), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Wallets section
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.wallets_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddWalletClick,
                    modifier = Modifier.testTag("add_wallet_header_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_wallet))
                }
            }
        }

        item {
            when (walletsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = walletsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (wallets.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .widthIn(max = 700.dp)
                                .fillMaxWidth()
                                .clickable(onClick = onAddWalletClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AsyncImage(
                                    model = R.drawable.img_empty_wallet_1795449652339,
                                    contentDescription = "Empty wallet illustration",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Create your first wallet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Add Cash, a Bank account, or an E-Wallet to begin tracking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(wallets, key = { it.id }) { wallet ->
                                WalletCard(
                                    wallet = wallet,
                                    currency = currency,
                                    hideAmounts = hideAmounts,
                                    onClick = { onEditWalletClick(wallet) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity header
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (transactions.isNotEmpty()) {
                    TextButton(onClick = onSeeAllTransactions) {
                        Text(stringResource(R.string.see_all))
                    }
                }
            }
        }

        when (transactionsState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = transactionsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is UiState.Success -> {
                if (transactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .widthIn(max = 700.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No activity yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Use + Income or + Expense above once you have a wallet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(transactions.take(6), key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            currency = currency,
                            hideAmounts = hideAmounts,
                            onClick = { onTransactionClick(tx) },
                            modifier = Modifier.widthIn(max = 700.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun SummaryStatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WalletCard(
    wallet: Wallet,
    currency: CurrencyOption,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    val color = WalletColor.fromRaw(wallet.colorKey)
    val type = WalletType.fromRaw(wallet.walletType)

    Box(
        modifier = Modifier
            .width(196.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(walletGradientBrush(color))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.format(wallet.balance, currency, hidden = hideAmounts),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
