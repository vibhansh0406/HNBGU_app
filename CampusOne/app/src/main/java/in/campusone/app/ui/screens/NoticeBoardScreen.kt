package `in`.campusone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.CoreUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoticeBoardScreen(
    uiState: CoreUiState,
    onMarkNoticeRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onPostNotice: (
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean
    ) -> Unit
) {
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }

    val canPost = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER
    val displayedNotices = if (unreadOnly) {
        uiState.notices.filter { !it.isRead }
    } else {
        uiState.notices
    }

    Scaffold(
        floatingActionButton = {
            if (canPost) {
                FloatingActionButton(onClick = { showComposer = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.post_notice))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = unreadOnly,
                        onClick = { unreadOnly = !unreadOnly },
                        label = {
                            Text("${stringResource(R.string.notices_unread_only)} (${uiState.unreadNoticeCount})")
                        }
                    )
                    if (uiState.unreadNoticeCount > 0) {
                        TextButton(onClick = onMarkAllRead) {
                            Icon(Icons.Default.DoneAll, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark all read")
                        }
                    }
                }
            }

            if (displayedNotices.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.empty_notices),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(displayedNotices, key = { it.id }) { notice ->
                    NoticeCard(
                        notice = notice,
                        onMarkRead = { onMarkNoticeRead(notice.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    if (showComposer) {
        PostNoticeDialog(
            onDismiss = { showComposer = false },
            onSubmit = { title, body, url, type, pinned ->
                onPostNotice(title, body, url, type, pinned)
                showComposer = false
            }
        )
    }
}

@Composable
private fun NoticeCard(
    notice: NoticeItem,
    onMarkRead: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    ElevatedCard(
        onClick = { if (!notice.isRead) onMarkRead() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notice.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "${notice.authorName} (${notice.authorRole.name})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!notice.isRead) {
                    AssistChip(
                        onClick = onMarkRead,
                        label = { Text("NEW") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (notice.isRead) FontWeight.SemiBold else FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notice.body,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!notice.attachmentUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AssistChip(
                    onClick = onMarkRead,
                    leadingIcon = { Icon(Icons.Default.Attachment, contentDescription = null) },
                    label = { Text("${notice.attachmentType.name}: ${notice.attachmentUrl}") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(notice.createdAtMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PostNoticeDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var attachmentUrl by rememberSaveable { mutableStateOf("") }
    var isPinned by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.post_notice)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Announcement Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Details / Instructions") },
                    minLines = 3
                )
                OutlinedTextField(
                    value = attachmentUrl,
                    onValueChange = { attachmentUrl = it },
                    label = { Text("Optional PDF / Drive / Image URL") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                    Text("Pin announcement to top")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && body.isNotBlank(),
                onClick = {
                    val attType = when {
                        attachmentUrl.isBlank() -> AttachmentType.NONE
                        attachmentUrl.endsWith(".pdf", ignoreCase = true) -> AttachmentType.PDF
                        attachmentUrl.endsWith(".png", true) || attachmentUrl.endsWith(".jpg", true) -> AttachmentType.IMAGE
                        else -> AttachmentType.LINK
                    }
                    onSubmit(title, body, attachmentUrl.ifBlank { null }, attType, isPinned)
                }
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
