package `in`.campusone.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import `in`.campusone.app.domain.usecase.SlotLiveState
import `in`.campusone.app.domain.usecase.classEvaluatedSlot
import `in`.campusone.app.ui.viewmodel.CoreUiState
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableScreen(
    uiState: CoreUiState,
    onUpdateReminderMinutes: (Int) -> Unit,
    onPublishSlot: (TimetableSlot) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Today, 1 = Weekly Grid
    var selectedDayOfWeek by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value.coerceIn(1, 6)) }
    var showAddSlotDialog by rememberSaveable { mutableStateOf(false) }

    val canEditTimetable = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER

    Scaffold(
        floatingActionButton = {
            if (canEditTimetable) {
                FloatingActionButton(onClick = { showAddSlotDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.publish_timetable))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.timetable_today)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.timetable_weekly)) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Configurable Class Reminder Lead-Time Chip Row
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.reminder_lead_time,
                                        uiState.profile.reminderLeadMinutes
                                    ),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 10, 15, 30).forEach { mins ->
                                    FilterChip(
                                        selected = uiState.profile.reminderLeadMinutes == mins,
                                        onClick = { onUpdateReminderMinutes(mins) },
                                        label = { Text("${mins}m before") }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    if (uiState.todaySchedule.isEmpty()) {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.no_more_classes_today),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.todaySchedule, key = { it.slot.id }) { evaluated ->
                            EvaluatedSlotCard(evaluated = evaluated)
                        }
                    }
                } else {
                    // Weekly Day Selector (Mon..Sat)
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val dayNames = listOf(
                                1 to "Mon",
                                2 to "Tue",
                                3 to "Wed",
                                4 to "Thu",
                                5 to "Fri",
                                6 to "Sat"
                            )
                            dayNames.forEach { (dayNum, label) ->
                                FilterChip(
                                    selected = selectedDayOfWeek == dayNum,
                                    onClick = { selectedDayOfWeek = dayNum },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    val daySlots = uiState.allTimetableSlots.filter { it.dayOfWeek == selectedDayOfWeek }
                    items(daySlots, key = { it.id }) { slot ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${slot.startTime} – ${slot.endTime} • ${slot.subjectCode}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = slot.subjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${slot.teacherName} • ${slot.roomNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

@Composable
private fun EvaluatedSlotCard(evaluated: classEvaluatedSlot) {
    val slot = evaluated.slot
    val isHighlighted = evaluated.state == SlotLiveState.ONGOING || evaluated.state == SlotLiveState.UP_NEXT

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = if (isHighlighted) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
        colors = if (isHighlighted) {
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        } else {
            CardDefaults.outlinedCardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${slot.startTime} – ${slot.endTime}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                when (evaluated.state) {
                    SlotLiveState.ONGOING -> AssistChip(
                        onClick = {},
                        label = { Text("${stringResource(R.string.ongoing_class_badge)} (${evaluated.minutesRemaining}m left)") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    SlotLiveState.UP_NEXT -> AssistChip(
                        onClick = {},
                        label = { Text("${stringResource(R.string.next_class_badge)} (in ${evaluated.minutesUntilStart}m)") }
                    )
                    SlotLiveState.COMPLETED -> Text(
                        text = "Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SlotLiveState.UPCOMING_LATER -> Text(
                        text = "In ${evaluated.minutesUntilStart}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = slot.subjectName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${slot.roomNumber} • ${slot.teacherName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.publish_timetable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subjectCode,
                    onValueChange = { subjectCode = it },
                    label = { Text("Subject Code") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Faculty Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room / Lab") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTime = startTime.replace(":", "")
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
                }
            ) {
                Text("Publish to Section")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
