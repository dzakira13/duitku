package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.ThemePreference
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.UiState

@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    walletsState: UiState<List<Wallet>>,
    transactionsState: UiState<List<MoneyTransaction>>,
    currentUserId: String,
    googleEmail: String,
    googleDisplayName: String,
    googleAvatarUrl: String?,
    onEditProfileClick: () -> Unit,
    onUpdatePreferences: (
        currency: CurrencyOption,
        theme: ThemePreference,
        budgetCycleDay: Int,
        budgetAlertPercent: Int,
        privacyBalanceHidden: Boolean,
        budgetAlertsEnabled: Boolean,
        weeklyDigestEnabled: Boolean
    ) -> Unit,
    onBuildExportText: (UserProfile?, List<Wallet>, List<MoneyTransaction>) -> String,
    onResetFinancialData: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val wallets = (walletsState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    val displayName = profile?.displayName?.ifBlank { googleDisplayName } ?: googleDisplayName
    val bio = profile?.bio.orEmpty()
    val avatarData = profile?.avatarData?.ifBlank { googleAvatarUrl.orEmpty() }
        ?: googleAvatarUrl.orEmpty()
    val contactEmail = profile?.email?.ifBlank { googleEmail } ?: googleEmail
    val contactPhone = profile?.phone.orEmpty()
    val contactLocation = profile?.location.orEmpty()

    val currentCurrency = CurrencyOption.fromCode(profile?.currencyCode)
    val currentTheme = ThemePreference.fromRaw(profile?.themeMode)
    val cycleDay = (profile?.budgetCycleDay ?: 1).coerceIn(1, 28)
    val alertPercent = (profile?.budgetAlertPercent ?: 80).coerceIn(50, 100)
    val privacyHidden = profile?.privacyBalanceHidden ?: false
    val alertsEnabled = profile?.budgetAlertsEnabled ?: true
    val digestEnabled = profile?.weeklyDigestEnabled ?: true

    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (profileState is UiState.Loading) {
            item {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // 1. User Profile Identity Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldPrimary)))
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileAvatar(
                                avatarData = avatarData,
                                displayName = displayName,
                                size = 80.dp,
                                showEditBadge = true,
                                onClick = onEditProfileClick,
                                modifier = Modifier.testTag("profile_screen_avatar")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (bio.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = bio,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFA7F3D0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (profile?.profileSetupDone == true) "Profile Ready" else "Tap Edit to Finish Setup",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Contact Information section inside Profile Card
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.contact_info_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = onEditProfileClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.edit_profile))
                            }
                        }

                        ContactInfoRow(
                            icon = Icons.Rounded.Email,
                            label = "Email",
                            value = contactEmail.ifBlank { "Not added yet" }
                        )
                        ContactInfoRow(
                            icon = Icons.Rounded.Phone,
                            label = "Phone",
                            value = contactPhone.ifBlank { "Add phone number" }
                        )
                        ContactInfoRow(
                            icon = Icons.Rounded.LocationOn,
                            label = "Location",
                            value = contactLocation.ifBlank { "Add city or address" }
                        )
                    }
                }
            }
        }

        // 2. User Preferences Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.preferences_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Currency selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Primary Currency",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyOption.entries.forEach { option ->
                                FilterChip(
                                    selected = currentCurrency == option,
                                    onClick = {
                                        onUpdatePreferences(
                                            option,
                                            currentTheme,
                                            cycleDay,
                                            alertPercent,
                                            privacyHidden,
                                            alertsEnabled,
                                            digestEnabled
                                        )
                                    },
                                    label = { Text("${option.code} (${option.symbol})") },
                                    modifier = Modifier.testTag("currency_chip_${option.code}")
                                )
                            }
                        }
                    }

                    // Theme selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemePreference.entries.forEachIndexed { idx, pref ->
                                SegmentedButton(
                                    selected = currentTheme == pref,
                                    onClick = {
                                        onUpdatePreferences(
                                            currentCurrency,
                                            pref,
                                            cycleDay,
                                            alertPercent,
                                            privacyHidden,
                                            alertsEnabled,
                                            digestEnabled
                                        )
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = idx,
                                        count = ThemePreference.entries.size
                                    ),
                                    modifier = Modifier.testTag("theme_segment_${pref.name}")
                                ) {
                                    Text(
                                        text = when (pref) {
                                            ThemePreference.SYSTEM -> "System"
                                            ThemePreference.LIGHT -> "Light"
                                            ThemePreference.DARK -> "Dark"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Budget cycle start day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Budget Cycle Start Day",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Resets monthly on day $cycleDay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val next = (cycleDay - 1).coerceAtLeast(1)
                                    onUpdatePreferences(
                                        currentCurrency, currentTheme, next, alertPercent,
                                        privacyHidden, alertsEnabled, digestEnabled
                                    )
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) { Text("-") }
                            Text(
                                text = "$cycleDay",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    val next = (cycleDay + 1).coerceAtMost(28)
                                    onUpdatePreferences(
                                        currentCurrency, currentTheme, next, alertPercent,
                                        privacyHidden, alertsEnabled, digestEnabled
                                    )
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) { Text("+") }
                        }
                    }

                    // Budget Alert Threshold
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Budget Warning Threshold",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(70, 80, 90, 100).forEach { pct ->
                                FilterChip(
                                    selected = alertPercent == pct,
                                    onClick = {
                                        onUpdatePreferences(
                                            currentCurrency, currentTheme, cycleDay, pct,
                                            privacyHidden, alertsEnabled, digestEnabled
                                        )
                                    },
                                    label = { Text("$pct%") }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    PreferenceSwitchRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "Privacy Mode",
                        subtitle = "Mask wallet balances and amounts on launch",
                        checked = privacyHidden,
                        testTag = "pref_privacy_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences(
                                currentCurrency, currentTheme, cycleDay, alertPercent,
                                checked, alertsEnabled, digestEnabled
                            )
                        }
                    )

                    PreferenceSwitchRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Budget Threshold Alerts",
                        subtitle = "Highlight categories reaching $alertPercent% of limit",
                        checked = alertsEnabled,
                        testTag = "pref_budget_alerts_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences(
                                currentCurrency, currentTheme, cycleDay, alertPercent,
                                privacyHidden, checked, digestEnabled
                            )
                        }
                    )

                    PreferenceSwitchRow(
                        icon = Icons.Rounded.Email,
                        title = "Weekly Spending Digest",
                        subtitle = "Include weekly cashflow recap in preferences",
                        checked = digestEnabled,
                        testTag = "pref_digest_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences(
                                currentCurrency, currentTheme, cycleDay, alertPercent,
                                privacyHidden, alertsEnabled, checked
                            )
                        }
                    )
                }
            }
        }

        // 3. Account Settings Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_settings_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = IncomeGreen
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Account Connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = googleEmail.ifBlank { "UID: ${currentUserId.take(10)}…" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    AccountActionRow(
                        icon = Icons.Rounded.Share,
                        title = stringResource(R.string.export_summary),
                        subtitle = "Share your wallets and recent activity",
                        tint = MaterialTheme.colorScheme.primary,
                        testTag = "export_summary_button",
                        onClick = {
                            val summary = onBuildExportText(profile, wallets, transactions)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "DuitKu Financial Summary")
                                putExtra(Intent.EXTRA_TEXT, summary)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export DuitKu Summary"))
                        }
                    )

                    AccountActionRow(
                        icon = Icons.Rounded.RestartAlt,
                        title = stringResource(R.string.reset_financial_data),
                        subtitle = "Clear wallets, transactions, and budgets while keeping profile",
                        tint = MaterialTheme.colorScheme.secondary,
                        testTag = "reset_data_button",
                        onClick = { showResetDialog = true }
                    )

                    AccountActionRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        title = stringResource(R.string.sign_out),
                        subtitle = "Sign out of DuitKu on this device",
                        tint = MaterialTheme.colorScheme.onSurface,
                        testTag = "sign_out_button",
                        onClick = onSignOutClick
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    AccountActionRow(
                        icon = Icons.Rounded.DeleteForever,
                        title = stringResource(R.string.delete_account_data),
                        subtitle = "Permanently remove profile, preferences, and financial records",
                        tint = MaterialTheme.colorScheme.error,
                        testTag = "delete_account_data_button",
                        onClick = { showDeleteAccountDialog = true }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Financial Data?") },
            text = {
                Text("This permanently deletes all your wallets, transactions, and category budgets. Your profile and preferences will stay intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetFinancialData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Profile & All Data?") },
            text = {
                Text("This permanently removes your DuitKu profile, contact info, preferences, wallets, transactions, and budgets, then signs you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AccountActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
