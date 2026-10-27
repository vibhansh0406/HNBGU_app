package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.domain.model.AssignmentItem
import com.campusone.app.domain.model.AttendanceStatus
import com.campusone.app.domain.model.ExamItem
import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.components.StaggeredEntranceSection
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun FirstLaunchSetupScreen(
    onCompleteSetup: (name: String, college: String, course: String, semester: Int, section: String, targetPct: Int) -> Unit
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    var name by rememberSaveable { mutableStateOf("") }
    var college by rememberSaveable { mutableStateOf("HNB Garhwal University") }
    var course by rememberSaveable { mutableStateOf("B.Tech CSE") }
    var semesterText by rememberSaveable { mutableStateOf("4") }
    var section by rememberSaveable { mutableStateOf("A") }
    var targetPct by rememberSaveable { mutableIntStateOf(75) }
    var showValidationError by rememberSaveable { mutableStateOf(false) }

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
                StaggeredEntranceSection(sectionIndex = 0, shouldAnimate = true) {
                    Column {
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
                }
            }

            item {
                AnimatedFormValidationBanner(
                    visible = showValidationError,
                    message = stringResource(R.string.setup_validation_error)
                )
            }

            item {
                StaggeredEntranceSection(sectionIndex = 1, shouldAnimate = true) {
                    InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    if (it.isNotBlank()) showValidationError = false
                                },
                                label = { Text(stringResource(R.string.setup_name_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = college,
                                onValueChange = { college = it },
                                label = { Text(stringResource(R.string.setup_college_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = course,
                                onValueChange = {
                                    course = it
                                    if (it.isNotBlank()) showValidationError = false
                                },
                                label = { Text(stringResource(R.string.setup_course_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                    }
                }
            }

            item {
                StaggeredEntranceSection(sectionIndex = 2, shouldAnimate = true) {
                    InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AnimatedContent(
                                targetState = targetPct,
                                transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                                label = "setup_target_pct"
                            ) { pct ->
                                Text(
                                    text = "${stringResource(R.string.setup_target_attendance)}: $pct%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = targetPct.toFloat(),
                                onValueChange = { targetPct = it.toInt() },
                                valueRange = 60f..90f,
                                steps = 5
                            )
                        }
                    }
                }
            }

            item {
                StaggeredEntranceSection(sectionIndex = 3, shouldAnimate = true) {
                    Button(
                        onClick = {
                            if (name.isBlank() || course.isBlank()) {
                                showValidationError = true
                            } else {
                                FluidMotion.performTickHaptic(view, reducedMotion)
                                val sem = semesterText.toIntOrNull()?.coerceIn(1, 10) ?: 4
                                onCompleteSetup(name, college, course, sem, section, targetPct)
                            }
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
}

@Composable
fun HomeDashboardScreen(
    coreState: CoreUiState,
    upcomingAssignments: List<AssignmentItem>,
    upcomingExams: List<ExamItem>,
    onNavigateToTab: (String) -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val today = remember { LocalDate.now() }

    // One-shot staggered entrance guard: plays once per session, never replays on tab return
    var playStaggerEntrance by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 0: Hero Greeting & Animated Attendance Ring
        item(key = "home_hero") {
            StaggeredEntranceSection(
                sectionIndex = 0,
                shouldAnimate = playStaggerEntrance,
                onAnimationConsumed = { playStaggerEntrance = false }
            ) {
                InteractiveGlassCard(
                    isHighlighted = true,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                    onClick = { onNavigateToTab("attendance") },
                    modifier = Modifier.fillMaxWidth()
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
        }

        // Section 1: Next Class Card with AnimatedContent state transition
        item(key = "home_next_class") {
            StaggeredEntranceSection(
                sectionIndex = 1,
                shouldAnimate = playStaggerEntrance
            ) {
                InteractiveGlassCard(
                    isHighlighted = coreState.nextClassToday != null,
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
                        AnimatedContent(
                            targetState = coreState.nextClassToday,
                            transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                            contentKey = { it?.id ?: "none" },
                            label = "home_next_class_transition"
                        ) { nextSlot ->
                            if (nextSlot != null) {
                                Column {
                                    Text(
                                        text = "${nextSlot.subjectCode} • ${nextSlot.subjectName}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "${nextSlot.startTime} - ${nextSlot.endTime} • ${nextSlot.room} (${nextSlot.teacherName})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
            }
        }

        // Section 2: Next 3 Deadlines (Stable keys while scrolling)
        item(key = "home_deadlines") {
            StaggeredEntranceSection(
                sectionIndex = 2,
                shouldAnimate = playStaggerEntrance
            ) {
                InteractiveGlassCard(
                    onClick = { onNavigateToTab("more") },
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
                        AnimatedVisibility(
                            visible = topThree.isEmpty(),
                            enter = FluidMotion.expandCollapseEnter(reducedMotion),
                            exit = FluidMotion.expandCollapseExit(reducedMotion)
                        ) {
                            Text(
                                text = stringResource(R.string.assignments_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        topThree.forEach { item ->
                            val dueDate = Instant.ofEpochMilli(item.dueEpochMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                    onClick = { onNavigateToTab("more") },
                                    label = { Text(dueDate.format(dateFormatter)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Exam Countdowns
        item(key = "home_exams") {
            StaggeredEntranceSection(
                sectionIndex = 3,
                shouldAnimate = playStaggerEntrance
            ) {
                InteractiveGlassCard(
                    onClick = { onNavigateToTab("more") },
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
                                    onClick = { onNavigateToTab("more") },
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
}

@Composable
fun AttendanceProgressRing(
    percentage: Double,
    targetPercentage: Int,
    sizeDp: Int = 72
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val animatedSweep by animateFloatAsState(
        targetValue = ((percentage.toFloat().coerceIn(0f, 100f)) / 100f) * 360f,
        animationSpec = FluidMotion.largeTween(reducedMotion),
        label = "attendance_ring_sweep"
    )
    val targetColor = when {
        percentage >= targetPercentage -> Color(0xFF10B981)
        percentage >= targetPercentage - 10 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val ringColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = FluidMotion.standardTween(reducedMotion),
        label = "attendance_ring_color"
    )

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
        AnimatedContent(
            targetState = percentage.toInt(),
            transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
            label = "attendance_ring_label"
        ) { pctInt ->
            Text(
                text = "$pctInt%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AttendanceTrackerScreen(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    showAddDialog = true
                }
            ) {
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
            item(key = "att_disclaimer") {
                InteractiveGlassCard(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
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
                    onMark = { status ->
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        viewModel.markAttendance(subject.id, status)
                    }
                )
            }

            if (coreState.recentHistory.isNotEmpty()) {
                item(key = "att_history_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.attendance_history),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(coreState.recentHistory.take(10), key = { it.id }) { rec ->
                    InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
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
        var code by rememberSaveable { mutableStateOf("") }
        var name by rememberSaveable { mutableStateOf("") }
        var prof by rememberSaveable { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.attendance_add_subject)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedFormValidationBanner(
                        visible = showError,
                        message = stringResource(R.string.form_validation_required)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(stringResource(R.string.attendance_subject_code)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) showError = false
                        },
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
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        viewModel.addSubject(code, name, prof, coreState.profile.semester)
                        showAddDialog = false
                    }
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
    val reducedMotion = rememberReducedMotionEnabled()
    val advice = subject.advice
    val adviceTone by animateColorAsState(
        targetValue = if (advice.isAboveTarget) Color(0xFF059669) else Color(0xFFD97706),
        animationSpec = FluidMotion.standardTween(reducedMotion),
        label = "subject_advice_tone"
    )

    InteractiveGlassCard(
        isHighlighted = !advice.isAboveTarget && advice.totalConducted > 0,
        modifier = Modifier.fillMaxWidth()
    ) {
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
                    AnimatedContent(
                        targetState = Pair(subject.attendedCount, subject.totalClassesCount),
                        transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                        label = "subject_fraction"
                    ) { (att, tot) ->
                        Text(
                            text = "${subject.professor} • $att/$tot classes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
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
                    AnimatedContent(
                        targetState = adviceText,
                        transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                        label = "subject_advice_text"
                    ) { textValue ->
                        Text(
                            text = textValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = adviceTone,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                AttendanceProgressRing(
                    percentage = advice.currentPercentage,
                    targetPercentage = advice.targetPercentage,
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
