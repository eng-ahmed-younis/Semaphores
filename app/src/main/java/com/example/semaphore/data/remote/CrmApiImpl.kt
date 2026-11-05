package com.example.semaphore.data.remote

import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.ContactResponse
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class CrmApiImpl : CrmApi {
    private val activeRequests = AtomicInteger(0)

    var peakConcurrentRequests: Int = 0
        private set

    override suspend fun upsertContact(contact: Contact): ContactResponse {
        val current = activeRequests.incrementAndGet()
        synchronized(this) {
            if (current > peakConcurrentRequests) {
                peakConcurrentRequests = current
            }
        }

        try {
            // Simulate network latency (100-300ms)
            delay(Random.nextLong(100, 300))

            // Simulate occasional transient network error (5%)
            if (Random.nextDouble() < 0.05) {
                throw IOException("Server temporary failure (503 Service Unavailable)")
            }

            return ContactResponse(
                contactId = contact.id,
                status = "SUCCESS"
            )
        } finally {
            activeRequests.decrementAndGet()
        }
    }
}
