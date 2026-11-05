package com.example.semaphore.data.repository

import com.example.semaphore.data.remote.CrmApi
import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.SyncResult
import com.example.semaphore.domain.repository.ContactRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val api: CrmApi
) : ContactRepository {

    override suspend fun syncContacts(
        contacts: List<Contact>,
        maxConcurrency: Int,
        onProgress: suspend (completed: Int, total: Int) -> Unit
    ): SyncResult = coroutineScope {

        val semaphore = Semaphore(permits = maxConcurrency.coerceAtLeast(1))
        val completedCount = AtomicInteger(0)
        val total = contacts.size

        val results = contacts.map { contact ->
            async {
                val result = runCatching {
                    semaphore.withPermit {
                        api.upsertContact(contact)
                    }
                }
                val currentCompleted = completedCount.incrementAndGet()
                onProgress(currentCompleted, total)
                result
            }
        }.awaitAll()

        SyncResult(
            successful = results.mapNotNull { it.getOrNull() },
            failed = results.mapIndexedNotNull { index, result ->
                result.exceptionOrNull()?.let { contacts[index] to it }
            },
            totalProcessed = total
        )
    }

    override fun getSampleContacts(count: Int): List<Contact> {
        return (1..count).map { i ->
            Contact(
                id = "contact_$i",
                name = "Contact Name $i",
                email = "contact$i@example.com",
                phone = "+1-555-${String.format("%04d", i)}"
            )
        }
    }
}
