package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubjectsScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingSubject by rememberSaveable { mutableStateOf<SubjectEntity?>(null) }
    var subjectName by rememberSaveable { mutableStateOf("") }
    var subjectCode by rememberSaveable { mutableStateOf("") }
    var credits by rememberSaveable { mutableIntStateOf(4) }
    var isElective by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = {
                editingSubject = null
                subjectName = ""
                subjectCode = ""
                credits = 4
                isElective = false
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.subjects, key = { it.subjectId }) { subject ->
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = subject.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = subject.subjectCode, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${subject.credits} credits ${if (subject.isElective) "• Elective" else ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    editingSubject = subject
                                    subjectName = subject.subjectName
                                    subjectCode = subject.subjectCode
                                    credits = subject.credits
                                    isElective = subject.isElective
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteSubject(subject.subjectId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SubjectDialog(
            subjectName = subjectName,
            subjectCode = subjectCode,
            credits = credits,
            isElective = isElective,
            onSubjectNameChange = { subjectName = it },
            onSubjectCodeChange = { subjectCode = it },
            onCreditsChange = { credits = it },
            onElectiveChange = { isElective = it },
            onDismiss = { showDialog = false },
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
                showDialog = false
            }
        )
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
                C1TextField(value = subjectName, onValueChange = onSubjectNameChange, label = "Subject Name", singleLine = true, modifier = Modifier.fillMaxWidth())
                C1TextField(value = subjectCode, onValueChange = onSubjectCodeChange, label = "Subject Code", singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(value = credits.toString(), onValueChange = { onCreditsChange(it.toIntOrNull() ?: 4) }, label = "Credits", singleLine = true, modifier = Modifier.weight(1f))
                    Checkbox(checked = isElective, onCheckedChange = onElectiveChange)
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