package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@Composable
fun AdminTimetableScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingSlot by rememberSaveable { mutableStateOf<TimetableSlotEntity?>(null) }
    var subjectName by rememberSaveable { mutableStateOf("") }
    var subjectCode by rememberSaveable { mutableStateOf("") }
    var teacherName by rememberSaveable { mutableStateOf("") }
    var roomNumber by rememberSaveable { mutableStateOf("") }
    var dayOfWeek by rememberSaveable { mutableIntStateOf(1) }
    var startTime by rememberSaveable { mutableStateOf("09:00") }
    var endTime by rememberSaveable { mutableStateOf("10:00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Timetable") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = {
                editingSlot = null
                subjectName = ""
                subjectCode = ""
                teacherName = ""
                roomNumber = ""
                dayOfWeek = 1
                startTime = "09:00"
                endTime = "10:00"
                showDialog = true
            }) {
                Icon(Add, contentDescription = "Add Slot")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Day: ${dayName(dayOfWeek)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(uiState.timetableSlots.filter { it.dayOfWeek == dayOfWeek }, key = { it.id }) { slot ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "${slot.subjectName} (${slot.subjectCode})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "${slot.teacherName} • ${slot.roomNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(text = "${slot.startTime} – ${slot.endTime}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                editingSlot = slot
                                subjectName = slot.subjectName
                                subjectCode = slot.subjectCode
                                teacherName = slot.teacherName
                                roomNumber = slot.roomNumber
                                dayOfWeek = slot.dayOfWeek
                                startTime = slot.startTime
                                endTime = slot.endTime
                                showDialog = true
                            }) {
                                Icon(Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { }) {
                                Icon(Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TimetableDialog(
            subjectName = subjectName,
            subjectCode = subjectCode,
            teacherName = teacherName,
            roomNumber = roomNumber,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            onSubjectNameChange = { subjectName = it },
            onSubjectCodeChange = { subjectCode = it },
            onTeacherNameChange = { teacherName = it },
            onRoomNumberChange = { roomNumber = it },
            onStartTimeChange = { startTime = it },
            onEndTimeChange = { endTime = it },
            onDismiss = { showDialog = false },
            onSave = {
                val slot = TimetableSlotEntity(
                    id = editingSlot?.id ?: "${uiState.sectionKey}_d${dayOfWeek}_${startTime.replace(":", "")}",
                    sectionKey = uiState.sectionKey,
                    subjectId = subjectCode,
                    subjectName = subjectName,
                    subjectCode = subjectCode,
                    teacherName = teacherName,
                    roomNumber = roomNumber,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    isElective = false,
                    updatedAtMillis = System.currentTimeMillis()
                )
                viewModel.saveTimetableSlot(slot)
                showDialog = false
            }
        )
    }
}

private fun dayName(day: Int): String = when (day) {
    1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
    5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"; else -> "Unknown"
}

@Composable
private fun TimetableDialog(
    subjectName: String,
    subjectCode: String,
    teacherName: String,
    roomNumber: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    onSubjectNameChange: (String) -> Unit,
    onSubjectCodeChange: (String) -> Unit,
    onTeacherNameChange: (String) -> Unit,
    onRoomNumberChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timetable Slot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = subjectName, onValueChange = onSubjectNameChange, label = { Text("Subject Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectCode, onValueChange = onSubjectCodeChange, label = { Text("Subject Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = teacherName, onValueChange = onTeacherNameChange, label = { Text("Teacher") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = roomNumber, onValueChange = onRoomNumberChange, label = { Text("Room") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(text = "Day: ${dayName(dayOfWeek)} (1=Mon .. 7=Sun)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = onStartTimeChange, label = { Text("Start (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTime, onValueChange = onEndTimeChange, label = { Text("End (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = subjectName.isNotBlank() && subjectCode.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
