package `in`.campusone.app.ui.screens.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.theme.C1Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    var attachmentUrl by rememberSaveable { mutableStateOf("") }
    var attachmentType by rememberSaveable { mutableStateOf(AttachmentType.NONE.name) }
    val canManageOfficialNotices = uiState.identity.isOwner
    var isUploadingAttachment by rememberSaveable { mutableStateOf(false) }
    var attachmentUploadError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            val mimeType = context.contentResolver.getType(selectedUri).orEmpty()
            scope.launch {
                isUploadingAttachment = true
                attachmentUploadError = null
                val result = viewModel.uploadNoticeAttachment(selectedUri.toString(), mimeType)
                if (result.isSuccess) {
                    attachmentUrl = result.getOrNull().orEmpty()
                    attachmentType = if (
                        mimeType.equals("application/pdf", ignoreCase = true) ||
                        selectedUri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
                    ) AttachmentType.PDF.name else AttachmentType.IMAGE.name
                } else {
                    attachmentUploadError = result.exceptionOrNull()?.message
                        ?: "Attachment upload failed"
                }
                isUploadingAttachment = false
            }
        }
    }

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
                                Box(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .padding(horizontal = C1Spacing.sm)
                                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        .align(Alignment.CenterVertically),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Official", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        if (!notice.isOfficial || canManageOfficialNotices) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                            IconButton(onClick = {
                                editingNotice = notice
                                title = notice.title
                                body = notice.body
                                isPinned = notice.isPinned
                                category = notice.category
                                priority = notice.priority
                                isOfficial = notice.isOfficial
                                attachmentUrl = notice.attachmentUrl.orEmpty()
                                attachmentType = notice.attachmentType.name
                                attachmentUploadError = null
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
                                viewModel.saveNoticeRemote(notice, shouldNotify = editingNotice == null)
                            }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Sync to cloud")
                            }
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
            canMarkOfficial = canManageOfficialNotices,
            attachmentUrl = attachmentUrl,
            attachmentType = attachmentType,
            isUploadingAttachment = isUploadingAttachment,
            attachmentUploadError = attachmentUploadError,
            onAttachmentUrlChange = {
                attachmentUrl = it
                attachmentType = AttachmentType.NONE.name
            },
            onUploadAttachment = { mimeType ->
                attachmentPicker.launch(arrayOf(mimeType))
            },
            onTitleChange = { title = it },
            onBodyChange = { body = it },
            onPinnedChange = { isPinned = it },
            onCategoryChange = { category = it },
            onPriorityChange = { priority = it },
            onOfficialChange = { isOfficial = it },
            onDismiss = { showDialog = false },
            onSave = {
                val notice = NoticeEntity(
                    id = editingNotice?.id ?: java.util.UUID.randomUUID().toString(),
                    sectionKey = editingNotice?.sectionKey ?: uiState.sectionKey,
                    title = title,
                    body = body,
                    isPinned = isPinned,
                    category = category,
                    priority = priority,
                    isOfficial = canManageOfficialNotices && isOfficial,
                    attachmentUrl = attachmentUrl.ifBlank { null },
                    attachmentType = when {
                        attachmentUrl.isBlank() -> AttachmentType.NONE
                        attachmentType != AttachmentType.NONE.name -> AttachmentType.valueOf(attachmentType)
                        attachmentUrl.substringBefore('?').endsWith(".pdf", ignoreCase = true) -> AttachmentType.PDF
                        attachmentUrl.substringBefore('?').let { url ->
                            url.endsWith(".png", true) || url.endsWith(".jpg", true) ||
                                url.endsWith(".jpeg", true) || url.endsWith(".webp", true)
                        } -> AttachmentType.IMAGE
                        else -> AttachmentType.LINK
                    },
                    authorUid = uiState.profile.uid,
                    authorName = uiState.profile.name,
                    authorRole = uiState.profile.role,
                    createdAtMillis = editingNotice?.createdAtMillis ?: System.currentTimeMillis(),
                    isRead = false
                )
                viewModel.saveNotice(notice)
                // Persist every notice centrally; important notices also trigger FCM.
                viewModel.saveNoticeRemote(notice)
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
    category: String,
    priority: String,
    isOfficial: Boolean,
    canMarkOfficial: Boolean,
    attachmentUrl: String,
    attachmentType: String,
    isUploadingAttachment: Boolean,
    attachmentUploadError: String?,
    onAttachmentUrlChange: (String) -> Unit,
    onUploadAttachment: (mimeType: String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onOfficialChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (title.isNotBlank()) "Edit Notice" else "Add Notice") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                C1TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = "Body / Details",
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = attachmentUrl,
                    onValueChange = onAttachmentUrlChange,
                    label = "Attachment URL (optional)",
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    C1Button(
                        text = if (isUploadingAttachment) "Uploading..." else "Upload PDF",
                        onClick = { onUploadAttachment("application/pdf") },
                        modifier = Modifier.weight(1f),
                        intent = C1ButtonIntent.OUTLINED,
                        enabled = !isUploadingAttachment,
                        loading = isUploadingAttachment
                    )
                    C1Button(
                        text = "Upload Image",
                        onClick = { onUploadAttachment("image/*") },
                        modifier = Modifier.weight(1f),
                        intent = C1ButtonIntent.OUTLINED,
                        enabled = !isUploadingAttachment
                    )
                }
                if (attachmentUrl.isNotBlank() && attachmentType != AttachmentType.NONE.name) {
                    Text(
                        text = "Attachment added ($attachmentType)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!attachmentUploadError.isNullOrBlank()) {
                    Text(
                        text = attachmentUploadError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = isPinned, onCheckedChange = onPinnedChange)
                    Text("Pin to top")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    C1TextField(
                        value = category,
                        onValueChange = onCategoryChange,
                        label = "Category",
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = priority,
                        onValueChange = onPriorityChange,
                        label = "Priority",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canMarkOfficial) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(checked = isOfficial, onCheckedChange = onOfficialChange)
                        Text("Official announcement")
                    }
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Publish notice",
                onClick = onSave,
                enabled = title.isNotBlank() && body.isNotBlank() && !isUploadingAttachment
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

