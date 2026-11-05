package com.example.semaphore.domain.usecase

import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.SyncResult
import com.example.semaphore.domain.repository.ContactRepository
import javax.inject.Inject

class SyncContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(
        contacts: List<Contact>,
        maxConcurrency: Int = 4,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): SyncResult {
        return contactRepository.syncContacts(contacts, maxConcurrency, onProgress)
    }
}
