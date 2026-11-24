package `in`.campusone.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.StudyAndCampusUiState
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import java.util.Locale

private val CampusPageGreen = Color(0xFF004A35)
private val CampusPageGreenSoft = Color(0xFFDCECE1)
private val CampusPageCanvas = Color(0xFFF9FAF5)
private val CampusPageBlue = Color(0xFFDDEEF0)
private val CampusPageOchre = Color(0xFFF5EAD5)
private val CampusPageInk = Color(0xFF17231F)
private val CampusPageMuted = Color(0xFF66766D)
private val CampusPageBorder = Color(0xFFE2E8E1)

@Composable
fun FlagshipCampusCommunityOverview(
    uiState: StudyAndCampusUiState,
    onOpenEvents: () -> Unit,
    onOpenLostFound: () -> Unit,
    onOpenDirectory: () -> Unit
) {
    val events = uiState.events.filter { it.isApproved && !it.isFlagged }.sortedBy { it.eventEpochMillis }
    val clubs = uiState.clubs.filter { it.isApproved && !it.isFlagged }
    val items = uiState.lostFoundItems.filter { !it.isArchived && !it.isHidden && !it.isFlagged }.sortedByDescending { it.createdAtMillis }
    val contacts = uiState.directoryContacts
    Column(modifier = Modifier.fillMaxSize().background(CampusPageCanvas)) {
        FlagshipAppHeader(uiState.profile.collegeName, uiState.profile.name, showActions = false)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(15.dp, 10.dp, 15.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                FlagshipPageTitle("Campus & Community", "Birla & Chauras campus life and utilities", "LIVE CAMPUS")
            }
            item {
                FlagshipCard(containerColor = Color(0xFFE7F0E8)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your campus, connected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CampusPageInk)
                            Text("Discover verified events, recover belongings and reach university contacts.", fontSize = 12.sp, color = CampusPageMuted)
                        }
                        Surface(color = CampusPageGreenSoft, shape = CircleShape) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = CampusPageGreen, modifier = Modifier.padding(12.dp).size(23.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverviewMetric(events.size.toString(), "Events")
                        OverviewMetric(clubs.size.toString(), "Societies")
                        OverviewMetric(items.size.toString(), "Open items")
                        OverviewMetric(contacts.size.toString(), "Contacts")
                    }
                }
            }
            item {
                FlagshipSectionTitle("Events & Societies", "Published campus activity", "View all", onOpenEvents)
                if (events.isEmpty() && clubs.isEmpty()) {
                    FlagshipCard {
                        Text("No verified campus activity yet", fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                        Text("Published events and approved societies will appear here.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
            items(events.take(2), key = { "event_" + it.id }) { event ->
                OverviewEventRow(event)
            }
            items(clubs.take(2), key = { "club_" + it.id }) { club ->
                OverviewClubRow(club)
            }
            item {
                FlagshipSectionTitle("Lost & Found", "Campus recovery board", "Open board", onLostFoundSafe(onOpenLostFound))
                if (items.isEmpty()) {
                    FlagshipCard {
                        Text("No open reports", fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                        Text("Lost and found reports from the university community will appear here.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
            items(items.take(3), key = { "lost_" + it.id }) { item ->
                OverviewLostFoundRow(item, onOpenLostFound)
            }
            item {
                FlagshipSectionTitle("Campus Directory", "University offices and academic contacts", "Directory", onOpenDirectory)
                if (contacts.isEmpty()) {
                    FlagshipCard {
                        Text("Directory details are not available yet", fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                        Text("The directory will populate from the verified campus contact list.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
            items(contacts.take(3), key = { "contact_" + it.id }) { contact ->
                OverviewContactRow(contact)
            }
        }
    }
}

private fun onLostFoundSafe(action: () -> Unit): (() -> Unit) = action

@Composable
private fun RowScope.OverviewMetric(value: String, label: String) {
    Surface(modifier = Modifier.weight(1f), color = Color.White, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CampusPageGreen)
            Text(label, fontSize = 9.sp, color = CampusPageMuted)
        }
    }
}

@Composable
private fun OverviewEventRow(event: CampusEventEntity) {
    FlagshipCard(contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CampusPageBlue, shape = RoundedCornerShape(11.dp)) {
                Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF367F8D), modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                Text(event.clubName + " · " + event.category.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = CampusPageMuted)
                Text(event.eventDateFormatted + " · " + event.venue, fontSize = 10.sp, color = CampusPageMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun OverviewClubRow(club: ClubEntity) {
    FlagshipCard(contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CampusPageGreenSoft, shape = RoundedCornerShape(11.dp)) {
                Text(club.logoEmojiOrUrl, modifier = Modifier.padding(10.dp), fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                Text(club.category.lowercase().replaceFirstChar { it.uppercase() } + " · " + club.memberCount + " members", fontSize = 11.sp, color = CampusPageMuted)
            }
            FlagshipPill(if (club.isFollowedByMe) "Following" else "Active")
        }
    }
}

@Composable
private fun OverviewLostFoundRow(item: LostFoundEntity, onClick: () -> Unit) {
    FlagshipCard(modifier = Modifier.clickable(onClick = onClick), contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = if (item.type == "LOST") CampusPageOchre else CampusPageBlue, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.padding(11.dp).size(20.dp), tint = CampusPageGreen)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CampusPageInk)
                Text(item.location + " · " + item.dateFormatted, fontSize = 10.sp, color = CampusPageMuted)
            }
            FlagshipPill(if (item.type == "FOUND") "Found" else "Lost", if (item.type == "FOUND") CampusPageGreenSoft else CampusPageOchre)
        }
    }
}

@Composable
private fun OverviewContactRow(contact: DirectoryContactEntity) {
    FlagshipCard(contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CampusPageGreenSoft, shape = RoundedCornerShape(11.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp), tint = CampusPageGreen)
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CampusPageInk)
                Text(contact.designation + " · " + contact.officeLocation, fontSize = 10.sp, color = CampusPageMuted)
            }
            if (contact.isEmergencyOrImportant) FlagshipPill("Priority", CampusPageOchre, Color(0xFF9C641F))
        }
    }
}

@Composable
fun FlagshipEventsAndClubsScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("ALL") }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val events = uiState.events.filter { it.isApproved && !it.isFlagged }
        .filter { category == "ALL" || it.category.equals(category, true) }
        .filter { query.isBlank() || it.title.contains(query, true) || it.clubName.contains(query, true) || it.venue.contains(query, true) }
        .sortedBy { it.eventEpochMillis }
    val clubs = uiState.clubs.filter { it.isApproved && !it.isFlagged }
        .filter { category == "ALL" || it.category.equals(category, true) }
        .filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) }
    val approvedClubs = uiState.clubs.filter { it.isApproved && !it.isFlagged }
    val canCreate = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.ADMIN || uiState.profile.role == UserRole.MODERATOR
    Column(modifier = Modifier.fillMaxSize().background(CampusPageCanvas)) {
        FlagshipAppHeader(uiState.profile.collegeName, uiState.profile.name, showActions = false)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FlagshipPageTitle("Events & Clubs", "Verified student activities and registered societies", "CAMPUS LIFE", trailing = {
                if (canCreate) TextButton(onClick = { showCreate = true }) { Text("＋ Create", color = CampusPageGreen, fontWeight = FontWeight.SemiBold) }
            })
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                placeholder = { Text("Search events, societies or venues", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("ALL", "TECH", "CULTURAL", "SPORTS", "LITERARY", "SOCIAL").size) { index ->
                    val value = listOf("ALL", "TECH", "CULTURAL", "SPORTS", "LITERARY", "SOCIAL")[index]
                    FilterChip(selected = category == value, onClick = { category = value }, label = { Text(if (value == "ALL") "All" else value.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            item { FlagshipSectionTitle("Verified Events", events.size.toString() + " published") }
            if (events.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No events match this view", fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                        Text("New verified events will appear here when published by an approved society.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
            items(events, key = { "event_" + it.id }) { event ->
                FlagshipEventCard(event, onRsvp = { status -> viewModel.updateEventRsvp(event.id, status) }, onReport = { viewModel.reportEvent(event.id) })
            }
            item { FlagshipSectionTitle("Institutional Society Directory", clubs.size.toString() + " approved societies") }
            if (clubs.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No approved societies available", fontWeight = FontWeight.SemiBold, color = CampusPageInk)
                        Text("Society pages appear after moderator approval.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
            items(clubs, key = { "club_" + it.id }) { club ->
                FlagshipClubCard(club, onFollow = { viewModel.toggleFollowClub(club.id, !club.isFollowedByMe) })
            }
        }
    }
    if (showCreate) {
        var clubIndex by rememberSaveable { mutableIntStateOf(0) }
        var title by rememberSaveable { mutableStateOf("") }
        var description by rememberSaveable { mutableStateOf("") }
        var venue by rememberSaveable { mutableStateOf("") }
        var categoryInput by rememberSaveable { mutableStateOf("TECH") }
        var daysInput by rememberSaveable { mutableStateOf("7") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Propose campus event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (approvedClubs.isEmpty()) {
                        Text("An approved society is required before an event can be proposed.", color = CampusPageMuted, fontSize = 12.sp)
                    } else {
                        Text("Society: " + approvedClubs[clubIndex.coerceIn(0, approvedClubs.lastIndex)].name, color = CampusPageMuted, fontSize = 12.sp)
                        TextButton(onClick = { clubIndex = (clubIndex + 1) % approvedClubs.size }) { Text("Choose another society") }
                    }
                    OutlinedTextField(title, { title = it }, label = { Text("Event title") }, singleLine = true)
                    OutlinedTextField(categoryInput, { categoryInput = it }, label = { Text("Category (TECH / CULTURAL / SPORTS)") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
                    OutlinedTextField(venue, { venue = it }, label = { Text("Venue") }, singleLine = true)
                    OutlinedTextField(daysInput, { daysInput = it.filter(Char::isDigit).take(3) }, label = { Text("Days from now") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = approvedClubs.isNotEmpty() && title.isNotBlank() && venue.isNotBlank() && (daysInput.toIntOrNull() ?: 0) in 0..365,
                    onClick = {
                        val club = approvedClubs[clubIndex.coerceIn(0, approvedClubs.lastIndex)]
                        val days = daysInput.toInt()
                        viewModel.createClubEvent(club.id, club.name, categoryInput.trim().uppercase(Locale.ROOT), title.trim(), description.trim(), venue.trim(), if (days == 0) "Today" else "In " + days + " days", days)
                        showCreate = false
                    }
                ) { Text("Submit proposal") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FlagshipEventCard(event: CampusEventEntity, onRsvp: (EventRsvpStatus) -> Unit, onReport: () -> Unit) {
    FlagshipCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CampusPageBlue, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF367F8D), modifier = Modifier.padding(11.dp).size(23.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CampusPageInk)
                Text("Organised by " + event.clubName, fontSize = 11.sp, color = CampusPageMuted)
            }
            FlagshipPill("Verified", CampusPageGreenSoft, CampusPageGreen)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FlagshipPill(event.category.lowercase().replaceFirstChar { it.uppercase() }, CampusPageBlue, Color(0xFF367F8D))
            FlagshipPill(event.eventDateFormatted, CampusPageOchre, Color(0xFF9C641F))
        }
        Text(event.description, fontSize = 12.sp, color = CampusPageMuted, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = CampusPageGreen, modifier = Modifier.size(15.dp))
            Text(event.venue, fontSize = 11.sp, color = CampusPageMuted)
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = event.myRsvpStatus == "GOING", onClick = { onRsvp(if (event.myRsvpStatus == "GOING") EventRsvpStatus.NONE else EventRsvpStatus.GOING) }, label = { Text("Going " + event.goingCount, fontSize = 10.sp) })
            FilterChip(selected = event.myRsvpStatus == "INTERESTED", onClick = { onRsvp(if (event.myRsvpStatus == "INTERESTED") EventRsvpStatus.NONE else EventRsvpStatus.INTERESTED) }, label = { Text("Interested " + event.interestedCount, fontSize = 10.sp) })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReport) { Text("Report", color = CampusPageMuted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun FlagshipClubCard(club: ClubEntity, onFollow: () -> Unit) {
    FlagshipCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CampusPageGreenSoft, shape = RoundedCornerShape(13.dp)) {
                Text(club.logoEmojiOrUrl, modifier = Modifier.padding(12.dp), fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CampusPageInk)
                Text(club.category.lowercase().replaceFirstChar { it.uppercase() } + " · " + club.memberCount + " members", fontSize = 11.sp, color = CampusPageMuted)
            }
            FlagshipPill(if (club.isFollowedByMe) "Following" else "Active")
        }
        Spacer(Modifier.height(8.dp))
        Text(club.description, fontSize = 12.sp, lineHeight = 17.sp, color = CampusPageMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(club.contactEmail, modifier = Modifier.weight(1f), fontSize = 10.sp, color = CampusPageMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (club.contactEmail.isNotBlank()) {
                TextButton(onClick = onFollow) { Text(if (club.isFollowedByMe) "Joined" else "Join society", color = CampusPageGreen) }
            }
        }
    }
}

@Composable
fun FlagshipLostAndFoundScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var type by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var claimItem by remember { mutableStateOf<LostFoundEntity?>(null) }
    val visible = uiState.lostFoundItems.filter { !it.isArchived && !it.isHidden && !it.isFlagged }
        .filter { type == "ALL" || it.type == type }
        .filter { query.isBlank() || it.itemTitle.contains(query, true) || it.description.contains(query, true) || it.location.contains(query, true) }
        .sortedByDescending { it.createdAtMillis }
    val pendingClaims = uiState.contactRequests.filter { request ->
        request.status == "PENDING" && uiState.lostFoundItems.any { it.id == request.itemId && it.posterUid == uiState.profile.uid }
    }
    Column(modifier = Modifier.fillMaxSize().background(CampusPageCanvas)) {
        FlagshipAppHeader(uiState.profile.collegeName, uiState.profile.name, showActions = false)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FlagshipCard(containerColor = CampusPageBlue) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF367F8D), modifier = Modifier.size(20.dp))
                    Column {
                        Text("Private contact protocol", fontWeight = FontWeight.Bold, color = Color(0xFF1B4046), fontSize = 13.sp)
                        Text("Your contact details are shared only after the item poster accepts a claim request.", fontSize = 11.sp, color = Color(0xFF456A76))
                    }
                }
            }
            FlagshipPageTitle("Lost & Found", "Campus recovery board", "CAMPUS SERVICES", trailing = {
                Button(onClick = { showAdd = true }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = CampusPageGreen)) { Text("＋ Report", fontSize = 11.sp) }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(13.dp),
                    placeholder = { Text("Search item or location", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Surface(color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CampusPageBorder), shape = RoundedCornerShape(13.dp)) {
                    Text("All campus", modifier = Modifier.padding(horizontal = 10.dp, vertical = 17.dp), fontSize = 10.sp, color = CampusPageMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("ALL", "LOST", "FOUND").forEach { key ->
                    FilterChip(selected = type == key, onClick = { type = key }, label = { Text(if (key == "ALL") "All Items (" + visible.size + ")" else key.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pendingClaims.isNotEmpty()) {
                item { FlagshipSectionTitle("Claim requests", pendingClaims.size.toString() + " awaiting your response") }
                items(pendingClaims, key = { "request_" + it.id }) { request ->
                    FlagshipCard(containerColor = CampusPageBlue) {
                        Text(request.requesterName + " wants to contact you about " + request.itemTitle, fontWeight = FontWeight.SemiBold, color = CampusPageInk, fontSize = 13.sp)
                        Text(request.message, fontSize = 11.sp, color = CampusPageMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlagshipPrimaryButton("Accept & share", onClick = { viewModel.respondToClaimRequest(request, true) }, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { viewModel.respondToClaimRequest(request, false) }, modifier = Modifier.weight(1f)) { Text("Ignore") }
                        }
                    }
                }
            }
            item { FlagshipSectionTitle("Reported items", visible.size.toString() + " open reports") }
            if (visible.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No matching reports", fontWeight = FontWeight.Bold, color = CampusPageInk)
                        Text("Try a different search or report a lost/found item to get started.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
            items(visible, key = { it.id }) { item ->
                FlagshipLostFoundCard(item, isOwner = item.posterUid == uiState.profile.uid, onClaim = { claimItem = item }, onResolve = { viewModel.resolveLostFound(item.id) }, onReport = { viewModel.reportLostFound(item.id) })
            }
        }
    }
    if (showAdd) {
        FlagshipLostFoundReportDialog(onDismiss = { showAdd = false }) { itemType, category, title, description, location, contact ->
            viewModel.addLostFound(itemType, category, title, description, location, contact)
            showAdd = false
        }
    }
    claimItem?.let { item ->
        var message by remember(item.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { claimItem = null },
            title = { Text("Verify claim · " + item.itemTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Describe a detail that helps the poster verify ownership. Contact information stays private until accepted.", fontSize = 12.sp, color = CampusPageMuted)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Verification message") }, minLines = 3)
                }
            },
            confirmButton = { TextButton(enabled = message.isNotBlank(), onClick = { viewModel.sendClaimRequest(item, message.trim()); claimItem = null }) { Text("Send request") } },
            dismissButton = { TextButton(onClick = { claimItem = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FlagshipLostFoundCard(item: LostFoundEntity, isOwner: Boolean, onClaim: () -> Unit, onResolve: () -> Unit, onReport: () -> Unit) {
    FlagshipCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagshipPill(if (item.type == "FOUND") "FOUND" else "LOST", if (item.type == "FOUND") CampusPageGreenSoft else CampusPageOchre, if (item.type == "FOUND") CampusPageGreen else Color(0xFF9C641F))
            Spacer(Modifier.width(7.dp))
            FlagshipPill(item.category.lowercase().replaceFirstChar { it.uppercase() }, CampusPageBlue, Color(0xFF367F8D))
            Spacer(Modifier.weight(1f))
            Text(item.dateFormatted, fontSize = 10.sp, color = CampusPageMuted)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (item.type == "FOUND") CampusPageGreenSoft else CampusPageOchre, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.padding(11.dp).size(23.dp), tint = CampusPageGreen)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CampusPageInk)
                Text(item.location, fontSize = 11.sp, color = CampusPageMuted)
                Text("Posted by " + item.posterName, fontSize = 10.sp, color = CampusPageMuted)
            }
        }
        if (item.description.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(item.description, fontSize = 12.sp, color = CampusPageMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (item.isResolved) {
            Spacer(Modifier.height(8.dp))
            FlagshipPill("Resolved", CampusPageGreenSoft, CampusPageGreen)
        } else {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isOwner) {
                    FlagshipPrimaryButton("Claim with verification", onClaim, modifier = Modifier.weight(1f))
                } else {
                    FlagshipPrimaryButton("Mark resolved", onResolve, modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onReport) { Text("Report", color = CampusPageMuted, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun FlagshipLostFoundReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String) -> Unit
) {
    var type by rememberSaveable { mutableStateOf("LOST") }
    var category by rememberSaveable { mutableStateOf("ELECTRONICS") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report an item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("LOST", "FOUND").forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item name") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Last seen / found at") }, singleLine = true)
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Preferred contact (private)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && location.isNotBlank() && contact.isNotBlank(), onClick = { onSubmit(type, category.uppercase(Locale.ROOT), title.trim(), description.trim(), location.trim(), contact.trim()) }) { Text("Submit report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FlagshipCampusDirectoryScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var query by rememberSaveable { mutableStateOf("") }
    var campus by rememberSaveable { mutableStateOf("ALL") }
    var category by rememberSaveable { mutableStateOf("ALL") }
    var correction by remember { mutableStateOf<DirectoryContactEntity?>(null) }
    val context = LocalContext.current
    val filtered = uiState.directoryContacts.filter { contact ->
        (campus == "ALL" || contact.officeLocation.contains(campus, true)) &&
            (category == "ALL" || when (category) {
                "IMPORTANT" -> contact.isEmergencyOrImportant
                else -> contact.category.equals(category, true)
            }) &&
            (query.isBlank() || listOf(contact.name, contact.designation, contact.department, contact.officeLocation, contact.phone, contact.email).any { it.contains(query, true) })
    }.sortedWith(compareByDescending<DirectoryContactEntity> { it.isEmergencyOrImportant }.thenBy { it.department }.thenBy { it.name })
    val emergency = filtered.filter { it.isEmergencyOrImportant }
    val regular = filtered.filterNot { it.isEmergencyOrImportant }
    val grouped = regular.groupBy { it.department }

    Column(modifier = Modifier.fillMaxSize().background(CampusPageCanvas)) {
        FlagshipAppHeader(uiState.profile.collegeName, uiState.profile.name, showActions = false)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FlagshipPageTitle("Campus Directory", "Find university offices, faculty and support", "OFFICIAL CONTACTS")
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(13.dp),
                placeholder = { Text("Search department, faculty, cabin or intercom", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = CampusPageMuted) }
            )
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("ALL", "Birla", "Chauras").size) { index ->
                    val key = listOf("ALL", "Birla", "Chauras")[index]
                    FilterChip(selected = campus == key, onClick = { campus = key }, label = { Text(if (key == "ALL") "All contacts" else key + " Campus", fontSize = 11.sp) })
                }
            }
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("ALL", "IMPORTANT", "FACULTY", "DEPARTMENT", "FACILITY").size) { index ->
                    val key = listOf("ALL", "IMPORTANT", "FACULTY", "DEPARTMENT", "FACILITY")[index]
                    FilterChip(selected = category == key, onClick = { category = key }, label = { Text(if (key == "IMPORTANT") "Emergency & Priority" else key.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) })
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (emergency.isNotEmpty()) {
                item { FlagshipSectionTitle("Emergency & Priority Desk", emergency.size.toString() + " priority contacts") }
                items(emergency, key = { "emergency_" + it.id }) { contact ->
                    FlagshipDirectoryContactCard(contact, onCall = {
                        if (contact.phone.isNotBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.phone))) }
                    }, onEmail = {
                        if (contact.email.isNotBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + contact.email))) }
                    }, onCorrection = { correction = contact })
                }
            }
            grouped.forEach { (department, departmentContacts) ->
                item(key = "heading_" + department) {
                    FlagshipSectionTitle(department, departmentContacts.firstOrNull()?.officeLocation)
                }
                items(departmentContacts, key = { it.id }) { contact ->
                    FlagshipDirectoryContactCard(contact, onCall = {
                        if (contact.phone.isNotBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.phone))) }
                    }, onEmail = {
                        if (contact.email.isNotBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + contact.email))) }
                    }, onCorrection = { correction = contact })
                }
            }
            if (filtered.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No contacts found", fontWeight = FontWeight.Bold, color = CampusPageInk)
                        Text("Try another campus, category or search term. Contact information is shown only when available in the verified directory.", fontSize = 12.sp, color = CampusPageMuted)
                    }
                }
            }
        }
    }
    correction?.let { contact ->
        var value by remember(contact.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { correction = null },
            title = { Text("Suggest directory correction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("For " + contact.name, fontSize = 12.sp, color = CampusPageMuted)
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Proposed update") }, minLines = 3)
                }
            },
            confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = { viewModel.submitDirectoryCorrection(contact.id, contact.name, value.trim()); correction = null }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { correction = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FlagshipDirectoryContactCard(
    contact: DirectoryContactEntity,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onCorrection: () -> Unit
) {
    FlagshipCard(contentPadding = PaddingValues(13.dp), containerColor = if (contact.isEmergencyOrImportant) Color(0xFFFFF9F7) else Color.White) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (contact.isEmergencyOrImportant) FlagshipPill("Priority contact", CampusPageOchre, Color(0xFF9C641F))
                Text(contact.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CampusPageInk)
                Text(contact.designation + " · " + contact.department, fontSize = 11.sp, color = CampusPageMuted)
                Text(contact.officeLocation, fontSize = 10.sp, color = CampusPageGreen, fontWeight = FontWeight.SemiBold)
                if (contact.officeHours.isNotBlank()) Text(contact.officeHours, fontSize = 10.sp, color = CampusPageMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (contact.phone.isNotBlank() || contact.email.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (contact.phone.isNotBlank()) Surface(color = CampusPageGreenSoft, shape = CircleShape, modifier = Modifier.clickable(onClick = onCall)) { Icon(Icons.Default.Call, contentDescription = "Call " + contact.name, tint = CampusPageGreen, modifier = Modifier.padding(10.dp).size(18.dp)) }
                    if (contact.email.isNotBlank()) Surface(color = CampusPageBlue, shape = CircleShape, modifier = Modifier.clickable(onClick = onEmail)) { Icon(Icons.Default.Email, contentDescription = "Email " + contact.name, tint = Color(0xFF367F8D), modifier = Modifier.padding(10.dp).size(18.dp)) }
                }
            }
        }
        if (contact.phone.isNotBlank() || contact.email.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text((if (contact.phone.isNotBlank()) contact.phone else "") + (if (contact.phone.isNotBlank() && contact.email.isNotBlank()) " · " else "") + (if (contact.email.isNotBlank()) contact.email else ""), fontSize = 10.sp, color = CampusPageMuted)
        }
        TextButton(onClick = onCorrection, contentPadding = PaddingValues(0.dp)) { Text("Suggest correction", fontSize = 10.sp, color = CampusPageGreen) }
    }
}
