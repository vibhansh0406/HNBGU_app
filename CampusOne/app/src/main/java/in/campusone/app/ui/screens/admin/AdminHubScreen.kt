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
import `in`.campusone.app.data.repository.AdminPermission

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
    val visibleActions = remember(state.identity) {
        adminActions.filter { action ->
            action.permission == null || state.identity.can(action.permission!!)
        }
    }

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
                    items = visibleActions,
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

private val adminActions = listOf(
    AdminAction(route = "SUBJECTS", label = "Subjects", permission = null),
    AdminAction(route = "TIMETABLE", label = "Timetable", permission = AdminPermission.MANAGE_TIMETABLE),
    AdminAction(route = "NOTICES", label = "Notices", permission = AdminPermission.MANAGE_NOTICES),
    AdminAction(route = "ASSIGNMENTS", label = "Assignments", permission = null),
    AdminAction(route = "EXAMS", label = "Exams", permission = null),
    AdminAction(route = "CLUBS", label = "Clubs", permission = null),
    AdminAction(route = "EVENTS", label = "Events", permission = null),
    AdminAction(route = "LOST_FOUND", label = "Lost & Found", permission = null),
    AdminAction(route = "DIRECTORY", label = "Directory", permission = null),
    AdminAction(route = "USERS", label = "Users", permission = AdminPermission.VIEW_USERS),
    AdminAction(route = "REVIEW", label = "Reports & Review", permission = AdminPermission.MODERATE_REPORTS),
    AdminAction(route = "HELP", label = "Help Center", permission = null)
)

private data class AdminAction(
    val route: String,
    val label: String,
    val permission: AdminPermission?
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
