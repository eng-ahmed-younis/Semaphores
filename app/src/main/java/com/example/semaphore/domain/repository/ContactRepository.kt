package com.example.semaphore.domain.repository

import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.SyncResult

interface ContactRepository {
    suspend fun syncContacts(
        contacts: List<Contact>,
        maxConcurrency: Int = 4,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): SyncResult
    
    fun getSampleContacts(count: Int = 200): List<Contact>
}
