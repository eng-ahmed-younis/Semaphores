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
