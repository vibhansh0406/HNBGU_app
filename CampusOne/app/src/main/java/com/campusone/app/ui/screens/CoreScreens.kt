package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusone.app.R
import com.campusone.app.domain.model.AssignmentItem
import com.campusone.app.domain.model.AttendanceRecordItem
import com.campusone.app.domain.model.AttendanceStatus
import com.campusone.app.domain.model.ExamItem
import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.domain.usecase.AttendanceCalculator
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.components.StaggeredEntranceSection
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val SUBJECT_COLOR_PALETTE: List<String> = listOf(
    "#0F4C81",
    "#006B5E",
    "#7C3AED",
    "#D97706",
    "#DC2626",
    "#2563EB",
    "#059669",
    "#DB2777"
)

fun parseSubjectColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (_: Exception) {
        Color(0xFF0F4C81)
    }
}

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
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
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
    var playStaggerEntrance by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                                imageVector = Icons.Default.DateRange,
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
                                imageVector = Icons.Default.DateRange,
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
        percentage >= targetPercentage - 8 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val ringColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = FluidMotion.standardTween(reducedMotion),
        label = "attendance_ring_color"
    )
    val talkBackDesc = "Attendance ${percentage.toInt()} percent, target $targetPercentage percent"

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp.dp)
            .semantics { contentDescription = talkBackDesc }
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
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()

    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectSummary?>(null) }

    val tabLabels = listOf(
        stringResource(R.string.attendance_tab_subjects),
        stringResource(R.string.attendance_tab_safe_bunk),
        stringResource(R.string.attendance_tab_analytics)
    )

    Scaffold(
        floatingActionButton = {
            if (selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        showAddDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.attendance_add_subject)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedSubTab, edgePadding = 16.dp) {
                tabLabels.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedSubTab == idx,
                        onClick = {
                            if (selectedSubTab != idx) {
                                FluidMotion.performTickHaptic(view, reducedMotion)
                                selectedSubTab = idx
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            AnimatedVisibility(
                visible = coreState.lastUndoRecordId != null,
                enter = FluidMotion.expandCollapseEnter(reducedMotion),
                exit = FluidMotion.expandCollapseExit(reducedMotion)
            ) {
                InteractiveGlassCard(
                    isHighlighted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = coreState.lastUndoLabel ?: stringResource(R.string.attendance_undo_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    FluidMotion.performTickHaptic(view, reducedMotion)
                                    viewModel.undoLastAttendance(context)
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.attendance_undo_btn))
                            }
                            TextButton(onClick = { viewModel.dismissUndo() }) {
                                Text(stringResource(R.string.common_close))
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = selectedSubTab,
                transitionSpec = {
                    val dir = if (targetState >= initialState) 1 else -1
                    FluidMotion.directionalContentTransform(dir, reducedMotion)
                },
                label = "attendance_subtab_switch"
            ) { tabIdx ->
                when (tabIdx) {
                    0 -> AttendanceSubjectsSubTab(
                        coreState = coreState,
                        onMark = { subjectId, status ->
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            viewModel.markAttendance(context, subjectId, status)
                        },
                        onEditSubject = { subject ->
                            editingSubject = subject
                        },
                        onClearHighlight = {
                            viewModel.setHighlightedSubject(null)
                        }
                    )
                    1 -> SafeBunkPlannerSubTab(
                        coreState = coreState,
                        viewModel = viewModel
                    )
                    else -> AttendanceAnalyticsAndHistorySubTab(
                        coreState = coreState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        SubjectAddOrEditDialog(
            initialSubject = null,
            defaultTargetPct = coreState.profile.targetAttendancePercent,
            onDismiss = { showAddDialog = false },
            onSave = { code, name, prof, targetPct, colorHex ->
                viewModel.addSubject(
                    context = context,
                    code = code,
                    name = name,
                    professor = prof,
                    semester = coreState.profile.semester,
                    targetPercentage = targetPct,
                    colorHex = colorHex
                )
                showAddDialog = false
            },
            onDelete = null
        )
    }

    editingSubject?.let { subjectToEdit ->
        SubjectAddOrEditDialog(
            initialSubject = subjectToEdit,
            defaultTargetPct = subjectToEdit.targetPercentage,
            onDismiss = { editingSubject = null },
            onSave = { code, name, prof, targetPct, colorHex ->
                viewModel.updateSubjectDetails(
                    context = context,
                    subjectId = subjectToEdit.id,
                    code = code,
                    name = name,
                    professor = prof,
                    targetPercentage = targetPct,
                    colorHex = colorHex
                )
                editingSubject = null
            },
            onDelete = {
                viewModel.deleteSubject(context, subjectToEdit.id)
                editingSubject = null
            }
        )
    }
}

@Composable
private fun AttendanceSubjectsSubTab(
    coreState: CoreUiState,
    onMark: (String, AttendanceStatus) -> Unit,
    onEditSubject: (SubjectSummary) -> Unit,
    onClearHighlight: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.attendance_swipe_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (coreState.subjects.isEmpty()) {
            item(key = "att_empty") {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.attendance_no_classes_logged),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(coreState.subjects, key = { it.id }) { subject ->
                val isDeepLinkHighlighted = coreState.highlightedSubjectId == subject.id
                SubjectAttendanceCard(
                    subject = subject,
                    isDeepLinkHighlighted = isDeepLinkHighlighted,
                    onMark = { status ->
                        if (isDeepLinkHighlighted) onClearHighlight()
                        onMark(subject.id, status)
                    },
                    onEdit = { onEditSubject(subject) }
                )
            }
        }
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject: SubjectSummary,
    isDeepLinkHighlighted: Boolean,
    onMark: (AttendanceStatus) -> Unit,
    onEdit: () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val advice = subject.advice
    val subjectColor = remember(subject.colorHex) { parseSubjectColor(subject.colorHex) }

    val warningColor = when {
        !advice.isAboveTarget && advice.totalConducted > 0 -> Color(0xFFDC2626)
        advice.isWarningZone && advice.totalConducted > 0 -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }
    val animatedTone by animateColorAsState(
        targetValue = warningColor,
        animationSpec = FluidMotion.standardTween(reducedMotion),
        label = "subject_advice_tone"
    )

    var cumulativeDrag by remember { mutableFloatStateOf(0f) }

    InteractiveGlassCard(
        isHighlighted = isDeepLinkHighlighted || (!advice.isAboveTarget && advice.totalConducted > 0),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(subject.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (cumulativeDrag > 110f) {
                            onMark(AttendanceStatus.PRESENT)
                        } else if (cumulativeDrag < -110f) {
                            onMark(AttendanceStatus.ABSENT)
                        }
                        cumulativeDrag = 0f
                    },
                    onDragCancel = { cumulativeDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        cumulativeDrag += dragAmount
                    }
                )
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${subject.code} • ${subject.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.attendance_edit_subject)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = Pair(subject.attendedCount, subject.totalClassesCount),
                        transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                        label = "subject_fraction"
                    ) { (att, tot) ->
                        Text(
                            text = "${subject.professor} • $att/$tot (${stringResource(R.string.attendance_target_chip, subject.targetPercentage)})",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (advice.isWarningZone && advice.totalConducted > 0) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = animatedTone,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        AnimatedContent(
                            targetState = adviceText,
                            transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                            label = "subject_advice_text"
                        ) { textValue ->
                            Text(
                                text = textValue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = animatedTone,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                AttendanceProgressRing(
                    percentage = advice.currentPercentage,
                    targetPercentage = subject.targetPercentage,
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.attendance_mark_present))
                }
                OutlinedButton(
                    onClick = { onMark(AttendanceStatus.ABSENT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.attendance_mark_absent))
                }
                TextButton(
                    onClick = { onMark(AttendanceStatus.CANCELLED) },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.attendance_mark_cancelled))
                }
            }
        }
    }
}

@Composable
private fun SafeBunkPlannerSubTab(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    val today = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    val chosenEndDate = remember(coreState.profile.semesterEndEpochDay, today) {
        val raw = coreState.profile.semesterEndEpochDay
        if (raw > today.toEpochDay()) LocalDate.ofEpochDay(raw) else today.plusDays(60)
    }
    val horizonDaysOptions = remember { listOf(30, 60, 90, 120) }
    val currentDaysAhead = ChronoUnit.DAYS.between(today, chosenEndDate).toInt().coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "planner_end_date_card") {
            InteractiveGlassCard(
                isHighlighted = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.safe_bunk_planner_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.safe_bunk_end_date_label,
                            chosenEndDate.format(dateFormatter),
                            currentDaysAhead
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(horizonDaysOptions, key = { it }) { days ->
                            FilterChip(
                                selected = kotlin.math.abs(currentDaysAhead - days) <= 7,
                                onClick = {
                                    FluidMotion.performTickHaptic(view, reducedMotion)
                                    viewModel.updateSemesterEndEpochDay(today.plusDays(days.toLong()).toEpochDay())
                                },
                                label = { Text("+$days Days") }
                            )
                        }
                    }
                }
            }
        }

        if (coreState.subjects.isEmpty()) {
            item(key = "planner_empty") {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.attendance_no_classes_logged),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(coreState.subjects, key = { "proj_${it.id}" }) { subject ->
                var whatIfSkip by rememberSaveable(subject.id) { mutableIntStateOf(0) }
                val projection = remember(subject, coreState.timetableSlots, today, chosenEndDate, whatIfSkip) {
                    AttendanceCalculator.projectSubjectWithTimetable(
                        subject = subject,
                        timetableSlots = coreState.timetableSlots,
                        fromDate = today,
                        endDate = chosenEndDate,
                        whatIfSkipCount = whatIfSkip
                    )
                }

                InteractiveGlassCard(
                    isHighlighted = !projection.isWhatIfAboveTarget && (subject.totalClassesCount + whatIfSkip) > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${projection.subjectCode} • ${projection.subjectName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Current: ${projection.attended}/${projection.totalConducted} (${projection.currentPercentage}%) • Target: ${projection.targetPercentage}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.safe_bunk_projected_summary,
                                projection.remainingScheduledClasses,
                                projection.projectedSafeBunksUntilEnd,
                                projection.projectedMustAttendUntilEnd
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val whatIfColor = if (projection.isWhatIfAboveTarget) {
                            Color(0xFF059669)
                        } else {
                            Color(0xFFDC2626)
                        }
                        Text(
                            text = stringResource(
                                R.string.safe_bunk_what_if_label,
                                projection.whatIfSkipCount,
                                projection.whatIfPercentage
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = whatIfColor
                        )
                        Slider(
                            value = whatIfSkip.toFloat(),
                            onValueChange = { whatIfSkip = it.toInt() },
                            valueRange = 0f..15f,
                            steps = 14
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceAnalyticsAndHistorySubTab(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()

    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var editingRecord by remember { mutableStateOf<AttendanceRecordItem?>(null) }

    val activeYearMonth = remember(monthOffset) {
        YearMonth.now().plusMonths(monthOffset.toLong())
    }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    val heatmapDays = remember(coreState.recentHistory, activeYearMonth, selectedSubjectId) {
        AttendanceCalculator.buildMonthlyHeatmap(
            records = coreState.recentHistory,
            yearMonth = activeYearMonth,
            subjectIdFilter = selectedSubjectId
        )
    }

    val trendPoints = remember(coreState.recentHistory, selectedSubjectId) {
        AttendanceCalculator.buildAttendanceTrend(
            records = coreState.recentHistory,
            subjectIdFilter = selectedSubjectId
        )
    }

    val filteredHistory = remember(coreState.recentHistory, selectedSubjectId) {
        if (selectedSubjectId.isNullOrBlank()) {
            coreState.recentHistory
        } else {
            coreState.recentHistory.filter { it.subjectId == selectedSubjectId }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "analytics_subject_filter") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedSubjectId = null
                        },
                        label = { Text(stringResource(R.string.notices_filter_all)) }
                    )
                }
                items(coreState.subjects, key = { it.id }) { sub ->
                    FilterChip(
                        selected = selectedSubjectId == sub.id,
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedSubjectId = sub.id
                        },
                        label = { Text(sub.code) }
                    )
                }
            }
        }

        // Monthly Calendar Heatmap Card
        item(key = "analytics_heatmap_card") {
            InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { monthOffset -= 1 }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                        }
                        Text(
                            text = activeYearMonth.format(monthFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { monthOffset += 1 }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val rows = heatmapDays.chunked(7)
                    rows.forEach { weekDays ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDays.forEach { day ->
                                val cellColor = when {
                                    day.presentCount > 0 && day.absentCount == 0 -> Color(0xFF10B981).copy(alpha = 0.85f)
                                    day.presentCount > 0 && day.absentCount > 0 -> Color(0xFFF59E0B).copy(alpha = 0.85f)
                                    day.absentCount > 0 -> Color(0xFFEF4444).copy(alpha = 0.85f)
                                    day.cancelledCount > 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val textColor = if (day.presentCount > 0 || day.absentCount > 0) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cellColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            repeat(7 - weekDays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Attendance Trend Canvas Chart Card
        item(key = "analytics_trend_card") {
            InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.attendance_trend_chart_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (trendPoints.isEmpty()) {
                        Text(
                            text = stringResource(R.string.attendance_no_classes_logged),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        AttendanceTrendCanvasChart(
                            points = trendPoints.map { it.cumulativePercentage.toFloat() },
                            targetPercent = coreState.profile.targetAttendancePercent.toFloat()
                        )
                    }
                }
            }
        }

        // Past Attendance Entries with Edit & Delete
        item(key = "analytics_history_header") {
            Text(
                text = stringResource(R.string.attendance_history),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (filteredHistory.isEmpty()) {
            item(key = "analytics_history_empty") {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.attendance_no_classes_logged),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredHistory.take(40), key = { it.id }) { rec ->
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                        IconButton(onClick = { editingRecord = rec }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_save))
                        }
                        IconButton(onClick = { viewModel.deleteAttendanceRecord(context, rec.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                        }
                    }
                }
            }
        }
    }

    editingRecord?.let { record ->
        var selectedStatus by remember { mutableStateOf(record.status) }
        var editedEpochDay by remember { mutableStateOf(record.epochDay) }
        val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

        AlertDialog(
            onDismissRequest = { editingRecord = null },
            title = { Text(stringResource(R.string.attendance_edit_entry_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(record.subjectName, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { editedEpochDay -= 1 }) { Text("-1d") }
                        Text(
                            text = LocalDate.ofEpochDay(editedEpochDay).format(dateFmt),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { editedEpochDay += 1 }) { Text("+1d") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttendanceStatus.entries.forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(st.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editAttendanceRecord(
                        context = context,
                        recordId = record.id,
                        newStatus = selectedStatus,
                        newEpochDay = editedEpochDay
                    )
                    editingRecord = null
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRecord = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun AttendanceTrendCanvasChart(
    points: List<Float>,
    targetPercent: Float
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val targetLineColor = Color(0xFFD97706)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 8.dp)
    ) {
        if (points.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height

        // Draw target threshold dashed line
        val targetY = h - ((targetPercent.coerceIn(0f, 100f) / 100f) * h)
        drawLine(
            color = targetLineColor,
            start = Offset(0f, targetY),
            end = Offset(w, targetY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )

        val stepX = if (points.size > 1) w / (points.size - 1) else w / 2f
        val offsets = points.mapIndexed { idx, pct ->
            val ratio = (pct.coerceIn(0f, 100f)) / 100f
            Offset(x = idx * stepX, y = h - (ratio * h))
        }
        for (i in 0 until offsets.size - 1) {
            drawLine(
                color = lineColor,
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        offsets.forEach { pt ->
            drawCircle(color = lineColor, radius = 9f, center = pt)
        }
    }
}

@Composable
private fun SubjectAddOrEditDialog(
    initialSubject: SubjectSummary?,
    defaultTargetPct: Int,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, prof: String, targetPct: Int, colorHex: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()

    var code by rememberSaveable { mutableStateOf(initialSubject?.code ?: "") }
    var name by rememberSaveable { mutableStateOf(initialSubject?.name ?: "") }
    var prof by rememberSaveable { mutableStateOf(initialSubject?.professor ?: "") }
    var targetPct by rememberSaveable { mutableIntStateOf(initialSubject?.targetPercentage ?: defaultTargetPct) }
    var selectedColor by rememberSaveable {
        mutableStateOf(initialSubject?.colorHex ?: SUBJECT_COLOR_PALETTE.first())
    }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialSubject == null) stringResource(R.string.attendance_add_subject)
                else stringResource(R.string.attendance_edit_subject)
            )
        },
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.setup_target_attendance)}: $targetPct%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = targetPct.toFloat(),
                    onValueChange = { targetPct = it.toInt() },
                    valueRange = 50f..95f,
                    steps = 8
                )
                Text(
                    text = stringResource(R.string.timetable_pick_color),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SUBJECT_COLOR_PALETTE.forEach { hex ->
                        val isPicked = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parseSubjectColor(hex))
                                .border(
                                    width = if (isPicked) 3.dp else 0.dp,
                                    color = if (isPicked) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.common_delete))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    showError = true
                } else {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    onSave(code, name, prof, targetPct, selectedColor)
                }
            }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
