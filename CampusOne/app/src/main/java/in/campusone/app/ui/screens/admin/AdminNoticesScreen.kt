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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Notices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ArrowBack, contentDescription = "Back")
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
                showDialog = true
            }) {
                Icon(Add, contentDescription = "Add Notice")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.notices, key = { it.id }) { notice ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (notice.isPinned) {
                                Icon(PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = notice.body,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${notice.authorName} (${notice.authorRole.name})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notice.createdAtMillis)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                editingNotice = notice
                                title = notice.title
                                body = notice.body
                                isPinned = notice.isPinned
                                showDialog = true
                            }) {
                                Icon(Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { }) {
                                Icon(Delete, contentDescription = "Delete")
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
            onTitleChange = { title = it },
            onBodyChange = { body = it },
            onPinnedChange = { isPinned = it },
            onDismiss = { showDialog = false },
            onSave = {
                val notice = NoticeEntity(
                    id = editingNotice?.id ?: "notice_${System.currentTimeMillis()}",
                    sectionKey = uiState.sectionKey,
                    title = title,
                    body = body,
                    isPinned = isPinned,
                    authorUid = uiState.profile.uid,
                    authorName = uiState.profile.name,
                    authorRole = uiState.profile.role,
                    createdAtMillis = editingNotice?.createdAtMillis ?: System.currentTimeMillis(),
                    isRead = false
                )
                viewModel.saveNotice(notice)
                showDialog = false
            }
        )
    }
}

@Composable
private fun NoticeDialog(
    title: String,
    body: String,
    isPinned: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (title.isNotBlank()) "Edit Notice" else "Add Notice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Body / Details") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPinned, onCheckedChange = onPinnedChange)
                    Text("Pin to top")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = title.isNotBlank() && body.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
