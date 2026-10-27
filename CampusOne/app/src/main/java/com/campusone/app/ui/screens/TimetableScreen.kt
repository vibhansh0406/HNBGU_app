package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.domain.model.EvaluatedSlot
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.usecase.TimetableEngine
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private data class DayTabOption(
    val dayOfWeek: Int,
    val labelResId: Int
)

@Composable
fun TimetableScreen(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    val coroutineScope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val todayDow = today.dayOfWeek.value.coerceIn(1, 6)
    val mondayOfThisWeek = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val pagerState = rememberPagerState(
        initialPage = (todayDow - 1).coerceIn(0, 5),
        pageCount = { 6 }
    )

    var showSlotDialog by rememberSaveable { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlot?>(null) }

    val dayOptions = remember {
        listOf(
            DayTabOption(1, R.string.timetable_day_mon),
            DayTabOption(2, R.string.timetable_day_tue),
            DayTabOption(3, R.string.timetable_day_wed),
            DayTabOption(4, R.string.timetable_day_thu),
            DayTabOption(5, R.string.timetable_day_fri),
            DayTabOption(6, R.string.timetable_day_sat)
        )
    }

    val nowTime = remember(coreState.timetableSlots, pagerState.currentPage) { LocalTime.now() }
    val currentMinutes = nowTime.hour * 60 + nowTime.minute
    val timeLabelFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    editingSlot = null
                    showSlotDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.timetable_add_slot)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Share Bar (Share as PDF or PNG Image)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        viewModel.shareTimetableAndAttendancePdf(context)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.timetable_share_pdf))
                }
                OutlinedButton(
                    onClick = {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        viewModel.shareTimetableAndAttendancePng(context)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.timetable_share_png))
                }
            }

            AnimatedVisibility(
                visible = coreState.exportBannerMessage != null,
                enter = FluidMotion.expandCollapseEnter(reducedMotion),
                exit = FluidMotion.expandCollapseExit(reducedMotion)
            ) {
                InteractiveGlassCard(
                    isHighlighted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = coreState.exportBannerMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearExportBanner() }) {
                            Text(stringResource(R.string.common_close))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Week Strip synced with HorizontalPager
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayOptions, key = { it.dayOfWeek }) { option ->
                    val pageIdx = option.dayOfWeek - 1
                    val isSelected = pagerState.currentPage == pageIdx
                    val dateForDay = mondayOfThisWeek.plusDays((option.dayOfWeek - 1).toLong())
                    val chipScale by animateFloatAsState(
                        targetValue = if (isSelected && !reducedMotion) 1.04f else 1.0f,
                        animationSpec = FluidMotion.MicroSpring,
                        label = "day_chip_scale_${option.dayOfWeek}"
                    )
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pageIdx)
                            }
                        },
                        label = {
                            Text("${stringResource(option.labelResId)} ${dateForDay.dayOfMonth}")
                        },
                        modifier = Modifier.graphicsLayer {
                            scaleX = chipScale
                            scaleY = chipScale
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live "NOW" Line when viewing today's schedule
            if (pagerState.currentPage + 1 == today.dayOfWeek.value) {
                LiveNowTimelineBanner(nowLabel = nowTime.format(timeLabelFormatter))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Day Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val dayInt = page + 1
                val evaluatedSlots: List<EvaluatedSlot> = remember(coreState.timetableSlots, dayInt, currentMinutes) {
                    TimetableEngine.evaluateTodaySlots(
                        allSlots = coreState.timetableSlots,
                        currentDayOfWeek = dayInt,
                        currentMinutesOfDay = currentMinutes
                    )
                }

                if (evaluatedSlots.isEmpty()) {
                    InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.no_classes_today),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.timetable_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                        items(evaluatedSlots, key = { it.slot.id }) { evaluated ->
                            val slot = evaluated.slot
                            val isToday = dayInt == today.dayOfWeek.value
                            val isCompleted = isToday && !evaluated.isOngoing && evaluated.minutesRemaining == 0
                            val cardAlpha by animateFloatAsState(
                                targetValue = if (isCompleted) 0.74f else 1.0f,
                                animationSpec = FluidMotion.standardTween(reducedMotion),
                                label = "slot_alpha_${slot.id}"
                            )
                            val slotColor = remember(slot.colorHex) { parseSubjectColor(slot.colorHex) }

                            InteractiveGlassCard(
                                isHighlighted = isToday && evaluated.isOngoing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = cardAlpha }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(slotColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${slot.startTime} - ${slot.endTime}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = slotColor,
                                            fontWeight = FontWeight.Bold
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
                                        if (isToday) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            when {
                                                evaluated.isOngoing -> {
                                                    AssistChip(
                                                        onClick = {},
                                                        label = {
                                                            Text(
                                                                stringResource(
                                                                    R.string.timetable_status_ongoing,
                                                                    evaluated.minutesRemaining
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                                evaluated.minutesUntilStart > 0 -> {
                                                    AssistChip(
                                                        onClick = {},
                                                        label = {
                                                            Text(
                                                                stringResource(
                                                                    R.string.timetable_status_upcoming,
                                                                    evaluated.minutesUntilStart
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                                else -> {
                                                    Text(
                                                        text = stringResource(R.string.timetable_status_completed),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            FluidMotion.performTickHaptic(view, reducedMotion)
                                            editingSlot = slot
                                            showSlotDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.timetable_edit_slot)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            FluidMotion.performTickHaptic(view, reducedMotion)
                                            viewModel.deleteTimetableSlot(context, slot.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.common_delete)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSlotDialog) {
        TimetableSlotAddOrEditDialog(
            initialSlot = editingSlot,
            defaultDayOfWeek = (pagerState.currentPage + 1).coerceIn(1, 6),
            allSlots = coreState.timetableSlots,
            subjects = coreState.subjects,
            onDismiss = {
                showSlotDialog = false
                editingSlot = null
            },
            onSave = { slotId, subId, subName, subCode, dayOfWeek, start, end, room, teacher, colorHex ->
                viewModel.saveTimetableSlot(
                    context = context,
                    slotId = slotId,
                    subjectId = subId,
                    subjectName = subName,
                    subjectCode = subCode,
                    dayOfWeek = dayOfWeek,
                    startTime = start,
                    endTime = end,
                    room = room,
                    teacherName = teacher,
                    colorHex = colorHex
                )
                showSlotDialog = false
                editingSlot = null
            }
        )
    }
}

@Composable
private fun LiveNowTimelineBanner(nowLabel: String) {
    val lineColor = Color(0xFFEF4444)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = lineColor,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "NOW $nowLabel",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        ) {
            drawLine(
                color = lineColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
private fun TimetableSlotAddOrEditDialog(
    initialSlot: TimetableSlot?,
    defaultDayOfWeek: Int,
    allSlots: List<TimetableSlot>,
    subjects: List<com.campusone.app.domain.model.SubjectSummary>,
    onDismiss: () -> Unit,
    onSave: (
        slotId: String?,
        subjectId: String,
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String,
        colorHex: String
    ) -> Unit
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()

    var subjectId by rememberSaveable { mutableStateOf(initialSlot?.subjectId ?: "") }
    var subjectName by rememberSaveable { mutableStateOf(initialSlot?.subjectName ?: "") }
    var subjectCode by rememberSaveable { mutableStateOf(initialSlot?.subjectCode ?: "") }
    var dayOfWeek by rememberSaveable { mutableIntStateOf(initialSlot?.dayOfWeek ?: defaultDayOfWeek) }
    var startTime by rememberSaveable { mutableStateOf(initialSlot?.startTime ?: "09:00") }
    var endTime by rememberSaveable { mutableStateOf(initialSlot?.endTime ?: "10:00") }
    var room by rememberSaveable { mutableStateOf(initialSlot?.room ?: "Room 101") }
    var teacher by rememberSaveable { mutableStateOf(initialSlot?.teacherName ?: "") }
    var selectedColor by rememberSaveable {
        mutableStateOf(initialSlot?.colorHex ?: SUBJECT_COLOR_PALETTE.first())
    }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val detectedClashes = remember(allSlots, dayOfWeek, startTime, endTime, initialSlot?.id) {
        TimetableEngine.detectClashes(
            allSlots = allSlots,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            ignoreSlotId = initialSlot?.id
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialSlot == null) stringResource(R.string.timetable_add_slot)
                else stringResource(R.string.timetable_edit_slot)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedFormValidationBanner(
                    visible = validationMessage != null,
                    message = validationMessage ?: ""
                )

                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects, key = { it.id }) { sub ->
                            AssistChip(
                                onClick = {
                                    subjectId = sub.id
                                    subjectName = sub.name
                                    subjectCode = sub.code
                                    teacher = sub.professor
                                    selectedColor = sub.colorHex
                                    validationMessage = null
                                },
                                label = { Text(sub.code) }
                            )
                        }
                    }
                }

                // Day selector chips (1..6)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")) { (d, lbl) ->
                        FilterChip(
                            selected = dayOfWeek == d,
                            onClick = { dayOfWeek = d },
                            label = { Text(lbl) }
                        )
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = {
                        subjectName = it
                        if (it.isNotBlank()) validationMessage = null
                    },
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
                        onValueChange = {
                            startTime = it
                            validationMessage = null
                        },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {
                            endTime = it
                            validationMessage = null
                        },
                        label = { Text("End (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (detectedClashes.isNotEmpty()) {
                    val firstClash = detectedClashes.first()
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.timetable_clash_warning,
                                    firstClash.conflictingSlot.subjectCode,
                                    firstClash.conflictingSlot.startTime,
                                    firstClash.conflictingSlot.endTime,
                                    firstClash.overlapMinutes
                                ),
                                color = Color(0xFF92400E),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
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
                                .size(28.dp)
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
            }
        },
        confirmButton = {
            Button(onClick = {
                if (subjectName.isBlank()) {
                    validationMessage = "Please enter a subject name."
                } else if (!TimetableEngine.isValidSlotRange(startTime, endTime)) {
                    validationMessage = "Enter valid 24h times (HH:mm) where End > Start."
                } else {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    onSave(
                        initialSlot?.id,
                        subjectId,
                        subjectName,
                        subjectCode,
                        dayOfWeek,
                        startTime,
                        endTime,
                        room,
                        teacher,
                        selectedColor
                    )
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
