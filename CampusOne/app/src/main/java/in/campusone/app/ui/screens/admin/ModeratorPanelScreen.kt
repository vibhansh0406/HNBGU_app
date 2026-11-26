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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directory
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.repository.AdminPermission
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1ElevatedCard
import `in`.campusone.app.ui.components.C1FilterChip
import `in`.campusone.app.ui.components.C1IconButton
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeratorPanelScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val canManageNotices = uiState.can(AdminPermission.MANAGE_NOTICES)
    val canManageAssignments = uiState.can(AdminPermission.MANAGE_ASSIGNMENTS)
    val canManageEvents = uiState.can(AdminPermission.MANAGE_EVENTS)
    val canManageClubs = uiState.can(AdminPermission.MANAGE_EVENTS)
    val canModerateReports = uiState.can(AdminPermission.MODERATE_REPORTS)

    var openNoticeDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<NoticeEntity?>(null) }
    var openAssignmentDialog by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<AssignmentEntity?>(null) }
    var openEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CampusEventEntity?>(null) }
    var openClubDialog by remember { mutableStateOf(false) }
    var editingClub by remember { mutableStateOf<ClubEntity?>(null) }
    var openDirectoryDialog by remember { mutableStateOf(false) }
    var editingDirectoryContact by remember { mutableStateOf<DirectoryContactEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moderator Panel") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item {
                C1SectionHeader(
                    title = "Campus Content Management",
                    subtitle = "Add and update notices, assignments, events, clubs and more",
                    actionLabel = null,
                    onAction = null
                )
            }

            // Notices
            item {
                ModeratorContentCard(
                    title = "Notices",
                    subtitle = "${uiState.notices.size} campus notices",
                    icon = Icons.Default.Campaign,
                    addEnabled = canManageNotices,
                    onAdd = { openNoticeDialog = true; editingNotice = null },
                    emptyText = "No notices yet",
                    items = uiState.notices,
                    idExtractor = { it.id },
                    onItemClick = { editingNotice = it; openNoticeDialog = true },
                    onDelete = { viewModel.deleteNoticeRemote(it.id) },
                    onSync = { viewModel.saveNoticeRemote(it, shouldNotify = false) },
                    itemContent = { notice ->
                        Column {
                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = notice.body.take(80),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Assignments
            item {
                ModeratorContentCard(
                    title = "Assignments",
                    subtitle = "${uiState.assignments.size} active assignments",
                    icon = Icons.Default.Assignment,
                    addEnabled = canManageAssignments,
                    onAdd = { openAssignmentDialog = true; editingAssignment = null },
                    emptyText = "No assignments yet",
                    items = uiState.assignments,
                    idExtractor = { it.id },
                    onItemClick = { editingAssignment = it; openAssignmentDialog = true },
                    onDelete = { viewModel.deleteAssignment(it.id) },
                    onSync = null,
                    itemContent = { assignment ->
                        Column {
                            Text(
                                text = assignment.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${assignment.subjectTag} • Due: ${assignment.dueDateFormatted}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Events
            item {
                ModeratorContentCard(
                    title = "Events",
                    subtitle = "${uiState.events.size} upcoming events",
                    icon = Icons.Default.Event,
                    addEnabled = canManageEvents,
                    onAdd = { openEventDialog = true; editingEvent = null },
                    emptyText = "No events yet",
                    items = uiState.events,
                    idExtractor = { it.id },
                    onItemClick = { editingEvent = it; openEventDialog = true },
                    onDelete = { viewModel.deleteEvent(it.id) },
                    onSync = null,
                    itemContent = { event ->
                        Column {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${event.clubName} • ${event.eventDateFormatted}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Clubs
            item {
                ModeratorContentCard(
                    title = "Clubs",
                    subtitle = "${uiState.clubs.size} campus clubs",
                    icon = Icons.Default.Group,
                    addEnabled = canManageClubs,
                    onAdd = { openClubDialog = true; editingClub = null },
                    emptyText = "No clubs yet",
                    items = uiState.clubs,
                    idExtractor = { it.id },
                    onItemClick = { editingClub = it; openClubDialog = true },
                    onDelete = { viewModel.deleteClub(it.id) },
                    onSync = null,
                    itemContent = { club ->
                        Column {
                            Text(
                                text = "${club.logoEmojiOrUrl}  ${club.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = club.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Lost & Found
            item {
                ModeratorContentCard(
                    title = "Lost & Found",
                    subtitle = "${uiState.lostFound.size} items reported",
                    icon = Icons.Default.FindInPage,
                    addEnabled = false,
                    onAdd = { },
                    emptyText = "No lost & found items",
                    items = uiState.lostFound,
                    idExtractor = { it.id },
                    onItemClick = { },
                    onDelete = null,
                    onSync = null,
                    itemContent = { item ->
                        Column {
                            Text(
                                text = "${if (item.type == "LOST") "🔍" else "📦"}  ${item.itemTitle}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.type} • ${item.location}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    extraActions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                            if (!item.isResolved) {
                                C1Button(
                                    text = "Resolve",
                                    onClick = { viewModel.resolveLostFound(item.id) },
                                    intent = C1ButtonIntent.PRIMARY
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Resolved", tint = MaterialTheme.colorScheme.primary)
                            }
                            C1IconButton(
                                onClick = { viewModel.hideLostFound(item.id) },
                                content = { Icon(Icons.Default.FindInPage, contentDescription = "Hide") }
                            )
                        }
                    }
                }
            }

            // Directory
            item {
                ModeratorContentCard(
                    title = "Directory",
                    subtitle = "${uiState.directory.size} contacts",
                    icon = Icons.Default.Directory,
                    addEnabled = true,
                    onAdd = { openDirectoryDialog = true; editingDirectoryContact = null },
                    emptyText = "No directory contacts yet",
                    items = uiState.directory,
                    idExtractor = { it.id },
                    onItemClick = { editingDirectoryContact = it; openDirectoryDialog = true },
                    onDelete = null,
                    onSync = null,
                    itemContent = { contact ->
                        Column {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${contact.designation} • ${contact.department}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = contact.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Reports & Moderation
            if (canModerateReports) {
                item {
                    ModeratorContentCard(
                        title = "Reports & Moderation",
                        subtitle = "${uiState.flaggedContent.size} items need review",
                        icon = Icons.Default.Notifications,
                        addEnabled = false,
                        onAdd = { },
                        emptyText = "Review flagged content in the Super Admin panel",
                        items = emptyList(),
                        idExtractor = { "" },
                        onItemClick = { },
                        onDelete = null,
                        onSync = null,
                        itemContent = { }
                    }
                }
            }
        }
    }

    // Notice Dialog
    if (openNoticeDialog) {
        NoticeEditorDialog(
            notice = editingNotice,
            onDismiss = { openNoticeDialog = false },
            onSave = { title, body, isPinned, category, priority, isOfficial ->
                val now = System.currentTimeMillis()
                val notice = NoticeEntity(
                    id = editingNotice?.id ?: "notice_$now",
                    sectionKey = uiState.sectionKey,
                    title = title,
                    body = body,
                    isPinned = isPinned,
                    category = category,
                    priority = priority,
                    isOfficial = isOfficial,
                    authorName = uiState.profile.name,
                    authorRole = uiState.profile.role,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
                viewModel.saveNoticeRemote(notice, shouldNotify = true)
                openNoticeDialog = false
            }
        )
    }

    // Assignment Dialog
    if (openAssignmentDialog) {
        AssignmentEditorDialog(
            assignment = editingAssignment,
            onDismiss = { openAssignmentDialog = false },
            onSave = { subjectTag, title, description, priority, dueDate ->
                val now = System.currentTimeMillis()
                val assignment = AssignmentEntity(
                    id = editingAssignment?.id ?: "assign_$now",
                    sectionKey = uiState.sectionKey,
                    subjectId = subjectTag.substringAfterLast("•").trim(),
                    subjectTag = subjectTag,
                    title = title,
                    description = description,
                    priority = priority,
                    dueDateEpochMillis = now + 86_400_000L,
                    dueDateFormatted = dueDate.ifEmpty { "TBD" },
                    reminderEnabled = true,
                    remind1DayBefore = true,
                    remind2HoursBefore = true,
                    isCompleted = false,
                    isSharedByCrOrTeacher = false,
                    isAddedToPersonalList = true,
                    authorName = uiState.profile.name,
                    isSynced = false,
                    updatedAtMillis = now
                )
                viewModel.saveAssignment(assignment)
                openAssignmentDialog = false
            }
        )
    }

    // Event Dialog
    if (openEventDialog) {
        EventEditorDialog(
            event = editingEvent,
            onDismiss = { openEventDialog = false },
            onSave = { clubName, category, title, description, venue, eventDate ->
                val now = System.currentTimeMillis()
                val event = CampusEventEntity(
                    id = editingEvent?.id ?: "event_$now",
                    collegeId = uiState.collegeId,
                    clubName = clubName,
                    category = category,
                    title = title,
                    description = description,
                    venue = venue,
                    eventDateFormatted = eventDate,
                    eventEpochMillis = now,
                    isApproved = true,
                    updatedAtMillis = now
                )
                viewModel.saveEvent(event)
                openEventDialog = false
            }
        )
    }

    // Club Dialog
    if (openClubDialog) {
        ClubEditorDialog(
            club = editingClub,
            onDismiss = { openClubDialog = false },
            onSave = { name, category, description, contactEmail ->
                val now = System.currentTimeMillis()
                val club = ClubEntity(
                    id = editingClub?.id ?: "club_$now",
                    collegeId = uiState.collegeId,
                    name = name,
                    description = description,
                    category = category,
                    contactEmail = contactEmail,
                    adminName = uiState.profile.name,
                    updatedAtMillis = now
                )
                viewModel.saveClub(club)
                openClubDialog = false
            }
        )
    }

    // Directory Dialog
    if (openDirectoryDialog) {
        DirectoryEditorDialog(
            contact = editingDirectoryContact,
            onDismiss = { openDirectoryDialog = false },
            onSave = { name, designation, department, phone, email, officeLocation, officeHours ->
                val now = System.currentTimeMillis()
                val contact = DirectoryContactEntity(
                    id = editingDirectoryContact?.id ?: "dir_$now",
                    collegeId = uiState.collegeId,
                    name = name,
                    designation = designation,
                    department = department,
                    phone = phone,
                    email = email,
                    officeLocation = officeLocation,
                    officeHours = officeHours,
                    updatedAtMillis = now
                )
                viewModel.saveDirectoryContact(contact)
                openDirectoryDialog = false
            }
        )
    }
}

@Composable
private fun <T> ModeratorContentCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    addEnabled: Boolean,
    onAdd: () -> Unit,
    emptyText: String,
    items: List<T>,
    idExtractor: (T) -> String,
    onItemClick: (T) -> Unit,
    onDelete: ((String) -> Unit)?,
    onSync: ((T) -> Unit)?,
    itemContent: @Composable (T) -> Unit,
    extraActions: @Composable () -> Unit = {}
) {
    C1ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (addEnabled) {
                        C1IconButton(
                            onClick = { openDirectoryDialog = true },
                            content = { Icon(Icons.Default.Add, contentDescription = "Add Directory Contact") }
                        )
                }
            }
            Spacer(modifier = Modifier.height(C1Spacing.md))
            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    C1SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onItemClick(item) }
                    ) {
                        Column(modifier = Modifier.padding(C1Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemContent(item)
                                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                    extraActions()
                                    if (onDelete != null) {
                                        C1IconButton(
                                            onClick = { onDelete(idExtractor(item)) },
                                            content = { Icon(Icons.Default.Delete, contentDescription = "Delete $title") }
                                        )
                                    }
                                    if (onSync != null) {
                                        C1IconButton(
                                            onClick = { onSync(item) },
                                            content = { Icon(Icons.Default.CloudUpload, contentDescription = "Sync $title") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeEditorDialog(
    notice: NoticeEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String, String, Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(notice?.title ?: "") }
    var body by rememberSaveable { mutableStateOf(notice?.body ?: "") }
    var isPinned by rememberSaveable { mutableStateOf(notice?.isPinned ?: false) }
    var category by rememberSaveable { mutableStateOf(notice?.category ?: "GENERAL") }
    var priority by rememberSaveable { mutableStateOf(notice?.priority ?: "NORMAL") }
    var isOfficial by rememberSaveable { mutableStateOf(notice?.isOfficial ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                androidx.compose.material3.TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Body") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pinned: ", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Official: ", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.Checkbox(
                        checked = isOfficial,
                        onCheckedChange = { isOfficial = it }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        listOf("GENERAL", "ACADEMIC", "EVENT", "URGENT").forEach { c ->
                            C1FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = c
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Priority: $priority", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        listOf("HIGH", "NORMAL", "LOW").forEach { p ->
                            C1FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = p
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = { onSave(title, body, isPinned, category, priority, isOfficial) },
                intent = C1ButtonIntent.PRIMARY,
                enabled = title.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun AssignmentEditorDialog(
    assignment: AssignmentEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var subjectTag by rememberSaveable { mutableStateOf(assignment?.subjectTag ?: "") }
    var title by rememberSaveable { mutableStateOf(assignment?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(assignment?.description ?: "") }
    var priority by rememberSaveable { mutableStateOf(assignment?.priority ?: "MEDIUM") }
    var dueDate by rememberSaveable { mutableStateOf(assignment?.dueDateFormatted ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assignment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                androidx.compose.material3.TextField(
                    value = subjectTag,
                    onValueChange = { subjectTag = it },
                    label = { Text("Subject Tag (e.g. OS • CS302)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Priority: $priority", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            C1FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = p
                            )
                        }
                    }
                }
                androidx.compose.material3.TextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (e.g. In 3 days • 05:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = { onSave(subjectTag, title, description, priority, dueDate) },
                intent = C1ButtonIntent.PRIMARY,
                enabled = title.isNotBlank() && subjectTag.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun EventEditorDialog(
    event: CampusEventEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var clubName by rememberSaveable { mutableStateOf(event?.clubName ?: "") }
    var category by rememberSaveable { mutableStateOf(event?.category ?: "TECH") }
    var title by rememberSaveable { mutableStateOf(event?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(event?.description ?: "") }
    var venue by rememberSaveable { mutableStateOf(event?.venue ?: "") }
    var eventDate by rememberSaveable { mutableStateOf(event?.eventDateFormatted ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                androidx.compose.material3.TextField(
                    value = clubName,
                    onValueChange = { clubName = it },
                    label = { Text("Club Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                )
                androidx.compose.material3.TextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        listOf("TECH", "CULTURAL", "SPORTS", "WORKSHOP").forEach { c ->
                            C1FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = c
                            )
                        }
                    }
                }
                androidx.compose.material3.TextField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    label = { Text("Event Date (e.g. 15 Nov, 10:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = { onSave(clubName, category, title, description, venue, eventDate) },
                intent = C1ButtonIntent.PRIMARY,
                enabled = title.isNotBlank() && clubName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun ClubEditorDialog(
    club: ClubEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(club?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(club?.category ?: "TECH") }
    var description by rememberSaveable { mutableStateOf(club?.description ?: "") }
    var contactEmail by rememberSaveable { mutableStateOf(club?.contactEmail ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Club", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                androidx.compose.material3.TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Club Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        listOf("TECH", "CULTURAL", "SPORTS", "LITERARY", "SOCIAL").forEach { c ->
                            C1FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = c
                            )
                        }
                    }
                }
                androidx.compose.material3.TextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Contact Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = { onSave(name, category, description, contactEmail) },
                intent = C1ButtonIntent.PRIMARY,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun DirectoryEditorDialog(
    contact: DirectoryContactEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(contact?.name ?: "") }
    var designation by rememberSaveable { mutableStateOf(contact?.designation ?: "") }
    var department by rememberSaveable { mutableStateOf(contact?.department ?: "") }
    var phone by rememberSaveable { mutableStateOf(contact?.phone ?: "") }
    var email by rememberSaveable { mutableStateOf(contact?.email ?: "") }
    var officeLocation by rememberSaveable { mutableStateOf(contact?.officeLocation ?: "Chauras Campus") }
    var officeHours by rememberSaveable { mutableStateOf(contact?.officeHours ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Directory Contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                androidx.compose.material3.TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = officeLocation,
                    onValueChange = { officeLocation = it },
                    label = { Text("Office Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.TextField(
                    value = officeHours,
                    onValueChange = { officeHours = it },
                    label = { Text("Office Hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = { onSave(name, designation, department, phone, email, officeLocation, officeHours) },
                intent = C1ButtonIntent.PRIMARY,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
