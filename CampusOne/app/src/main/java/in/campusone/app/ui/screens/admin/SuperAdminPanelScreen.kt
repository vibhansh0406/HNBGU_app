package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.clickable
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
import `in`.campusone.app.ui.components.C1CompactStatCard
import `in`.campusone.app.ui.components.C1FeatureCard
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
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
    AUDIT("AUDIT"),
    ANALYTICS("ANALYTICS"),
    ACADEMIC("ACADEMIC")
}

/**
 * Owner-level administration area. Rendered only when the server-verified
 * identity says the caller may see it; every action inside is re-checked by the
 * database, so a modified APK still cannot perform a privileged mutation.
 *
 * Layout rationale (purposeful, not random):
 * 1. Owner Banner — establishes identity & trust immediately.
 * 2. Quick Actions — the 4 things an owner does daily (approve roles, send notice,
 *    view reports, manage users) are one tap away, not buried in cards.
 * 3. Platform Overview (2×2 grid) — the 4 most important numbers at a glance.
 *    Badges on "Pending Approvals" and "Open Reports" draw attention to work
 *    that needs the owner's decision.
 * 4. Grouped sections — related actions are clustered so the owner doesn't
 *    hunt: USER & ACCESS, CONTENT, MODERATION, PLATFORM.
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

        if (!identity.isOwner) {
            AccessDeniedState(padding = padding, onBack = onBack)
            return@Scaffold
        }

        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val error = MaterialTheme.colorScheme.error

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Owner Banner — identity & trust
            item { OwnerBanner(state = state) }

            // 2. Quick Actions — daily owner tasks, one tap away
            item {
                QuickActionsRow(
                    pendingApprovals = state.roleRequests.count { it.status == "PENDING" },
                    openReports = state.flaggedContent.size,
                    onApproveRoles = { onOpenSection(SuperAdminSection.ROLES) },
                    onSendNotice = { onOpenSection(SuperAdminSection.CONTENT) },
                    onViewReports = { onOpenSection(SuperAdminSection.MODERATION) },
                    onManageUsers = { onOpenSection(SuperAdminSection.USERS) }
                )
            }

            // 3. Platform Overview — 2×2 grid of key numbers
            item {
                PlatformOverviewGrid(
                    totalUsers = state.users.size.toString(),
                    activeUsers = users.count { !it.isSuspended }.toString(),
                    pendingApprovals = state.roleRequests.count { it.status == "PENDING" },
                    openReports = state.flaggedContent.size,
                    onPendingApprovals = { onOpenSection(SuperAdminSection.ROLES) },
                    onOpenReports = { onOpenSection(SuperAdminSection.MODERATION) }
                )
            }

            // 4. Grouped sections — related actions clustered together
            item {
                SectionGroup(
                    title = "USER & ACCESS",
                    subtitle = "Manage accounts, roles and permissions",
                    items = listOf(
                        SectionItem(
                            title = "User Management",
                            subtitle = "${state.users.size} accounts • search, filter, suspend",
                            icon = Icons.Default.Group,
                            tint = primary,
                            onClick = { onOpenSection(SuperAdminSection.USERS) }
                        ),
                        SectionItem(
                            title = "Roles & Permissions",
                            subtitle = if (state.roleRequests.count { it.status == "PENDING" } > 0) {
                                "${state.roleRequests.count { it.status == "PENDING" }} pending requests"
                            } else {
                                "No pending requests"
                            },
                            icon = Icons.Default.Shield,
                            tint = secondary,
                            onClick = { onOpenSection(SuperAdminSection.ROLES) }
                        )
                    )
                )
            }

            item {
                SectionGroup(
                    title = "CONTENT",
                    subtitle = "Notices, resources, assignments, events, clubs",
                    items = listOf(
                        SectionItem(
                            title = "Content Management",
                            subtitle = "Publish, edit and delete campus content",
                            icon = Icons.Default.Description,
                            tint = tertiary,
                            onClick = { onOpenSection(SuperAdminSection.CONTENT) }
                        )
                    )
                )
            }

            item {
                SectionGroup(
                    title = "MODERATION",
                    subtitle = "Review flagged content and content reports",
                    items = listOf(
                        SectionItem(
                            title = "Reports & Moderation",
                            subtitle = if (state.flaggedContent.size > 0) {
                                "${state.flaggedContent.size} items need review"
                            } else {
                                "Queue is clear"
                            },
                            icon = Icons.Default.FactCheck,
                            tint = error,
                            onClick = { onOpenSection(SuperAdminSection.MODERATION) }
                        )
                    )
                )
            }

            item {
                SectionGroup(
                    title = "PLATFORM",
                    subtitle = "Settings, audit, analytics and academic structure",
                    items = listOf(
                        SectionItem(
                            title = "Platform Controls",
                            subtitle = "Maintenance, banners, feature flags",
                            icon = Icons.Default.Tune,
                            tint = primary,
                            onClick = { onOpenSection(SuperAdminSection.SETTINGS) }
                        ),
                        SectionItem(
                            title = "Audit Logs",
                            subtitle = "Append-only record of admin actions",
                            icon = Icons.Default.History,
                            tint = secondary,
                            onClick = { onOpenSection(SuperAdminSection.AUDIT) }
                        ),
                        SectionItem(
                            title = "Analytics & Reporting",
                            subtitle = "Usage trends and activity charts",
                            icon = Icons.Default.InsertChart,
                            tint = tertiary,
                            onClick = { onOpenSection(SuperAdminSection.ANALYTICS) }
                        ),
                        SectionItem(
                            title = "Academic Structure",
                            subtitle = "Subjects, timetable and academic settings",
                            icon = Icons.Default.School,
                            tint = error,
                            onClick = { onOpenSection(SuperAdminSection.ACADEMIC) }
                        )
                    )
                )
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

// =========================================================================
// QUICK ACTIONS ROW
// =========================================================================
@Composable
private fun QuickActionsRow(
    pendingApprovals: Int,
    openReports: Int,
    onApproveRoles: () -> Unit,
    onSendNotice: () -> Unit,
    onViewReports: () -> Unit,
    onManageUsers: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionChip(
                label = "Approve Roles",
                subtitle = if (pendingApprovals > 0) "$pendingApprovals pending" else "None pending",
                icon = Icons.Default.Shield,
                tint = MaterialTheme.colorScheme.secondary,
                badge = if (pendingApprovals > 0) pendingApprovals.toString() else null,
                onClick = onApproveRoles
            )
            QuickActionChip(
                label = "Send Notice",
                subtitle = "Broadcast to campus",
                icon = Icons.Default.Campaign,
                tint = MaterialTheme.colorScheme.primary,
                onClick = onSendNotice
            )
            QuickActionChip(
                label = "View Reports",
                subtitle = if (openReports > 0) "$openReports open" else "Queue clear",
                icon = Icons.Default.FactCheck,
                tint = MaterialTheme.colorScheme.error,
                badge = if (openReports > 0) openReports.toString() else null,
                onClick = onViewReports
            )
            QuickActionChip(
                label = "Manage Users",
                subtitle = "Suspend, reactivate, remove",
                icon = Icons.Default.People,
                tint = MaterialTheme.colorScheme.tertiary,
                onClick = onManageUsers
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.error)
                            .clip(CircleShape)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// PLATFORM OVERVIEW GRID (2×2)
// =========================================================================
@Composable
private fun PlatformOverviewGrid(
    totalUsers: String,
    activeUsers: String,
    pendingApprovals: Int,
    openReports: Int,
    onPendingApprovals: () -> Unit,
    onOpenReports: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Platform Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewStatCard(
                title = "Total Users",
                value = totalUsers,
                icon = Icons.Default.People,
                tint = MaterialTheme.colorScheme.primary
            )
            OverviewStatCard(
                title = "Active Users",
                value = activeUsers,
                icon = Icons.Default.CheckCircle,
                tint = MaterialTheme.colorScheme.secondary
            )
            OverviewStatCard(
                title = "Pending Approvals",
                value = pendingApprovals.toString(),
                icon = Icons.Default.FactCheck,
                tint = MaterialTheme.colorScheme.secondary,
                badge = if (pendingApprovals > 0) pendingApprovals.toString() else null,
                onClick = onPendingApprovals
            )
            OverviewStatCard(
                title = "Open Reports",
                value = openReports.toString(),
                icon = Icons.Default.History,
                tint = MaterialTheme.colorScheme.error,
                badge = if (openReports > 0) openReports.toString() else null,
                onClick = onOpenReports
            )
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    badge: String? = null,
    onClick: () -> Unit = {}
) {
    C1SurfaceCard(
        modifier = Modifier
            .weight(1f)
            .clickable(enabled = onClick != {}, onClick = onClick),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.error)
                            .clip(CircleShape)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// SECTION GROUP
// =========================================================================
private data class SectionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun SectionGroup(
    title: String,
    subtitle: String,
    items: List<SectionItem>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        items(items, key = { it.title }) { item ->
            SectionCard(
                title = item.title,
                subtitle = item.subtitle,
                icon = item.icon,
                tint = item.tint,
                onClick = item.onClick
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    C1FeatureCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        icon = icon,
        title = title,
        description = subtitle,
        iconColor = tint,
        onClick = onClick
    )
}

@Composable
private fun TextButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text) }
}
