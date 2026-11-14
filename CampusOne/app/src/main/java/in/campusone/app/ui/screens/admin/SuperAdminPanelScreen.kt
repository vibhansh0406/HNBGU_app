package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.campusone.app.data.repository.AdminPermission
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import kotlinx.coroutines.flow.StateFlow

/** Sections of the Super Admin panel. */
enum class SuperAdminSection(val route: String) {
    OVERVIEW("OVERVIEW"),
    USERS("USERS"),
    ROLES("ROLES"),
    CONTENT("CONTENT"),
    MODERATION("MODERATION"),
    SETTINGS("SETTINGS"),
    AUDIT("AUDIT")
}

/**
 * Owner-level administration area. Rendered only when the server-verified
 * identity says the caller may see it; every action inside is re-checked by the
 * database, so a modified APK still cannot perform a privileged mutation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminPanelScreen(
    uiState: StateFlow<AdminUiState>,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onOpenSection: (SuperAdminSection) -> Unit = {}
) {
    val state by uiState.collectAsStateWithLifecycle()
    val identity = state.identity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.identityLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Verifying owner identity with the server...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        if (!identity.isOwner && !identity.can(`in`.campusone.app.data.repository.AdminPermission.VIEW_USERS)) {
            AccessDeniedState(padding = padding, onBack = onBack)
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { OwnerBanner(state = state) }

            item {
                Text(
                    text = "Platform overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(overviewStats(state), key = { it.title }) { stat ->
                StatRow(stat.title, stat.value, stat.icon, stat.tint)
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(adminSections(state), key = { it.title }) { section ->
                SectionCard(section.title, section.subtitle, section.icon) {
                    onOpenSection(section.target)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AccessDeniedState(padding: PaddingValues, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Owner access required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This area is restricted to the verified platform owner. " +
                "Your account is signed in but does not hold the required permission.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun OwnerBanner(state: AdminUiState) {
    val identity = state.identity
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = state.profile.name.ifBlank { "Platform owner" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.profile.email.ifBlank { "Not signed in" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Role verified by server: ${identity.role}" +
                    if (identity.isSuspended) " • account suspended" else "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (state.sessionExpired) {
                Text(
                    text = "Session expired. Sign in again to continue privileged actions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private data class StatItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
private fun overviewStats(state: AdminUiState): List<StatItem> {
    val users = state.users
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    return listOf(
        StatItem("Total users", users.size.toString(), Icons.Default.People, primary),
        StatItem(
            "Active",
            users.count { !it.isSuspended }.toString(),
            Icons.Default.CheckCircle,
            secondary
        ),
        StatItem(
            "Suspended",
            users.count { it.isSuspended }.toString(),
            Icons.Default.Block,
            tertiary
        ),
        StatItem(
            "Students",
            users.count { it.role == UserRole.STUDENT }.toString(),
            Icons.Default.Person,
            primary
        ),
        StatItem(
            "CRs",
            users.count { it.role == UserRole.CR }.toString(),
            Icons.Default.School,
            secondary
        ),
        StatItem(
            "Teachers",
            users.count { it.role == UserRole.TEACHER }.toString(),
            Icons.Default.Assignment,
            tertiary
        ),
        StatItem(
            "Club admins",
            users.count { it.role == UserRole.CLUB_ADMIN }.toString(),
            Icons.Default.Group,
            primary
        ),
        StatItem(
            "Pending approvals",
            state.roleRequests.count { it.status == "PENDING" }.toString(),
            Icons.Default.FactCheck,
            secondary
        ),
        StatItem("Notices", state.noticeCount.toString(), Icons.Default.Campaign, tertiary),
        StatItem(
            "Resources",
            state.studyResources.size.toString(),
            Icons.Default.Description,
            primary
        ),
        StatItem("Assignments", state.assignmentCount.toString(), Icons.Default.Assignment, secondary),
        StatItem("Events", state.eventCount.toString(), Icons.Default.Event, tertiary),
        StatItem("Clubs", state.clubCount.toString(), Icons.Default.Group, primary),
        StatItem(
            "Open reports",
            state.flaggedContent.size.toString(),
            Icons.Default.History,
            secondary
        )
    )
}

private data class SectionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val target: SuperAdminSection
)

private fun adminSections(state: AdminUiState): List<SectionItem> {
    val pending = state.roleRequests.count { it.status == "PENDING" }
    val reports = state.flaggedContent.size
    return listOf(
        SectionItem(
            "User management",
            "${state.users.size} accounts • search, filter, suspend",
            Icons.Default.Group,
            SuperAdminSection.USERS
        ),
        SectionItem(
            "Roles & permissions",
            if (pending > 0) "$pending pending requests" else "No pending requests",
            Icons.Default.Shield,
            SuperAdminSection.ROLES
        ),
        SectionItem(
            "Content management",
            "Notices, resources, assignments, events",
            Icons.Default.Description,
            SuperAdminSection.CONTENT
        ),
        SectionItem(
            "Reports & moderation",
            if (reports > 0) "$reports items need review" else "Queue is clear",
            Icons.Default.FactCheck,
            SuperAdminSection.MODERATION
        ),
        SectionItem(
            "Platform controls",
            "Maintenance, banners, feature flags",
            Icons.Default.Tune,
            SuperAdminSection.SETTINGS
        ),
        SectionItem(
            "Audit logs",
            "Append-only record of admin actions",
            Icons.Default.History,
            SuperAdminSection.AUDIT
        )
    )
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRow(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text) }
}