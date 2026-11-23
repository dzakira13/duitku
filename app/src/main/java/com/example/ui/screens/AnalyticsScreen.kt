package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryBudget
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.ui.components.CategorySpendSlice
import com.example.ui.components.ChartSliceColors
import com.example.ui.components.SpendingDonutChart
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.UiState
import com.example.util.CurrencyFormatter

@Composable
fun AnalyticsScreen(
    profileState: UiState<UserProfile?>,
    transactionsState: UiState<List<MoneyTransaction>>,
    budgetsState: UiState<List<CategoryBudget>>,
    onAddBudgetClick: () -> Unit,
    onEditBudgetClick: (CategoryBudget) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val currency = CurrencyOption.fromCode(profile?.currencyCode)
    val hideAmounts = profile?.privacyBalanceHidden ?: false
    val alertThresholdPct = profile?.budgetAlertPercent ?: 80
    val alertsEnabled = profile?.budgetAlertsEnabled ?: true

    val monthKey = remember { CurrencyFormatter.currentMonthKey() }
    val monthLabel = remember(monthKey) { CurrencyFormatter.currentMonthLabel(monthKey) }

    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val monthlyExpenses = remember(transactions, monthKey) {
        transactions.filter {
            TransactionType.fromRaw(it.transactionType) == TransactionType.EXPENSE &&
                CurrencyFormatter.currentMonthKey(it.occurredAtMillis) == monthKey
        }
    }

    val spentByCategory = remember(monthlyExpenses) {
        monthlyExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    val slices = remember(spentByCategory) {
        spentByCategory.entries
            .sortedByDescending { it.value }
            .mapIndexed { idx, entry ->
                CategorySpendSlice(
                    category = entry.key,
                    amount = entry.value,
                    color = ChartSliceColors[idx % ChartSliceColors.size]
                )
            }
    }

    val budgets = (budgetsState as? UiState.Success)?.data.orEmpty()
        .filter { it.monthKey == monthKey }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Budgets & Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Spending Donut Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Spending Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    if (slices.isEmpty()) {
                        Text(
                            text = "No expenses recorded for $monthLabel yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SpendingDonutChart(
                            slices = slices,
                            currency = currency,
                            hideAmounts = hideAmounts
                        )
                    }
                }
            }
        }

        // Monthly Budgets Header
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddBudgetClick,
                    modifier = Modifier.testTag("add_budget_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Set Budget")
                }
            }
        }

        when (budgetsState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { Text(budgetsState.message, color = MaterialTheme.colorScheme.error) }
            }
            is UiState.Success -> {
                if (budgets.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .widthIn(max = 700.dp)
                                .fillMaxWidth()
                                .clickable(onClick = onAddBudgetClick)
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No category budgets yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap Set Budget to cap monthly spending in Food, Transport, or Shopping.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(budgets, key = { it.id }) { budget ->
                        val spent = spentByCategory[budget.category] ?: 0.0
                        val ratio = (spent / budget.monthlyLimit.coerceAtLeast(1.0)).toFloat()
                        val pct = (ratio * 100f).toInt()
                        val isOver = pct >= 100
                        val isNear = alertsEnabled && pct >= alertThresholdPct && !isOver
                        val barColor = when {
                            isOver -> ExpenseCoral
                            isNear -> WarningAmber
                            else -> IncomeGreen
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .widthIn(max = 700.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onEditBudgetClick(budget) }
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = budget.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isOver || isNear) {
                                            Icon(
                                                imageVector = Icons.Rounded.WarningAmber,
                                                contentDescription = "Budget alert",
                                                tint = barColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { ratio.coerceIn(0f, 1f) },
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spent: ${CurrencyFormatter.format(spent, currency, hidden = hideAmounts)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Cap: ${CurrencyFormatter.format(budget.monthlyLimit, currency, hidden = hideAmounts)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}
