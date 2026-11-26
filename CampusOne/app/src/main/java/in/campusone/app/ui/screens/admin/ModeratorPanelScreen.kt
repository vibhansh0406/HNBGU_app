package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete

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
import androidx.compose.runtime.remember
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
                                    text = "Notices",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.notices.size} campus notices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManageNotices) {
                                C1IconButton(
                                    onClick = { openNoticeDialog = true; editingNotice = null },
                                    content = { Icon(Icons.Default.Add, contentDescription = "Add Notice") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        if (uiState.notices.isEmpty()) {
                            Text(
                                text = "No notices yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.notices.forEach { notice ->
                                C1SurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { editingNotice = notice; openNoticeDialog = true }
                                ) {
                                    Column(modifier = Modifier.padding(C1Spacing.md)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                                if (canManageNotices) {
                                                    C1IconButton(
                                                        onClick = { viewModel.deleteNoticeRemote(notice.id) },
                                                        content = { Icon(Icons.Default.Delete, contentDescription = "Delete Notice") }
                                                    )
                                                }
                                                C1IconButton(
                                                    onClick = { viewModel.saveNoticeRemote(notice, shouldNotify = false) },
                                                    content = { Icon(Icons.Default.CloudUpload, contentDescription = "Sync Notice") }
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

            // Assignments
            item {
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
                                    text = "Assignments",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.assignments.size} active assignments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManageAssignments) {
                                C1IconButton(
                                    onClick = { openAssignmentDialog = true; editingAssignment = null },
                                    content = { Icon(Icons.Default.Add, contentDescription = "Add Assignment") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        if (uiState.assignments.isEmpty()) {
                            Text(
                                text = "No assignments yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.assignments.forEach { assignment ->
                                C1SurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { editingAssignment = assignment; openAssignmentDialog = true }
                                ) {
                                    Column(modifier = Modifier.padding(C1Spacing.md)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                                if (canManageAssignments) {
                                                    C1IconButton(
                                                        onClick = { viewModel.deleteAssignment(assignment.id) },
                                                        content = { Icon(Icons.Default.Delete, contentDescription = "Delete Assignment") }
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

            // Events
            item {
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
                                    text = "Events",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.events.size} upcoming events",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManageEvents) {
                                C1IconButton(
                                    onClick = { openEventDialog = true; editingEvent = null },
                                    content = { Icon(Icons.Default.Add, contentDescription = "Add Event") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        if (uiState.events.isEmpty()) {
                            Text(
                                text = "No events yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.events.forEach { event ->
                                C1SurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { editingEvent = event; openEventDialog = true }
                                ) {
                                    Column(modifier = Modifier.padding(C1Spacing.md)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                                if (canManageEvents) {
                                                    C1IconButton(
                                                        onClick = { viewModel.deleteEvent(event.id) },
                                                        content = { Icon(Icons.Default.Delete, contentDescription = "Delete Event") }
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

            // Clubs
            item {
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
                                    text = "Clubs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.clubs.size} campus clubs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManageClubs) {
                                C1IconButton(
                                    onClick = { openClubDialog = true; editingClub = null },
                                    content = { Icon(Icons.Default.Add, contentDescription = "Add Club") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        if (uiState.clubs.isEmpty()) {
                            Text(
                                text = "No clubs yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.clubs.forEach { club ->
                                C1SurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { editingClub = club; openClubDialog = true }
                                ) {
                                    Column(modifier = Modifier.padding(C1Spacing.md)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                                if (canManageClubs) {
                                                    C1IconButton(
                                                        onClick = { viewModel.deleteClub(club.id) },
                                                        content = { Icon(Icons.Default.Delete, contentDescription = "Delete Club") }
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

            // Lost & Found
            item {
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
                                    text = "Lost & Found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.lostFound.size} items reported",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        if (uiState.lostFound.isEmpty()) {
                            Text(
                                text = "No lost & found items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.lostFound.forEach { item ->
                                C1SurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { }
                                ) {
                                    Column(modifier = Modifier.padding(C1Spacing.md)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                            }
                        }
                    }
                }
            }

            // Directory
            item {
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
                                    text = "Directory",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.directory.size} contacts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            C1IconButton(
                                onClick = { openDirectoryDialog = true; editingDirectoryContact = null },
                                content = { Icon(Icons.Default.Add, contentDescription = "Add Directory Contact") }
                            )
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        if (uiState.directory.isEmpty()) {
                            Text(
                                text = "No directory contacts yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.directory.forEach { contact ->
                                C1SurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { editingDirectoryContact = contact; openDirectoryDialog = true }
                                ) {
                                    Column(modifier = Modifier.padding(C1Spacing.md)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                            }
                        }
                    }
                }
            }

            // Reports & Moderation
            if (canModerateReports) {
                item {
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
                                        text = "Reports & Moderation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${uiState.flaggedContent.size} items need review",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(C1Spacing.md))
                            Text(
                                text = "Review flagged content in the Super Admin panel.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    authorUid = uiState.profile.uid,
                    createdAtMillis = now
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
                    createdAtMillis = now,
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
                    clubId = "club_gdsc_hnbgu",
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
                    socialLink = "",
                    adminUid = uiState.profile.uid,
                    adminName = uiState.profile.name,
                    isFollowedByMe = false,
                    isApproved = true,
                    reportCount = 0,
                    isFlagged = false,
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
                    category = "FACULTY",
                    department = department,
                    name = name,
                    designation = designation,
                    phone = phone,
                    email = email,
                    officeLocation = officeLocation,
                    officeHours = officeHours,
                    isEmergencyOrImportant = false
                )
                viewModel.saveDirectoryContact(contact)
                openDirectoryDialog = false
            }
        )
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
