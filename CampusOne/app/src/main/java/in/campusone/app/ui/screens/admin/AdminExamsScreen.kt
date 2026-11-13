package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@Composable
fun AdminExamsScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingExam by rememberSaveable { mutableStateOf<ExamEntity?>(null) }
    var subjectCode by rememberSaveable { mutableStateOf("") }
    var subjectName by rememberSaveable { mutableStateOf("") }
    var examTitle by rememberSaveable { mutableStateOf("") }
    var examDate by rememberSaveable { mutableStateOf("") }
    var examTime by rememberSaveable { mutableStateOf("") }
    var venue by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Exams") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = {
                editingExam = null
                subjectCode = ""
                subjectName = ""
                examTitle = ""
                examDate = ""
                examTime = ""
                venue = ""
                showDialog = true
            }) {
                Icon(Add, contentDescription = "Add Exam")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.exams, key = { it.id }) { exam ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "${exam.subjectCode} • ${exam.subjectName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = exam.examTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${exam.examDateFormatted} • ${exam.roomOrCenter}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                editingExam = exam
                                subjectCode = exam.subjectCode
                                subjectName = exam.subjectName
                                examTitle = exam.examTitle
                                examDate = exam.examDateIso
                                examTime = exam.examTimeFormatted
                                venue = exam.roomOrCenter
                                showDialog = true
                            }) {
                                Icon(Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteExam(exam.id) }) {
                                Icon(Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ExamDialog(
            subjectCode = subjectCode,
            subjectName = subjectName,
            examTitle = examTitle,
            examDate = examDate,
            examTime = examTime,
            venue = venue,
            onSubjectCodeChange = { subjectCode = it },
            onSubjectNameChange = { subjectName = it },
            onExamTitleChange = { examTitle = it },
            onExamDateChange = { examDate = it },
            onExamTimeChange = { examTime = it },
            onVenueChange = { venue = it },
            onDismiss = { showDialog = false },
            onSave = {
                val exam = ExamEntity(
                    id = editingExam?.id ?: "exam_${System.currentTimeMillis()}",
                    sectionKey = uiState.sectionKey,
                    subjectId = subjectCode,
                    subjectName = subjectName,
                    subjectCode = subjectCode,
                    examTitle = examTitle,
                    examDateEpochMillis = System.currentTimeMillis(),
                    examDateIso = examDate.ifEmpty { "2026-11-14" },
                    examTimeFormatted = examTime.ifEmpty { "10:00 AM – 01:00 PM" },
                    examDateFormatted = "${examDate.ifEmpty { "TBD" }} • $examTime",
                    roomOrCenter = venue.ifEmpty { "TBD" },
                    reminderEnabled = true,
                    publishedByCrOrTeacher = true,
                    updatedAtMillis = System.currentTimeMillis()
                )
                viewModel.saveExam(exam)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ExamDialog(
    subjectCode: String,
    subjectName: String,
    examTitle: String,
    examDate: String,
    examTime: String,
    venue: String,
    onSubjectCodeChange: (String) -> Unit,
    onSubjectNameChange: (String) -> Unit,
    onExamTitleChange: (String) -> Unit,
    onExamDateChange: (String) -> Unit,
    onExamTimeChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = subjectCode, onValueChange = onSubjectCodeChange, label = { Text("Subject Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectName, onValueChange = onSubjectNameChange, label = { Text("Subject Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = examTitle, onValueChange = onExamTitleChange, label = { Text("Exam Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = examDate, onValueChange = onExamDateChange, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = examTime, onValueChange = onExamTimeChange, label = { Text("Time") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = venue, onValueChange = onVenueChange, label = { Text("Venue / Hall") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = subjectCode.isNotBlank() && subjectName.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
