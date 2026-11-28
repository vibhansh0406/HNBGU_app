package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.data.repository.AdminPermission
import `in`.campusone.app.data.repository.AdminUserRecord
import `in`.campusone.app.data.repository.AuditEntry
import `in`.campusone.app.data.repository.FlaggedItem
import `in`.campusone.app.data.repository.PlatformSettings
import `in`.campusone.app.data.repository.RoleRequestRecord
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import `in`.campusone.app.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val assignableRoles = listOf(
    UserRole.MODERATOR,
    UserRole.CR,
    UserRole.STUDENT
)

private val dateFormat: SimpleDateFormat
    get() = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

private fun formatMillis(millis: Long): String =
    if (millis <= 0L) "—" else dateFormat.format(Date(millis))

// =========================================================================
// USERS
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperAdminUsersScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf<UserRole?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<AdminUserRecord?>(null) }
    var confirmSuspend by remember { mutableStateOf<AdminUserRecord?>(null) }
    var confirmRemove by remember { mutableStateOf<AdminUserRecord?>(null) }

    val canManageUsers = uiState.identity.can(AdminPermission.MANAGE_USERS)
    val canManageRoles = uiState.identity.can(AdminPermission.MANAGE_ROLES)

    val filtered = uiState.users.filter { user ->
        val matchesQuery = query.isBlank() ||
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true) ||
            user.collegeName.contains(query, ignoreCase = true) ||
            user.courseName.contains(query, ignoreCase = true)
        val matchesRole = roleFilter == null || user.role == roleFilter
        val matchesStatus = when (statusFilter) {
            "ACTIVE" -> !user.isSuspended
            "SUSPENDED" -> user.isSuspended
            else -> true
        }
        matchesQuery && matchesRole && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Total", uiState.users.size.toString(), Modifier.weight(1f))
                    MiniStat(
                        "Active",
                        uiState.users.count { !it.isSuspended }.toString(),
                        Modifier.weight(1f)
                    )
                    MiniStat(
                        "Suspended",
                        uiState.users.count { it.isSuspended }.toString(),
                        Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search name, email, college or course") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = roleFilter == null,
                        onClick = { roleFilter = null },
                        label = { Text("All roles") }
                    )
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = roleFilter == role,
                            onClick = { roleFilter = role },
                            label = { Text(role.name) }
                        )
                    }
                }
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to null, "ACTIVE" to "ACTIVE", "SUSPENDED" to "SUSPENDED")
                        .forEach { (label, value) ->
                            FilterChip(
                                selected = statusFilter == value,
                                onClick = { statusFilter = value },
                                label = { Text(label) }
                            )
                        }
                }
            }

            item {
                Text(
                    text = "${filtered.size} of ${uiState.users.size} users",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.remoteLoading && uiState.users.isEmpty()) {
                item {
                    Text(
                        text = "Loading users from the server...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!uiState.remoteLoading && uiState.users.isEmpty()) {
                item {
                    Text(
                        text = uiState.remoteError ?: "No users found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(filtered, key = { it.uid }) { user ->
                UserRow(user = user, onClick = { selected = user })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    selected?.let { user ->
        UserDetailDialog(
            user = user,
            isSelf = user.uid == uiState.profile.uid,
            canManageUsers = canManageUsers,
            canManageRoles = canManageRoles,
            attendance = uiState.selectedUserAttendance.takeIf { uiState.attendanceUserId == user.uid },
            onLoadAttendance = { viewModel.loadUserAttendance(user.uid) },
            onRoleChange = { role ->
                viewModel.changeUserRole(user.uid, role)
                selected = null
            },
            onSuspend = { confirmSuspend = user },
            onRemove = { confirmRemove = user },
            onDismiss = { selected = null }
        )
    }

    confirmSuspend?.let { user ->
        AlertDialog(
            onDismissRequest = { confirmSuspend = null },
            title = { Text(if (user.isSuspended) "Reactivate account?" else "Suspend account?") },
            text = {
                Text(
                    if (user.isSuspended) {
                        "${user.email} will regain access with the STUDENT role."
                    } else {
                        "${user.email} will be signed out of privileged features and demoted to STUDENT. " +
                            "This is recorded in the audit log."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setUserSuspended(user.uid, !user.isSuspended)
                    confirmSuspend = null
                    selected = null
                }) { Text(if (user.isSuspended) "Reactivate" else "Suspend") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSuspend = null }) { Text("Cancel") }
            }
        )
    }

    confirmRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove user?") },
            text = {
                Text(
                    "This deletes the profile row for ${user.email}. " +
                        "Their local device data is untouched. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeUser(user.uid)
                    confirmRemove = null
                    selected = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = extendedColors.brandForeground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserRow(user: AdminUserRecord, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = user.name.ifBlank { "Unnamed user" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = listOfNotNull(
                        user.courseName.takeIf { it.isNotBlank() },
                        user.section.takeIf { it.isNotBlank() }?.let { "Sec $it" },
                        "Sem ${user.semester}"
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = user.role.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.brandForeground
                )
                if (user.isSuspended) {
                    Text(
                        text = "SUSPENDED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserDetailDialog(
    user: AdminUserRecord,
    isSelf: Boolean,
    canManageUsers: Boolean,
    canManageRoles: Boolean,
    attendance: List<`in`.campusone.app.data.repository.UserAttendanceRow>?,
    onLoadAttendance: () -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onSuspend: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRole by remember(user.uid, user.role) { mutableStateOf(user.role) }
    val isOwnerAccount = user.email.equals("vvibhansh@gmail.com", ignoreCase = true)
    val locked = isOwnerAccount || isSelf

    LaunchedEffect(user.uid) { onLoadAttendance() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(user.name.ifBlank { user.email }) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = listOfNotNull(
                        user.collegeName.takeIf { it.isNotBlank() },
                        user.courseName.takeIf { it.isNotBlank() },
                        "Sem ${user.semester}",
                        "Sec ${user.section}"
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Joined ${formatMillis(user.createdAtMillis)} • updated ${formatMillis(user.updatedAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text(text = "Role", style = MaterialTheme.typography.labelLarge)
                if (!canManageRoles) {
                    Text(
                        text = "You do not hold MANAGE_ROLES.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (locked) {
                    Text(
                        text = if (isOwnerAccount) {
                            "Owner account — role is protected by the server."
                        } else {
                            "You cannot change your own role."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        assignableRoles.forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role.name) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(text = "Attendance", style = MaterialTheme.typography.labelLarge)
                when {
                    attendance == null -> Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    attendance.isEmpty() -> Text(
                        text = "No attendance records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> attendance.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = row.subjectId,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "P ${row.present} • A ${row.absent}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (canManageUsers && !locked) {
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onSuspend) {
                            Text(if (user.isSuspended) "Reactivate" else "Suspend")
                        }
                        TextButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRoleChange(selectedRole) },
                enabled = canManageRoles && !locked && selectedRole != user.role
            ) { Text("Save role") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// =========================================================================
// ROLES
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminRolesScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val pending = uiState.roleRequests.filter { it.status == "PENDING" }
    val reviewed = uiState.roleRequests.filter { it.status != "PENDING" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roles & permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Capability matrix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.brandForeground
                )
            }
            item {
                PermissionMatrixCard(uiState = uiState)
            }

            item {
                Text(
                    text = "Pending requests (${pending.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.brandForeground
                )
            }

            if (pending.isEmpty()) {
                item {
                    Text(
                        text = "No pending role requests.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(pending, key = { it.id }) { request ->
                RoleRequestCard(
                    request = request,
                    onApprove = { viewModel.approveRequest(request) },
                    onReject = { viewModel.rejectRequest(request) }
                )
            }

            if (reviewed.isNotEmpty()) {
                item {
                    Text(
                        text = "Recently reviewed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.brandForeground
                    )
                }
                items(reviewed, key = { it.id }) { request ->
                    Text(
                        text = "${request.email} • ${request.requestedRole.name} • ${request.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionMatrixCard(uiState: AdminUiState) {
    val rows = listOf(
        "Platform owner" to AdminPermission.entries.toList(),
        "ADMIN" to listOf(
            AdminPermission.VIEW_USERS,
            AdminPermission.MANAGE_USERS,
            AdminPermission.MANAGE_NOTICES,
            AdminPermission.MANAGE_RESOURCES,
            AdminPermission.MANAGE_ASSIGNMENTS,
            AdminPermission.MODERATE_REPORTS,
            AdminPermission.MANAGE_EVENTS,
            AdminPermission.MANAGE_TIMETABLE
        ),
        "MODERATOR" to listOf(
            AdminPermission.MANAGE_NOTICES,
            AdminPermission.MANAGE_RESOURCES,
            AdminPermission.MANAGE_ASSIGNMENTS,
            AdminPermission.MODERATE_REPORTS,
            AdminPermission.MANAGE_EVENTS,
            AdminPermission.MANAGE_TIMETABLE
        ),
        "CR" to listOf(
            AdminPermission.MANAGE_NOTICES,
            AdminPermission.MANAGE_ASSIGNMENTS
        )
    )
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { (label, granted) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (granted.size == AdminPermission.entries.size) {
                        "All permissions"
                    } else {
                        granted.joinToString(", ") { it.code }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            Text(
                text = "Your server-verified permissions: " +
                    if (uiState.identity.isOwner) {
                        "all (owner)"
                    } else {
                        uiState.identity.permissions.joinToString(", ") { it.code }.ifBlank { "none" }
                    },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = extendedColors.brandForeground
            )
        }
    }
}

@Composable
private fun RoleRequestCard(
    request: RoleRequestRecord,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = request.email.ifBlank { "Unknown user" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Requests ${request.requestedRole.name}" +
                    if (request.note.isBlank()) "" else " • ${request.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMillis(request.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onApprove) { Text("Approve") }
                TextButton(onClick = onReject) { Text("Reject") }
            }
        }
    }
}

// =========================================================================
// CONTENT
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminContentScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<FlaggedItem?>(null) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<NoticeEntity?>(null) }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeBody by remember { mutableStateOf("") }
    var noticeCategory by remember { mutableStateOf("GENERAL") }
    var noticePriority by remember { mutableStateOf("NORMAL") }
    var noticePinned by remember { mutableStateOf(false) }
    var noticeOfficial by remember { mutableStateOf(false) }
    var showResourceDialog by remember { mutableStateOf(false) }
    var editingResource by remember { mutableStateOf<StudyResourceEntity?>(null) }
    var resourceTitle by remember { mutableStateOf("") }
    var resourceSubjectId by remember { mutableStateOf("") }
    var resourceSubjectName by remember { mutableStateOf("") }
    var resourceCategory by remember { mutableStateOf("NOTES") }
    var resourceFileUrl by remember { mutableStateOf("") }
    var resourceFileType by remember { mutableStateOf("PDF") }
    var resourceSemester by remember { mutableIntStateOf(1) }
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<AssignmentEntity?>(null) }
    var assignmentTitle by remember { mutableStateOf("") }
    var assignmentSubjectId by remember { mutableStateOf("") }
    var assignmentSubjectTag by remember { mutableStateOf("") }
    var assignmentDescription by remember { mutableStateOf("") }
    var assignmentPriority by remember { mutableStateOf("MEDIUM") }
    var assignmentDueDate by remember { mutableStateOf("") }
    var showExamDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamEntity?>(null) }
    var examTitle by remember { mutableStateOf("") }
    var examSubjectName by remember { mutableStateOf("") }
    var examSubjectCode by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var examTime by remember { mutableStateOf("10:00 AM – 01:00 PM") }
    var examRoom by remember { mutableStateOf("") }
    var showSyllabusDialog by remember { mutableStateOf(false) }
    var editingSyllabus by remember { mutableStateOf<SyllabusTopicEntity?>(null) }
    var syllabusTopicTitle by remember { mutableStateOf("") }
    var syllabusSubjectName by remember { mutableStateOf("") }
    var syllabusUnitNumber by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Live content counts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.brandForeground
                )
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ContentCountRow("Notices", uiState.noticeCount.toString())
                        ContentCountRow("Study resources", uiState.studyResources.size.toString())
                        ContentCountRow("Assignments", uiState.assignmentCount.toString())
                        ContentCountRow("Exams", uiState.examCount.toString())
                        ContentCountRow("Clubs", uiState.clubCount.toString())
                        ContentCountRow("Events", uiState.eventCount.toString())
                        ContentCountRow("Lost & Found", uiState.lostFound.size.toString())
                        ContentCountRow("Directory contacts", uiState.directory.size.toString())
                        ContentCountRow("Subjects", uiState.subjectCount.toString())
                        ContentCountRow("Timetable slots", uiState.timetableSlots.size.toString())
                    }
                }
            }

            item {
                Text(
                    text = "Flagged for review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.brandForeground
                )
            }

            if (uiState.flaggedContent.isEmpty()) {
                item {
                    Text(
                        text = "Nothing is flagged right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.flaggedContent, key = { "${it.kind}_${it.id}" }) { item ->
                FlaggedRow(
                    item = item,
                    onHide = { viewModel.moderateFlagged(item, "HIDE") },
                    onRestore = { viewModel.moderateFlagged(item, "RESTORE") },
                    onDelete = { pendingDelete = item }
                )
            }

            item {
                SectionHeader("Notices", "Institution-wide announcements")
            }
            item {
                if (uiState.notices.isEmpty()) {
                    Text(
                        text = "No notices yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.notices.forEach { notice ->
                        NoticeRow(
                            notice = notice,
                            onEdit = {
                                editingNotice = notice
                                noticeTitle = notice.title
                                noticeBody = notice.body
                                noticeCategory = notice.category
                                noticePriority = notice.priority
                                noticePinned = notice.isPinned
                                noticeOfficial = notice.isOfficial
                                showNoticeDialog = true
                            },
                            onDelete = { viewModel.deleteNoticeRemote(notice.id) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Study resources", "Notes, PYQs and study materials")
                    TextButton(onClick = {
                        editingResource = null
                        resourceTitle = ""
                        resourceSubjectId = ""
                        resourceSubjectName = ""
                        resourceCategory = "NOTES"
                        resourceFileUrl = ""
                        resourceFileType = "PDF"
                        resourceSemester = 1
                        showResourceDialog = true
                    }) {
                        Text("Add resource")
                    }
                }
            }
            item {
                if (uiState.studyResources.isEmpty()) {
                    Text(
                        text = "No study resources yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.studyResources.forEach { resource ->
                        ResourceRow(
                            resource = resource,
                            onEdit = {
                                editingResource = resource
                                resourceTitle = resource.title
                                resourceSubjectId = resource.subjectId
                                resourceSubjectName = resource.subjectName
                                resourceCategory = resource.category
                                resourceFileUrl = resource.fileUrl.orEmpty()
                                resourceFileType = resource.fileType
                                resourceSemester = resource.semester
                                showResourceDialog = true
                            },
                            onDelete = { viewModel.deleteStudyResource(resource.id) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Assignments", "Published course assignments")
                    TextButton(onClick = {
                        editingAssignment = null
                        assignmentTitle = ""
                        assignmentSubjectId = ""
                        assignmentSubjectTag = ""
                        assignmentDescription = ""
                        assignmentPriority = "MEDIUM"
                        assignmentDueDate = ""
                        showAssignmentDialog = true
                    }) {
                        Text("Add assignment")
                    }
                }
            }
            item {
                if (uiState.assignments.isEmpty()) {
                    Text(
                        text = "No assignments yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.assignments.forEach { assignment ->
                        AssignmentRow(
                            assignment = assignment,
                            onEdit = {
                                editingAssignment = assignment
                                assignmentTitle = assignment.title
                                assignmentSubjectId = assignment.subjectId
                                assignmentSubjectTag = assignment.subjectTag
                                assignmentDescription = assignment.description
                                assignmentPriority = assignment.priority
                                assignmentDueDate = assignment.dueDateFormatted
                                showAssignmentDialog = true
                            },
                            onDelete = { viewModel.deleteAssignment(assignment.id) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Exams", "Scheduled examinations")
                    TextButton(onClick = {
                        editingExam = null
                        examTitle = ""
                        examSubjectName = ""
                        examSubjectCode = ""
                        examDate = ""
                        examTime = "10:00 AM – 01:00 PM"
                        examRoom = ""
                        showExamDialog = true
                    }) {
                        Text("Add exam")
                    }
                }
            }
            item {
                if (uiState.exams.isEmpty()) {
                    Text(
                        text = "No exams yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.exams.forEach { exam ->
                        ExamRow(
                            exam = exam,
                            onEdit = {
                                editingExam = exam
                                examTitle = exam.examTitle
                                examSubjectName = exam.subjectName
                                examSubjectCode = exam.subjectCode
                                examDate = exam.examDateFormatted
                                examTime = exam.examTimeFormatted
                                examRoom = exam.roomOrCenter
                                showExamDialog = true
                            },
                            onDelete = { viewModel.deleteExam(exam.id) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Syllabus", "Unit-wise topic breakdown")
                    TextButton(onClick = {
                        editingSyllabus = null
                        syllabusTopicTitle = ""
                        syllabusSubjectName = ""
                        syllabusUnitNumber = 1
                        showSyllabusDialog = true
                    }) {
                        Text("Add topic")
                    }
                }
            }
            item {
                if (uiState.syllabusTopics.isEmpty()) {
                    Text(
                        text = "No syllabus topics yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.syllabusTopics.forEach { topic ->
                        SyllabusRow(
                            topic = topic,
                            onEdit = {
                                editingSyllabus = topic
                                syllabusTopicTitle = topic.topicTitle
                                syllabusSubjectName = topic.subjectName
                                syllabusUnitNumber = topic.unitNumber
                                showSyllabusDialog = true
                            },
                            onDelete = { viewModel.deleteSyllabusTopic(topic.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    FloatingActionButton(
        onClick = {
            editingNotice = null
            noticeTitle = ""
            noticeBody = ""
            noticeCategory = "GENERAL"
            noticePriority = "NORMAL"
            noticePinned = false
            noticeOfficial = false
            showNoticeDialog = true
        }
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add notice")
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete permanently?") },
            text = { Text("${item.title} will be removed from the database.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.moderateFlagged(item, "DELETE")
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showNoticeDialog) {
        NoticeDialog(
            title = noticeTitle,
            body = noticeBody,
            category = noticeCategory,
            priority = noticePriority,
            isPinned = noticePinned,
            isOfficial = noticeOfficial,
            onTitleChange = { noticeTitle = it },
            onBodyChange = { noticeBody = it },
            onCategoryChange = { noticeCategory = it },
            onPriorityChange = { noticePriority = it },
            onPinnedChange = { noticePinned = it },
            onOfficialChange = { noticeOfficial = it },
            onDismiss = { showNoticeDialog = false },
            onSave = {
                val notice = NoticeEntity(
                    id = editingNotice?.id ?: java.util.UUID.randomUUID().toString(),
                    sectionKey = editingNotice?.sectionKey ?: uiState.sectionKey,
                    title = noticeTitle,
                    body = noticeBody,
                    isPinned = noticePinned,
                    category = noticeCategory,
                    priority = noticePriority,
                    isOfficial = noticeOfficial,
                    attachmentUrl = null,
                    attachmentType = AttachmentType.NONE,
                    authorUid = uiState.profile.uid,
                    authorName = uiState.profile.name,
                    authorRole = uiState.profile.role,
                    createdAtMillis = editingNotice?.createdAtMillis ?: System.currentTimeMillis(),
                    isRead = false
                )
                viewModel.saveNotice(notice)
                viewModel.saveNoticeRemote(notice)
                showNoticeDialog = false
            }
        )
    }

    if (showResourceDialog) {
        ResourceDialog(
            title = resourceTitle,
            subjectId = resourceSubjectId,
            subjectName = resourceSubjectName,
            category = resourceCategory,
            fileUrl = resourceFileUrl,
            fileType = resourceFileType,
            semester = resourceSemester,
            onTitleChange = { resourceTitle = it },
            onSubjectIdChange = { resourceSubjectId = it },
            onSubjectNameChange = { resourceSubjectName = it },
            onCategoryChange = { resourceCategory = it },
            onFileUrlChange = { resourceFileUrl = it },
            onFileTypeChange = { resourceFileType = it },
            onSemesterChange = { resourceSemester = it },
            onDismiss = { showResourceDialog = false },
            onSave = {
                val resource = StudyResourceEntity(
                    id = editingResource?.id ?: java.util.UUID.randomUUID().toString(),
                    sectionKey = editingResource?.sectionKey ?: uiState.sectionKey,
                    subjectId = resourceSubjectId,
                    subjectName = resourceSubjectName,
                    semester = resourceSemester,
                    title = resourceTitle,
                    category = resourceCategory,
                    fileUrl = resourceFileUrl,
                    fileType = resourceFileType,
                    uploaderUid = uiState.profile.uid,
                    uploaderName = uiState.profile.name
                )
                viewModel.saveStudyResource(resource)
                showResourceDialog = false
            }
        )
    }

    if (showAssignmentDialog) {
        AssignmentDialog(
            title = assignmentTitle,
            subjectId = assignmentSubjectId,
            subjectTag = assignmentSubjectTag,
            description = assignmentDescription,
            priority = assignmentPriority,
            dueDate = assignmentDueDate,
            onTitleChange = { assignmentTitle = it },
            onSubjectIdChange = { assignmentSubjectId = it },
            onSubjectTagChange = { assignmentSubjectTag = it },
            onDescriptionChange = { assignmentDescription = it },
            onPriorityChange = { assignmentPriority = it },
            onDueDateChange = { assignmentDueDate = it },
            onDismiss = { showAssignmentDialog = false },
            onSave = {
                val assignment = AssignmentEntity(
                    id = editingAssignment?.id ?: java.util.UUID.randomUUID().toString(),
                    sectionKey = editingAssignment?.sectionKey ?: uiState.sectionKey,
                    subjectId = assignmentSubjectId,
                    subjectTag = assignmentSubjectTag,
                    title = assignmentTitle,
                    description = assignmentDescription,
                    priority = assignmentPriority,
                    dueDateEpochMillis = System.currentTimeMillis(),
                    dueDateFormatted = assignmentDueDate,
                    authorName = uiState.profile.name
                )
                viewModel.saveAssignment(assignment)
                showAssignmentDialog = false
            }
        )
    }

    if (showExamDialog) {
        ExamDialog(
            title = examTitle,
            subjectName = examSubjectName,
            subjectCode = examSubjectCode,
            date = examDate,
            time = examTime,
            room = examRoom,
            onTitleChange = { examTitle = it },
            onSubjectNameChange = { examSubjectName = it },
            onSubjectCodeChange = { examSubjectCode = it },
            onDateChange = { examDate = it },
            onTimeChange = { examTime = it },
            onRoomChange = { examRoom = it },
            onDismiss = { showExamDialog = false },
            onSave = {
                val exam = ExamEntity(
                    id = editingExam?.id ?: java.util.UUID.randomUUID().toString(),
                    sectionKey = editingExam?.sectionKey ?: uiState.sectionKey,
                    subjectId = examSubjectCode,
                    subjectName = examSubjectName,
                    subjectCode = examSubjectCode,
                    examTitle = examTitle,
                    examDateEpochMillis = System.currentTimeMillis(),
                    examDateIso = examDate,
                    examTimeFormatted = examTime,
                    examDateFormatted = examDate,
                    roomOrCenter = examRoom
                )
                viewModel.saveExam(exam)
                showExamDialog = false
            }
        )
    }

    if (showSyllabusDialog) {
        SyllabusDialog(
            topicTitle = syllabusTopicTitle,
            subjectName = syllabusSubjectName,
            unitNumber = syllabusUnitNumber,
            onTopicTitleChange = { syllabusTopicTitle = it },
            onSubjectNameChange = { syllabusSubjectName = it },
            onUnitNumberChange = { syllabusUnitNumber = it },
            onDismiss = { showSyllabusDialog = false },
            onSave = {
                val topic = SyllabusTopicEntity(
                    id = editingSyllabus?.id ?: java.util.UUID.randomUUID().toString(),
                    sectionKey = editingSyllabus?.sectionKey ?: uiState.sectionKey,
                    subjectId = syllabusSubjectName,
                    subjectName = syllabusSubjectName,
                    unitNumber = syllabusUnitNumber,
                    topicTitle = syllabusTopicTitle
                )
                viewModel.saveSyllabusTopic(topic)
                showSyllabusDialog = false
            }
        )
    }
}

@Composable
private fun ContentCountRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = extendedColors.brandForeground
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlaggedRow(
    item: FlaggedItem,
    onHide: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.kind.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = extendedColors.brandForeground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onHide) { Text("Hide") }
                TextButton(onClick = onRestore) { Text("Restore") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

// =========================================================================
// MODERATION
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperAdminModerationScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & moderation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Reported content (${uiState.flaggedContent.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.brandForeground
                )
            }

            if (uiState.flaggedContent.isEmpty()) {
                item {
                    Text(
                        text = "Nothing to review. The queue is clear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.flaggedContent, key = { "${it.kind}_${it.id}" }) { item ->
                FlaggedRow(
                    item = item,
                    onHide = { viewModel.moderateFlagged(item, "HIDE") },
                    onRestore = { viewModel.moderateFlagged(item, "RESTORE") },
                    onDelete = { viewModel.moderateFlagged(item, "DELETE") }
                )
            }

            if (uiState.contentReports.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Submitted reports (${uiState.contentReports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.brandForeground
                    )
                }
                items(uiState.contentReports, key = { it.id }) { report ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${report.targetType} • ${report.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = extendedColors.brandForeground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = report.reason.ifBlank { "No reason given" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${report.reporterEmail.ifBlank { "anonymous" }} • ${formatMillis(report.createdAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (report.status == "PENDING") {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { viewModel.loadReportPreview(report) }) {
                                        Text("Preview")
                                    }
                                    TextButton(onClick = { viewModel.moderateReport(report, "HIDE") }) {
                                        Text("Hide")
                                    }
                                    TextButton(onClick = { viewModel.moderateReport(report, "DELETE") }) {
                                        Text("Delete")
                                    }
                                    TextButton(onClick = { viewModel.moderateReport(report, "DISMISS") }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    uiState.reportPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::clearReportPreview,
            title = { Text(preview.title) },
            text = { Text(preview.body) },
            confirmButton = {
                TextButton(onClick = viewModel::clearReportPreview) { Text("Close") }
            }
        )
    }
}

// =========================================================================
// SETTINGS
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminSettingsScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var draft by remember(uiState.settings) { mutableStateOf(uiState.settings) }
    var confirmMaintenance by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Platform controls") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Changes apply to every user on the next sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { SettingsGroupTitle("Availability") }
            item {
                ToggleRow(
                    title = "Registration enabled",
                    subtitle = "Allow new students to sign up",
                    checked = draft.registrationEnabled,
                    onChange = { draft = draft.copy(registrationEnabled = it) }
                )
            }
            item {
                ToggleRow(
                    title = "Resource uploads",
                    subtitle = "Allow CRs and moderators to upload files",
                    checked = draft.resourceUploadsEnabled,
                    onChange = { draft = draft.copy(resourceUploadsEnabled = it) }
                )
            }

            item { SettingsGroupTitle("Features") }
            item {
                ToggleRow(
                    title = "Notices",
                    checked = draft.featureNotices,
                    onChange = { draft = draft.copy(featureNotices = it) }
                )
            }
            item {
                ToggleRow(
                    title = "Study resources",
                    checked = draft.featureResources,
                    onChange = { draft = draft.copy(featureResources = it) }
                )
            }
            item {
                ToggleRow(
                    title = "Assignments",
                    checked = draft.featureAssignments,
                    onChange = { draft = draft.copy(featureAssignments = it) }
                )
            }
            item {
                ToggleRow(
                    title = "Events & clubs",
                    checked = draft.featureEvents,
                    onChange = { draft = draft.copy(featureEvents = it) }
                )
            }
            item {
                ToggleRow(
                    title = "Attendance",
                    checked = draft.featureAttendance,
                    onChange = { draft = draft.copy(featureAttendance = it) }
                )
            }

            item { SettingsGroupTitle("Announcement banner") }
            item {
                OutlinedTextField(
                    value = draft.announcementBanner,
                    onValueChange = { draft = draft.copy(announcementBanner = it) },
                    label = { Text("Banner text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ToggleRow(
                    title = "Show banner",
                    checked = draft.announcementEnabled,
                    onChange = { draft = draft.copy(announcementEnabled = it) }
                )
            }

            item { SettingsGroupTitle("Maintenance") }
            item {
                ToggleRow(
                    title = "Maintenance mode",
                    subtitle = "Blocks writes platform-wide",
                    checked = draft.maintenanceMode,
                    onChange = { request ->
                        if (request) confirmMaintenance = true
                        else draft = draft.copy(maintenanceMode = false)
                    }
                )
            }
            item {
                OutlinedTextField(
                    value = draft.maintenanceMessage,
                    onValueChange = { draft = draft.copy(maintenanceMessage = it) },
                    label = { Text("Maintenance message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { draft = uiState.settings },
                        modifier = Modifier.weight(1f)
                    ) { Text("Discard") }
                    TextButton(
                        onClick = { viewModel.saveSettings(draft) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save changes") }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (confirmMaintenance) {
        AlertDialog(
            onDismissRequest = { confirmMaintenance = false },
            title = { Text("Enable maintenance mode?") },
            text = {
                Text(
                    "Maintenance mode blocks writes for every user, including you. " +
                        "You can turn it off again from this panel."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    draft = draft.copy(maintenanceMode = true)
                    confirmMaintenance = false
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMaintenance = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = extendedColors.brandForeground
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

// =========================================================================
// AUDIT
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminAuditScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Append-only. Entries cannot be edited or deleted from the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.auditLogs.isEmpty()) {
                item {
                    Text(
                        text = "No administrative actions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.auditLogs, key = { it.id }) { entry ->
                AuditRow(entry)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AuditRow(entry: AuditEntry) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.action,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.result,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.result == "SUCCESS") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                text = "${entry.actorEmail.ifBlank { "system" }} • ${formatMillis(entry.createdAtMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.targetType.isNotBlank() || entry.targetId.isNotBlank()) {
                Text(
                    text = "Target: ${entry.targetType} ${entry.targetId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.detail.isNotBlank()) {
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================================
// ACADEMIC STRUCTURE
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminAcademicScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var showSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectName by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("") }
    var credits by remember { mutableIntStateOf(4) }
    var isElective by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academic structure") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                SectionHeader("Subjects", "Courses offered in this section")
            }
            item {
                if (uiState.subjects.isEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
                            )
                            Text(
                                text = "No subjects yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add subjects to build the academic catalogue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    uiState.subjects.forEach { subject ->
                        SubjectRow(
                            subject = subject,
                            onEdit = {
                                editingSubject = subject
                                subjectName = subject.subjectName
                                subjectCode = subject.subjectCode
                                credits = subject.credits
                                isElective = subject.isElective
                                showSubjectDialog = true
                            },
                            onDelete = { viewModel.deleteSubject(subject.subjectId) }
                        )
                    }
                }
            }

            item {
                SectionHeader("Timetable", "Weekly class schedule by day and time")
            }
            item {
                if (uiState.timetableSlots.isEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
                            )
                            Text(
                                text = "No timetable slots yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add timetable slots to publish the weekly schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    uiState.timetableSlots.forEach { slot ->
                        TimetableRow(
                            slot = slot,
                            subjects = uiState.subjects
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    FloatingActionButton(
        onClick = {
            editingSubject = null
            subjectName = ""
            subjectCode = ""
            credits = 4
            isElective = false
            showSubjectDialog = true
        }
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add subject")
    }

    if (showSubjectDialog) {
        SubjectDialog(
            subjectName = subjectName,
            subjectCode = subjectCode,
            credits = credits,
            isElective = isElective,
            onSubjectNameChange = { subjectName = it },
            onSubjectCodeChange = { subjectCode = it },
            onCreditsChange = { credits = it },
            onElectiveChange = { isElective = it },
            onDismiss = { showSubjectDialog = false },
            onSave = {
                val subject = SubjectEntity(
                    subjectId = editingSubject?.subjectId ?: subjectCode,
                    sectionKey = uiState.sectionKey,
                    subjectName = subjectName,
                    subjectCode = subjectCode,
                    teacherName = "",
                    isElective = isElective,
                    isEnrolled = true,
                    credits = credits
                )
                viewModel.saveSubject(subject)
                showSubjectDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = extendedColors.brandForeground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubjectRow(
    subject: SubjectEntity,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = subject.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subject.subjectCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${subject.credits} credits ${if (subject.isElective) "• Elective" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit subject")
                    }
                    IconButton(onClick = { onDelete(subject.subjectId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete subject")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableRow(
    slot: TimetableSlotEntity,
    subjects: List<SubjectEntity>
) {
    val subjectName = subjects.firstOrNull { it.subjectId == slot.subjectId }?.subjectName
        ?: slot.subjectName
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${slot.subjectCode} • Day ${slot.dayOfWeek} • ${slot.startTime}–${slot.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (slot.roomNumber.isNotBlank()) {
                        Text(
                            text = "Room ${slot.roomNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectDialog(
    subjectName: String,
    subjectCode: String,
    credits: Int,
    isElective: Boolean,
    onSubjectNameChange: (String) -> Unit,
    onSubjectCodeChange: (String) -> Unit,
    onCreditsChange: (Int) -> Unit,
    onElectiveChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                C1TextField(
                    value = subjectName,
                    onValueChange = onSubjectNameChange,
                    label = "Subject Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectCode,
                    onValueChange = onSubjectCodeChange,
                    label = "Subject Code",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = credits.toString(),
                        onValueChange = { onCreditsChange(it.toIntOrNull() ?: 4) },
                        label = "Credits",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.Checkbox(
                        checked = isElective,
                        onCheckedChange = onElectiveChange
                    )
                    Text("Elective", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = onSave,
                enabled = subjectName.isNotBlank() && subjectCode.isNotBlank()
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

@Composable
private fun NoticeRow(
    notice: NoticeEntity,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = notice.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${notice.category} • ${notice.priority} ${if (notice.isPinned) "• Pinned" else ""} ${if (notice.isOfficial) "• Official" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit notice")
                    }
                    IconButton(onClick = { onDelete(notice.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete notice")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeDialog(
    title: String,
    body: String,
    category: String,
    priority: String,
    isPinned: Boolean,
    isOfficial: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = category,
                        onValueChange = onCategoryChange,
                        label = "Category",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = priority,
                        onValueChange = onPriorityChange,
                        label = "Priority",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isPinned,
                        onCheckedChange = onPinnedChange
                    )
                    Text("Pin to top", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isOfficial,
                        onCheckedChange = onOfficialChange
                    )
                    Text("Official announcement", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Publish notice",
                onClick = onSave,
                enabled = title.isNotBlank() && body.isNotBlank()
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

@Composable
private fun ResourceRow(
    resource: StudyResourceEntity,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${resource.subjectName} • ${resource.category} • Sem ${resource.semester}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${resource.fileType} • ${resource.fileSizeBytes / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit resource")
                    }
                    IconButton(onClick = { onDelete(resource.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete resource")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceDialog(
    title: String,
    subjectId: String,
    subjectName: String,
    category: String,
    fileUrl: String,
    fileType: String,
    semester: Int,
    onTitleChange: (String) -> Unit,
    onSubjectIdChange: (String) -> Unit,
    onSubjectNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onFileUrlChange: (String) -> Unit,
    onFileTypeChange: (String) -> Unit,
    onSemesterChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (title.isNotBlank()) "Edit Resource" else "Add Resource") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                C1TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectName,
                    onValueChange = onSubjectNameChange,
                    label = "Subject Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectId,
                    onValueChange = onSubjectIdChange,
                    label = "Subject ID",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = category,
                        onValueChange = onCategoryChange,
                        label = "Category (NOTES/PYQ/ASSIGNMENT/OTHER)",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = semester.toString(),
                        onValueChange = { onSemesterChange(it.toIntOrNull() ?: 1) },
                        label = "Semester",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                C1TextField(
                    value = fileUrl,
                    onValueChange = onFileUrlChange,
                    label = "File URL",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = fileType,
                        onValueChange = onFileTypeChange,
                        label = "File Type (PDF/IMAGE)",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Save resource",
                onClick = onSave,
                enabled = title.isNotBlank() && subjectId.isNotBlank() && fileUrl.isNotBlank()
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

@Composable
private fun AssignmentRow(
    assignment: AssignmentEntity,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${assignment.subjectTag} • ${assignment.priority} • Due ${assignment.dueDateFormatted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (assignment.description.isNotBlank()) {
                        Text(
                            text = assignment.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit assignment")
                    }
                    IconButton(onClick = { onDelete(assignment.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete assignment")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamRow(
    exam: ExamEntity,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exam.examTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exam.subjectName} (${exam.subjectCode}) • ${exam.examDateFormatted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${exam.examTimeFormatted} • ${exam.roomOrCenter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit exam")
                    }
                    IconButton(onClick = { onDelete(exam.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete exam")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusRow(
    topic: SyllabusTopicEntity,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = topic.topicTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${topic.subjectName} • Unit ${topic.unitNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit syllabus topic")
                    }
                    IconButton(onClick = { onDelete(topic.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete syllabus topic")
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentDialog(
    title: String,
    subjectId: String,
    subjectTag: String,
    description: String,
    priority: String,
    dueDate: String,
    onTitleChange: (String) -> Unit,
    onSubjectIdChange: (String) -> Unit,
    onSubjectTagChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (title.isNotBlank()) "Edit Assignment" else "Add Assignment") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                C1TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectTag,
                    onValueChange = onSubjectTagChange,
                    label = "Subject Tag",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectId,
                    onValueChange = onSubjectIdChange,
                    label = "Subject ID",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = "Description",
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = priority,
                        onValueChange = onPriorityChange,
                        label = "Priority (HIGH/MEDIUM/LOW)",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = dueDate,
                        onValueChange = onDueDateChange,
                        label = "Due Date",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Save assignment",
                onClick = onSave,
                enabled = title.isNotBlank() && subjectId.isNotBlank()
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

@Composable
private fun ExamDialog(
    title: String,
    subjectName: String,
    subjectCode: String,
    date: String,
    time: String,
    room: String,
    onTitleChange: (String) -> Unit,
    onSubjectNameChange: (String) -> Unit,
    onSubjectCodeChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onRoomChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (title.isNotBlank()) "Edit Exam" else "Add Exam") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                C1TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "Exam Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectName,
                    onValueChange = onSubjectNameChange,
                    label = "Subject Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectCode,
                    onValueChange = onSubjectCodeChange,
                    label = "Subject Code",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = date,
                        onValueChange = onDateChange,
                        label = "Date",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = time,
                        onValueChange = onTimeChange,
                        label = "Time",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                C1TextField(
                    value = room,
                    onValueChange = onRoomChange,
                    label = "Room / Center",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save exam",
                onClick = onSave,
                enabled = title.isNotBlank() && subjectName.isNotBlank()
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

@Composable
private fun SyllabusDialog(
    topicTitle: String,
    subjectName: String,
    unitNumber: Int,
    onTopicTitleChange: (String) -> Unit,
    onSubjectNameChange: (String) -> Unit,
    onUnitNumberChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (topicTitle.isNotBlank()) "Edit Syllabus Topic" else "Add Syllabus Topic") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                C1TextField(
                    value = topicTitle,
                    onValueChange = onTopicTitleChange,
                    label = "Topic Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = subjectName,
                    onValueChange = onSubjectNameChange,
                    label = "Subject Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = unitNumber.toString(),
                    onValueChange = { onUnitNumberChange(it.toIntOrNull() ?: 1) },
                    label = "Unit Number",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save topic",
                onClick = onSave,
                enabled = topicTitle.isNotBlank() && subjectName.isNotBlank()
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