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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.repository.AdminUserRecord
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.data.repository.RoleRequestRecord
import `in`.campusone.app.data.repository.UserAttendanceRow
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

private val assignableRoles = listOf(
    UserRole.STUDENT,
    UserRole.CR,
    UserRole.TEACHER,
    UserRole.MODERATOR,
    UserRole.CLUB_ADMIN
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedUid by rememberSaveable { mutableStateOf<String?>(null) }
    var removeUid by rememberSaveable { mutableStateOf<String?>(null) }

    val pendingRequests = uiState.roleRequests.filter { it.status == "PENDING" }
    val filteredUsers = uiState.users.filter { user ->
        query.isBlank() ||
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users & Roles") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Total users", uiState.users.size.toString(), Modifier.weight(1f))
                    StatTile(
                        "Staff",
                        uiState.users.count { it.role != UserRole.STUDENT }.toString(),
                        Modifier.weight(1f)
                    )
                    StatTile("Requests", pendingRequests.size.toString(), Modifier.weight(1f))
                }
            }

            if (pendingRequests.isNotEmpty()) {
                item { SectionTitle("Pending role requests") }
                items(pendingRequests, key = { "request_${it.id}" }) { request ->
                    RoleRequestCard(
                        request = request,
                        onApprove = { viewModel.approveRequest(request) },
                        onReject = { viewModel.rejectRequest(request) }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by name or email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionTitle("All users (${uiState.users.size})") }

            items(filteredUsers, key = { it.uid }) { user ->
                UserCard(user = user, onClick = { selectedUid = user.uid })
            }

            if (uiState.users.isEmpty()) {
                item {
                    Text(
                        text = if (uiState.remoteLoading) "Loading users..." else "No users found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    val selectedUser = selectedUid?.let { uid -> uiState.users.firstOrNull { it.uid == uid } }
    if (selectedUser != null) {
        UserDialog(
            user = selectedUser,
            isSelf = selectedUser.uid == uiState.profile.uid,
            attendanceUserId = uiState.attendanceUserId,
            attendanceRows = uiState.selectedUserAttendance,
            onLoadAttendance = { viewModel.loadUserAttendance(selectedUser.uid) },
            onSaveRole = { role ->
                viewModel.changeUserRole(selectedUser.uid, role)
                selectedUid = null
            },
            onRemove = { removeUid = selectedUser.uid },
            onDismiss = { selectedUid = null }
        )
    }

    val removalTarget = removeUid?.let { uid -> uiState.users.firstOrNull { it.uid == uid } }
    if (removalTarget != null) {
        AlertDialog(
            onDismissRequest = { removeUid = null },
            title = { Text("Remove user?") },
            text = {
                Text(
                    "This deletes ${removalTarget.name.ifBlank { removalTarget.email }} from the campus database. " +
                        "Their app data stays on their device until they sign out."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeUser(removalTarget.uid)
                    removeUid = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removeUid = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onApprove) { Text("Approve") }
                TextButton(onClick = onReject) { Text("Reject") }
            }
        }
    }
}

@Composable
private fun UserCard(user: AdminUserRecord, onClick: () -> Unit) {
    val isOwner = user.email.equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true)
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
            Text(
                text = if (isOwner) "OWNER" else user.role.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserDialog(
    user: AdminUserRecord,
    isSelf: Boolean,
    attendanceUserId: String,
    attendanceRows: List<UserAttendanceRow>,
    onLoadAttendance: () -> Unit,
    onSaveRole: (UserRole) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOwner = user.email.equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true)
    val locked = isOwner || isSelf
    var selectedRole by remember(user.uid) { mutableStateOf(user.role) }

    LaunchedEffect(user.uid) { onLoadAttendance() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(user.name.ifBlank { user.email }) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
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

                HorizontalDivider()

                Text(text = "Role", style = MaterialTheme.typography.labelLarge)
                if (locked) {
                    Text(
                        text = if (isOwner) "Owner account — always ADMIN" else "You cannot change your own role",
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
                if (attendanceUserId != user.uid) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (attendanceRows.isEmpty()) {
                    Text(
                        text = "No attendance records yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    attendanceRows.forEach { row ->
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

                if (!locked) {
                    HorizontalDivider()
                    TextButton(onClick = onRemove) { Text("Remove user") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSaveRole(selectedRole) },
                enabled = !locked && selectedRole != user.role
            ) { Text("Save role") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
