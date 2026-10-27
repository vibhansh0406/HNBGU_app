package `in`.campusone.app.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AssignmentFilter
import `in`.campusone.app.domain.model.GradingScale
import `in`.campusone.app.domain.model.MaterialType
import `in`.campusone.app.domain.model.PriorityLevel
import `in`.campusone.app.domain.model.StudyMaterialItem
import `in`.campusone.app.domain.usecase.GpaCalculator
import `in`.campusone.app.ui.viewmodel.StudyAndCampusUiState
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import `in`.campusone.app.util.CampusIntentHelper
import `in`.campusone.app.util.StudyFileAndPdfHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun StudyHubScreen(
    state: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.study_tab_assignments),
        stringResource(R.string.study_tab_exams),
        stringResource(R.string.study_tab_gpa),
        stringResource(R.string.study_tab_notes)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        state.statusBannerMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { viewModel.clearBanner() }) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> AssignmentsSubScreen(state, viewModel)
            1 -> ExamsAndSyllabusSubScreen(state, viewModel)
            2 -> GpaCalculatorSubScreen(state, viewModel)
            3 -> StudyVaultSubScreen(state, viewModel)
        }
    }
}

@Composable
private fun AssignmentsSubScreen(
    state: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(AssignmentFilter.UPCOMING) }
    var showAdd by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val endOfToday = now + 86_400_000L

    val filtered = remember(state.assignments, filter) {
        state.assignments.filter { item ->
            when (filter) {
                AssignmentFilter.TODAY -> !item.isCompleted && item.dueEpochMillis <= endOfToday
                AssignmentFilter.UPCOMING -> !item.isCompleted
                AssignmentFilter.OVERDUE -> !item.isCompleted && item.dueEpochMillis < now
                AssignmentFilter.COMPLETED -> item.isCompleted
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterChip(
                        selected = filter == AssignmentFilter.UPCOMING,
                        onClick = { filter = AssignmentFilter.UPCOMING },
                        label = { Text(stringResource(R.string.assignments_filter_upcoming)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == AssignmentFilter.TODAY,
                        onClick = { filter = AssignmentFilter.TODAY },
                        label = { Text(stringResource(R.string.assignments_filter_today)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == AssignmentFilter.OVERDUE,
                        onClick = { filter = AssignmentFilter.OVERDUE },
                        label = { Text(stringResource(R.string.assignments_filter_overdue)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == AssignmentFilter.COMPLETED,
                        onClick = { filter = AssignmentFilter.COMPLETED },
                        label = { Text(stringResource(R.string.assignments_filter_completed)) }
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.assignments_empty),
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { checked ->
                                    viewModel.toggleAssignment(item.id, checked)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${item.subjectName} • Priority: ${item.priority.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.notes.isNotBlank()) {
                                    Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteAssignment(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("Design & Analysis of Algorithms") }
        var daysText by remember { mutableStateOf("3") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.assignments_add_btn)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(stringResource(R.string.attendance_subject_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter { c -> c.isDigit() } },
                        label = { Text("Due in days") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val days = daysText.toIntOrNull()?.coerceIn(0, 120) ?: 3
                    viewModel.addAssignment(context, title, subject, days, PriorityLevel.HIGH, notes)
                    showAdd = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ExamsAndSyllabusSubScreen(
    state: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var showAddExam by remember { mutableStateOf(false) }
    var showAddTopic by remember { mutableStateOf(false) }

    val completedTopics = state.syllabusTopics.count { it.isCompleted }
    val totalTopics = state.syllabusTopics.size
    val progressPct = if (totalTopics > 0) (completedTopics * 100) / totalTopics else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { showAddExam = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.exams_add_exam))
                }
                OutlinedButton(onClick = { showAddTopic = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.exams_add_topic))
                }
            }
        }

        items(state.exams, key = { it.id }) { exam ->
            val examDate = Instant.ofEpochMilli(exam.examEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val daysLeft = ChronoUnit.DAYS.between(today, examDate).toInt().coerceAtLeast(0)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${exam.subjectCode} • ${exam.subjectName}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${exam.examTitle} • ${exam.timeSlot}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = exam.venue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AssistChip(
                            onClick = {
                                CampusIntentHelper.addExamOrEventToCalendar(
                                    context = context,
                                    title = "${exam.subjectCode}: ${exam.subjectName}",
                                    location = exam.venue,
                                    description = exam.notes,
                                    startEpochMillis = exam.examEpochMillis
                                )
                            },
                            label = {
                                Text(
                                    if (daysLeft == 0) stringResource(R.string.exams_today_badge)
                                    else stringResource(R.string.exams_countdown_days, daysLeft)
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(
                            R.string.exams_syllabus_progress,
                            progressPct,
                            completedTopics,
                            totalTopics
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(state.syllabusTopics, key = { it.id }) { topic ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = topic.isCompleted,
                        onCheckedChange = { checked ->
                            viewModel.toggleSyllabusTopic(topic.id, checked)
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unit ${topic.unitNumber}: ${topic.topicTitle}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = topic.subjectName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddExam) {
        var subject by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("CSE-405") }
        var daysText by remember { mutableStateOf("10") }
        var venue by remember { mutableStateOf("Exam Hall A") }

        AlertDialog(
            onDismissRequest = { showAddExam = false },
            title = { Text(stringResource(R.string.exams_add_exam)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.attendance_subject_name)) }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text(stringResource(R.string.attendance_subject_code)) }, singleLine = true)
                    OutlinedTextField(value = daysText, onValueChange = { daysText = it.filter { c -> c.isDigit() } }, label = { Text("Days from today") }, singleLine = true)
                    OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addExam(subject, code, "Semester Exam", daysText.toIntOrNull() ?: 10, "10:00 AM - 01:00 PM", venue, "")
                    showAddExam = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddExam = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showAddTopic) {
        var subject by remember { mutableStateOf("Design & Analysis of Algorithms") }
        var unitText by remember { mutableStateOf("1") }
        var topicTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTopic = false },
            title = { Text(stringResource(R.string.exams_add_topic)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.attendance_subject_name)) }, singleLine = true)
                    OutlinedTextField(value = unitText, onValueChange = { unitText = it.filter { c -> c.isDigit() } }, label = { Text("Unit #") }, singleLine = true)
                    OutlinedTextField(value = topicTitle, onValueChange = { topicTitle = it }, label = { Text("Topics") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addSyllabusTopic(subject, unitText.toIntOrNull() ?: 1, topicTitle)
                    showAddTopic = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopic = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun GpaCalculatorSubScreen(
    state: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var showAddGrade by remember { mutableStateOf(false) }
    var targetCgpaText by rememberSaveable { mutableStateOf("8.8") }
    var nextSemCreditsText by rememberSaveable { mutableStateOf("20") }

    val completedCredits = state.semesterSummaries.sumOf { it.totalCredits }
    val targetCgpa = targetCgpaText.toDoubleOrNull() ?: 8.8
    val nextCredits = nextSemCreditsText.toIntOrNull() ?: 20
    val requiredSgpa = GpaCalculator.requiredSgpaForTargetCgpa(
        currentCgpa = state.cumulativeCgpa,
        completedCredits = completedCredits,
        nextSemesterCredits = nextCredits,
        targetCgpa = targetCgpa
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GradingScale.entries) { scale ->
                    FilterChip(
                        selected = state.gradingScale == scale,
                        onClick = { viewModel.setGradingScale(scale) },
                        label = { Text(scale.displayName) }
                    )
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_cgpa_result, state.cumulativeCgpa),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.semesterSummaries.isNotEmpty()) {
                        SgpaTrendChart(
                            sgpaPoints = state.semesterSummaries.map { it.sgpa.toFloat() },
                            maxScale = state.gradingScale.maxPoints.toFloat()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showAddGrade = true }, modifier = Modifier.weight(1f)) {
                            Text("Add Subject Grade")
                        }
                        OutlinedButton(onClick = { viewModel.exportGpaPdf(context) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gpa_export_pdf))
                        }
                    }
                }
            }
        }

        // What Do I Need Calculator Card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_what_if_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetCgpaText,
                            onValueChange = { targetCgpaText = it },
                            label = { Text("Target CGPA") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = nextSemCreditsText,
                            onValueChange = { nextSemCreditsText = it.filter { c -> c.isDigit() } },
                            label = { Text("Next Sem Credits") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Required Next Semester SGPA: ${String.format("%.2f", requiredSgpa)} / ${state.gradingScale.maxPoints}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        items(state.semesterSummaries, key = { it.semester }) { summary ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Semester ${summary.semester} • ${stringResource(R.string.gpa_sgpa_result, summary.sgpa)} (${summary.totalCredits} Cr)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    summary.courses.forEach { course ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${course.subjectName} (${course.credits} Cr)", modifier = Modifier.weight(1f))
                            Text(course.gradePointOrMarks.toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteCourseGrade(course.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGrade) {
        var semText by remember { mutableStateOf("4") }
        var subject by remember { mutableStateOf("") }
        var creditsText by remember { mutableStateOf("4") }
        var gpText by remember { mutableStateOf("9.0") }

        AlertDialog(
            onDismissRequest = { showAddGrade = false },
            title = { Text("Add Subject Grade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = semText, onValueChange = { semText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.setup_semester_label)) }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.attendance_subject_name)) }, singleLine = true)
                    OutlinedTextField(value = creditsText, onValueChange = { creditsText = it.filter { c -> c.isDigit() } }, label = { Text("Credits") }, singleLine = true)
                    OutlinedTextField(value = gpText, onValueChange = { gpText = it }, label = { Text("Grade Point / Marks") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCourseGrade(
                        semester = semText.toIntOrNull() ?: 4,
                        subjectName = subject,
                        credits = creditsText.toIntOrNull() ?: 4,
                        gradePointOrMarks = gpText.toDoubleOrNull() ?: 8.5
                    )
                    showAddGrade = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddGrade = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun SgpaTrendChart(
    sgpaPoints: List<Float>,
    maxScale: Float
) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(vertical = 8.dp)
    ) {
        if (sgpaPoints.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = if (sgpaPoints.size > 1) w / (sgpaPoints.size - 1) else w / 2f
        val points = sgpaPoints.mapIndexed { idx, value ->
            val ratio = (value / maxScale.coerceAtLeast(1f)).coerceIn(0f, 1f)
            Offset(x = idx * stepX, y = h - (ratio * h))
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 6f
            )
        }
        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 9f, center = pt)
        }
    }
}

@Composable
private fun StudyVaultSubScreen(
    state: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var bookmarkedOnly by rememberSaveable { mutableStateOf(false) }
    var activeViewingItem by remember { mutableStateOf<StudyMaterialItem?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importStudyMaterialFromUri(
                context = context,
                uri = uri,
                subjectName = "Computer Science",
                semester = 4,
                type = MaterialType.NOTES
            )
        }
    }

    val filtered = remember(state.studyMaterials, searchQuery, bookmarkedOnly) {
        state.studyMaterials.filter { item ->
            val matchesBookmark = !bookmarkedOnly || item.isBookmarked
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subjectName.contains(searchQuery, ignoreCase = true)
            matchesBookmark && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.notes_import_btn))
            }
            FilterChip(
                selected = bookmarkedOnly,
                onClick = { bookmarkedOnly = !bookmarkedOnly },
                label = { Text(stringResource(R.string.notes_bookmark_filter)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.common_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.notes_empty),
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { mat ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mat.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${mat.subjectName} • Sem ${mat.semester} • ${mat.type.name} (${mat.fileSizeBytes / 1024} KB)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleBookmarkMaterial(mat.id, !mat.isBookmarked) }
                                ) {
                                    Icon(
                                        imageVector = if (mat.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteStudyMaterial(mat.id) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { activeViewingItem = mat },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.notes_open_viewer))
                            }
                        }
                    }
                }
            }
        }
    }

    activeViewingItem?.let { item ->
        InAppPdfOrImageReaderDialog(
            item = item,
            onDismiss = { activeViewingItem = null }
        )
    }
}

@Composable
private fun InAppPdfOrImageReaderDialog(
    item: StudyMaterialItem,
    onDismiss: () -> Unit
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(item.localFilePath, pageIndex) {
        if (item.mimeType.contains("pdf", ignoreCase = true) || item.localFilePath.endsWith(".pdf", ignoreCase = true)) {
            StudyFileAndPdfHelper.renderPdfPage(item.localFilePath, pageIndex)
                .onSuccess { (bmp, total) ->
                    renderedBitmap = bmp
                    pageCount = total
                    errorMessage = null
                }
                .onFailure { err ->
                    errorMessage = err.message ?: "Unable to render PDF page."
                }
        } else {
            runCatching {
                android.graphics.BitmapFactory.decodeFile(item.localFilePath)
            }.onSuccess { bmp ->
                renderedBitmap = bmp
                pageCount = 1
            }.onFailure { err ->
                errorMessage = err.message
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.title} (Page ${pageIndex + 1}/$pageCount)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = Color.Red)
                } else if (renderedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 3.5f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = renderedBitmap!!.asImageBitmap(),
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    }
                } else {
                    Text("Rendering document page...")
                }

                if (pageCount > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            enabled = pageIndex > 0,
                            onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }
                        ) { Text("Previous") }
                        TextButton(
                            enabled = pageIndex < pageCount - 1,
                            onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) }
                        ) { Text("Next") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}
