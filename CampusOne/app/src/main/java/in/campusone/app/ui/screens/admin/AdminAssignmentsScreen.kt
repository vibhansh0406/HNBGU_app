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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.ui.components.C1AppBar
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1ElevatedCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAssignmentsScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingAssignment by rememberSaveable { mutableStateOf<AssignmentEntity?>(null) }
    var subjectTag by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("MEDIUM") }
    var dueDate by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            C1AppBar(
                title = "Manage Assignments",
                onBack = onBack
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = {
                editingAssignment = null
                subjectTag = ""
                title = ""
                description = ""
                priority = "MEDIUM"
                dueDate = ""
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Assignment")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = C1Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            items(uiState.assignments, key = { it.id }) { assignment ->
                C1ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingAssignment = assignment
                        subjectTag = assignment.subjectTag
                        title = assignment.title
                        description = assignment.description
                        priority = assignment.priority
                        dueDate = assignment.dueDateFormatted
                        showDialog = true
                    }
                ) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = assignment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "${assignment.subjectTag} • Due: ${assignment.dueDateFormatted}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = assignment.priority,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (assignment.priority == "HIGH") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (assignment.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(C1Spacing.xs))
                            Text(text = assignment.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                            IconButton(onClick = {
                                editingAssignment = assignment
                                subjectTag = assignment.subjectTag
                                title = assignment.title
                                description = assignment.description
                                priority = assignment.priority
                                dueDate = assignment.dueDateFormatted
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteAssignment(assignment.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AssignmentDialog(
            subjectTag = subjectTag,
            title = title,
            description = description,
            priority = priority,
            dueDate = dueDate,
            onSubjectTagChange = { subjectTag = it },
            onTitleChange = { title = it },
            onDescriptionChange = { description = it },
            onPriorityChange = { priority = it },
            onDueDateChange = { dueDate = it },
            onDismiss = { showDialog = false },
            onSave = {
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
                    isSharedByCrOrModerator = false,
                    isAddedToPersonalList = true,
                    authorName = uiState.profile.name,
                    isSynced = false,
                    updatedAtMillis = now
                )
                viewModel.saveAssignment(assignment)
                showDialog = false
            }
        )
    }
}

@Composable
private fun AssignmentDialog(
    subjectTag: String,
    title: String,
    description: String,
    priority: String,
    dueDate: String,
    onSubjectTagChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assignment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                C1TextField(
                    value = subjectTag,
                    onValueChange = onSubjectTagChange,
                    label = "Subject Tag (e.g. OS • CS302)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = "Description",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Priority: $priority", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            `in`.campusone.app.ui.components.C1FilterChip(
                                selected = priority == p,
                                onClick = { onPriorityChange(p) },
                                label = p
                            )
                        }
                    }
                }
                C1TextField(
                    value = dueDate,
                    onValueChange = onDueDateChange,
                    label = "Due Date (e.g. In 3 days • 05:00 PM)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            C1Button(
                text = "Save",
                onClick = onSave,
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
