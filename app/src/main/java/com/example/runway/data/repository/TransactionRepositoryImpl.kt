package com.example.runway.data.repository

import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toTimestamp
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TransactionRepository {

    override suspend fun addAll(transactions: List<Transaction>): AppResult<Unit> = appResultOf {
        val uid = auth.currentUser?.uid ?: return@appResultOf
        val collection = firestore.collection("users").document(uid).collection("transactions")
        val batch = firestore.batch()
        transactions.forEach { tx ->
            val data = mapOf(
                "type" to tx.type.name,
                "amountMinor" to tx.amountMinor,
                "currency" to tx.currency.name,
                "fxRate" to tx.fxRate,
                "amountLkrMinor" to tx.amountLkrMinor,
                "categoryId" to tx.categoryId,
                "accountId" to tx.accountId,
                "toAccountId" to tx.toAccountId,
                "incomeSourceId" to tx.incomeSourceId,
                "relatedTxId" to tx.relatedTxId,
                "note" to tx.note,
                "date" to tx.date.toTimestamp(),
                "monthKey" to tx.monthKey,
                "archived" to tx.archived,
                "createdAt" to tx.createdAt.toTimestamp(),
                "updatedAt" to tx.updatedAt.toTimestamp(),
            )
            batch.set(collection.document(tx.id), data)
        }
        batch.commit().logWriteFailure("transactions batch (${transactions.size})")
    }
}
