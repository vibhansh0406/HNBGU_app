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
import androidx.compose.material3.Button
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
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.AssignmentEntity
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
            TopAppBar(
                title = { Text("Manage Assignments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.assignments, key = { it.id }) { assignment ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = assignment.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    isSharedByCrOrTeacher = false,
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
        title = { Text("Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = subjectTag, onValueChange = onSubjectTagChange, label = { Text("Subject Tag (e.g. OS • CS302)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = onDescriptionChange, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.DropdownMenu(
                        expanded = true,
                        onDismissRequest = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            androidx.compose.material3.DropdownMenuItem(
                                onClick = { onPriorityChange(p) },
                                text = { Text(p) }
                            )
                        }
                    }
                    Text("Priority: $priority", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(value = dueDate, onValueChange = onDueDateChange, label = { Text("Due Date (e.g. In 3 days • 05:00 PM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = title.isNotBlank() && subjectTag.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
