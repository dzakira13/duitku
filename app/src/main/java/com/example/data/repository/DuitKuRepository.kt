package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AvatarPresets
import com.example.data.model.CategoryBudget
import com.example.data.model.CurrencyOption
import com.example.data.model.MoneyTransaction
import com.example.data.model.ThemePreference
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WalletColor
import com.example.data.model.WalletType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DuitKuRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun walletsCol(uid: String) = userDoc(uid).collection("wallets")
    private fun transactionsCol(uid: String) = userDoc(uid).collection("transactions")
    private fun budgetsCol(uid: String) = userDoc(uid).collection("budgets")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDoc(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeWallets(userId: String): Flow<List<Wallet>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/wallets"
        emitAll(
            walletsCol(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Wallet::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.balance }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<MoneyTransaction>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/transactions"
        emitAll(
            transactionsCol(uid)
                .orderBy("occurredAtMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MoneyTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBudgets(userId: String): Flow<List<CategoryBudget>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/budgets"
        emitAll(
            budgetsCol(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CategoryBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureInitialUserProfile(
        userId: String,
        defaultName: String,
        defaultEmail: String,
        defaultAvatarUrl: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = defaultName.trim().ifBlank { "DuitKu User" }.take(60)
                val safeEmail = defaultEmail.trim().take(120)
                val safeAvatar = defaultAvatarUrl?.trim()?.take(80000)
                    ?.takeIf { it.isNotEmpty() } ?: AvatarPresets.EMERALD

                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "bio" to "",
                    "avatarData" to safeAvatar,
                    "email" to safeEmail,
                    "phone" to "",
                    "location" to "",
                    "currencyCode" to CurrencyOption.IDR.code,
                    "themeMode" to ThemePreference.SYSTEM.name,
                    "budgetCycleDay" to 1,
                    "budgetAlertPercent" to 80,
                    "privacyBalanceHidden" to false,
                    "budgetAlertsEnabled" to true,
                    "weeklyDigestEnabled" to true,
                    "profileSetupDone" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(
        userId: String,
        displayName: String,
        bio: String,
        avatarData: String,
        email: String,
        phone: String,
        location: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanName = displayName.trim()
            require(cleanName.length in 1..60) { "Display name must be 1 to 60 characters." }
            val cleanEmail = email.trim()
            require(cleanEmail.length <= 120) { "Email must be 120 characters or fewer." }
            val cleanBio = bio.trim().take(160)
            val cleanPhone = phone.trim().take(30)
            val cleanLocation = location.trim().take(100)
            val cleanAvatar = avatarData.trim().ifBlank { AvatarPresets.EMERALD }
            require(cleanAvatar.length <= 80000) { "Profile picture data exceeds allowed size." }

            val docRef = userDoc(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "bio" to cleanBio,
                        "avatarData" to cleanAvatar,
                        "email" to cleanEmail,
                        "phone" to cleanPhone,
                        "location" to cleanLocation,
                        "profileSetupDone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "bio" to cleanBio,
                        "avatarData" to cleanAvatar,
                        "email" to cleanEmail,
                        "phone" to cleanPhone,
                        "location" to cleanLocation,
                        "currencyCode" to CurrencyOption.IDR.code,
                        "themeMode" to ThemePreference.SYSTEM.name,
                        "budgetCycleDay" to 1,
                        "budgetAlertPercent" to 80,
                        "privacyBalanceHidden" to false,
                        "budgetAlertsEnabled" to true,
                        "weeklyDigestEnabled" to true,
                        "profileSetupDone" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        currencyCode: CurrencyOption,
        themeMode: ThemePreference,
        budgetCycleDay: Int,
        budgetAlertPercent: Int,
        privacyBalanceHidden: Boolean,
        budgetAlertsEnabled: Boolean,
        weeklyDigestEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeCycleDay = budgetCycleDay.coerceIn(1, 28)
            val safeAlertPct = budgetAlertPercent.coerceIn(50, 100)
            userDoc(uid).update(
                mapOf(
                    "currencyCode" to currencyCode.code,
                    "themeMode" to themeMode.name,
                    "budgetCycleDay" to safeCycleDay,
                    "budgetAlertPercent" to safeAlertPct,
                    "privacyBalanceHidden" to privacyBalanceHidden,
                    "budgetAlertsEnabled" to budgetAlertsEnabled,
                    "weeklyDigestEnabled" to weeklyDigestEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertWallet(
        userId: String,
        walletId: String?,
        name: String,
        walletType: WalletType,
        balance: Double,
        colorKey: WalletColor
    ): Result<Unit> {
        var path = "users/$userId/wallets"
        return try {
            val uid = requireUserId(userId)
            val cleanName = name.trim()
            require(cleanName.length in 1..40) { "Wallet name must be 1 to 40 characters." }
            require(balance in -999999999999.0..999999999999.0) { "Balance is out of supported range." }

            if (walletId.isNullOrBlank()) {
                val docRef = walletsCol(uid).document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "walletType" to walletType.name,
                        "balance" to balance,
                        "colorKey" to colorKey.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = walletsCol(uid).document(walletId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "walletType" to walletType.name,
                        "balance" to balance,
                        "colorKey" to colorKey.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWallet(userId: String, walletId: String): Result<Unit> {
        val path = "users/$userId/wallets/$walletId"
        return try {
            val uid = requireUserId(userId)
            require(walletId.isNotBlank()) { "Wallet ID must not be blank." }
            walletsCol(uid).document(walletId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(
        userId: String,
        title: String,
        amount: Double,
        transactionType: TransactionType,
        category: String,
        wallet: Wallet,
        note: String,
        occurredAtMillis: Long
    ): Result<Unit> {
        var path = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            require(cleanTitle.length in 1..80) { "Title must be 1 to 80 characters." }
            require(amount > 0.0 && amount <= 999999999999.0) { "Amount must be greater than zero." }
            require(wallet.id.isNotBlank()) { "Please select a valid wallet." }
            val cleanCategory = category.trim().ifBlank { "Other" }.take(40)
            val cleanWalletName = wallet.name.trim().ifBlank { "Wallet" }.take(40)
            val cleanNote = note.trim().take(200)
            val safeOccurredAt = occurredAtMillis.coerceAtLeast(1L)

            val txRef = transactionsCol(uid).document()
            path = txRef.path
            val walletRef = walletsCol(uid).document(wallet.id)

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "transactionType" to transactionType.name,
                "category" to cleanCategory,
                "walletId" to wallet.id,
                "walletName" to cleanWalletName,
                "occurredAtMillis" to safeOccurredAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote.isNotEmpty()) {
                payload["note"] = cleanNote
            }

            val delta = if (transactionType == TransactionType.INCOME) amount else -amount

            val batch = db.batch()
            batch.set(txRef, payload)
            batch.update(
                walletRef,
                mapOf(
                    "balance" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        userId: String,
        oldTransaction: MoneyTransaction,
        title: String,
        amount: Double,
        transactionType: TransactionType,
        category: String,
        newWallet: Wallet,
        note: String,
        occurredAtMillis: Long
    ): Result<Unit> {
        val path = "users/$userId/transactions/${oldTransaction.id}"
        return try {
            val uid = requireUserId(userId)
            require(oldTransaction.id.isNotBlank()) { "Transaction ID must not be blank." }
            val cleanTitle = title.trim()
            require(cleanTitle.length in 1..80) { "Title must be 1 to 80 characters." }
            require(amount > 0.0 && amount <= 999999999999.0) { "Amount must be greater than zero." }
            require(newWallet.id.isNotBlank()) { "Please select a valid wallet." }

            val cleanCategory = category.trim().ifBlank { "Other" }.take(40)
            val cleanWalletName = newWallet.name.trim().ifBlank { "Wallet" }.take(40)
            val cleanNote = note.trim().take(200)
            val safeOccurredAt = occurredAtMillis.coerceAtLeast(1L)

            val txRef = transactionsCol(uid).document(oldTransaction.id)
            val batch = db.batch()

            val updatePayload = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "amount" to amount,
                "transactionType" to transactionType.name,
                "category" to cleanCategory,
                "walletId" to newWallet.id,
                "walletName" to cleanWalletName,
                "note" to cleanNote,
                "occurredAtMillis" to safeOccurredAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.update(txRef, updatePayload)

            val oldSigned = if (TransactionType.fromRaw(oldTransaction.transactionType) == TransactionType.INCOME) {
                oldTransaction.amount
            } else {
                -oldTransaction.amount
            }
            val newSigned = if (transactionType == TransactionType.INCOME) amount else -amount

            if (oldTransaction.walletId == newWallet.id) {
                val netDelta = newSigned - oldSigned
                if (netDelta != 0.0) {
                    batch.update(
                        walletsCol(uid).document(newWallet.id),
                        mapOf(
                            "balance" to FieldValue.increment(netDelta),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                if (oldTransaction.walletId.isNotBlank()) {
                    val oldWalletSnap = walletsCol(uid).document(oldTransaction.walletId).get().await()
                    if (oldWalletSnap.exists()) {
                        batch.update(
                            walletsCol(uid).document(oldTransaction.walletId),
                            mapOf(
                                "balance" to FieldValue.increment(-oldSigned),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                batch.update(
                    walletsCol(uid).document(newWallet.id),
                    mapOf(
                        "balance" to FieldValue.increment(newSigned),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transaction: MoneyTransaction): Result<Unit> {
        val path = "users/$userId/transactions/${transaction.id}"
        return try {
            val uid = requireUserId(userId)
            require(transaction.id.isNotBlank()) { "Transaction ID must not be blank." }
            val txRef = transactionsCol(uid).document(transaction.id)
            val batch = db.batch()
            batch.delete(txRef)

            if (transaction.walletId.isNotBlank()) {
                val walletRef = walletsCol(uid).document(transaction.walletId)
                val walletSnap = walletRef.get().await()
                if (walletSnap.exists()) {
                    val revertDelta = if (TransactionType.fromRaw(transaction.transactionType) == TransactionType.INCOME) {
                        -transaction.amount
                    } else {
                        transaction.amount
                    }
                    batch.update(
                        walletRef,
                        mapOf(
                            "balance" to FieldValue.increment(revertDelta),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertBudget(
        userId: String,
        budgetId: String?,
        category: String,
        monthlyLimit: Double,
        monthKey: String
    ): Result<Unit> {
        var path = "users/$userId/budgets"
        return try {
            val uid = requireUserId(userId)
            val cleanCategory = category.trim()
            require(cleanCategory.length in 1..40) { "Category must be 1 to 40 characters." }
            require(monthlyLimit > 0.0 && monthlyLimit <= 999999999999.0) { "Budget limit must be positive." }
            require(monthKey.length == 7) { "Month key must be YYYY-MM." }

            if (budgetId.isNullOrBlank()) {
                val docRef = budgetsCol(uid).document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "category" to cleanCategory,
                        "monthlyLimit" to monthlyLimit,
                        "monthKey" to monthKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = budgetsCol(uid).document(budgetId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "category" to cleanCategory,
                        "monthlyLimit" to monthlyLimit,
                        "monthKey" to monthKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val path = "users/$userId/budgets/$budgetId"
        return try {
            val uid = requireUserId(userId)
            require(budgetId.isNotBlank()) { "Budget ID must not be blank." }
            budgetsCol(uid).document(budgetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearFinancialData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            deleteAllInCollection(transactionsCol(uid))
            deleteAllInCollection(walletsCol(uid))
            deleteAllInCollection(budgetsCol(uid))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            deleteAllInCollection(transactionsCol(uid))
            deleteAllInCollection(walletsCol(uid))
            deleteAllInCollection(budgetsCol(uid))
            userDoc(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteAllInCollection(collection: com.google.firebase.firestore.CollectionReference) {
        val snapshot = collection.get().await()
        if (snapshot.isEmpty) return
        val docs = snapshot.documents
        var index = 0
        while (index < docs.size) {
            val chunk = docs.subList(index, minOf(index + 400, docs.size))
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
            index += 400
        }
    }
}
