package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import `in`.campusone.app.ui.theme.SafeGreen
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubScreen(
    uiState: StateFlow<AdminUiState>,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onManageModerators: () -> Unit = {}
) {
    val state by uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
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
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add new item")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
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
            items(
                listOf(
                    StatCard("Subjects", state.subjectCount.toString(), Icons.Default.School, SafeGreen),
                    StatCard("Notices", state.noticeCount.toString(), Icons.Default.Campaign, MaterialTheme.colorScheme.primary),
                    StatCard("Assignments", state.assignmentCount.toString(), Icons.Default.Assignment, MaterialTheme.colorScheme.secondary),
                    StatCard("Exams", state.examCount.toString(), Icons.Default.CalendarMonth, MaterialTheme.colorScheme.tertiary),
                    StatCard("Clubs", state.clubCount.toString(), Icons.Default.Group, MaterialTheme.colorScheme.primary),
                    StatCard("Events", state.eventCount.toString(), Icons.Default.Event, MaterialTheme.colorScheme.secondary)
                ),
                key = { it.first }
            ) { (title, count, icon, color) ->
                StatCardItem(title = title, count = count, icon = icon, color = color)
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick Actions
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(
                listOf(
                    ActionCard("Manage Subjects", Icons.Default.School, { /* navigate */ }),
                    ActionCard("Manage Timetable", Icons.Default.Schedule, { /* navigate */ }),
                    ActionCard("Manage Notices", Icons.Default.Campaign, { /* navigate */ }),
                    ActionCard("Manage Assignments", Icons.Default.Assignment, { /* navigate */ }),
                    ActionCard("Manage Exams", Icons.Default.CalendarMonth, { /* navigate */ }),
                    ActionCard("Manage Clubs", Icons.Default.Group, { /* navigate */ }),
                    ActionCard("Manage Events", Icons.Default.Event, { /* navigate */ }),
                    ActionCard("Lost & Found", Icons.Default.FindInPage, { /* navigate */ }),
                    ActionCard("Directory", Icons.Default.Info, { /* navigate */ }),
                    ActionCard("Users & Roles", Icons.Default.Group, { /* navigate */ }),
                    ActionCard("Manage Moderators", Icons.Default.PersonAdd, onManageModerators)
                ),
                key = { it.first }
            ) { (title, icon, onClick) ->
                ActionCardItem(title = title, icon = icon, color = MaterialTheme.colorScheme.primary, onClick = onClick)
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private data class StatCard(
    val title: String,
    val count: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatCardItem(title: String, count: String, icon: ImageVector, color: Color) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ActionCard(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCardItem(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
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
                tint = color
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
