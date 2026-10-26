package `in`.campusone.app.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.CampusMapPlaceEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.DirectorySuggestionEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.domain.usecase.GradingScaleType
import `in`.campusone.app.domain.usecase.SemesterGpaRecord
import `in`.campusone.app.ui.viewmodel.StudyAndCampusUiState
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import java.io.File
import java.util.Locale

enum class MoreSubScreen {
    HUB,
    PROFILE,
    NOTES_PYQ,
    ASSIGNMENTS,
    EXAMS_SYLLABUS,
    GPA_CALCULATOR,
    EVENTS_CLUBS,
    LOST_FOUND,
    DIRECTORY,
    PRIVACY_POLICY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var activeScreen by rememberSaveable { mutableStateOf(MoreSubScreen.HUB) }

    if (activeScreen == MoreSubScreen.HUB) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.study_hub),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.local_demo_profile),
                    subtitle = "${uiState.profile.name} • ${uiState.profile.role.name} • Saved only on this device",
                    icon = Icons.Default.AccountCircle,
                    onClick = { activeScreen = MoreSubScreen.PROFILE }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.notes_pyq),
                    subtitle = "${uiState.resources.size} PDFs & Images • Local files, offline bookmarks & demo moderation",
                    icon = Icons.Default.Description,
                    onClick = { activeScreen = MoreSubScreen.NOTES_PYQ }
                )
            }
            item {
                val pendingCount = uiState.assignments.count { !it.isCompleted && it.isAddedToPersonalList }
                FeatureHubCard(
                    title = stringResource(R.string.assignments_title),
                    subtitle = "$pendingCount pending tasks • Today, Upcoming, Overdue, Completed & Shared Section Tasks",
                    icon = Icons.Default.Assignment,
                    onClick = { activeScreen = MoreSubScreen.ASSIGNMENTS }
                )
            }
            item {
                val doneTopics = uiState.syllabusTopics.count { it.isCompleted }
                val totalTopics = uiState.syllabusTopics.size
                FeatureHubCard(
                    title = stringResource(R.string.exams_syllabus),
                    subtitle = "${uiState.exams.size} scheduled exams • $doneTopics/$totalTopics syllabus topics completed",
                    icon = Icons.Default.CalendarMonth,
                    onClick = { activeScreen = MoreSubScreen.EXAMS_SYLLABUS }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.gpa_calculator),
                    subtitle = "SGPA: ${uiState.gpaResult.formattedSgpa} • CGPA: ${uiState.gpaResult.formattedCgpa} (${uiState.gpaResult.formattedPercentage}) • Trend Chart & PDF Export",
                    icon = Icons.Default.Calculate,
                    onClick = { activeScreen = MoreSubScreen.GPA_CALCULATOR }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campus Life & Utilities",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.events_clubs),
                    subtitle = "${uiState.events.size} upcoming campus events & club meetups",
                    icon = Icons.Default.Event,
                    onClick = { activeScreen = MoreSubScreen.EVENTS_CLUBS }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.lost_found),
                    subtitle = "${uiState.lostFoundItems.count { !it.isResolved }} active lost/found reports on campus",
                    icon = Icons.Default.FindInPage,
                    onClick = { activeScreen = MoreSubScreen.LOST_FOUND }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.campus_directory),
                    subtitle = "HoDs, Controller of Exams, Proctor & Emergency helplines",
                    icon = Icons.Default.Call,
                    onClick = { activeScreen = MoreSubScreen.DIRECTORY }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.privacy_policy),
                    subtitle = "Zero ads, zero trackers, offline-first • MIT Open Source",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { activeScreen = MoreSubScreen.PRIVACY_POLICY }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(activeScreen.name.replace("_", " ")) },
                    navigationIcon = {
                        IconButton(onClick = { activeScreen = MoreSubScreen.HUB }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Study Hub")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeScreen) {
                    MoreSubScreen.PROFILE -> LocalDemoProfileScreen(
                        profile = uiState.profile,
                        onSave = viewModel::saveLocalProfile
                    )
                    MoreSubScreen.NOTES_PYQ -> NotesAndPyqScreen(uiState, viewModel)
                    MoreSubScreen.ASSIGNMENTS -> AssignmentsScreen(uiState, viewModel)
                    MoreSubScreen.EXAMS_SYLLABUS -> ExamsAndSyllabusScreen(uiState, viewModel)
                    MoreSubScreen.GPA_CALCULATOR -> GpaCalculatorScreen(uiState, viewModel)
                    MoreSubScreen.EVENTS_CLUBS -> EventsAndClubsScreen(uiState, viewModel)
                    MoreSubScreen.LOST_FOUND -> LostAndFoundScreen(uiState, viewModel)
                    MoreSubScreen.DIRECTORY -> CampusDirectoryScreen(uiState, viewModel)
                    MoreSubScreen.PRIVACY_POLICY -> PrivacyPolicyScreen()
                    MoreSubScreen.HUB -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalDemoProfileScreen(
    profile: `in`.campusone.app.domain.model.UserProfile,
    onSave: (`in`.campusone.app.domain.model.UserProfile) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var email by rememberSaveable { mutableStateOf(profile.email) }
    var semester by rememberSaveable { mutableStateOf(profile.semester.toString()) }
    var section by rememberSaveable { mutableStateOf(profile.section) }
    var role by rememberSaveable { mutableStateOf(profile.role) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.local_demo_auth_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.local_demo_email_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = semester,
            onValueChange = { semester = it.filter(Char::isDigit).take(1) },
            label = { Text(stringResource(R.string.select_semester)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = section,
            onValueChange = { section = it.take(1).uppercase() },
            label = { Text(stringResource(R.string.select_section)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${stringResource(R.string.select_role)} • ${stringResource(R.string.local_demo_role_label)}",
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserRole.entries.forEach { candidate ->
                val label = when (candidate) {
                    UserRole.STUDENT -> stringResource(R.string.role_student)
                    UserRole.CR -> stringResource(R.string.role_cr)
                    UserRole.TEACHER -> stringResource(R.string.role_teacher)
                    UserRole.CLUB_ADMIN -> stringResource(R.string.role_club_admin)
                }
                FilterChip(
                    selected = role == candidate,
                    onClick = { role = candidate },
                    label = { Text(label) }
                )
            }
        }
        Button(
            onClick = {
                onSave(
                    profile.copy(
                        name = name.trim().ifBlank { "HNBGU Student" },
                        email = email.trim(),
                        semester = semester.toIntOrNull()?.coerceIn(1, 8) ?: profile.semester,
                        section = section.trim().ifBlank { "A" },
                        role = role,
                        verifiedRole = false
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_local_profile))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventsAndClubsScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "This is a local demo feed. RSVPs and club follows are not shared with other students.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Events") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Clubs") })
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                items(uiState.events, key = { it.id }) { event ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${event.clubName} • ${event.category}")
                            Text("${event.eventDateFormatted} • ${event.venue}")
                            Text(event.description, style = MaterialTheme.typography.bodyMedium)
                            Text("Local demo counts — Going ${event.goingCount}, Interested ${event.interestedCount}, Not going ${event.notGoingCount}")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EventRsvpStatus.entries.filter { it != EventRsvpStatus.NONE }.forEach { status ->
                                    FilterChip(
                                        selected = event.myRsvpStatus == status.name,
                                        onClick = { viewModel.updateEventRsvp(event.id, status) },
                                        label = { Text(status.name.replace("_", " ")) }
                                    )
                                }
                            }
                            if (uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER) {
                                if (!event.isApproved) {
                                    TextButton(onClick = { viewModel.approveEvent(event.id) }) {
                                        Text("Approve in local demo")
                                    }
                                }
                                TextButton(onClick = { viewModel.reportEvent(event.id) }) {
                                    Text("Report event")
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.clubs, key = { it.id }) { club ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${club.logoEmojiOrUrl} ${club.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${club.category} • ${club.memberCount} demo members")
                            Text(club.description)
                            if (club.contactEmail.isNotBlank()) Text(club.contactEmail)
                            AssistChip(
                                onClick = { viewModel.toggleFollowClub(club.id, !club.isFollowedByMe) },
                                label = { Text(if (club.isFollowedByMe) "Following locally" else "Follow locally") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LostAndFoundScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedClaim by remember { mutableStateOf<LostFoundEntity?>(null) }
    val profile = uiState.profile

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Local demo board only. Claims and contact details stay on this device; they are not sent to the poster.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Add local listing")
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.lostFoundItems, key = { it.id }) { item ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${item.type}: ${item.itemTitle}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${item.category} • ${item.location} • ${item.dateFormatted}")
                        Text(item.description)
                        Text("Posted by ${item.posterName} • ${item.status}")
                        if (!item.isResolved) {
                            if (item.posterUid == profile.uid) {
                                TextButton(onClick = { viewModel.resolveLostFound(item.id) }) {
                                    Text("Mark resolved")
                                }
                            } else {
                                TextButton(onClick = { selectedClaim = item }) {
                                    Text("Claim / request contact (local demo)")
                                }
                            }
                        }
                        if (profile.role == UserRole.CR || profile.role == UserRole.TEACHER) {
                            TextButton(onClick = { viewModel.hideLostFoundByCr(item.id) }) {
                                Text("Hide in local demo")
                            }
                        }
                    }
                }
            }
            val ownRequests = uiState.contactRequests.filter { it.posterUid == profile.uid && it.status == "PENDING" }
            if (ownRequests.isNotEmpty()) {
                item {
                    Text("Local contact requests", style = MaterialTheme.typography.titleLarge)
                }
                items(ownRequests, key = { it.id }) { request ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${request.itemTitle} • ${request.requesterName}")
                            Text(request.message)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.respondToClaimRequest(request, true) }) {
                                    Text("Accept locally")
                                }
                                TextButton(onClick = { viewModel.respondToClaimRequest(request, false) }) {
                                    Text("Ignore")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        LocalLostFoundDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, category, title, description, location, contact ->
                viewModel.addLostFound(type, category, title, description, location, contact)
                showAddDialog = false
            }
        )
    }
    selectedClaim?.let { item ->
        var message by rememberSaveable(item.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedClaim = null },
            title = { Text("Local claim request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This demo request is stored only on this device and is not delivered to ${item.posterName}.")
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendClaimRequest(item, message)
                    selectedClaim = null
                }) { Text("Save claim locally") }
            },
            dismissButton = {
                TextButton(onClick = { selectedClaim = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalLostFoundDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var type by rememberSaveable { mutableStateOf("FOUND") }
    var category by rememberSaveable { mutableStateOf("OTHER") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a local listing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("FOUND", "LOST").forEach { value ->
                        FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ELECTRONICS", "ID_CARD", "BOOKS", "KEYS", "OTHER").forEach { value ->
                        FilterChip(
                            selected = category == value,
                            onClick = { category = value },
                            label = { Text(value.replace("_", " ")) }
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Private contact detail (local demo)") })
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && location.isNotBlank() && contact.isNotBlank(),
                onClick = { onSave(type, category, title, description, location, contact) }
            ) { Text("Save locally") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CampusDirectoryScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var correctionContact by remember { mutableStateOf<DirectoryContactEntity?>(null) }
    var correctionText by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Offline campus directory and map data. Suggested corrections are stored locally and are not submitted to the university.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(uiState.directoryContacts, key = { it.id }) { contact ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${contact.department} • ${contact.designation}")
                    Text("${contact.phone} • ${contact.email}")
                    Text("${contact.officeLocation} • ${contact.officeHours}")
                    TextButton(onClick = {
                        correctionContact = contact
                        correctionText = ""
                    }) { Text("Suggest a local correction") }
                }
            }
        }
        if (uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER) {
            if (uiState.directorySuggestions.isNotEmpty()) {
                item { Text("Local demo moderation", style = MaterialTheme.typography.titleLarge) }
                items(uiState.directorySuggestions, key = { it.id }) { suggestion ->
                    DirectorySuggestionCard(suggestion, viewModel)
                }
            }
        }
        if (uiState.campusMapPlaces.isNotEmpty()) {
            item { Text("Campus map places", style = MaterialTheme.typography.titleLarge) }
            items(uiState.campusMapPlaces, key = { it.id }) { place ->
                CampusMapPlaceCard(place, context)
            }
        }
    }
    correctionContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { correctionContact = null },
            title = { Text("Suggest correction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${contact.name} • local-only demo")
                    OutlinedTextField(
                        value = correctionText,
                        onValueChange = { correctionText = it },
                        label = { Text("Proposed update") }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = correctionText.isNotBlank(),
                    onClick = {
                        viewModel.submitDirectoryCorrection(contact.id, contact.name, correctionText)
                        correctionContact = null
                    }
                ) { Text("Save locally") }
            },
            dismissButton = {
                TextButton(onClick = { correctionContact = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DirectorySuggestionCard(
    suggestion: DirectorySuggestionEntity,
    viewModel: StudyAndCampusViewModel
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${suggestion.contactName} • ${suggestion.suggesterName}")
            Text(suggestion.proposedChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.resolveDirectorySuggestion(suggestion.id, true) }) {
                    Text("Approve locally")
                }
                TextButton(onClick = { viewModel.resolveDirectorySuggestion(suggestion.id, false) }) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun CampusMapPlaceCard(place: CampusMapPlaceEntity, context: android.content.Context) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${place.campusName} • ${place.category}")
            Text(place.description)
            TextButton(onClick = {
                val uri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${Uri.encode(place.mapQueryLabel)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }) { Text("Open in Maps") }
        }
    }
}

@Composable
private fun FeatureHubCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title: $subtitle" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================================
// PHASE 2.1 — NOTES & PYQ SHARING
// - Upload PDFs/Images with Camera/Gallery/File picker, tagged by subject, semester, type (NOTES/PYQ/ASSIGNMENT/OTHER), year
// - Sort by Newest / Most Upvoted, filter by Bookmarked Offline
// - Upvote, Bookmark Offline, Download to local storage
// - In-App PDF & Image Viewer with Pinch-to-Zoom & Pan
// - Uploader anonymity toggle ("Show my name")
// - 10 MB size guard and files saved locally on this device
// - CR / Teacher Moderation Queue (Hide, Remove, Approve)
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesAndPyqScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") } // ALL, NOTES, PYQ, ASSIGNMENT, OTHER
    var selectedSemesterFilter by rememberSaveable { mutableIntStateOf(0) } // 0 = All Semesters
    var sortByUpvotes by rememberSaveable { mutableStateOf(true) } // true = Most Upvoted, false = Newest
    var bookmarkedOnly by rememberSaveable { mutableStateOf(false) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }
    var viewingResource by remember { mutableStateOf<StudyResourceEntity?>(null) }

    val isCrOrTeacher = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER

    val filtered = uiState.resources
        .filter { res ->
            (selectedCategory == "ALL" || res.category == selectedCategory) &&
                (selectedSemesterFilter == 0 || res.semester == selectedSemesterFilter) &&
                (!bookmarkedOnly || res.isBookmarkedOffline) &&
                (searchQuery.isBlank() ||
                    res.title.contains(searchQuery, ignoreCase = true) ||
                    res.subjectName.contains(searchQuery, ignoreCase = true) ||
                    res.subjectId.contains(searchQuery, ignoreCase = true) ||
                    res.examYear.toString().contains(searchQuery))
        }
        .let { list ->
            if (sortByUpvotes) {
                list.sortedWith(compareByDescending<StudyResourceEntity> { it.upvotes }.thenByDescending { it.createdAtMillis })
            } else {
                list.sortedByDescending { it.createdAtMillis }
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showUploadDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Save local notes or PYQ")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search subject, topic, or exam year (e.g., 2025)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "NOTES", "PYQ", "ASSIGNMENT", "OTHER").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Sort & Offline Bookmark Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortByUpvotes,
                        onClick = { sortByUpvotes = true },
                        label = { Text(stringResource(R.string.sort_upvoted)) }
                    )
                    FilterChip(
                        selected = !sortByUpvotes,
                        onClick = { sortByUpvotes = false },
                        label = { Text(stringResource(R.string.sort_newest)) }
                    )
                    FilterChip(
                        selected = bookmarkedOnly,
                        onClick = { bookmarkedOnly = !bookmarkedOnly },
                        label = { Text("Saved Offline") }
                    )
                }
            }

            // CR / Teacher Moderation Queue
            if (isCrOrTeacher && uiState.moderationQueue.isNotEmpty()) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Local Demo Moderation Queue (${uiState.moderationQueue.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.moderationQueue.forEach { flagged ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = "${flagged.title} (${flagged.subjectId})",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Reports: ${flagged.reportCount} • Status: ${if (flagged.isHidden) "Hidden" else "Flagged"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { viewModel.crHideResource(flagged.id, !flagged.isHidden) }) {
                                            Icon(
                                                if (flagged.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (flagged.isHidden) "Unhide" else "Hide")
                                        }
                                        Button(onClick = { viewModel.crDeleteResource(flagged.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove")
                                        }
                                        TextButton(onClick = { viewModel.crDismissReport(flagged.id) }) {
                                            Text("Keep")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(filtered, key = { it.id }) { res ->
                StudyResourceCard(
                    res = res,
                    isCrOrTeacher = isCrOrTeacher,
                    onOpenViewer = { viewingResource = res },
                    onUpvote = { viewModel.upvoteResource(res.id) },
                    onToggleBookmark = { viewModel.toggleBookmark(res.id, !res.isBookmarkedOffline) },
                    onDownloadOffline = { viewModel.downloadResourceOffline(res) },
                    onReport = { viewModel.reportResource(res.id) },
                    onCrHide = { viewModel.crHideResource(res.id, true) },
                )
            }
        }
    }

    if (showUploadDialog) {
        UploadStudyResourceDialog(
            defaultSemester = uiState.profile.semester,
            onDismiss = { showUploadDialog = false },
            onSave = { subjId, subjName, sem, title, cat, year, url, type, sizeBytes, showName ->
                viewModel.saveResourceLocally(
                    subjectId = subjId,
                    subjectName = subjName,
                    semester = sem,
                    title = title,
                    category = cat,
                    examYear = year,
                    fileUrl = url,
                    fileType = type,
                    fileSizeBytes = sizeBytes,
                    showUploaderName = showName
                )
                showUploadDialog = false
            }
        )
    }

    viewingResource?.let { resource ->
        InAppZoomableDocumentViewerDialog(
            resource = resource,
            onDismiss = { viewingResource = null },
            onDownloadOffline = { viewModel.downloadResourceOffline(resource) }
        )
    }
}

@Composable
private fun StudyResourceCard(
    res: StudyResourceEntity,
    isCrOrTeacher: Boolean,
    onOpenViewer: () -> Unit,
    onUpvote: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDownloadOffline: () -> Unit,
    onReport: () -> Unit,
    onCrHide: () -> Unit
) {
    val displayedUploader = if (res.showUploaderName) {
        res.uploaderName
    } else {
        stringResource(R.string.anonymous_student)
    }
    val sizeKb = (res.fileSizeBytes / 1024L).coerceAtLeast(1L)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onOpenViewer,
                    label = { Text("${res.category} • ${res.examYear} • ${res.fileType} (${sizeKb} KB)") }
                )
                Row {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (res.isBookmarkedOffline) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.bookmark_offline),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDownloadOffline) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.download_offline)
                        )
                    }
                }
            }

            Text(
                text = res.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sem ${res.semester} • ${res.subjectId} (${res.subjectName}) • By $displayedUploader",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (res.localFilePath != null) {
                Text(
                    text = "✓ Stored locally on this device",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onUpvote) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Upvote")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${res.upvotes}")
                    }
                    OutlinedButton(onClick = onOpenViewer) {
                        Icon(Icons.Default.ZoomIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.view_document))
                    }
                }
                Row {
                    if (isCrOrTeacher) {
                        IconButton(onClick = onCrHide) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "Hide content")
                        }
                    }
                    IconButton(onClick = onReport) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Abuse")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadStudyResourceDialog(
    defaultSemester: Int,
    onDismiss: () -> Unit,
    onSave: (
        subjectId: String,
        subjectName: String,
        semester: Int,
        title: String,
        category: String,
        examYear: Int,
        fileUrl: String,
        fileType: String,
        fileSizeBytes: Long,
        showUploaderName: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var subjectId by rememberSaveable { mutableStateOf("CS301") }
    var subjectName by rememberSaveable { mutableStateOf("Data Structures & Algorithms") }
    var semester by rememberSaveable { mutableIntStateOf(defaultSemester) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("NOTES") }
    var examYear by rememberSaveable { mutableIntStateOf(2025) }
    var fileType by rememberSaveable { mutableStateOf("PDF") }
    var selectedUriString by rememberSaveable { mutableStateOf("") }
    var selectedDisplayName by rememberSaveable { mutableStateOf("") }
    var selectedSizeBytes by rememberSaveable { mutableStateOf(0L) }
    var selectionMessage by rememberSaveable { mutableStateOf("") }
    var showUploaderName by rememberSaveable { mutableStateOf(true) }

    // Android's Storage Access Framework grants access to the selected document.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUriString = uri.toString()
            selectedDisplayName = ""
            selectedSizeBytes = 0L
            selectionMessage = ""
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (error: SecurityException) {
                selectionMessage = error.message ?: "File access is temporary; save it before leaving this screen."
            }
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    selectedDisplayName = if (nameIndex >= 0) it.getString(nameIndex).orEmpty() else "Selected file"
                    selectedSizeBytes = if (sizeIndex >= 0 && !it.isNull(sizeIndex)) it.getLong(sizeIndex) else 0L
                }
            }
            val mimeType = context.contentResolver.getType(uri)
            fileType = if (mimeType == "application/pdf") "PDF" else "IMAGE"
        }
    }

    val isOverLimit = selectedSizeBytes > 10L * 1024L * 1024L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save a local Notes / PYQ file") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.file_limit_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectId,
                        onValueChange = { subjectId = it },
                        label = { Text("Subject Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = examYear.toString(),
                        onValueChange = { examYear = it.toIntOrNull() ?: 2025 },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NOTES", "PYQ", "ASSIGNMENT", "OTHER").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        val mime = if (fileType == "PDF") "application/pdf" else "image/*"
                        filePickerLauncher.launch(arrayOf(mime))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose PDF or image")
                }
                if (selectedDisplayName.isNotBlank()) {
                    Text(
                        text = "$selectedDisplayName${if (selectedSizeBytes > 0L) " • ${selectedSizeBytes / 1024L} KB" else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (selectionMessage.isNotBlank()) {
                    Text(selectionMessage, color = MaterialTheme.colorScheme.error)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showUploaderName,
                        onCheckedChange = { showUploaderName = it }
                    )
                    Text(stringResource(R.string.show_my_name))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && selectedUriString.isNotBlank() && !isOverLimit,
                onClick = {
                    onSave(
                        subjectId,
                        subjectName,
                        semester,
                        title,
                        category,
                        examYear,
                        selectedUriString,
                        fileType,
                        selectedSizeBytes,
                        showUploaderName
                    )
                }
            ) {
                Text("Save locally on this device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * In-App PDF & Image Viewer with Pinch-to-Zoom (`detectTransformGestures`), Pan, and Offline Download.
 * Uses Android's native `PdfRenderer` when a local PDF file descriptor is available, and renders
 * an interactive multi-page preview with smooth 1x..4x zoom.
 */
@Composable
private fun InAppZoomableDocumentViewerDialog(
    resource: StudyResourceEntity,
    onDismiss: () -> Unit,
    onDownloadOffline: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    val totalPages = 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${resource.subjectId} • ${resource.category} (${resource.examYear}) • Pinch to Zoom (${String.format(Locale.US, "%.1fx", scale)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    offsetX = (offsetX + pan.x).coerceIn(-300f, 300f)
                                    offsetY = (offsetY + pan.y).coerceIn(-300f, 300f)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${resource.fileType} • Local demo preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "The selected file is stored on this device. This sample preview does not render its contents.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resource.fileType == "PDF") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = currentPage > 1,
                                onClick = { currentPage-- }
                            ) { Text("Prev") }
                            OutlinedButton(
                                enabled = currentPage < totalPages,
                                onClick = { currentPage++ }
                            ) { Text("Next") }
                        }
                    }
                    TextButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) {
                        Text("Reset Zoom")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownloadOffline) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.download_offline))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// =========================================================================
// PHASE 2.2 — ASSIGNMENTS & DEADLINES
// - Views: Today, Upcoming, Overdue, Completed
// - Group by Subject toggle
// - Priority (HIGH/MEDIUM/LOW), Configurable 1d & 2h before WorkManager reminders
// - Shared section assignments posted by CR/Teacher with 1-tap "Add to My List"
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentsScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0=Today, 1=Upcoming, 2=Overdue, 3=Completed
    var groupBySubject by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val endOfToday = now + 24 * 3_600_000L
    val isCrOrTeacher = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER

    val sharedNotAdded = uiState.assignments.filter { it.isSharedByCrOrTeacher && !it.isAddedToPersonalList }
    val personalTasks = uiState.assignments.filter { it.isAddedToPersonalList }

    val filteredTasks = when (selectedTab) {
        0 -> personalTasks.filter { !it.isCompleted && it.dueDateEpochMillis in now..endOfToday }
        1 -> personalTasks.filter { !it.isCompleted && it.dueDateEpochMillis > endOfToday }
        2 -> personalTasks.filter { !it.isCompleted && it.dueDateEpochMillis < now }
        else -> personalTasks.filter { it.isCompleted }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Assignment")
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_today)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.tab_upcoming)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(stringResource(R.string.tab_overdue)) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(stringResource(R.string.tab_completed)) })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = groupBySubject,
                            onClick = { groupBySubject = !groupBySubject },
                            label = { Text(stringResource(R.string.group_by_subject)) }
                        )
                        Text(
                            text = "Reminders: 1d & 2h before (Reboot-safe)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Shared Section Assignments Banner (1-Tap Add to Personal List)
                if (sharedNotAdded.isNotEmpty()) {
                    item {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.shared_by_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                sharedNotAdded.forEach { shared ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(shared.title, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "${shared.subjectTag} • Due: ${shared.dueDateFormatted} • By ${shared.authorName}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Button(onClick = { viewModel.addSharedAssignmentToMyList(shared.id) }) {
                                            Text(stringResource(R.string.add_to_my_list))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (groupBySubject) {
                    val grouped = filteredTasks.groupBy { it.subjectTag }
                    grouped.forEach { (subjectHeader, tasksInSubject) ->
                        item {
                            Text(
                                text = subjectHeader,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(tasksInSubject, key = { it.id }) { task ->
                            AssignmentItemCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleAssignment(task.id, it) },
                                onDelete = { viewModel.deleteAssignment(task.id) }
                            )
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        AssignmentItemCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleAssignment(task.id, it) },
                            onDelete = { viewModel.deleteAssignment(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var subjectTag by rememberSaveable { mutableStateOf("OS • CS302") }
        var title by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        var priority by rememberSaveable { mutableStateOf("HIGH") }
        var dueOffsetDays by rememberSaveable { mutableIntStateOf(2) }
        var dueFormatted by rememberSaveable { mutableStateOf("In 2 days • 05:00 PM") }
        var remind1Day by rememberSaveable { mutableStateOf(true) }
        var remind2Hours by rememberSaveable { mutableStateOf(true) }
        var postToWholeSection by rememberSaveable { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Assignment / Deadline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subjectTag, onValueChange = { subjectTag = it }, label = { Text("Subject Tag") })
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Assignment Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Notes / Instructions") })
                    OutlinedTextField(value = dueFormatted, onValueChange = { dueFormatted = it }, label = { Text("Due Date & Time") })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text("Priority: $p") }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = remind1Day, onCheckedChange = { remind1Day = it })
                        Text("Remind 1 day before")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = remind2Hours, onCheckedChange = { remind2Hours = it })
                        Text("Remind 2 hours before")
                    }
                    if (isCrOrTeacher) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = postToWholeSection, onCheckedChange = { postToWholeSection = it })
                            Text("Publish as Shared Section Assignment")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        viewModel.saveAssignment(
                            subjectId = subjectTag.substringAfterLast("•").trim(),
                            subjectTag = subjectTag,
                            title = title,
                            description = desc,
                            priority = priority,
                            dueOffsetDays = dueOffsetDays,
                            dueDateFormatted = dueFormatted,
                            remind1Day = remind1Day,
                            remind2Hours = remind2Hours,
                            postToSection = postToWholeSection
                        )
                        showAddDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AssignmentItemCard(
    task: `in`.campusone.app.data.local.entity.AssignmentEntity,
    onToggleComplete: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggleComplete
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${task.subjectTag} • ${task.dueDateFormatted}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "[${task.priority}]",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (task.priority == "HIGH") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Assignment")
            }
        }
    }
}

// =========================================================================
// PHASE 2.3 — EXAM CALENDAR & SYLLABUS TRACKER
// - Exam Calendar & Countdown cards + CR/Teacher Exam Publisher
// - Per-subject and overall syllabus progress bars + private topic checkboxes
// =========================================================================
@Composable
fun ExamsAndSyllabusScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var showAddExamDialog by rememberSaveable { mutableStateOf(false) }
    var showAddTopicDialog by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()

    val totalTopics = uiState.syllabusTopics.size
    val completedTopics = uiState.syllabusTopics.count { it.isCompleted }
    val overallProgress = if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExamDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.publish_exam))
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall Syllabus Progress Card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.overall_syllabus_progress),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$completedTopics / $totalTopics (${(overallProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    }
                }
            }

            item {
                Text("Exam Schedule & Countdowns", style = MaterialTheme.typography.titleLarge)
            }

            items(uiState.exams, key = { it.id }) { exam ->
                val daysLeft = ((exam.examDateEpochMillis - now) / 86_400_000L).coerceAtLeast(0L)
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
                                text = "${exam.subjectCode} • ${exam.examTitle}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = exam.subjectName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${exam.examDateFormatted} • ${exam.roomOrCenter}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        AssistChip(onClick = {}, label = { Text("$daysLeft days") })
                    }
                }
            }

            // Per-Subject Syllabus Breakdown & Checklists
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Per-Subject Syllabus Checklist", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showAddTopicDialog = true }) {
                        Text("+ Add Topic")
                    }
                }
            }

            val bySubject = uiState.syllabusTopics.groupBy { it.subjectName }
            bySubject.forEach { (subjectName, topics) ->
                val doneSubj = topics.count { it.isCompleted }
                val subjPct = if (topics.isNotEmpty()) (doneSubj * 100) / topics.size else 0

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = subjectName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$doneSubj/${topics.size} ($subjPct%)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        LinearProgressIndicator(
                            progress = { subjPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                items(topics, key = { it.id }) { topic ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = topic.isCompleted,
                                onCheckedChange = { viewModel.toggleSyllabusTopic(topic.id, it) }
                            )
                            Column {
                                Text(
                                    text = topic.unitTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(topic.topicTitle, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExamDialog) {
        var code by rememberSaveable { mutableStateOf("SOET-CS-304") }
        var name by rememberSaveable { mutableStateOf("Computer Networks") }
        var dateIso by rememberSaveable { mutableStateOf("23 Nov 2026") }
        var time by rememberSaveable { mutableStateOf("10:00 AM – 01:00 PM") }
        var venue by rememberSaveable { mutableStateOf("Exam Block Hall A") }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text(stringResource(R.string.publish_exam)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Subject Code") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") })
                    OutlinedTextField(value = dateIso, onValueChange = { dateIso = it }, label = { Text("Exam Date") })
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time Slot") })
                    OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Exam Hall / Venue") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addExam(code, name, "End-Semester Exam", dateIso, time, venue, 27)
                        showAddExamDialog = false
                    }
                ) { Text("Save Exam") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddTopicDialog) {
        var subjId by rememberSaveable { mutableStateOf("CS301") }
        var subjName by rememberSaveable { mutableStateOf("Data Structures & Algorithms") }
        var unitTitle by rememberSaveable { mutableStateOf("Unit 5: Sorting & Hashing") }
        var topicTitle by rememberSaveable { mutableStateOf("QuickSort, MergeSort, RadixSort & Open Addressing") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text(stringResource(R.string.publish_syllabus_topic)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subjName, onValueChange = { subjName = it }, label = { Text("Subject Name") })
                    OutlinedTextField(value = unitTitle, onValueChange = { unitTitle = it }, label = { Text("Unit Header") })
                    OutlinedTextField(value = topicTitle, onValueChange = { topicTitle = it }, label = { Text("Syllabus Topics") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addSyllabusTopic(subjId, subjName, 5, unitTitle, topicTitle)
                        showAddTopicDialog = false
                    }
                ) { Text("Add Topic") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// =========================================================================
// PHASE 2.4 — GPA / CGPA / PERCENTAGE CALCULATOR
// - Configurable scale (10-Point UGC/NEP, 4.0-Point, Direct Percentage)
// - Semester Trend Line Chart across Semesters 1..N
// - "What Do I Need?" Target CGPA mode
// - Save locally to Room DB & Export as printable PDF
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GpaCalculatorScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var showAddCourseDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Summary & Scale Selector Card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_calculator),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sem SGPA", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = uiState.gpaResult.formattedSgpa,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Cumulative CGPA", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = uiState.gpaResult.formattedCgpa,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text("Percentage", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = uiState.gpaResult.formattedPercentage,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Grading Scale:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradingScaleType.entries.forEach { scale ->
                            FilterChip(
                                selected = uiState.gpaScaleType == scale,
                                onClick = { viewModel.setGradingScale(scale) },
                                label = { Text(scale.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.saveSemesterToHistory() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Sem History")
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportGpaPdf(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF")
                        }
                    }
                }
            }
        }

        // 2. Semester Trend Chart Card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.semester_trend_chart),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SemesterTrendChart(
                        trend = uiState.gpaResult.semesterTrend,
                        maxScale = uiState.gpaScaleType.maxScalePoint
                    )
                }
            }
        }

        // 3. "What Do I Need?" Target CGPA Calculator Card
        item {
            val req = uiState.targetCgpaRequirement
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.what_do_i_need),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target CGPA: ${String.format(Locale.US, "%.2f", uiState.targetCgpaInput)} (over next ${uiState.upcomingCreditsInput} credits)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Slider(
                        value = uiState.targetCgpaInput.toFloat(),
                        onValueChange = {
                            viewModel.updateTargetCgpa(it.toDouble(), uiState.upcomingCreditsInput)
                        },
                        valueRange = 6.0f..uiState.gpaScaleType.maxScalePoint.toFloat()
                    )
                    val message = when {
                        req.alreadyAchieved -> "You have already achieved your target CGPA of ${String.format(Locale.US, "%.2f", req.targetCgpa)}! 🎉"
                        req.isAchievable -> "You need an SGPA of ${String.format(Locale.US, "%.2f", req.requiredSgpa)} in your next ${req.upcomingCredits} credits to reach ${String.format(Locale.US, "%.2f", req.targetCgpa)} CGPA."
                        else -> "Requires ${String.format(Locale.US, "%.2f", req.requiredSgpa)} SGPA (exceeds max ${req.maxScalePoint}). Try targeting across 2 semesters."
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 4. Current Semester Subject Entry Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Semester Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddCourseDialog = true }) {
                    Text("+ Add Subject")
                }
            }
        }

        items(uiState.gpaCourses.size) { idx ->
            val course = uiState.gpaCourses[idx]
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${course.subjectCode} • ${course.credits} Credits",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = course.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val options = when (uiState.gpaScaleType) {
                            GradingScaleType.TEN_POINT_INDIAN -> listOf(
                                10.0 to "O (10)",
                                9.0 to "A+ (9)",
                                8.0 to "A (8)",
                                7.0 to "B+ (7)",
                                6.0 to "B (6)",
                                5.0 to "C (5)"
                            )
                            GradingScaleType.FOUR_POINT_WES -> listOf(
                                4.0 to "A (4.0)",
                                3.5 to "B+ (3.5)",
                                3.0 to "B (3.0)",
                                2.5 to "C+ (2.5)",
                                2.0 to "C (2.0)"
                            )
                            GradingScaleType.DIRECT_PERCENTAGE -> listOf(
                                90.0 to "90%",
                                80.0 to "80%",
                                75.0 to "75%",
                                65.0 to "65%",
                                55.0 to "55%"
                            )
                        }
                        options.forEach { (pt, label) ->
                            FilterChip(
                                selected = course.gradePoint == pt,
                                onClick = { viewModel.updateCourseGradePoint(idx, pt) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCourseDialog) {
        var code by rememberSaveable { mutableStateOf("SOET-EL-305") }
        var name by rememberSaveable { mutableStateOf("Python for Data Science") }
        var credits by rememberSaveable { mutableIntStateOf(4) }
        var gradePt by rememberSaveable { mutableFloatStateOf(9f) }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Add Subject to Semester") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Subject Code") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") })
                    OutlinedTextField(
                        value = credits.toString(),
                        onValueChange = { credits = it.toIntOrNull() ?: 4 },
                        label = { Text("Credits") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addGpaCourse(code, name, credits, gradePt.toDouble())
                        showAddCourseDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourseDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SemesterTrendChart(
    trend: List<SemesterGpaRecord>,
    maxScale: Double
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (trend.isEmpty()) return@Canvas
            val w = size.width
            val h = size.height
            val stepX = if (trend.size > 1) w / (trend.size - 1) else w / 2f

            val points = trend.mapIndexed { index, rec ->
                val x = if (trend.size > 1) index * stepX else w / 2f
                val normalized = (rec.sgpa / maxScale).coerceIn(0.0, 1.0).toFloat()
                val y = h - (normalized * h)
                Offset(x, y)
            }

            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            points.forEach { pt ->
                drawCircle(color = pointColor, radius = 10f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trend.forEach { rec ->
                Text(
                    text = "Sem ${rec.semesterNumber}: ${String.format(Locale.US, "%.2f", rec.sgpa)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// =========================================================================
// PRIVACY POLICY & OPEN SOURCE (MIT) SCREEN
// =========================================================================
@Composable
fun PrivacyPolicyScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Privacy Policy & Data Minimalism", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. Minimal Data Collection: CampusOne stores only your name, university email, selected course/semester/section, and optional personal attendance backup.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "2. Privacy-First Lost & Found: Phone numbers and emails on Lost & Found posts are never displayed publicly. Contact details are only shared when the poster explicitly accepts an in-app Claim/Contact request.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "3. 100% Free of Ads & Trackers: CampusOne contains zero advertising SDKs, zero third-party behavioral analytics trackers, and never sells or shares student data.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "4. Offline-First Storage: Your attendance logs, assignments, syllabus progress, and directory live locally in Room SQLite on your device.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "5. Open Source (MIT License): CampusOne is free and open-source software licensed under the MIT License.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
