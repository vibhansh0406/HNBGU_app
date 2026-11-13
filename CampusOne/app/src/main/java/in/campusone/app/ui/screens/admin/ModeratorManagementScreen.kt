package `in`.campusone.app.ui.screens.admin

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.ModeratorViewModel
import `in`.campusone.app.ui.viewmodel.ModeratorViewModel.ModeratorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeratorManagementScreen(
    uiState: ModeratorUiState,
    viewModel: ModeratorViewModel,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingUid by rememberSaveable { mutableStateOf<String?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var editEmail by rememberSaveable { mutableStateOf("") }
    var editScope by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Moderators") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.isAdmin) {
                androidx.compose.material3.FloatingActionButton(onClick = {
                    editingUid = null
                    editName = ""
                    editScope = ""
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Moderator")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = stringResource(R.string.moderator_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.moderator_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Info if not admin
            if (!uiState.isAdmin && !uiState.isActiveModerator) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.security_access_denied),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Approved moderators section
            val approved = viewModel.activeModerators()
            if (approved.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.moderator_approved),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(approved, key = { it.uid }) { mod ->
                        ModeratorRow(
                            mod = mod,
                            isAdmin = uiState.isAdmin,
                            onApproveChange = { approved ->
                                viewModel.setModeratorApproval(mod.uid, approved)
                            },
                            onScopeChange = { newScope ->
                                viewModel.updateModeratorScope(mod.uid, newScope)
                                editScope = newScope
                            },
                            onRevoke = { viewModel.revokeModerator(mod.uid) },
                            onEdit = {
                                editingUid = mod.uid
                                editName = mod.name
                                editScope = mod.moderatorScope
                                showDialog = true
                            }
                        )
                    }
                }
            }

            // Pending moderators section
            val pending = viewModel.pendingModerators()
            if (pending.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.moderator_pending),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pending, key = { it.uid }) { mod ->
                        ModeratorRow(
                            mod = mod,
                            isAdmin = uiState.isAdmin,
                            onApproveChange = { approved ->
                                viewModel.setModeratorApproval(mod.uid, approved)
                            },
                            onScopeChange = { newScope ->
                                viewModel.updateModeratorScope(mod.uid, newScope)
                            },
                            onRevoke = { viewModel.revokeModerator(mod.uid) },
                            onEdit = null
                        )
                    }
                }
            }

            if (approved.isEmpty() && pending.isEmpty()) {
                Text(
                    text = "No moderators yet. Appoint your first department moderator.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDialog) {
        ModeratorDialog(
            isAdmin = uiState.isAdmin,
            name = editName,
            email = editEmail,
            scope = editScope,
            onNameChange = { editName = it },
            onEmailChange = { editEmail = it },
            onScopeChange = { editScope = it },
            onDismiss = { showDialog = false; editingUid = null },
            onSave = {
                if (editingUid == null) {
                    viewModel.appointModerator(editName, editEmail, editScope)
                } else {
                    // For editing an existing moderator, update scope
                    val target = uiState.moderators.find { it.uid == editingUid }
                    if (target != null) {
                        viewModel.updateModeratorScope(editingUid ?: "", editScope)
                    }
                }
                showDialog = false
                editingUid = null
            }
        )
    }
}

@Composable
private fun ModeratorRow(
    mod: UserProfile,
    isAdmin: Boolean,
    onApproveChange: (Boolean) -> Unit,
    onScopeChange: (String) -> Unit,
    onRevoke: () -> Unit,
    onEdit: (() -> Unit)?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mod.name.ifEmpty { "Moderator" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = mod.email.ifEmpty { "no email" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stringResource(R.string.moderator_scope)}: ${mod.moderatorScope.ifEmpty { "—" }}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    if (isAdmin) {
                        Checkbox(
                            checked = mod.isModeratorApproved,
                            onCheckedChange = onApproveChange
                        )
                        if (mod.role == UserRole.MODERATOR) {
                            IconButton(onClick = onRevoke) {
                                Icon(Icons.Default.Delete, contentDescription = "Revoke moderator")
                            }
                        }
                        IconButton(onClick = { onEdit?.invoke() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (mod.isActiveModerator) Icons.Default.CheckCircle else Icons.Default.School,
                    contentDescription = null,
                    tint = if (mod.isActiveModerator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (mod.isActiveModerator) "Active • ${mod.moderatorScope}" else "Pending approval",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mod.isActiveModerator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModeratorDialog(
    isAdmin: Boolean,
    name: String,
    email: String,
    scope: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdmin) "Appoint Moderator" else "Moderator") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = scope,
                    onValueChange = onScopeChange,
                    label = { Text("Department / Course (e.g. BSc Life Science)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = name.isNotBlank() && email.isNotBlank() && scope.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
