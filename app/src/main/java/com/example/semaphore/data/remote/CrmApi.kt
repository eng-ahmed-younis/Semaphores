package com.example.semaphore.data.remote

import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.ContactResponse

interface CrmApi {
    suspend fun upsertContact(contact: Contact): ContactResponse
}
