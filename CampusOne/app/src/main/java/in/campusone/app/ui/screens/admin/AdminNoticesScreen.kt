package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.theme.C1Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNoticesScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingNotice by rememberSaveable { mutableStateOf<NoticeEntity?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("GENERAL") }
    var priority by rememberSaveable { mutableStateOf("NORMAL") }
    var isOfficial by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Notices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = {
                editingNotice = null
                title = ""
                body = ""
                isPinned = false
                category = "GENERAL"
                priority = "NORMAL"
                isOfficial = false
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Notice")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = C1Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            items(uiState.notices, key = { it.id }) { notice ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (notice.isPinned) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.xs))
                        Text(
                            text = notice.body,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(C1Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                            Text(
                                text = "${notice.authorName} (${notice.authorRole.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notice.createdAtMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (notice.isOfficial) {
                                androidx.compose.material3.Chip(
                                    onClick = { },
                                    label = { Text("Official", style = MaterialTheme.typography.labelSmall) },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                            IconButton(onClick = {
                                editingNotice = notice
                                title = notice.title
                                body = notice.body
                                isPinned = notice.isPinned
                                category = notice.category
                                priority = notice.priority
                                isOfficial = notice.isOfficial
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                viewModel.deleteNoticeRemote(notice.id)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete from cloud")
                            }
                            IconButton(onClick = {
                                viewModel.saveNoticeRemote(notice)
                            }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Sync to cloud")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        NoticeDialog(
            title = title,
            body = body,
            isPinned = isPinned,
            category = category,
            priority = priority,
            isOfficial = isOfficial,
            onTitleChange = { title = it },
            onBodyChange = { body = it },
            onPinnedChange = { isPinned = it },
            onCategoryChange = { category = it },
            onPriorityChange = { priority = it },
            onOfficialChange = { isOfficial = it },
            onDismiss = { showDialog = false },
            onSave = {
                val notice = NoticeEntity(
                    id = editingNotice?.id ?: "notice_${System.currentTimeMillis()}",
                    sectionKey = uiState.sectionKey,
                    title = title,
                    body = body,
                    isPinned = isPinned,
                    category = category,
                    priority = priority,
                    isOfficial = isOfficial,
                    authorUid = uiState.profile.uid,
                    authorName = uiState.profile.name,
                    authorRole = uiState.profile.role,
                    createdAtMillis = editingNotice?.createdAtMillis ?: System.currentTimeMillis(),
                    isRead = false
                )
                viewModel.saveNotice(notice)
                if (isOfficial || priority == "HIGH" || priority == "URGENT") {
                    viewModel.saveNoticeRemote(notice)
                }
                showDialog = false
            }
        )
    }
}
