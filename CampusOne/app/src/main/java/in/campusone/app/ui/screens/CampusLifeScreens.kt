package `in`.campusone.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.components.C1AppBar
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1Dialog
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
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
            C1AppBar(
                title = "Events & Clubs",
                onBack = { }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && canCreateEvents) {
                FloatingActionButton(onClick = { showCreateEventDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Event")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
        ) {
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
        C1EmptyState(
            icon = Icons.Default.Event,
            title = "No upcoming events",
            message = "Events will appear here once published."
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(C1Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        items(events, key = { it.id }) { event ->
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(C1Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(C1Spacing.sm))
                        Text(event.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(event.clubName, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(C1Spacing.xs))
                    Text(event.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Text("Venue: ${event.venue} | ${event.eventDateFormatted}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
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
        C1EmptyState(
            icon = Icons.Default.Group,
            title = "No clubs yet",
            message = "Clubs will appear here once approved."
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(C1Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        items(clubs, key = { it.id }) { club ->
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(C1Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(club.logoEmojiOrUrl, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(C1Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(club.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${club.category} • ${club.memberCount} members", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Text(club.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Text(club.contactEmail, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Propose Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                if (clubs.isEmpty()) {
                    Text("No approved clubs available.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Club: ${clubs[clubIndex.coerceIn(0, clubs.lastIndex)].name}", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { clubIndex = (clubIndex + 1) % clubs.size }) { Text("Switch club") }
                }
                C1TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Event title",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = "Description",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = "Venue",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = "Category (TECH/CULTURAL/SPORTS)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(
                    value = daysFromNow,
                    onValueChange = { daysFromNow = it },
                    label = "Days from now",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.md))
            }
        },
        confirmButton = {
            C1Button(
                text = "Create",
                onClick = {
                    if (clubs.isNotEmpty() && title.isNotBlank() && venue.isNotBlank()) {
                        val club = clubs[clubIndex.coerceIn(0, clubs.lastIndex)]
                        onCreate(club.id, club.name, category.uppercase(), title.trim(), desc.trim(), venue.trim(), "In $daysFromNow day(s)", daysFromNow.toIntOrNull() ?: 7)
                    }
                },
                intent = C1ButtonIntent.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
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
            C1AppBar(
                title = "Lost & Found",
                onBack = { }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Report Item")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = C1Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                listOf("ALL", "LOST", "FOUND").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) }
                    )
                }
            }
            val myPendingClaims = uiState.contactRequests.filter { req ->
                req.status == "PENDING" && uiState.lostFoundItems.any {
                    it.id == req.itemId && it.posterUid == uiState.profile.uid
                }
            }
            if (myPendingClaims.isNotEmpty()) {
                Text(
                    text = "Claim requests for your items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = C1Spacing.lg, vertical = C1Spacing.sm)
                )
                myPendingClaims.forEach { req ->
                    C1SurfaceCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = C1Spacing.lg, vertical = C1Spacing.xs),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(C1Spacing.md)) {
                            Text(
                                text = "${req.requesterName} wants to contact you",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                                C1Button(text = "Accept & share contact", onClick = { viewModel.respondToClaimRequest(req, true) }, intent = C1ButtonIntent.PRIMARY)
                                C1Button(text = "Ignore", onClick = { viewModel.respondToClaimRequest(req, false) }, intent = C1ButtonIntent.GHOST)
                            }
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                C1EmptyState(
                    icon = Icons.Default.Search,
                    title = "Nothing reported yet",
                    message = "Lost & found items will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(C1Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
                ) {
                    items(items, key = { it.id }) { item ->
                        C1SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(C1Spacing.lg)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.type,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.type == "LOST") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(C1Spacing.sm))
                                    Text(item.category, style = MaterialTheme.typography.labelSmall)
                                    if (item.isResolved) {
                                        Spacer(modifier = Modifier.width(C1Spacing.sm))
                                        Text("RESOLVED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(C1Spacing.sm))
                                Text(item.itemTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                                Spacer(modifier = Modifier.height(C1Spacing.xs))
                                Text("Location: ${item.location} | ${item.dateFormatted}", style = MaterialTheme.typography.bodySmall)
                                Text("Posted by ${item.posterName}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(C1Spacing.sm))
                                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                                    if (!item.isResolved) {
                                        C1Button(text = "I found / claim", onClick = { claimItem = item }, intent = C1ButtonIntent.OUTLINED)
                                        if (item.posterUid == uiState.profile.uid) {
                                            C1Button(text = "Mark resolved", onClick = { viewModel.resolveLostFound(item.id) }, intent = C1ButtonIntent.DANGER)
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

    C1Dialog(
        title = "Report Item",
        onDismiss = onDismiss,
        confirmLabel = "Submit",
        onConfirm = {
            if (title.isNotBlank() && location.isNotBlank() && contact.isNotBlank()) {
                onSubmit(type, category.uppercase(), title.trim(), desc.trim(), location.trim(), contact.trim())
            }
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    listOf("LOST", "FOUND").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(value = category, onValueChange = { category = it }, label = "Category", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(value = title, onValueChange = { title = it }, label = "Item title", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(value = desc, onValueChange = { desc = it }, label = "Description", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(value = location, onValueChange = { location = it }, label = "Location", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                C1TextField(value = contact, onValueChange = { contact = it }, label = "Preferred contact (kept private)", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(C1Spacing.md))
            }
        }
    )
}

@Composable
private fun ClaimRequestDialog(
    item: LostFoundEntity,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    C1Dialog(
        title = "Contact ${item.posterName}",
        onDismiss = onDismiss,
        confirmLabel = "Send request",
        onConfirm = { if (message.isNotBlank()) onSubmit(message.trim()) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                Text(
                    "Your contact details stay private until ${item.posterName} accepts your request.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(C1Spacing.md))
                C1TextField(
                    value = message,
                    onValueChange = { message = it },
                    label = "Message (proof of ownership helps)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.md))
            }
        }
    )
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                C1AppBar(
                    title = "Campus Directory",
                    onBack = { }
                )
                C1TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search name / department",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = C1Spacing.lg)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = C1Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                items(listOf("ALL", "FACULTY", "IMPORTANT_OFFICE", "EMERGENCY", "HOSTEL_MEDICAL")) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(if (cat == "ALL") "All" else cat.replace('_', ' ')) }
                    )
                }
            }
            if (contacts.isEmpty()) {
                C1EmptyState(
                    icon = Icons.Default.Search,
                    title = "No contacts found",
                    message = "Directory contacts will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(C1Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        C1SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(C1Spacing.lg)) {
                                Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${contact.designation}, ${contact.department}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(C1Spacing.xs))
                                Text("Office: ${contact.officeLocation} | ${contact.officeHours}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(C1Spacing.sm))
                                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                                    C1Button(
                                        text = "Call",
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            } catch (_: Exception) { }
                                        },
                                        icon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                    C1Button(
                                        text = "Email",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } catch (_: Exception) { }
                                        },
                                        intent = C1ButtonIntent.OUTLINED,
                                        icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                    C1Button(text = "Suggest fix", onClick = { correctionContact = contact }, intent = C1ButtonIntent.GHOST)
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

    C1Dialog(
        title = "Suggest correction",
        onDismiss = onDismiss,
        confirmLabel = "Submit",
        onConfirm = { if (change.isNotBlank()) onSubmit(change.trim()) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                Text("For: ${contact.name}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(C1Spacing.md))
                C1TextField(
                    value = change,
                    onValueChange = { change = it },
                    label = "Proposed change",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(C1Spacing.md))
            }
        }
    )
}

// ============================================================================
// SHARED HELPERS
// ============================================================================