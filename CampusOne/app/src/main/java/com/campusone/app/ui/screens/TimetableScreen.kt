package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.domain.model.EvaluatedSlot
import com.campusone.app.domain.usecase.TimetableEngine
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel
import java.time.LocalDate
import java.time.LocalTime

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

    var selectedDay by rememberSaveable {
        mutableIntStateOf(LocalDate.now().dayOfWeek.value.coerceIn(1, 6))
    }
    var showAddSlotDialog by rememberSaveable { mutableStateOf(false) }

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

    val currentMinutes = remember(selectedDay, coreState.timetableSlots) {
        val now = LocalTime.now()
        now.hour * 60 + now.minute
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    showAddSlotDialog = true
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
                .padding(16.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayOptions, key = { it.dayOfWeek }) { option ->
                    val isSelected = selectedDay == option.dayOfWeek
                    val chipScale by animateFloatAsState(
                        targetValue = if (isSelected && !reducedMotion) 1.04f else 1.0f,
                        animationSpec = FluidMotion.MicroSpring,
                        label = "day_chip_scale_${option.dayOfWeek}"
                    )
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (selectedDay != option.dayOfWeek) {
                                FluidMotion.performTickHaptic(view, reducedMotion)
                                selectedDay = option.dayOfWeek
                            }
                        },
                        label = { Text(stringResource(option.labelResId)) },
                        modifier = Modifier.graphicsLayer {
                            scaleX = chipScale
                            scaleY = chipScale
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Directional AnimatedContent when switching days of the week
            AnimatedContent(
                targetState = selectedDay,
                transitionSpec = {
                    val dir = if (targetState >= initialState) 1 else -1
                    FluidMotion.directionalContentTransform(dir, reducedMotion)
                },
                label = "timetable_day_content"
            ) { dayInt ->
                val evaluatedSlots: List<EvaluatedSlot> = remember(coreState.timetableSlots, dayInt, currentMinutes) {
                    TimetableEngine.evaluateTodaySlots(
                        allSlots = coreState.timetableSlots,
                        currentDayOfWeek = dayInt,
                        currentMinutesOfDay = currentMinutes
                    )
                }

                if (evaluatedSlots.isEmpty()) {
                    InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.no_classes_today),
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(evaluatedSlots, key = { it.slot.id }) { evaluated ->
                            val slot = evaluated.slot
                            val isCompleted = !evaluated.isOngoing && evaluated.minutesRemaining == 0
                            val cardAlpha by animateFloatAsState(
                                targetValue = if (isCompleted) 0.72f else 1.0f,
                                animationSpec = FluidMotion.standardTween(reducedMotion),
                                label = "slot_alpha_${slot.id}"
                            )

                            InteractiveGlassCard(
                                isHighlighted = evaluated.isOngoing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = cardAlpha }
                            ) {
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
                                    IconButton(
                                        onClick = {
                                            FluidMotion.performTickHaptic(view, reducedMotion)
                                            viewModel.deleteTimetableSlot(slot.id)
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

    if (showAddSlotDialog) {
        var subjectName by rememberSaveable { mutableStateOf("") }
        var subjectCode by rememberSaveable { mutableStateOf("") }
        var startTime by rememberSaveable { mutableStateOf("09:00") }
        var endTime by rememberSaveable { mutableStateOf("10:00") }
        var room by rememberSaveable { mutableStateOf("Room 101") }
        var teacher by rememberSaveable { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            title = { Text(stringResource(R.string.timetable_add_slot)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedFormValidationBanner(
                        visible = showError,
                        message = stringResource(R.string.form_validation_required)
                    )
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = {
                            subjectName = it
                            if (it.isNotBlank()) showError = false
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
                    if (subjectName.isBlank()) {
                        showError = true
                    } else {
                        FluidMotion.performTickHaptic(view, reducedMotion)
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
                    }
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
