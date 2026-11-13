package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyGridCell
import androidx.compose.foundation.lazy.LazyGridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.ui.theme.SafeGreen
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@Composable
fun AdminHubScreen(
    uiState: androidx.compose.runtime.StateFlow<AdminUiState>,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onManageModerators: () -> Unit = {}
) {
    val state by uiState.collectAsStateWithLifecycle()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                colors = MaterialTheme.colorScheme.primaryContainer,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add new item")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
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
            item {
                LazyGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                LazyGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

private data class StatCard(
    val title: String,
    val count: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
private fun StatCardItem(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .span(horizontalSpan = LazyGridItemSpan(1)),
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
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun ActionCardItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .span(horizontalSpan = LazyGridItemSpan(1)),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
