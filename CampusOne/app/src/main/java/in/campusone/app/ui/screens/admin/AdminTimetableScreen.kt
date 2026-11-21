package `in`.campusone.app.ui.screens.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.C1Tone
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import `in`.campusone.app.util.TimetableCsvParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimetableScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showPreviewDialog by rememberSaveable { mutableStateOf(false) }
    var previewRows by rememberSaveable { mutableStateOf<List<TimetableCsvParser.ParsedRow>>(emptyList()) }
    var previewErrors by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var previewFileName by rememberSaveable { mutableStateOf("") }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var historyImports by rememberSaveable { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var historyLoading by rememberSaveable { mutableStateOf(false) }

    val pickCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { processCsvUri(it) }
    }

    val profile = uiState.profile
    val courseCode = profile.courseId
    val courseName = profile.courseName
    val semester = profile.semester
    val section = profile.section
    val academicYear = "${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}-${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1}"

    fun processCsvUri(uri: Uri) {
        viewModel.viewModelScope.launch {
            try {
                val content = uri?.let { LocalContext.current.contentResolver.openInputStream(it)?.use { it.readText() } }
                if (content.isNullOrBlank()) {
                    viewModel.emitMessage("Could not read file")
                    return@launch
                }
                val result = TimetableCsvParser.parse(content!!)
                previewRows = result.rows
                previewErrors = result.errors
                previewFileName = uri.lastPathSegment ?: "timetable.csv"
                showPreviewDialog = true
            } catch (e: Exception) {
                viewModel.emitMessage("Failed to parse CSV: ${e.message}")
            }
        }
    }

    fun publishPreview() {
        if (previewRows.isEmpty()) return
        val rows = previewRows.map { it.toRemoteMap() }
        viewModel.publishTimetable(courseCode, courseName, semester, section, academicYear, previewFileName, rows)
        showPreviewDialog = false
        previewRows = emptyList()
        previewErrors = emptyList()
    }

    fun loadHistory() {
        historyLoading = true
        viewModel.viewModelScope.launch {
            val result = viewModel.repository.listTimetableImports()
            historyImports = if (result.isSuccess) result.getOrNull() ?: emptyList() else emptyList()
            historyLoading = false
        }
        showHistoryDialog = true
    }

    fun deleteImport(importId: String) {
        viewModel.deleteTimetableImport(importId)
        historyImports = historyImports.filterNot { it["id"] == importId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(it),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.lg)
        ) {
            // Scope header
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(C1Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(C1Spacing.sm))
                        Column {
                            Text("Course Scope", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$courseName · Semester $semester · Section $section",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Action cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                C1SurfaceCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(C1Spacing.sm))
                            Text("Upload CSV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.xs))
                        Text(
                            "Import a timetable CSV for this course. Template: Subject Code, Subject Name, Teacher, Room, Day (1-7), Start (HH:mm), End (HH:mm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        C1Button(
                            text = "Choose CSV File",
                            onClick = { pickCsv.launch(arrayOf("text/csv", "application/csv")) },
                            intent = C1ButtonIntent.PRIMARY,
                            fullWidth = true,
                            icon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                        )
                    }
                }

                C1SurfaceCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(C1Spacing.sm))
                            Text("Import History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.xs))
                        Text(
                            "View previously published timetables and delete if needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        C1Button(
                            text = "View History",
                            onClick = loadHistory,
                            intent = C1ButtonIntent.TONAL,
                            fullWidth = true,
                            icon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                    }
                }
            }

            // Current local slots preview
            if (uiState.timetableSlots.isNotEmpty()) {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Current Local Slots", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${uiState.timetableSlots.size} slots", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.sm))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                        ) {
                            items(uiState.timetableSlots, key = { it.id }) { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = C1Spacing.xs, horizontal = C1Spacing.sm)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${slot.subjectName} (${slot.subjectCode})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("${slot.teacherName} · ${slot.roomNumber} · Day ${slot.dayOfWeek} · ${slot.startTime}–${slot.endTime}",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                        IconButton(onClick = { viewModel.repository.deleteTimetableSlot(slot.id, uiState.sectionKey) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    C1EmptyState(
                        icon = Icons.Default.Schedule,
                        title = "No local timetable",
                        message = "Upload a CSV to publish the timetable for this course."
                    )
                }
            }
        }
    }

    // Preview Dialog
    if (showPreviewDialog) {
        PreviewDialog(
            fileName = previewFileName,
            rows = previewRows,
            errors = previewErrors,
            onDismiss = { showPreviewDialog = false },
            onPublish = publishPreview
        )
    }

    // History Dialog
    if (showHistoryDialog) {
        HistoryDialog(
            imports = historyImports,
            loading = historyLoading,
            onDismiss = { showHistoryDialog = false },
            onDelete = deleteImport
        )
    }
}

@Composable
private fun PreviewDialog(
    fileName: String,
    rows: List<TimetableCsvParser.ParsedRow>,
    errors: List<String>,
    onDismiss: () -> Unit,
    onPublish: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview: $fileName") },
        text = {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                if (errors.isNotEmpty()) {
                    C1SurfaceCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Column(modifier = Modifier.padding(C1Spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(C1Spacing.sm))
                                Text("Validation Errors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(C1Spacing.xs))
                            errors.forEach { err ->
                                Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
                Text("Valid rows: ${rows.size}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                ) {
                    items(rows, key = { it.subjectCode + it.dayOfWeek + it.startTime }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(C1Spacing.sm)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${row.subjectName} (${row.subjectCode})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${row.teacherName} · ${row.roomNumber} · Day ${row.dayOfWeek} · ${row.startTime}–${row.endTime}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Publish",
                onClick = onPublish,
                enabled = rows.isNotEmpty() && errors.isEmpty(),
                intent = C1ButtonIntent.PRIMARY
            )
        },
        dismissButton = {
            C1Button(text = "Cancel", onClick = onDismiss, intent = C1ButtonIntent.GHOST)
        }
    )
}

@Composable
private fun HistoryDialog(
    imports: List<Map<String, Any>>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import History") },
        text = {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                if (loading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(C1Spacing.xxxl))
                } else if (imports.isEmpty()) {
                    C1EmptyState(
                        icon = Icons.Default.History,
                        title = "No imports yet",
                        message = "Published timetables will appear here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                    ) {
                        items(imports, key = { it["id"] as String }) { imp ->
                            C1SurfaceCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(C1Spacing.md)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text(imp["file_name"] as? String ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            Text(
                                                "Course: ${imp["course_code"]} · Sem ${imp["semester"]} · Sec ${imp["section"]} · ${imp["row_count"]} rows",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Published: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date((imp["created_at"] as? Number)?.toLong() ?: 0L))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDelete(imp["id"] as String) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            C1Button(text = "Close", onClick = onDismiss, intent = C1ButtonIntent.GHOST)
        }
    )
}
