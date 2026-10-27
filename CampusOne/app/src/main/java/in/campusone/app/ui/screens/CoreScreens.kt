package `in`.campusone.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AssignmentItem
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.ExamItem
import `in`.campusone.app.domain.model.SubjectSummary
import `in`.campusone.app.domain.usecase.TimetableEngine
import `in`.campusone.app.ui.viewmodel.CoreUiState
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun FirstLaunchSetupScreen(
    onCompleteSetup: (name: String, college: String, course: String, semester: Int, section: String, targetPct: Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var college by rememberSaveable { mutableStateOf("HNB Garhwal University") }
    var course by rememberSaveable { mutableStateOf("B.Tech CSE") }
    var semesterText by rememberSaveable { mutableStateOf("4") }
    var section by rememberSaveable { mutableStateOf("A") }
    var targetPct by rememberSaveable { mutableIntStateOf(75) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 32.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.setup_welcome_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.setup_welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.setup_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = college,
                    onValueChange = { college = it },
                    label = { Text(stringResource(R.string.setup_college_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text(stringResource(R.string.setup_course_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = semesterText,
                        onValueChange = { semesterText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.setup_semester_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text(stringResource(R.string.setup_section_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column {
                    Text(
                        text = "${stringResource(R.string.setup_target_attendance)}: $targetPct%",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = targetPct.toFloat(),
                        onValueChange = { targetPct = it.toInt() },
                        valueRange = 60f..90f,
                        steps = 5
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val sem = semesterText.toIntOrNull()?.coerceIn(1, 10) ?: 4
                        onCompleteSetup(name, college, course, sem, section, targetPct)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.setup_start_btn))
                }
            }
        }
    }
}

@Composable
fun HomeDashboardScreen(
    coreState: CoreUiState,
    upcomingAssignments: List<AssignmentItem>,
    upcomingExams: List<ExamItem>,
    onNavigateToTab: (String) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val today = remember { LocalDate.now() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.home_greeting,
                                coreState.profile.name.ifBlank { "Student" }
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${coreState.profile.course} • Sem ${coreState.profile.semester} (${coreState.profile.section})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = coreState.profile.college,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    AttendanceProgressRing(
                        percentage = coreState.overallPercentage,
                        targetPercentage = coreState.profile.targetAttendancePercent,
                        sizeDp = 76
                    )
                }
            }
        }

        // Next Class Card
        item {
            ElevatedCard(
                onClick = { onNavigateToTab("timetable") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_Next_class_card),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val nextSlot = coreState.nextClassToday
                    if (nextSlot != null) {
                        Text(
                            text = "${nextSlot.subjectCode} • ${nextSlot.subjectName}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${nextSlot.startTime} - ${nextSlot.endTime} • ${nextSlot.room} (${nextSlot.teacherName})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_classes_today),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Next 3 Deadlines
        item {
            ElevatedCard(
                onClick = { onNavigateToTab("study") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_upcoming_deadlines),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val topThree = upcomingAssignments.filter { !it.isCompleted }.take(3)
                    if (topThree.isEmpty()) {
                        Text(
                            text = stringResource(R.string.assignments_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topThree.forEach { item ->
                            val dueDate = Instant.ofEpochMilli(item.dueEpochMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.subjectName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AssistChip(
                                    onClick = { onNavigateToTab("study") },
                                    label = { Text(dueDate.format(dateFormatter)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Exam Countdowns
        item {
            ElevatedCard(
                onClick = { onNavigateToTab("study") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_upcoming_exams),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    upcomingExams.take(3).forEach { exam ->
                        val examDate = Instant.ofEpochMilli(exam.examEpochMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val daysLeft = ChronoUnit.DAYS.between(today, examDate).toInt().coerceAtLeast(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${exam.subjectCode} • ${exam.subjectName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${exam.timeSlot} • ${exam.venue}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { onNavigateToTab("study") },
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
        }
    }
}

@Composable
fun AttendanceProgressRing(
    percentage: Double,
    targetPercentage: Int,
    sizeDp: Int = 72
) {
    val animatedSweep by animateFloatAsState(
        targetValue = ((percentage.toFloat().coerceIn(0f, 100f)) / 100f) * 360f,
        label = "attendance_ring"
    )
    val ringColor = when {
        percentage >= targetPercentage -> Color(0xFF10B981)
        percentage >= targetPercentage - 10 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(sizeDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = ringColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AttendanceTrackerScreen(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.attendance_add_subject))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.attendance_disclaimer),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.attendance_target_chip,
                                coreState.profile.targetAttendancePercent
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(coreState.subjects, key = { it.id }) { subject ->
                SubjectAttendanceCard(
                    subject = subject,
                    onMark = { status -> viewModel.markAttendance(subject.id, status) }
                )
            }

            if (coreState.recentHistory.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.attendance_history),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(coreState.recentHistory.take(10), key = { it.id }) { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.subjectName, fontWeight = FontWeight.SemiBold)
                                Text(rec.dateLabel, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = rec.status.name,
                                fontWeight = FontWeight.Bold,
                                color = when (rec.status) {
                                    AttendanceStatus.PRESENT -> Color(0xFF10B981)
                                    AttendanceStatus.ABSENT -> Color(0xFFEF4444)
                                    AttendanceStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var prof by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.attendance_add_subject)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(stringResource(R.string.attendance_subject_code)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.attendance_subject_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = prof,
                        onValueChange = { prof = it },
                        label = { Text(stringResource(R.string.attendance_professor)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addSubject(code, name, prof, coreState.profile.semester)
                    showAddDialog = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject: SubjectSummary,
    onMark: (AttendanceStatus) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${subject.code} • ${subject.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${subject.professor} • ${subject.attendedCount}/${subject.totalClassesCount} classes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val advice = subject.advice
                    val adviceText = if (advice.totalConducted == 0) {
                        stringResource(R.string.attendance_no_classes_logged)
                    } else if (advice.isAboveTarget) {
                        stringResource(
                            R.string.attendance_safe_bunks,
                            advice.safeBunksAvailable,
                            advice.targetPercentage
                        )
                    } else {
                        stringResource(
                            R.string.attendance_must_attend,
                            advice.classesMustAttendToRecover,
                            advice.targetPercentage
                        )
                    }
                    Text(
                        text = adviceText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (advice.isAboveTarget) Color(0xFF059669) else Color(0xFFD97706),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AttendanceProgressRing(
                    percentage = subject.advice.currentPercentage,
                    targetPercentage = subject.advice.targetPercentage,
                    sizeDp = 64
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onMark(AttendanceStatus.PRESENT) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.attendance_mark_present))
                }
                OutlinedButton(
                    onClick = { onMark(AttendanceStatus.ABSENT) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.attendance_mark_absent))
                }
                TextButton(
                    onClick = { onMark(AttendanceStatus.CANCELLED) }
                ) {
                    Text(stringResource(R.string.attendance_mark_cancelled))
                }
            }
        }
    }
}

@Composable
fun TimetableScreen(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    val context = LocalContext.current
    var selectedDay by rememberSaveable {
        mutableIntStateOf(LocalDate.now().dayOfWeek.value.coerceIn(1, 6))
    }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    val dayLabels = listOf(
        1 to stringResource(R.string.timetable_day_mon),
        2 to stringResource(R.string.timetable_day_tue),
        3 to stringResource(R.string.timetable_day_wed),
        4 to stringResource(R.string.timetable_day_thu),
        5 to stringResource(R.string.timetable_day_fri),
        6 to stringResource(R.string.timetable_day_sat)
    )

    val daySlots = remember(coreState.timetableSlots, selectedDay) {
        TimetableEngine.slotsForDay(coreState.timetableSlots, selectedDay)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSlotDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.timetable_add_slot))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayLabels, key = { it.first }) { (dayInt, label) ->
                    FilterChip(
                        selected = selectedDay == dayInt,
                        onClick = { selectedDay = dayInt },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (daySlots.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.no_classes_today),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(daySlots, key = { it.id }) { slot ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${slot.startTime} - ${slot.endTime}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${slot.subjectCode} • ${slot.subjectName}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "${slot.room} • ${slot.teacherName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteTimetableSlot(slot.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSlotDialog) {
        var subjectName by remember { mutableStateOf("") }
        var subjectCode by remember { mutableStateOf("") }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("10:00") }
        var room by remember { mutableStateOf("Room 101") }
        var teacher by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            title = { Text(stringResource(R.string.timetable_add_slot)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text(stringResource(R.string.attendance_subject_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subjectCode,
                        onValueChange = { subjectCode = it },
                        label = { Text(stringResource(R.string.attendance_subject_code)) },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text(stringResource(R.string.timetable_room_label)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text(stringResource(R.string.attendance_professor)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addTimetableSlot(
                        context = context,
                        subjectName = subjectName,
                        subjectCode = subjectCode,
                        dayOfWeek = selectedDay,
                        startTime = startTime,
                        endTime = endTime,
                        room = room,
                        teacherName = teacher
                    )
                    showAddSlotDialog = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
