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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.repository.AdminPermission
import `in`.campusone.app.data.repository.AdminUserRecord
import `in`.campusone.app.data.repository.AuditEntry
import `in`.campusone.app.data.repository.FlaggedItem
import `in`.campusone.app.data.repository.PlatformSettings
import `in`.campusone.app.data.repository.RoleRequestRecord
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val assignableRoles = listOf(
    UserRole.STUDENT,
    UserRole.CR,
    UserRole.TEACHER,
    UserRole.MODERATOR,
    UserRole.CLUB_ADMIN
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
                color = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.primary
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
            AdminPermission.MANAGE_EVENTS
        ),
        "TEACHER / CR" to listOf(
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
                color = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.primary
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
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
            color = MaterialTheme.colorScheme.primary
        )
    }
}

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
                color = MaterialTheme.colorScheme.primary,
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
@OptIn(ExperimentalMaterial3Api::class)
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
                    color = MaterialTheme.colorScheme.primary
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
                    onDelete = { viewModel.moderateFlagged(item, "DISMISS") }
                )
            }

            if (uiState.contentReports.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Submitted reports (${uiState.contentReports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                                color = MaterialTheme.colorScheme.primary,
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
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
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
                    subtitle = "Allow CRs and teachers to upload files",
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
        color = MaterialTheme.colorScheme.primary
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