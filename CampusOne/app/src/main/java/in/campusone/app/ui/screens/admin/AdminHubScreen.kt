package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.C1Height
import `in`.campusone.app.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubScreen(
    uiState: AdminViewModel.AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onManageModerators: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val state = uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(C1Spacing.md)
        ) {
            Text(
                text = "Admin Panel",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(C1Spacing.md))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                items(
                    items = adminActions,
                    key = { it.route }
                ) { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onNavigate(action.route) })
                            .height(C1Height.listRow)
                    ) {
                        Icon(
                            imageVector = actionIcon(action.route),
                            contentDescription = action.label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.width(C1Spacing.md)
                        )
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private val adminActions = listOf(
    AdminAction(route = "SUBJECTS", label = "Subjects"),
    AdminAction(route = "TIMETABLE", label = "Timetable"),
    AdminAction(route = "NOTICES", label = "Notices"),
    AdminAction(route = "ASSIGNMENTS", label = "Assignments"),
    AdminAction(route = "EXAMS", label = "Exams"),
    AdminAction(route = "CLUBS", label = "Clubs"),
    AdminAction(route = "EVENTS", label = "Events"),
    AdminAction(route = "LOST_FOUND", label = "Lost & Found"),
    AdminAction(route = "DIRECTORY", label = "Directory"),
    AdminAction(route = "USERS", label = "Users"),
    AdminAction(route = "REVIEW", label = "Reports & Review"),
    AdminAction(route = "HELP", label = "Help Center")
)

private fun actionIcon(route: String) = when (route) {
    "SUBJECTS" -> Icons.Default.School
    "TIMETABLE" -> Icons.Default.Schedule
    "NOTICES" -> Icons.Default.Notifications
    "ASSIGNMENTS" -> Icons.Default.Assignment
    "EXAMS" -> Icons.Default.School
    "CLUBS" -> Icons.Default.Group
    "EVENTS" -> Icons.Default.Group
    "LOST_FOUND" -> Icons.Default.LocationOn
    "DIRECTORY" -> Icons.Default.People
    "USERS" -> Icons.Default.People
    "REVIEW" -> Icons.Default.Policy
    "HELP" -> Icons.Default.HelpCenter
    else -> Icons.Default.HelpCenter
}

private data class AdminAction(
    val route: String,
    val label: String
)
