package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AvatarPresets
import com.example.data.model.CategoryBudget
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.TransactionCategories
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WalletColor
import com.example.data.model.WalletType
import com.example.util.ImageEncodingUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileSheet(
    profile: UserProfile?,
    googlePhotoUrl: String?,
    googleDisplayName: String,
    googleEmail: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        bio: String,
        avatarData: String,
        email: String,
        phone: String,
        location: String
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var displayName by remember(profile) {
        mutableStateOf(profile?.displayName?.ifBlank { googleDisplayName } ?: googleDisplayName)
    }
    var bio by remember(profile) { mutableStateOf(profile?.bio.orEmpty()) }
    var avatarData by remember(profile) {
        mutableStateOf(
            profile?.avatarData?.ifBlank { googlePhotoUrl ?: AvatarPresets.EMERALD }
                ?: (googlePhotoUrl ?: AvatarPresets.EMERALD)
        )
    }
    var email by remember(profile) {
        mutableStateOf(profile?.email?.ifBlank { googleEmail } ?: googleEmail)
    }
    var phone by remember(profile) { mutableStateOf(profile?.phone.orEmpty()) }
    var location by remember(profile) { mutableStateOf(profile?.location.orEmpty()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                ImageEncodingUtil.encodePickedImageToDataUri(context, uri).fold(
                    onSuccess = { encoded ->
                        avatarData = encoded
                        errorText = null
                    },
                    onFailure = { err ->
                        errorText = err.localizedMessage ?: "Could not load selected image."
                    }
                )
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Edit Profile & Contact",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Avatar Preview + Photo Picker & Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileAvatar(
                    avatarData = avatarData,
                    displayName = displayName.ifBlank { "D" },
                    size = 76.dp,
                    showEditBadge = true,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("choose_profile_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = "Choose photo from gallery",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Photo")
                    }
                    if (!googlePhotoUrl.isNullOrBlank()) {
                        TextButton(
                            onClick = { avatarData = googlePhotoUrl },
                            modifier = Modifier.testTag("use_google_photo_button")
                        ) {
                            Text("Use Google Photo")
                        }
                    }
                }
            }

            Text(
                text = "Or pick an avatar badge",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarPresets.all.forEach { preset ->
                    val selected = avatarData == preset
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { avatarData = preset }
                            .testTag("avatar_preset_$preset")
                    ) {
                        PresetAvatarGraphic(presetKey = preset, size = 48.dp)
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 60) displayName = it },
                label = { Text("Full Name *") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Bio / Financial Goal") },
                placeholder = { Text("e.g. Saving for emergency fund & travel") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_bio_input")
            )

            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = { Text("Contact Email") },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_email_input")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 30) phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+62 812 3456 7890") },
                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_phone_input")
            )

            OutlinedTextField(
                value = location,
                onValueChange = { if (it.length <= 100) location = it },
                label = { Text("City / Address") },
                placeholder = { Text("Jakarta, Indonesia") },
                leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_location_input")
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val trimmedName = displayName.trim()
                        val trimmedEmail = email.trim()
                        when {
                            trimmedName.isEmpty() -> errorText = "Please enter your name."
                            trimmedEmail.isNotEmpty() && !trimmedEmail.contains("@") ->
                                errorText = "Please enter a valid email address."
                            else -> {
                                errorText = null
                                onSave(
                                    trimmedName,
                                    bio.trim(),
                                    avatarData,
                                    trimmedEmail,
                                    phone.trim(),
                                    location.trim()
                                )
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_profile_button")
                ) {
                    Text(if (isBusy) "Saving…" else "Save Profile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WalletEditorDialog(
    existingWallet: Wallet?,
    currency: CurrencyOption,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, type: WalletType, balance: Double, color: WalletColor) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(existingWallet) { mutableStateOf(existingWallet?.name.orEmpty()) }
    var selectedType by remember(existingWallet) {
        mutableStateOf(WalletType.fromRaw(existingWallet?.walletType))
    }
    var balanceText by remember(existingWallet) {
        mutableStateOf(
            if (existingWallet != null) {
                if (existingWallet.balance % 1.0 == 0.0) existingWallet.balance.toLong().toString()
                else existingWallet.balance.toString()
            } else ""
        )
    }
    var selectedColor by remember(existingWallet) {
        mutableStateOf(WalletColor.fromRaw(existingWallet?.colorKey))
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (existingWallet == null) "New Wallet" else "Edit Wallet")
                if (existingWallet != null && onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_wallet_button")) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete wallet",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text("Wallet Name *") },
                    placeholder = { Text("e.g. Cash, Bank BCA, GoPay") },
                    leadingIcon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_name_input")
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' || it == '-' }) {
                            balanceText = input
                        }
                    },
                    label = { Text("Balance (${currency.symbol})") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_balance_input")
                )

                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WalletType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                Text("Card Accent", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WalletColor.entries.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(walletGradientBrush(color))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = color.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val parsedBalance = balanceText.trim().ifEmpty { "0" }.toDoubleOrNull()
                    when {
                        cleanName.isEmpty() -> errorMsg = "Enter a wallet name."
                        parsedBalance == null -> errorMsg = "Enter a valid numeric balance."
                        else -> onSave(cleanName, selectedType, parsedBalance, selectedColor)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.testTag("save_wallet_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
    existingTransaction: MoneyTransaction?,
    initialType: TransactionType,
    wallets: List<Wallet>,
    currency: CurrencyOption,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        wallet: Wallet,
        note: String,
        occurredAtMillis: Long
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txType by remember(existingTransaction, initialType) {
        mutableStateOf(
            if (existingTransaction != null) TransactionType.fromRaw(existingTransaction.transactionType)
            else initialType
        )
    }
    var title by remember(existingTransaction) { mutableStateOf(existingTransaction?.title.orEmpty()) }
    var amountText by remember(existingTransaction) {
        mutableStateOf(
            if (existingTransaction != null) {
                if (existingTransaction.amount % 1.0 == 0.0) existingTransaction.amount.toLong().toString()
                else existingTransaction.amount.toString()
            } else ""
        )
    }
    val categories = if (txType == TransactionType.EXPENSE) {
        TransactionCategories.expenseCategories
    } else {
        TransactionCategories.incomeCategories
    }
    var selectedCategory by remember(existingTransaction, txType) {
        mutableStateOf(
            existingTransaction?.category?.takeIf { it in categories } ?: categories.first()
        )
    }
    var selectedWallet by remember(existingTransaction, wallets) {
        mutableStateOf(
            wallets.firstOrNull { it.id == existingTransaction?.walletId } ?: wallets.firstOrNull()
        )
    }
    var note by remember(existingTransaction) { mutableStateOf(existingTransaction?.note.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTransaction == null) "Record Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (existingTransaction != null && onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_transaction_button")) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = txType == type,
                        onClick = {
                            txType = type
                            val nextCats = if (type == TransactionType.EXPENSE) {
                                TransactionCategories.expenseCategories
                            } else {
                                TransactionCategories.incomeCategories
                            }
                            if (selectedCategory !in nextCats) {
                                selectedCategory = nextCats.first()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(type.label)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text("Title / Merchant *") },
                placeholder = { Text(if (txType == TransactionType.EXPENSE) "e.g. Lunch, Grab Ride" else "e.g. Monthly Salary") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        amountText = input
                    }
                },
                label = { Text("Amount (${currency.symbol}) *") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Text("Wallet", style = MaterialTheme.typography.labelMedium)
            if (wallets.isEmpty()) {
                Text(
                    text = "Create a wallet first to assign transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.forEach { wallet ->
                        FilterChip(
                            selected = selectedWallet?.id == wallet.id,
                            onClick = { selectedWallet = wallet },
                            label = { Text(wallet.name) }
                        )
                    }
                }
            }

            Text("Category", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                label = { Text("Note (optional)") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val cleanTitle = title.trim()
                        val amount = amountText.trim().toDoubleOrNull()
                        val wallet = selectedWallet
                        when {
                            cleanTitle.isEmpty() -> errorMsg = "Enter a transaction title."
                            amount == null || amount <= 0.0 -> errorMsg = "Enter an amount greater than zero."
                            wallet == null -> errorMsg = "Please select a wallet."
                            else -> {
                                errorMsg = null
                                onSave(
                                    cleanTitle,
                                    amount,
                                    txType,
                                    selectedCategory,
                                    wallet,
                                    note.trim(),
                                    existingTransaction?.occurredAtMillis ?: System.currentTimeMillis()
                                )
                            }
                        }
                    },
                    enabled = !isBusy && wallets.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_transaction_button")
                ) {
                    Text(if (isBusy) "Saving…" else "Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetEditorDialog(
    existingBudget: CategoryBudget?,
    currency: CurrencyOption,
    monthKey: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var selectedCategory by remember(existingBudget) {
        mutableStateOf(
            existingBudget?.category?.ifBlank { TransactionCategories.expenseCategories.first() }
                ?: TransactionCategories.expenseCategories.first()
        )
    }
    var limitText by remember(existingBudget) {
        mutableStateOf(
            if (existingBudget != null) {
                if (existingBudget.monthlyLimit % 1.0 == 0.0) existingBudget.monthlyLimit.toLong().toString()
                else existingBudget.monthlyLimit.toString()
            } else ""
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (existingBudget == null) "Set Category Budget" else "Edit Budget")
                if (existingBudget != null && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Month: $monthKey",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionCategories.expenseCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) limitText = input
                    },
                    label = { Text("Monthly Limit (${currency.symbol}) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.trim().toDoubleOrNull()
                    if (limit == null || limit <= 0.0) {
                        errorMsg = "Enter a monthly limit greater than zero."
                    } else {
                        errorMsg = null
                        onSave(selectedCategory, limit)
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
