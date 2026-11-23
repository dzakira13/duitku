package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.ui.components.TransactionRow
import com.example.ui.viewmodel.UiState

@Composable
fun TransactionsScreen(
    profileState: UiState<UserProfile?>,
    walletsState: UiState<List<Wallet>>,
    transactionsState: UiState<List<MoneyTransaction>>,
    onTransactionClick: (MoneyTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val currency = CurrencyOption.fromCode(profile?.currencyCode)
    val hideAmounts = profile?.privacyBalanceHidden ?: false
    val wallets = (walletsState as? UiState.Success)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedWalletId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Activity Ledger",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchant, category, or note…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("transactions_search_input")
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = typeFilter == null,
                onClick = { typeFilter = null },
                label = { Text("All") }
            )
            FilterChip(
                selected = typeFilter == TransactionType.EXPENSE,
                onClick = { typeFilter = TransactionType.EXPENSE },
                label = { Text("Expenses") }
            )
            FilterChip(
                selected = typeFilter == TransactionType.INCOME,
                onClick = { typeFilter = TransactionType.INCOME },
                label = { Text("Income") }
            )
            wallets.forEach { wallet ->
                FilterChip(
                    selected = selectedWalletId == wallet.id,
                    onClick = {
                        selectedWalletId = if (selectedWalletId == wallet.id) null else wallet.id
                    },
                    label = { Text(wallet.name) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when (transactionsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Text(transactionsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                val filtered = remember(transactionsState.data, searchQuery, typeFilter, selectedWalletId) {
                    transactionsState.data.filter { tx ->
                        val matchesQuery = searchQuery.isBlank() ||
                            tx.title.contains(searchQuery, ignoreCase = true) ||
                            tx.category.contains(searchQuery, ignoreCase = true) ||
                            tx.note.contains(searchQuery, ignoreCase = true)
                        val matchesType = typeFilter == null ||
                            TransactionType.fromRaw(tx.transactionType) == typeFilter
                        val matchesWallet = selectedWalletId == null || tx.walletId == selectedWalletId
                        matchesQuery && matchesType && matchesWallet
                    }
                }

                if (filtered.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No matching transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap + to record an income or expense entry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { tx ->
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
        }
    }
}
