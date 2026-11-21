package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.theme.C1Spacing
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
    val vm = viewModel

    var showUploadDialog by rememberSaveable { mutableStateOf(false) }
    var csvText by rememberSaveable { mutableStateOf("") }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var historyImports by rememberSaveable { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var historyLoading by rememberSaveable { mutableStateOf(false) }

    val profile = uiState.profile
    val courseCode = profile.courseId
    val courseName = profile.courseName
    val semester = profile.semester
    val section = profile.section
    val academicYear = "${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}-${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1}"

    fun publishCsv() {
        if (csvText.isBlank()) return
        val result = TimetableCsvParser.parse(csvText)
        if (result.errors.isNotEmpty()) {
            vm.emitMessage("Validation errors: ${result.errors.joinToString(", ")}")
            return
        }
        val rows = result.rows.map { it.toRemoteMap() }
        vm.publishTimetable(courseCode, courseName, semester, section, academicYear, "manual_entry.csv", rows)
        showUploadDialog = false
        csvText = ""
    }

    fun loadHistory() {
        historyLoading = true
        vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.viewModelScope.launch {
            val result = vm.repository.listTimetableImports()
            historyImports = if (result.isSuccess) result.getOrNull() ?: emptyList() else emptyList()
            historyLoading = false
        }
        showHistoryDialog = true
    }

    fun deleteImport(importId: String) {
        vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.viewModelScope.launch {
            vm.deleteTimetableImport(importId)
            historyImports = historyImports.filterNot { it["id"] == importId }
        }
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
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(C1Spacing.sm))
                            Text("Upload CSV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.xs))
                        Text(
                            "Paste CSV content: Subject Code, Subject Name, Teacher, Room, Day (1-7), Start (HH:mm), End (HH:mm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(C1Spacing.md))
                        C1Button(
                            text = "Open CSV Editor",
                            onClick = { showUploadDialog = true },
                            intent = C1ButtonIntent.PRIMARY,
                            fullWidth = true,
                            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
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
                            onClick = { loadHistory() },
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
                                        IconButton(onClick = { vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.vm.viewModelScope.launch { vm.repository.deleteTimetableSlot(slot.id, uiState.sectionKey) } }) {
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

    // Upload Dialog - simplified without AlertDialog
    if (showUploadDialog) {
        C1SurfaceCard(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .height(400.dp)
                    .padding(C1Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                Text("Paste CSV Content", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                C1TextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = "CSV Content",
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Format: subject_code,subject_name,teacher,room,day,start,end", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Example: SOET-CS-301,Data Structures,Dr. Smith,Room 101,1,09:00,10:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                    C1Button(text = "Cancel", onClick = { showUploadDialog = false }, intent = C1ButtonIntent.GHOST)
                    C1Button(text = "Publish", onClick = { publishCsv() }, enabled = csvText.isNotBlank(), intent = C1ButtonIntent.PRIMARY)
                }
            }
        }
    }

    // History Dialog - simplified without AlertDialog
    if (showHistoryDialog) {
        C1SurfaceCard(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .height(500.dp)
                    .padding(C1Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                Text("Import History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (historyLoading) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(C1Spacing.xxxl))
                } else if (historyImports.isEmpty()) {
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
                        items(historyImports, key = { it["id"] as String }) { imp ->
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
                                        IconButton(onClick = { deleteImport(imp["id"] as String) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                C1Button(text = "Close", onClick = { showHistoryDialog = false }, intent = C1ButtonIntent.GHOST)
            }
        }
    }
}
