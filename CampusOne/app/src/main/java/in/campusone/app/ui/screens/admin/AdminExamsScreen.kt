package `in`.campusone.app.ui.screens.admin

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
import androidx.compose.material.icons.filled.CalendarMonth
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
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.ui.components.C1AppBar
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1ElevatedCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
            C1AppBar(
                title = "Manage Exams",
                onBack = onBack
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
                Icon(Icons.Default.Add, contentDescription = "Add Exam")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = C1Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            items(uiState.exams, key = { it.id }) { exam ->
                C1ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingExam = exam
                        subjectCode = exam.subjectCode
                        subjectName = exam.subjectName
                        examTitle = exam.examTitle
                        examDate = exam.examDateIso
                        examTime = exam.examTimeFormatted
                        venue = exam.roomOrCenter
                        showDialog = true
                    }
                ) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "${exam.subjectCode} • ${exam.subjectName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = exam.examTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${exam.examDateFormatted} • ${exam.roomOrCenter}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
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
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteExam(exam.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
        title = { Text("Exam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                C1TextField(
                    value = subjectCode,
                    onValueChange = onSubjectCodeChange,
                    label = "Subject Code",
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
                    value = examTitle,
                    onValueChange = onExamTitleChange,
                    label = "Exam Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = examDate,
                    onValueChange = onExamDateChange,
                    label = "Date (YYYY-MM-DD)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = examTime,
                    onValueChange = onExamTimeChange,
                    label = "Time",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                C1TextField(
                    value = venue,
                    onValueChange = onVenueChange,
                    label = "Venue / Hall",
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
                enabled = subjectCode.isNotBlank() && subjectName.isNotBlank(),
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
