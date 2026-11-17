package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1CompactStatCard
import `in`.campusone.app.ui.components.C1FeatureCard
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubScreen(
    uiState: StateFlow<AdminUiState>,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onManageModerators: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val state by uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Admin Panel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add new item")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        text = "CampusOne Admin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage app data for ${state.profile.collegeName} • ${state.profile.role.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stat Cards
            val statCards = listOf(
                StatCard("Subjects", state.subjectCount.toString(), Icons.Default.School, MaterialTheme.colorScheme.primary),
                StatCard("Notices", state.noticeCount.toString(), Icons.Default.Campaign, MaterialTheme.colorScheme.tertiary),
                StatCard("Assignments", state.assignmentCount.toString(), Icons.Default.Assignment, MaterialTheme.colorScheme.secondary),
                StatCard("Exams", state.examCount.toString(), Icons.Default.CalendarMonth, Color(0xFF7A5C4A)),
                StatCard("Clubs", state.clubCount.toString(), Icons.Default.Group, MaterialTheme.colorScheme.primary),
                StatCard("Events", state.eventCount.toString(), Icons.Default.Event, MaterialTheme.colorScheme.tertiary),
                StatCard("Users", state.users.size.toString(), Icons.Default.People, MaterialTheme.colorScheme.secondary)
            )
            items(
                items = statCards,
                key = { it.title },
                itemContent = { card ->
                    StatCardItem(card = card)
                }
            )

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick Actions
            item(span = { GridItemSpan(2) }) {
                C1SectionHeader(title = "Quick Actions")
            }
            val actionCards = listOf(
                ActionCard("Manage Subjects", Icons.Default.School, { onNavigate("SUBJECTS") }),
                ActionCard("Manage Timetable", Icons.Default.Schedule, { onNavigate("TIMETABLE") }),
                ActionCard("Manage Notices", Icons.Default.Campaign, { onNavigate("NOTICES") }),
                ActionCard("Manage Assignments", Icons.Default.Assignment, { onNavigate("ASSIGNMENTS") }),
                ActionCard("Manage Exams", Icons.Default.CalendarMonth, { onNavigate("EXAMS") }),
                ActionCard("Manage Clubs", Icons.Default.Group, { onNavigate("CLUBS") }),
                ActionCard("Manage Events", Icons.Default.Event, { onNavigate("EVENTS") }),
                ActionCard("Lost & Found", Icons.Default.FindInPage, { onNavigate("LOST_FOUND") }),
                ActionCard("Directory", Icons.Default.Info, { onNavigate("DIRECTORY") }),
                ActionCard("Review Center", Icons.Default.Flag, { onNavigate("REVIEW") }),
                ActionCard("Users & Roles", Icons.Default.Group, { onNavigate("USERS") }),
                ActionCard("Manage Moderators", Icons.Default.PersonAdd, onManageModerators)
            )
            items(
                items = actionCards,
                key = { it.title },
                itemContent = { card ->
                    ActionCardItem(card = card)
                }
            )

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCardItem(card: StatCard) {
    C1CompactStatCard(
        modifier = Modifier.fillMaxWidth(),
        label = card.title,
        value = card.count,
        icon = card.icon,
        valueColor = card.color
    )
}

@Composable
private fun ActionCardItem(card: ActionCard) {
    C1FeatureCard(
        modifier = Modifier.fillMaxWidth(),
        icon = card.icon,
        title = card.title,
        description = "",
        iconColor = MaterialTheme.colorScheme.primary,
        onClick = card.onClick
    )
}

private data class StatCard(
    val title: String,
    val count: String,
    val icon: ImageVector,
    val color: Color
)

private data class ActionCard(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)