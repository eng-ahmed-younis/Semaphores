package com.example.semaphore.presentation.sync

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.SyncResult
import com.example.semaphore.domain.repository.ContactRepository
import com.example.semaphore.domain.usecase.SyncContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactSyncUiState(
    val contacts: List<Contact> = emptyList(),
    val isSyncing: Boolean = false,
    val progressCompleted: Int = 0,
    val totalContacts: Int = 0,
    val maxConcurrency: Int = 4,
    val syncResult: SyncResult? = null,
    val elapsedTimeMs: Long = 0L
)

@HiltViewModel
class ContactSyncViewModel @Inject constructor(
    private val syncContactsUseCase: SyncContactsUseCase,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactSyncUiState())
    val uiState: StateFlow<ContactSyncUiState> = _uiState.asStateFlow()

    init {
        loadContacts(200)
    }

    fun loadContacts(count: Int) {
        val sampleContacts = contactRepository.getSampleContacts(count)
        _uiState.update { 
            it.copy(
                contacts = sampleContacts,
                totalContacts = sampleContacts.size,
                syncResult = null,
                progressCompleted = 0
            ) 
        }
    }

    fun setMaxConcurrency(concurrency: Int) {
        _uiState.update { it.copy(maxConcurrency = concurrency) }
    }

    fun startSync() {
        val currentState = _uiState.value
        if (currentState.isSyncing) return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isSyncing = true, 
                    progressCompleted = 0, 
                    syncResult = null 
                ) 
            }
            
            val startTime = System.currentTimeMillis()
            val result = syncContactsUseCase(
                contacts = currentState.contacts,
                maxConcurrency = currentState.maxConcurrency,
                onProgress = { completed, _ ->
                    _uiState.update { it.copy(progressCompleted = completed) }
                }
            )
            val elapsedTime = System.currentTimeMillis() - startTime

            _uiState.update { 
                it.copy(
                    isSyncing = false,
                    syncResult = result,
                    elapsedTimeMs = elapsedTime
                ) 
            }
        }
    }
}
