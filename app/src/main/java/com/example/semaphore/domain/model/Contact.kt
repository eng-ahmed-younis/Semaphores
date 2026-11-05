package com.example.semaphore.domain.model

data class Contact(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
)

data class ContactResponse(
    val contactId: String,
    val status: String,
    val syncedAt: Long = System.currentTimeMillis()
)

data class SyncResult(
    val successful: List<ContactResponse>,
    val failed: List<Pair<Contact, Throwable>>,
    val totalProcessed: Int
)
