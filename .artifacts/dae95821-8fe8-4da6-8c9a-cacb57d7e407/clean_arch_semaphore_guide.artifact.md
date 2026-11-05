# Clean Architecture Implementation of Contact Synchronization with Kotlin Coroutines Semaphore

إليك تطبيق عملي متكامل وموسع لسيناريو مزامنة جهات الاتصال (200 جهة اتصال) مع خادم CRM الخارجي باستخدام **Clean Architecture** و **Kotlin Coroutines Semaphore** و **Hilt** و **Jetpack Compose**.

---

## 🏗️ بنية المشروع (Clean Architecture Directory Structure)

```text
com.example.semaphore/
│
├── domain/                  // الطبقة المجردة (القلب الناعم للتطبيق - لا تعتمد على Android أو Frameworks)
│   ├── model/
│   │   ├── Contact.kt
│   │   └── SyncResult.kt
│   ├── repository/
│   │   └── ContactRepository.kt
│   └── usecase/
│       └── SyncContactsUseCase.kt
│
├── data/                    // طبقة البيانات (التواصل مع الشبكة وتطبيق Semaphore للتحكم بالتزامن)
│   ├── remote/
│   │   ├── CrmApi.kt
│   │   └── CrmApiImpl.kt
│   └── repository/
│       └── ContactRepositoryImpl.kt
│
├── di/                      // حقن التبعيات (Dependency Injection عبر Hilt)
│   └── AppModule.kt
│
└── presentation/            // طبقة العرض (ViewModel + Jetpack Compose UI)
    ├── MainActivity.kt
    └── sync/
        ├── ContactSyncScreen.kt
        └── ContactSyncViewModel.kt
```

---

## 1. Domain Layer (طبقة النطاق)

### `Contact.kt` & `SyncResult.kt`
```kotlin
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
```

### `ContactRepository.kt` (عقد المستودع)
```kotlin
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
```

### `SyncContactsUseCase.kt` (حالة الاستخدام)
```kotlin
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
```

---

## 2. Data Layer (طبقة البيانات وتطبيق Semaphore)

### `CrmApi.kt` & `CrmApiImpl.kt`
```kotlin
package com.example.semaphore.data.remote

import com.example.semaphore.domain.model.Contact
import com.example.semaphore.domain.model.ContactResponse

interface CrmApi {
    suspend fun upsertContact(contact: Contact): ContactResponse
}

class CrmApiImpl : CrmApi {
    override suspend fun upsertContact(contact: Contact): ContactResponse {
        // محاكاة تأخير الشبكة
        kotlinx.coroutines.delay(150L)

        // محاكاة خطأ عشوائي بنسبة 5%
        if (kotlin.random.Random.nextDouble() < 0.05) {
            throw java.io.IOException("Server 503 Service Unavailable")
        }

        return ContactResponse(
            contactId = contact.id,
            status = "SUCCESS"
        )
    }
}
```

### `ContactRepositoryImpl.kt` (التنفيذ الفعلي مع `Semaphore`)
```kotlin
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
        // إنشاء Semaphore بالحد الأقصى للتزامن المطلوب (مثلاً 4 حسب اتفاقية SLA)
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
                phone = "+1-555-${String.format(java.util.Locale.US, "%04d", i)}"
            )
        }
    }
}
```

---

## 3. Dependency Injection (طبقة الحقن عبر Hilt)

### `AppModule.kt`
```kotlin
package com.example.semaphore.di

import com.example.semaphore.data.remote.CrmApi
import com.example.semaphore.data.remote.CrmApiImpl
import com.example.semaphore.data.repository.ContactRepositoryImpl
import com.example.semaphore.domain.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    companion object {
        @Provides
        @Singleton
        fun provideCrmApi(): CrmApi {
            return CrmApiImpl()
        }
    }
}
```

---

## 4. Presentation Layer (طبقة العرض - ViewModel & UI)

### `ContactSyncViewModel.kt`
```kotlin
package com.example.semaphore.presentation.sync

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
```

### `ContactSyncScreen.kt` (Jetpack Compose)
```kotlin
package com.example.semaphore.presentation.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSyncScreen(
    viewModel: ContactSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CRM Contact Sync (Semaphore SLA)") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Server SLA Concurrency Limit",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Max Concurrent Requests: ${uiState.maxConcurrency}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.maxConcurrency.toFloat(),
                        onValueChange = { viewModel.setMaxConcurrency(it.toInt()) },
                        valueRange = 1f..20f,
                        steps = 19,
                        enabled = !uiState.isSyncing
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Contacts: ${uiState.totalContacts}")
                        Button(
                            onClick = { viewModel.startSync() },
                            enabled = !uiState.isSyncing
                        ) {
                            Text(if (uiState.isSyncing) "Syncing..." else "Start Sync (200 Contacts)")
                        }
                    }
                }
            }

            // Progress Section
            if (uiState.isSyncing || uiState.syncResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isSyncing) {
                            LinearProgressIndicator(
                                progress = { if (uiState.totalContacts > 0) uiState.progressCompleted.toFloat() / uiState.totalContacts else 0f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("Progress: ${uiState.progressCompleted} / ${uiState.totalContacts}")
                        }

                        uiState.syncResult?.let { result ->
                            Text(
                                text = "Sync Completed in ${uiState.elapsedTimeMs} ms",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text("Successful: ${result.successful.size}")
                            Text("Failed / Retriable: ${result.failed.size}")
                        }
                    }
                }
            }

            // Contacts List Preview
            Text(
                text = "Contacts Queue (${uiState.contacts.size})",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.contacts.take(50)) { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                                Text(contact.email, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
```

---

## 💡 لماذا هذه البنية مثالية للإنتاج (Production-Ready)?
1. **فصل المسؤوليات (Separation of Concerns):** طبقة Domain مستقلة تماماً ولا تعرف شيئاً عن Hilt أو Jetpack Compose أو Retrofit.
2. **حقن التبعيات (Dependency Injection):** استخدام `@Singleton` لـ `ContactRepositoryImpl` و `CrmApiImpl` يضمن عدم إنشاء أكثر من مثيل واحد وتجنب تسريب الـ Semaphore permits عبر أجزاء التطبيق.
3. **التعامل مع الأخطاء بدون انهيار (Fault Tolerance):** استخدام `runCatching` مع `withPermit` يحافظ على استمرار مزامنة باقي جهات الاتصال حتى لو فشل طلب فردي.
