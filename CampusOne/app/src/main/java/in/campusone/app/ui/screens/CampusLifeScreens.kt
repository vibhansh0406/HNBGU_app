package `in`.campusone.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusUiState

// ============================================================================
// EVENTS & CLUBS SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsAndClubsScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Events, 1 = Clubs
    var showCreateEventDialog by remember { mutableStateOf(false) }

    val canCreateEvents = uiState.profile.role == UserRole.CR ||
        uiState.profile.role == UserRole.TEACHER ||
        uiState.profile.role == UserRole.ADMIN ||
        uiState.profile.role == UserRole.MODERATOR

    Scaffold(
        topBar = {
            Column {
                Text(
                    text = "Events & Clubs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Events") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Clubs") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && canCreateEvents) {
                FloatingActionButton(onClick = { showCreateEventDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Event")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> EventsList(
                    events = uiState.events.filter { it.isApproved && !it.isFlagged },
                    onRsvp = { event, status -> viewModel.updateEventRsvp(event.id, status) }
                )
                else -> ClubsList(
                    clubs = uiState.clubs.filter { it.isApproved && !it.isFlagged },
                    onToggleFollow = { club, follow -> viewModel.toggleFollowClub(club.id, follow) }
                )
            }
        }
    }

    if (showCreateEventDialog) {
        CreateEventDialog(
            clubs = uiState.clubs.filter { it.isApproved },
            onDismiss = { showCreateEventDialog = false },
            onCreate = { clubId, clubName, category, title, desc, venue, dateFormatted, daysFromNow ->
                viewModel.createClubEvent(clubId, clubName, category, title, desc, venue, dateFormatted, daysFromNow)
                showCreateEventDialog = false
            }
        )
    }
}

@Composable
private fun EventsList(
    events: List<CampusEventEntity>,
    onRsvp: (CampusEventEntity, EventRsvpStatus) -> Unit
) {
    if (events.isEmpty()) {
        EmptyState("No upcoming events", Icons.Default.Event)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(event.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(event.clubName, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Venue: ${event.venue} | ${event.eventDateFormatted}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = event.myRsvpStatus == "GOING",
                            onClick = {
                                onRsvp(event, if (event.myRsvpStatus == "GOING") EventRsvpStatus.NONE else EventRsvpStatus.GOING)
                            },
                            label = { Text("Going (${event.goingCount})") }
                        )
                        FilterChip(
                            selected = event.myRsvpStatus == "INTERESTED",
                            onClick = {
                                onRsvp(event, if (event.myRsvpStatus == "INTERESTED") EventRsvpStatus.NONE else EventRsvpStatus.INTERESTED)
                            },
                            label = { Text("Interested (${event.interestedCount})") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubsList(
    clubs: List<ClubEntity>,
    onToggleFollow: (ClubEntity, Boolean) -> Unit
) {
    if (clubs.isEmpty()) {
        EmptyState("No clubs yet", Icons.Default.Group)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(clubs, key = { it.id }) { club ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(club.logoEmojiOrUrl, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(club.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${club.category} • ${club.memberCount} members", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(club.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(club.contactEmail, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = club.isFollowedByMe,
                            onClick = { onToggleFollow(club, !club.isFollowedByMe) },
                            label = { Text(if (club.isFollowedByMe) "Following" else "Follow") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateEventDialog(
    clubs: List<ClubEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, String, Int) -> Unit
) {
    var clubIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("TECH") }
    var daysFromNow by remember { mutableStateOf("7") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Propose Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (clubs.isEmpty()) {
                    Text("No approved clubs available.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Club: ${clubs[clubIndex.coerceIn(0, clubs.lastIndex)].name}", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { clubIndex = (clubIndex + 1) % clubs.size }) { Text("Switch club") }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (TECH/CULTURAL/SPORTS)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = daysFromNow, onValueChange = { daysFromNow = it }, label = { Text("Days from now") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (clubs.isNotEmpty() && title.isNotBlank() && venue.isNotBlank()) {
                                val club = clubs[clubIndex.coerceIn(0, clubs.lastIndex)]
                                onCreate(club.id, club.name, category.uppercase(), title.trim(), desc.trim(), venue.trim(), "In $daysFromNow day(s)", daysFromNow.toIntOrNull() ?: 7)
                            }
                        }
                    ) { Text("Create") }
                }
            }
        }
    }
}

// ============================================================================
// LOST & FOUND SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var selectedType by rememberSaveable { mutableStateOf("ALL") } // ALL / LOST / FOUND
    var showAddDialog by remember { mutableStateOf(false) }
    var claimItem by remember { mutableStateOf<LostFoundEntity?>(null) }

    val items = uiState.lostFoundItems
        .filter { !it.isArchived && !it.isHidden && !it.isFlagged }
        .filter { selectedType == "ALL" || it.type == selectedType }

    Scaffold(
        topBar = {
            Text(
                text = "Lost & Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Report Item")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "LOST", "FOUND").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) }
                    )
                }
            }
            if (items.isEmpty()) {
                EmptyState("Nothing reported yet", Icons.Default.Search)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.type,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.type == "LOST") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.category, style = MaterialTheme.typography.labelSmall)
                                    if (item.isResolved) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("RESOLVED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(item.itemTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Location: ${item.location} | ${item.dateFormatted}", style = MaterialTheme.typography.bodySmall)
                                Text("Posted by ${item.posterName}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!item.isResolved) {
                                        OutlinedButton(onClick = { claimItem = item }) { Text("I found / claim") }
                                        if (item.posterUid == uiState.profile.uid) {
                                            Button(onClick = { viewModel.resolveLostFound(item.id) }) { Text("Mark resolved") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLostFoundDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { type, category, title, desc, location, contact ->
                viewModel.addLostFound(type, category, title, desc, location, contact)
                showAddDialog = false
            }
        )
    }

    claimItem?.let { item ->
        ClaimRequestDialog(
            item = item,
            onDismiss = { claimItem = null },
            onSubmit = { message ->
                viewModel.sendClaimRequest(item, message)
                claimItem = null
            }
        )
    }
}

@Composable
private fun AddLostFoundDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf("LOST") }
    var category by remember { mutableStateOf("ELECTRONICS") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Report Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOST", "FOUND").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Preferred contact (kept private)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && location.isNotBlank() && contact.isNotBlank()) {
                                onSubmit(type, category.uppercase(), title.trim(), desc.trim(), location.trim(), contact.trim())
                            }
                        }
                    ) { Text("Submit") }
                }
            }
        }
    }
}

@Composable
private fun ClaimRequestDialog(
    item: LostFoundEntity,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Contact ${item.posterName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your contact details stay private until ${item.posterName} accepts your request.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (proof of ownership helps)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { if (message.isNotBlank()) onSubmit(message.trim()) }
                    ) { Text("Send request") }
                }
            }
        }
    }
}

// ============================================================================
// CAMPUS DIRECTORY SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusDirectoryScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var correctionContact by remember { mutableStateOf<DirectoryContactEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val contacts = uiState.directoryContacts
        .filter { selectedCategory == "ALL" || it.category == selectedCategory }
        .filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.department.contains(searchQuery, ignoreCase = true) ||
                it.designation.contains(searchQuery, ignoreCase = true)
        }

    Scaffold(
        topBar = {
            Column {
                Text(
                    text = "Campus Directory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search name / department") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "FACULTY", "IMPORTANT_OFFICE", "EMERGENCY", "HOSTEL_MEDICAL").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(if (cat == "ALL") "All" else cat.replace('_', ' ')) }
                    )
                }
            }
            if (contacts.isEmpty()) {
                EmptyState("No contacts found", Icons.Default.Search)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${contact.designation}, ${contact.department}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Office: ${contact.officeLocation} | ${contact.officeHours}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                                            } catch (_: Exception) { }
                                        }
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call")
                                    }
                                    TextButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) { }
                                        }
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Email")
                                    }
                                    TextButton(onClick = { correctionContact = contact }) { Text("Suggest fix") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    correctionContact?.let { contact ->
        SuggestCorrectionDialog(
            contact = contact,
            onDismiss = { correctionContact = null },
            onSubmit = { change ->
                viewModel.submitDirectoryCorrection(contact.id, contact.name, change)
                correctionContact = null
            }
        )
    }
}

@Composable
private fun SuggestCorrectionDialog(
    contact: DirectoryContactEntity,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var change by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Suggest correction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("For: ${contact.name}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = change,
                    onValueChange = { change = it },
                    label = { Text("Proposed change") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { if (change.isNotBlank()) onSubmit(change.trim()) }) { Text("Submit") }
                }
            }
        }
    }
}

// ============================================================================
// SHARED HELPERS
// ============================================================================
@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
