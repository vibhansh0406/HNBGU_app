package `in`.campusone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabDefaults
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.domain.usecase.EvaluatedSlot
import `in`.campusone.app.domain.usecase.SlotLiveState
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.CoreUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableScreen(
    uiState: CoreUiState,
    onUpdateReminderMinutes: (Int) -> Unit,
    onPublishSlot: (TimetableSlot) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedDayOfWeek by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value.coerceIn(1, 6)) }
    var showAddSlotDialog by rememberSaveable { mutableStateOf(false) }

    val canEditTimetable = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER
    val locale = Locale.getDefault()
    val todayLabel = remember { LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) }

    Scaffold(
        floatingActionButton = {
            if (canEditTimetable) {
                FloatingActionButton(
                    onClick = { showAddSlotDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.publish_timetable))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tab row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(horizontal = C1Spacing.lg),
                    divider = { }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.timetable_today), style = MaterialTheme.typography.labelLarge) },
                        colors = TabDefaults.colors(
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.timetable_weekly), style = MaterialTheme.typography.labelLarge) },
                        colors = TabDefaults.colors(
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = C1Spacing.lg),
                contentPadding = PaddingValues(top = C1Spacing.lg, bottom = C1Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                item {
                    ReminderLeadTimeCard(
                        reminderLeadMinutes = uiState.profile.reminderLeadMinutes,
                        onUpdateReminderMinutes = onUpdateReminderMinutes
                    )
                }

                if (selectedTab == 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = todayLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.todaySchedule.isNotEmpty()) {
                                C1StatusChip(
                                    text = stringResource(R.string.timetable_classes_today, uiState.todaySchedule.size),
                                    tone = C1Tone.NEUTRAL
                                )
                            }
                        }
                    }

                    if (uiState.todaySchedule.isEmpty()) {
                        item {
                            EmptySchedule(
                                title = stringResource(R.string.timetable_no_classes_day),
                                message = stringResource(R.string.no_more_classes_today)
                            )
                        }
                    } else {
                        items(uiState.todaySchedule, key = { it.slot.id }) { evaluated ->
                            TimetableSlotCard(slot = evaluated.slot, evaluated = evaluated)
                        }
                    }
                } else {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                            (1..6).forEach { dayNum ->
                                FilterChip(
                                    selected = selectedDayOfWeek == dayNum,
                                    onClick = { selectedDayOfWeek = dayNum },
                                    label = {
                                        Text(
                                            DayOfWeek.of(dayNum)
                                                .getDisplayName(JavaTextStyle.SHORT, locale)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    val daySlots = uiState.allTimetableSlots.filter { it.dayOfWeek == selectedDayOfWeek }
                    if (daySlots.isEmpty()) {
                        item {
                            EmptySchedule(
                                title = stringResource(R.string.timetable_no_classes_day),
                                message = stringResource(R.string.timetable_empty_hint)
                            )
                        }
                    } else {
                        items(daySlots, key = { it.id }) { slot ->
                            TimetableSlotCard(slot = slot, evaluated = null)
                        }
                    }
                }
            }
        }
    }

    if (showAddSlotDialog) {
        PublishSlotDialog(
            sectionKey = uiState.profile.sectionKey,
            defaultDay = selectedDayOfWeek,
            onDismiss = { showAddSlotDialog = false },
            onPublish = { slot ->
                onPublishSlot(slot)
                showAddSlotDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderLeadTimeCard(
    reminderLeadMinutes: Int,
    onUpdateReminderMinutes: (Int) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(C1Spacing.lg),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(C1Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(C1Spacing.sm))
                Text(
                    text = stringResource(R.string.reminder_lead_time, reminderLeadMinutes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(C1Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                listOf(5, 10, 15, 30).forEach { mins ->
                    FilterChip(
                        selected = reminderLeadMinutes == mins,
                        onClick = { onUpdateReminderMinutes(mins) },
                        label = { Text(stringResource(R.string.timetable_reminder_before, mins)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySchedule(title: String, message: String) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        C1EmptyState(
            icon = Icons.Default.Schedule,
            title = title,
            message = message
        )
    }
}

@Composable
private fun TimetableSlotCard(slot: TimetableSlot, evaluated: EvaluatedSlot?) {
    val highlighted = evaluated?.state == SlotLiveState.ONGOING || evaluated?.state == SlotLiveState.UP_NEXT

    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        borderColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(modifier = Modifier.padding(C1Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(C1Spacing.xs))
                Text(
                    text = "${slot.startTime} – ${slot.endTime}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = slot.subjectCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(C1Spacing.sm))
            Text(
                text = slot.subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(C1Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(C1Spacing.xs))
                Text(
                    text = "${slot.roomNumber} • ${slot.teacherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (evaluated != null) {
                Spacer(modifier = Modifier.height(C1Spacing.sm))
                SlotStateChip(evaluated = evaluated)
            }
        }
    }
}

@Composable
private fun SlotStateChip(evaluated: EvaluatedSlot) {
    when (evaluated.state) {
        SlotLiveState.ONGOING -> C1StatusChip(
            text = "${stringResource(R.string.ongoing_class_badge)} · " +
                    stringResource(R.string.timetable_minutes_left, evaluated.minutesRemaining?.toInt() ?: 0),
            tone = C1Tone.SUCCESS
        )
        SlotLiveState.UP_NEXT -> C1StatusChip(
            text = "${stringResource(R.string.next_class_badge)} · " +
                    stringResource(R.string.timetable_starts_in, evaluated.minutesUntilStart?.toInt() ?: 0),
            tone = C1Tone.WARNING
        )
        SlotLiveState.COMPLETED -> C1StatusChip(
            text = stringResource(R.string.timetable_completed),
            tone = C1Tone.NEUTRAL
        )
        SlotLiveState.UPCOMING_LATER -> C1StatusChip(
            text = stringResource(R.string.timetable_starts_in, evaluated.minutesUntilStart?.toInt() ?: 0),
            tone = C1Tone.INFO
        )
    }
}

@Composable
private fun PublishSlotDialog(
    sectionKey: String,
    defaultDay: Int,
    onDismiss: () -> Unit,
    onPublish: (TimetableSlot) -> Unit
) {
    var subjectCode by rememberSaveable { mutableStateOf("SOET-CS-301") }
    var subjectName by rememberSaveable { mutableStateOf("Data Structures & Algorithms") }
    var teacherName by rememberSaveable { mutableStateOf("Dr. M. M. S. Rauthan") }
    var roomNumber by rememberSaveable { mutableStateOf("CS Block - Room 104") }
    var startTime by rememberSaveable { mutableStateOf("15:00") }
    var endTime by rememberSaveable { mutableStateOf("16:00") }
    val timeRegex = remember { Regex("^([01]\\d|2[0-3]):[0-5]\\d$") }
    val timesValid = timeRegex.matches(startTime.trim()) && timeRegex.matches(endTime.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.publish_timetable)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                C1TextField(
                    value = subjectCode,
                    onValueChange = { subjectCode = it },
                    label = stringResource(R.string.timetable_subject_code)
                )
                C1TextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = stringResource(R.string.timetable_subject_name)
                )
                C1TextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = stringResource(R.string.timetable_faculty)
                )
                C1TextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = stringResource(R.string.timetable_room)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    C1TextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = stringResource(R.string.timetable_start),
                        isError = !timeRegex.matches(startTime.trim()),
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = stringResource(R.string.timetable_end),
                        isError = !timeRegex.matches(endTime.trim()),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            C1Button(
                text = stringResource(R.string.action_publish),
                onClick = {
                    val cleanTime = startTime.trim().replace(":", "")
                    onPublish(
                        TimetableSlot(
                            id = "${sectionKey}_d${defaultDay}_$cleanTime",
                            subjectId = subjectCode.trim(),
                            subjectName = subjectName.trim(),
                            subjectCode = subjectCode.trim(),
                            teacherName = teacherName.trim(),
                            roomNumber = roomNumber.trim(),
                            dayOfWeek = defaultDay,
                            startTime = startTime.trim(),
                            endTime = endTime.trim()
                        )
                    )
                },
                enabled = subjectCode.isNotBlank() &&
                    subjectName.isNotBlank() &&
                    teacherName.isNotBlank() &&
                    timesValid
            )
        },
        dismissButton = {
            C1Button(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}