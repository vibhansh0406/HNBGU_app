package `in`.campusone.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.domain.usecase.GradingScaleType
import `in`.campusone.app.domain.usecase.SemesterGpaRecord
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1CompactStatCard
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1FeatureCard
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1StatTile
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.CommunityViewModel
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusUiState
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import `in`.campusone.app.ui.screens.admin.AdminAssignmentsScreen
import `in`.campusone.app.ui.screens.admin.AdminClubsScreen
import `in`.campusone.app.ui.screens.admin.AdminDirectoryScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import `in`.campusone.app.ui.screens.admin.AdminEventsScreen
import `in`.campusone.app.ui.screens.admin.AdminExamsScreen
import `in`.campusone.app.ui.screens.admin.AdminHubScreen
import `in`.campusone.app.ui.screens.admin.AdminLostFoundScreen
import `in`.campusone.app.ui.screens.admin.AdminNoticesScreen
import `in`.campusone.app.ui.screens.admin.AdminSubjectsScreen
import `in`.campusone.app.ui.screens.admin.AdminTimetableScreen
import `in`.campusone.app.ui.screens.admin.ModeratorManagementScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminAuditScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminContentScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminModerationScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminPanelScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminRolesScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminSection
import `in`.campusone.app.ui.screens.admin.SuperAdminSettingsScreen
import `in`.campusone.app.ui.screens.admin.SuperAdminUsersScreen
import `in`.campusone.app.ui.viewmodel.ModeratorViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MoreSubScreen {
    HUB,
    COMMUNITY,
    NOTIFICATIONS,
    PROFILE,
    NOTES_PYQ,
    ASSIGNMENTS,
    EXAMS_SYLLABUS,
    GPA_CALCULATOR,
    EVENTS_CLUBS,
    LOST_FOUND,
    DIRECTORY,
    PRIVACY_POLICY,
    ADMIN_PANEL,
    ADMIN_MODERATORS,
    ADMIN_SUBJECTS,
    ADMIN_TIMETABLE,
    ADMIN_NOTICES,
    ADMIN_ASSIGNMENTS,
    ADMIN_EXAMS,
    ADMIN_CLUBS,
    ADMIN_EVENTS,
    ADMIN_LOST_FOUND,
    ADMIN_DIRECTORY,
    SUPER_ADMIN,
    SUPER_ADMIN_USERS,
    SUPER_ADMIN_ROLES,
    SUPER_ADMIN_CONTENT,
    SUPER_ADMIN_MODERATION,
    SUPER_ADMIN_SETTINGS,
    SUPER_ADMIN_AUDIT
}

private val requestableRoles = listOf(
    UserRole.CR,
    UserRole.TEACHER,
    UserRole.MODERATOR,
    UserRole.CLUB_ADMIN
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoreHubScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel,
    onSignOut: () -> Unit = {},
    deepLink: MoreSubScreen? = null,
    onDeepLinkConsumed: () -> Unit = {},
    adminViewModel: `in`.campusone.app.ui.viewmodel.AdminViewModel = hiltViewModel(),
    moderatorViewModel: `in`.campusone.app.ui.viewmodel.ModeratorViewModel = hiltViewModel(),
    communityViewModel: CommunityViewModel = hiltViewModel(),
    coreViewModel: CoreViewModel = hiltViewModel(),
    onNotificationNavigate: (targetType: String, targetId: String) -> Unit = { _, _ -> },
    focusPostId: String? = null,
    onFocusPostConsumed: () -> Unit = {},
    focusAssignmentId: String? = null,
    onFocusAssignmentConsumed: () -> Unit = {}
) {
    var activeScreen by rememberSaveable { mutableStateOf(MoreSubScreen.HUB) }
    var showRoleRequest by rememberSaveable { mutableStateOf(false) }
    var requestedRole by rememberSaveable { mutableStateOf(UserRole.CR.name) }
    var roleRequestNote by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            activeScreen = deepLink
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(focusPostId) {
        if (focusPostId != null) {
            activeScreen = MoreSubScreen.COMMUNITY
            communityViewModel.openPost(focusPostId)
            onFocusPostConsumed()
        }
    }

    BackHandler(enabled = activeScreen != MoreSubScreen.HUB) {
        activeScreen = MoreSubScreen.HUB
    }

    val isAdmin = uiState.profile.role == `in`.campusone.app.domain.model.UserRole.ADMIN ||
        uiState.profile.email.equals(
            `in`.campusone.app.data.repository.CoreCampusRepository.OWNER_EMAIL,
            ignoreCase = true
        )
    val isAdminOrStaff = isAdmin ||
        uiState.profile.role == `in`.campusone.app.domain.model.UserRole.TEACHER ||
        uiState.profile.role == `in`.campusone.app.domain.model.UserRole.CR
    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
    val communityState by communityViewModel.uiState.collectAsStateWithLifecycle()
    val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
    val unreadNotifications by communityViewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val isSuperAdmin = adminState.identity.isOwner ||
        adminState.identity.can(`in`.campusone.app.data.repository.AdminPermission.VIEW_USERS)

    androidx.compose.runtime.LaunchedEffect(uiState.profile) {
        adminViewModel.init(uiState.profile)
        moderatorViewModel.init()
    }

    if (activeScreen == MoreSubScreen.COMMUNITY) {
        CommunityHubScreen(
            viewModel = communityViewModel,
            onBack = { activeScreen = MoreSubScreen.HUB },
            onOpenNotifications = { activeScreen = MoreSubScreen.NOTIFICATIONS },
            onOpenProfile = { activeScreen = MoreSubScreen.PROFILE }
        )
        return
    }
    if (activeScreen == MoreSubScreen.NOTIFICATIONS) {
        NotificationsScreen(
            viewModel = communityViewModel,
            onBack = { activeScreen = MoreSubScreen.HUB },
            onNavigate = { targetType, targetId ->
                activeScreen = MoreSubScreen.HUB
                onNotificationNavigate(targetType, targetId)
            }
        )
        return
    }
    if (activeScreen == MoreSubScreen.PROFILE) {
        StudentProfileScreen(
            uiState = coreState,
            onBack = { activeScreen = MoreSubScreen.HUB },
            onSaveName = coreViewModel::updateDisplayName
        )
        return
    }

    if (activeScreen == MoreSubScreen.HUB) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.study_hub),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.notes_pyq),
                    subtitle = stringResource(R.string.hub_resources_sub, uiState.resources.size),
                    icon = Icons.Default.Description,
                    onClick = { activeScreen = MoreSubScreen.NOTES_PYQ }
                )
            }
            item {
                val pendingCount = uiState.assignments.count { !it.isCompleted && it.isAddedToPersonalList }
                FeatureHubCard(
                    title = stringResource(R.string.assignments_title),
                    subtitle = stringResource(R.string.hub_assignments_sub, pendingCount),
                    icon = Icons.Default.Assignment,
                    onClick = { activeScreen = MoreSubScreen.ASSIGNMENTS }
                )
            }
            item {
                val doneTopics = uiState.syllabusTopics.count { it.isCompleted }
                val totalTopics = uiState.syllabusTopics.size
                FeatureHubCard(
                    title = stringResource(R.string.exams_syllabus),
                    subtitle = stringResource(R.string.hub_exams_sub, uiState.exams.size, doneTopics, totalTopics),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { activeScreen = MoreSubScreen.EXAMS_SYLLABUS }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.gpa_calculator),
                    subtitle = stringResource(R.string.hub_gpa_sub, uiState.gpaResult.formattedSgpa, uiState.gpaResult.formattedCgpa, uiState.gpaResult.formattedPercentage),
                    icon = Icons.Default.Calculate,
                    onClick = { activeScreen = MoreSubScreen.GPA_CALCULATOR }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.more_campus_community),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.community_title),
                    subtitle = stringResource(R.string.hub_community_sub, communityState.posts.size),
                    icon = Icons.Default.Forum,
                    onClick = { activeScreen = MoreSubScreen.COMMUNITY }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.notifications_title),
                    subtitle = if (unreadNotifications > 0) {
                        "$unreadNotifications ${stringResource(R.string.notifications_unread)} • ${stringResource(R.string.notifications_subtitle)}"
                    } else {
                        stringResource(R.string.notifications_subtitle)
                    },
                    icon = Icons.Default.Notifications,
                    onClick = { activeScreen = MoreSubScreen.NOTIFICATIONS }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.profile_title),
                    subtitle = stringResource(R.string.profile_subtitle),
                    icon = Icons.Default.Person,
                    onClick = { activeScreen = MoreSubScreen.PROFILE }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campus Life & Utilities",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.events_clubs),
                    subtitle = stringResource(R.string.hub_events_sub, uiState.events.size),
                    icon = Icons.Default.Event,
                    onClick = { activeScreen = MoreSubScreen.EVENTS_CLUBS }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.lost_found),
                    subtitle = stringResource(R.string.hub_lostfound_sub, uiState.lostFoundItems.count { !it.isResolved }),
                    icon = Icons.Default.FindInPage,
                    onClick = { activeScreen = MoreSubScreen.LOST_FOUND }
                )
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.campus_directory),
                    subtitle = stringResource(R.string.hub_directory_sub),
                    icon = Icons.Default.Call,
                    onClick = { activeScreen = MoreSubScreen.DIRECTORY }
                )
            }
            if (isSuperAdmin) {
                item {
                    FeatureHubCard(
                        title = "Super Admin",
                        subtitle = stringResource(R.string.hub_superadmin_sub),
                        icon = Icons.Default.Shield,
                        onClick = {
                            adminViewModel.init(uiState.profile)
                            activeScreen = MoreSubScreen.SUPER_ADMIN
                        }
                    )
                }
            }
            if (isAdminOrStaff) {
                item {
                    FeatureHubCard(
                        title = "Admin Panel",
                        subtitle = stringResource(R.string.hub_admin_sub),
                        icon = Icons.Default.School,
                        onClick = {
                            adminViewModel.init(uiState.profile)
                            activeScreen = MoreSubScreen.ADMIN_PANEL
                        }
                    )
                }
            }
            if (uiState.profile.role == UserRole.STUDENT) {
                item {
                    FeatureHubCard(
                        title = "Request a Role",
                        subtitle = stringResource(R.string.hub_role_sub),
                        icon = Icons.Default.AccountCircle,
                        onClick = { showRoleRequest = true }
                    )
                }
            }
            item {
                FeatureHubCard(
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.hub_privacy_sub),
                    icon = Icons.Default.PrivacyTip,
                    onClick = { activeScreen = MoreSubScreen.PRIVACY_POLICY }
                )
            }
            item {
                FeatureHubCard(
                    title = "Sign Out",
                    subtitle = stringResource(R.string.hub_signout_sub),
                    icon = Icons.Default.AccountCircle,
                    onClick = onSignOut
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
        if (showRoleRequest) {
            AlertDialog(
                onDismissRequest = { showRoleRequest = false },
                title = { Text("Request a role") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Your request is sent to the admin. Access is granted only after approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            requestableRoles.forEach { role ->
                                FilterChip(
                                    selected = requestedRole == role.name,
                                    onClick = { requestedRole = role.name },
                                    label = { Text(role.name) }
                                )
                            }
                        }
                        C1TextField(
                            value = roleRequestNote,
                            onValueChange = { roleRequestNote = it },
                            label = "Note (optional)",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    C1Button(
                        text = "Send request",
                        onClick = {
                            viewModel.requestRole(
                                UserRole.entries.firstOrNull { it.name == requestedRole } ?: UserRole.CR,
                                roleRequestNote.trim()
                            )
                            showRoleRequest = false
                        }
                    )
                },
                dismissButton = {
                    C1Button(
                        text = "Cancel",
                        onClick = { showRoleRequest = false },
                        intent = C1ButtonIntent.GHOST
                    )
                }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(activeScreen.name.replace("_", " ")) },
                    navigationIcon = {
                        IconButton(onClick = { activeScreen = MoreSubScreen.HUB }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Study Hub")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                when (activeScreen) {
                    MoreSubScreen.COMMUNITY,
                    MoreSubScreen.NOTIFICATIONS,
                    MoreSubScreen.PROFILE -> Unit
                    MoreSubScreen.NOTES_PYQ -> NotesAndPyqScreen(uiState, viewModel)
                    MoreSubScreen.ASSIGNMENTS -> AssignmentsScreen(
                        uiState,
                        viewModel,
                        focusAssignmentId = focusAssignmentId,
                        onFocusConsumed = onFocusAssignmentConsumed
                    )
                    MoreSubScreen.EXAMS_SYLLABUS -> ExamsAndSyllabusScreen(uiState, viewModel)
                    MoreSubScreen.GPA_CALCULATOR -> GpaCalculatorScreen(uiState, viewModel)
                    MoreSubScreen.EVENTS_CLUBS -> EventsAndClubsScreen(uiState, viewModel)
                    MoreSubScreen.LOST_FOUND -> LostAndFoundScreen(uiState, viewModel)
                    MoreSubScreen.DIRECTORY -> CampusDirectoryScreen(uiState, viewModel)
                    MoreSubScreen.PRIVACY_POLICY -> PrivacyPolicyScreen()
                    MoreSubScreen.ADMIN_PANEL -> AdminHubScreen(
                        uiState = adminState,
                        viewModel = adminViewModel,
                        onBack = { activeScreen = MoreSubScreen.HUB },
                        onManageModerators = { activeScreen = MoreSubScreen.ADMIN_MODERATORS },
                        onNavigate = { route ->
                            activeScreen = when (route) {
                                "SUBJECTS" -> MoreSubScreen.ADMIN_SUBJECTS
                                "TIMETABLE" -> MoreSubScreen.ADMIN_TIMETABLE
                                "NOTICES" -> MoreSubScreen.ADMIN_NOTICES
                                "ASSIGNMENTS" -> MoreSubScreen.ADMIN_ASSIGNMENTS
                                "EXAMS" -> MoreSubScreen.ADMIN_EXAMS
                                "CLUBS" -> MoreSubScreen.ADMIN_CLUBS
                                "EVENTS" -> MoreSubScreen.ADMIN_EVENTS
                                "LOST_FOUND" -> MoreSubScreen.ADMIN_LOST_FOUND
                                "DIRECTORY" -> MoreSubScreen.ADMIN_DIRECTORY
                                "USERS" -> MoreSubScreen.SUPER_ADMIN_USERS
                                "REVIEW" -> MoreSubScreen.SUPER_ADMIN_MODERATION
                                else -> MoreSubScreen.ADMIN_PANEL
                            }
                        }
                    )
                    MoreSubScreen.ADMIN_MODERATORS -> ModeratorManagementScreen(
                        uiState = moderatorViewModel.uiState.value,
                        viewModel = moderatorViewModel,
                        onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_SUBJECTS -> AdminSubjectsScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_TIMETABLE -> AdminTimetableScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_NOTICES -> AdminNoticesScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_ASSIGNMENTS -> AdminAssignmentsScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_EXAMS -> AdminExamsScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_CLUBS -> AdminClubsScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_EVENTS -> AdminEventsScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_LOST_FOUND -> AdminLostFoundScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.ADMIN_DIRECTORY -> AdminDirectoryScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.ADMIN_PANEL }
                    )
                    MoreSubScreen.SUPER_ADMIN -> SuperAdminPanelScreen(
                        uiState = adminViewModel.uiState,
                        viewModel = adminViewModel,
                        onBack = { activeScreen = MoreSubScreen.HUB },
                        onOpenSection = { section ->
                            activeScreen = when (section) {
                                SuperAdminSection.USERS -> MoreSubScreen.SUPER_ADMIN_USERS
                                SuperAdminSection.ROLES -> MoreSubScreen.SUPER_ADMIN_ROLES
                                SuperAdminSection.CONTENT -> MoreSubScreen.SUPER_ADMIN_CONTENT
                                SuperAdminSection.MODERATION -> MoreSubScreen.SUPER_ADMIN_MODERATION
                                SuperAdminSection.SETTINGS -> MoreSubScreen.SUPER_ADMIN_SETTINGS
                                SuperAdminSection.AUDIT -> MoreSubScreen.SUPER_ADMIN_AUDIT
                                SuperAdminSection.OVERVIEW -> MoreSubScreen.SUPER_ADMIN
                            }
                        }
                    )
                    MoreSubScreen.SUPER_ADMIN_USERS -> SuperAdminUsersScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.SUPER_ADMIN }
                    )
                    MoreSubScreen.SUPER_ADMIN_ROLES -> SuperAdminRolesScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.SUPER_ADMIN }
                    )
                    MoreSubScreen.SUPER_ADMIN_CONTENT -> SuperAdminContentScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.SUPER_ADMIN }
                    )
                    MoreSubScreen.SUPER_ADMIN_MODERATION -> SuperAdminModerationScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.SUPER_ADMIN }
                    )
                    MoreSubScreen.SUPER_ADMIN_SETTINGS -> SuperAdminSettingsScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.SUPER_ADMIN }
                    )
                    MoreSubScreen.SUPER_ADMIN_AUDIT -> SuperAdminAuditScreen(
                        adminState, adminViewModel, onBack = { activeScreen = MoreSubScreen.SUPER_ADMIN }
                    )
                    MoreSubScreen.HUB -> Unit
                }
            }
        }
    }
}

@Composable
private fun FeatureHubCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    C1FeatureCard(
        modifier = Modifier.fillMaxWidth(),
        icon = icon,
        title = title,
        description = subtitle,
        iconColor = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

// =========================================================================
// PHASE 2.1 — NOTES & PYQ SHARING
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesAndPyqScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") } // ALL, NOTES, PYQ, ASSIGNMENT, OTHER
    var selectedSemesterFilter by rememberSaveable { mutableIntStateOf(0) } // 0 = All Semesters
    var sortByUpvotes by rememberSaveable { mutableStateOf(true) } // true = Most Upvoted, false = Newest
    var bookmarkedOnly by rememberSaveable { mutableStateOf(false) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }
    var viewingResource by remember { mutableStateOf<StudyResourceEntity?>(null) }

    val isCrOrTeacher = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER

    val filtered = uiState.resources
        .filter { res ->
            (selectedCategory == "ALL" || res.category == selectedCategory) &&
                (selectedSemesterFilter == 0 || res.semester == selectedSemesterFilter) &&
                (!bookmarkedOnly || res.isBookmarkedOffline) &&
                (searchQuery.isBlank() ||
                    res.title.contains(searchQuery, ignoreCase = true) ||
                    res.subjectName.contains(searchQuery, ignoreCase = true) ||
                    res.subjectId.contains(searchQuery, ignoreCase = true) ||
                    res.examYear.toString().contains(searchQuery))
        }
        .let { list ->
            if (sortByUpvotes) {
                list.sortedWith(compareByDescending<StudyResourceEntity> { it.upvotes }.thenByDescending { it.createdAtMillis })
            } else {
                list.sortedByDescending { it.createdAtMillis }
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showUploadDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Upload Notes or PYQ")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                C1TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search subject, topic, or exam year (e.g., 2025)",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "NOTES", "PYQ", "ASSIGNMENT", "OTHER").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Sort & Offline Bookmark Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortByUpvotes,
                        onClick = { sortByUpvotes = true },
                        label = { Text(stringResource(R.string.sort_upvoted)) }
                    )
                    FilterChip(
                        selected = !sortByUpvotes,
                        onClick = { sortByUpvotes = false },
                        label = { Text(stringResource(R.string.sort_newest)) }
                    )
                    FilterChip(
                        selected = bookmarkedOnly,
                        onClick = { bookmarkedOnly = !bookmarkedOnly },
                        label = { Text("Saved Offline") }
                    )
                }
            }

            // CR / Teacher Moderation Queue
            if (isCrOrTeacher && uiState.moderationQueue.isNotEmpty()) {
                item {
                    C1SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "CR / Faculty Moderation Queue (${uiState.moderationQueue.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.moderationQueue.forEach { flagged ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = "${flagged.title} (${flagged.subjectId})",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Reports: ${flagged.reportCount} • Status: ${if (flagged.isHidden) "Hidden" else "Flagged"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        C1Button(
                                            text = if (flagged.isHidden) "Unhide" else "Hide",
                                            onClick = { viewModel.crHideResource(flagged.id, !flagged.isHidden) },
                                            intent = C1ButtonIntent.OUTLINED,
                                            icon = { Icon(if (flagged.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        C1Button(
                                            text = "Remove",
                                            onClick = { viewModel.crDeleteResource(flagged.id) },
                                            intent = C1ButtonIntent.DANGER,
                                            icon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        C1Button(
                                            text = "Keep",
                                            onClick = { viewModel.crDismissReport(flagged.id) },
                                            intent = C1ButtonIntent.GHOST
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(filtered, key = { it.id }) { res ->
                StudyResourceCard(
                    res = res,
                    isCrOrTeacher = isCrOrTeacher,
                    onOpenViewer = { viewingResource = res },
                    onUpvote = { viewModel.upvoteResource(res.id) },
                    onToggleBookmark = { viewModel.toggleBookmark(res.id, !res.isBookmarkedOffline) },
                    onDownloadOffline = { viewModel.downloadResourceOffline(res) },
                    onReport = { viewModel.reportResource(res.id) },
                    onCrHide = { viewModel.crHideResource(res.id, true) },
                    onRetryUpload = { viewModel.retryUpload(res.id) }
                )
            }
        }
    }

    if (showUploadDialog) {
        UploadStudyResourceDialog(
            defaultSemester = uiState.profile.semester,
            onDismiss = { showUploadDialog = false },
            onUpload = { subjId, subjName, sem, title, cat, year, uri, type, showName ->
                viewModel.uploadResource(
                    subjectId = subjId,
                    subjectName = subjName,
                    semester = sem,
                    title = title,
                    category = cat,
                    examYear = year,
                    contentUri = uri,
                    fileType = type,
                    showUploaderName = showName
                )
                showUploadDialog = false
            }
        )
    }

    viewingResource?.let { resource ->
        InAppZoomableDocumentViewerDialog(
            resource = resource,
            fileLoader = viewModel::obtainViewerFile,
            onDismiss = { viewingResource = null },
            onDownloadOffline = { viewModel.downloadResourceOffline(resource) }
        )
    }
}

@Composable
private fun StudyResourceCard(
    res: StudyResourceEntity,
    isCrOrTeacher: Boolean,
    onOpenViewer: () -> Unit,
    onUpvote: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDownloadOffline: () -> Unit,
    onReport: () -> Unit,
    onCrHide: () -> Unit,
    onRetryUpload: () -> Unit
) {
    val displayedUploader = if (res.showUploaderName) {
        res.uploaderName
    } else {
        stringResource(R.string.anonymous_student)
    }
    val sizeKb = (res.fileSizeBytes / 1024L).coerceAtLeast(1L)

    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onOpenViewer,
                    label = { Text("${res.category} • ${res.examYear} • ${res.fileType} (${sizeKb} KB)") }
                )
                Row {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (res.isBookmarkedOffline) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.bookmark_offline),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDownloadOffline) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.download_offline)
                        )
                    }
                }
            }

            // Resumable Upload Progress Indicator if currently uploading or failed
            if (res.uploadStatus != "COMPLETED") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Upload ${res.uploadStatus} (${res.uploadProgressPct}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (res.uploadStatus == "FAILED" || res.uploadStatus == "PAUSED") {
                        TextButton(onClick = onRetryUpload) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Text("Retry")
                            }
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { res.uploadProgressPct / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = res.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sem ${res.semester} • ${res.subjectId} (${res.subjectName}) • By $displayedUploader",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (res.localFilePath != null) {
                Text(
                    text = "✓ Downloaded for offline viewing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1Button(
                        text = "${res.upvotes}",
                        onClick = onUpvote,
                        intent = C1ButtonIntent.TONAL,
                        icon = { Icon(Icons.Default.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(16.dp)) }
                    )
                    C1Button(
                        text = stringResource(R.string.view_document),
                        onClick = onOpenViewer,
                        intent = C1ButtonIntent.OUTLINED,
                        icon = { Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                Row {
                    if (isCrOrTeacher) {
                        IconButton(onClick = onCrHide) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "Hide content")
                        }
                    }
                    IconButton(onClick = onReport) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Abuse")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadStudyResourceDialog(
    defaultSemester: Int,
    onDismiss: () -> Unit,
    onUpload: (
        subjectId: String,
        subjectName: String,
        semester: Int,
        title: String,
        category: String,
        examYear: Int,
        fileUri: String,
        fileType: String,
        showUploaderName: Boolean
    ) -> Unit
) {
    var subjectId by rememberSaveable { mutableStateOf("CS301") }
    var subjectName by rememberSaveable { mutableStateOf("Data Structures & Algorithms") }
    var semester by rememberSaveable { mutableIntStateOf(defaultSemester) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("NOTES") }
    var examYear by rememberSaveable { mutableIntStateOf(2025) }
    var fileType by rememberSaveable { mutableStateOf("PDF") }
    var selectedUriString by rememberSaveable { mutableStateOf("") }
    var pickedSizeMb by rememberSaveable { mutableFloatStateOf(0f) }
    var showUploaderName by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUriString = uri.toString()
            pickedSizeMb = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            }.getOrDefault(0L) / (1024f * 1024f)
            fileType = if (context.contentResolver.getType(uri)?.contains("pdf") == true) "PDF" else "IMAGE"
        }
    }

    val isOverLimit = pickedSizeMb > 10.0f
    val hasFile = selectedUriString.isNotBlank() && pickedSizeMb > 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Notes / PYQ / Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.file_limit_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(
                        value = subjectId,
                        onValueChange = { subjectId = it },
                        label = "Subject Code",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    C1TextField(
                        value = examYear.toString(),
                        onValueChange = { examYear = it.toIntOrNull() ?: 2025 },
                        label = "Year",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                C1TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Document Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NOTES", "PYQ", "ASSIGNMENT", "OTHER").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = fileType == "PDF",
                        onClick = { fileType = "PDF" },
                        label = { Text("PDF Document") }
                    )
                    FilterChip(
                        selected = fileType == "IMAGE",
                        onClick = { fileType = "IMAGE" },
                        label = { Text("Camera / Gallery Image") }
                    )
                }
                C1Button(
                    text = "Pick File / Gallery Image",
                    onClick = {
                        val mime = if (fileType == "PDF") "application/pdf" else "image/*"
                        runCatching { filePickerLauncher.launch(mime) }
                    },
                    intent = C1ButtonIntent.OUTLINED,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasFile) {
                    Text(
                        text = if (pickedSizeMb >= 1f) "Selected: %.1f MB".format(pickedSizeMb)
                        else "Selected: %.0f KB".format(pickedSizeMb * 1024f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showUploaderName,
                        onCheckedChange = { showUploaderName = it }
                    )
                    Text(stringResource(R.string.show_my_name))
                }
            }
        },
        confirmButton = {
            C1Button(
                text = "Start Upload",
                onClick = {
                    onUpload(
                        subjectId,
                        subjectName,
                        semester,
                        title,
                        category,
                        examYear,
                        selectedUriString,
                        fileType,
                        showUploaderName
                    )
                },
                enabled = title.isNotBlank() && hasFile && !isOverLimit
            )
        },
        dismissButton = {
            C1Button(
                text = "Cancel",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

@Composable
private fun InAppZoomableDocumentViewerDialog(
    resource: StudyResourceEntity,
    fileLoader: suspend (StudyResourceEntity) -> Pair<Int, File>?,
    onDismiss: () -> Unit,
    onDownloadOffline: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sourceFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resource.id) {
        val loaded = fileLoader(resource)
        if (loaded == null) {
            loading = false
            loadError = "Document could not be downloaded for viewing"
        } else {
            totalPages = loaded.first
            sourceFile = loaded.second
            currentPage = 1
        }
    }

    LaunchedEffect(sourceFile, currentPage, resource.fileType) {
        val file = sourceFile ?: return@LaunchedEffect
        loading = true
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (resource.fileType == "PDF") {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            renderer.openPage((currentPage - 1).coerceIn(0, renderer.pageCount - 1)).use { page ->
                                val bmp = Bitmap.createBitmap(
                                    page.width * 2,
                                    page.height * 2,
                                    Bitmap.Config.ARGB_8888
                                )
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp
                            }
                        }
                    }
                } else {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
            }.getOrNull()
        }
        pageBitmap = bitmap
        if (bitmap == null) loadError = "Page could not be rendered"
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${resource.subjectId} • ${resource.category} (${resource.examYear}) • Pinch to Zoom (${String.format(Locale.US, "%.1fx", scale)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    offsetX = (offsetX + pan.x).coerceIn(-300f, 300f)
                                    offsetY = (offsetY + pan.y).coerceIn(-300f, 300f)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = pageBitmap
                    when {
                        loading -> CircularProgressIndicator()
                        bmp != null -> Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = resource.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit
                        )
                        loadError != null -> Text(
                            text = loadError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> Text(
                            text = "No content",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resource.fileType == "PDF" && totalPages > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            C1Button(
                                text = "Prev",
                                onClick = { currentPage-- },
                                intent = C1ButtonIntent.OUTLINED,
                                enabled = currentPage > 1
                            )
                            C1Button(
                                text = "Next",
                                onClick = { currentPage++ },
                                intent = C1ButtonIntent.OUTLINED,
                                enabled = currentPage < totalPages
                            )
                            Text(
                                text = "$currentPage / $totalPages",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    C1Button(
                        text = "Reset Zoom",
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        intent = C1ButtonIntent.GHOST
                    )
                }
            }
        },
        confirmButton = {
            C1Button(
                text = stringResource(R.string.download_offline),
                onClick = onDownloadOffline,
                icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        },
        dismissButton = {
            C1Button(
                text = "Close",
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

// =========================================================================
// PHASE 2.2 — ASSIGNMENTS & DEADLINES
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentsScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel,
    focusAssignmentId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var groupBySubject by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val endOfToday = now + 24 * 3_600_000L
    val isCrOrTeacher = uiState.profile.role == UserRole.CR || uiState.profile.role == UserRole.TEACHER

    LaunchedEffect(focusAssignmentId, uiState.assignments) {
        val target = focusAssignmentId?.let { id -> uiState.assignments.firstOrNull { it.id == id } }
        if (target != null) {
            selectedTab = when {
                target.isCompleted -> 3
                target.dueDateEpochMillis < now -> 2
                target.dueDateEpochMillis <= endOfToday -> 0
                else -> 1
            }
            groupBySubject = false
            onFocusConsumed()
        }
    }

    val sharedNotAdded = uiState.assignments.filter { it.isSharedByCrOrTeacher && !it.isAddedToPersonalList }
    val personalTasks = uiState.assignments.filter { it.isAddedToPersonalList }
    val pendingCount = personalTasks.count { !it.isCompleted }
    val overdueCount = personalTasks.count { !it.isCompleted && it.dueDateEpochMillis < now }
    val completedCount = personalTasks.count { it.isCompleted }

    val filteredTasks = when (selectedTab) {
        0 -> personalTasks.filter { !it.isCompleted && it.dueDateEpochMillis in now..endOfToday }
        1 -> personalTasks.filter { !it.isCompleted && it.dueDateEpochMillis > endOfToday }
        2 -> personalTasks.filter { !it.isCompleted && it.dueDateEpochMillis < now }
        else -> personalTasks.filter { it.isCompleted }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_assignment))
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_today)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.tab_upcoming)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(stringResource(R.string.tab_overdue)) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(stringResource(R.string.tab_completed)) })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(C1Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                        C1StatTile(
                            label = stringResource(R.string.assignment_pending),
                            value = pendingCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        C1StatTile(
                            label = stringResource(R.string.tab_overdue),
                            value = overdueCount.toString(),
                            modifier = Modifier.weight(1f),
                            valueColor = MaterialTheme.colorScheme.error
                        )
                        C1StatTile(
                            label = stringResource(R.string.tab_completed),
                            value = completedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = groupBySubject,
                            onClick = { groupBySubject = !groupBySubject },
                            label = { Text(stringResource(R.string.group_by_subject)) }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.assignment_reminders_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }

                if (sharedNotAdded.isNotEmpty()) {
                    item {
                        SharedAssignmentsCard(
                            shared = sharedNotAdded,
                            onAdd = { id -> viewModel.addSharedAssignmentToMyList(id) }
                        )
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item { AssignmentsEmptyState() }
                } else if (groupBySubject) {
                    filteredTasks.groupBy { it.subjectTag }.forEach { (subjectHeader, tasksInSubject) ->
                        item {
                            C1SectionHeader(title = subjectHeader)
                        }
                        items(tasksInSubject, key = { it.id }) { task ->
                            AssignmentItemCard(
                                task = task,
                                now = now,
                                onToggleComplete = { viewModel.toggleAssignment(task.id, it) },
                                onDelete = { viewModel.deleteAssignment(task.id) }
                            )
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        AssignmentItemCard(
                            task = task,
                            now = now,
                            onToggleComplete = { viewModel.toggleAssignment(task.id, it) },
                            onDelete = { viewModel.deleteAssignment(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssignmentDialog(
            isCrOrTeacher = isCrOrTeacher,
            onDismiss = { showAddDialog = false },
            onSave = { subjectId, subjectTag, title, description, priority, dueOffsetDays, dueDateFormatted, remind1Day, remind2Hours, postToSection ->
                viewModel.saveAssignment(
                    subjectId = subjectId,
                    subjectTag = subjectTag,
                    title = title,
                    description = description,
                    priority = priority,
                    dueOffsetDays = dueOffsetDays,
                    dueDateFormatted = dueDateFormatted,
                    remind1Day = remind1Day,
                    remind2Hours = remind2Hours,
                    postToSection = postToSection
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AssignmentsEmptyState() {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        C1EmptyState(
            icon = Icons.Default.Assignment,
            title = stringResource(R.string.assignments_empty_title),
            message = stringResource(R.string.assignments_empty_hint)
        )
    }
}

@Composable
private fun SharedAssignmentsCard(
    shared: List<AssignmentEntity>,
    onAdd: (String) -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(C1Spacing.lg)) {
            Text(
                text = stringResource(R.string.shared_by_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            shared.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = C1Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${task.subjectTag} • ${task.dueDateFormatted} • ${task.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    C1Button(
                        text = stringResource(R.string.add_to_my_list),
                        onClick = { onAdd(task.id) },
                        intent = C1ButtonIntent.PRIMARY
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignmentItemCard(
    task: AssignmentEntity,
    now: Long,
    onToggleComplete: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val overdue = !task.isCompleted && task.dueDateEpochMillis < now
    val dueSoon = !task.isCompleted && !overdue && task.dueDateEpochMillis - now <= 86_400_000L

    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = if (overdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(C1Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggleComplete
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.subjectTag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                    if (task.isCompleted) {
                        C1StatusChip(
                            text = stringResource(R.string.tab_completed),
                            tone = C1Tone.SUCCESS
                        )
                    } else {
                        if (overdue) {
                            C1StatusChip(
                                text = stringResource(R.string.tab_overdue),
                                tone = C1Tone.DANGER
                            )
                        } else if (task.priority == "HIGH") {
                            C1StatusChip(
                                text = stringResource(R.string.assignment_urgent),
                                tone = C1Tone.DANGER
                            )
                        } else {
                            C1StatusChip(
                                text = stringResource(priorityLabelRes(task.priority)),
                                tone = priorityTone(task.priority)
                            )
                        }
                        if (dueSoon) {
                            C1StatusChip(
                                text = stringResource(R.string.assignment_due),
                                tone = C1Tone.WARNING
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                Text(
                    text = stringResource(R.string.assignment_due_date) + ": " + task.dueDateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_assignment)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddAssignmentDialog(
    isCrOrTeacher: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        subjectId: String,
        subjectTag: String,
        title: String,
        description: String,
        priority: String,
        dueOffsetDays: Int,
        dueDateFormatted: String,
        remind1Day: Boolean,
        remind2Hours: Boolean,
        postToSection: Boolean
    ) -> Unit
) {
    var subjectTag by rememberSaveable { mutableStateOf("OS • CS302") }
    var title by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("HIGH") }
    var dueOffsetDays by rememberSaveable { mutableIntStateOf(2) }
    var remind1Day by rememberSaveable { mutableStateOf(true) }
    var remind2Hours by rememberSaveable { mutableStateOf(true) }
    var postToWholeSection by rememberSaveable { mutableStateOf(false) }

    val dueMillis = remember(dueOffsetDays) { System.currentTimeMillis() + dueOffsetDays * 86_400_000L }
    val dueFormatted = remember(dueMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(dueMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_assignment)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                C1TextField(
                    value = subjectTag,
                    onValueChange = { subjectTag = it },
                    label = stringResource(R.string.assignment_subject_tag)
                )
                C1TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.assignment_title)
                )
                C1TextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = stringResource(R.string.assignment_notes),
                    singleLine = false
                )
                Text(
                    text = stringResource(R.string.assignment_due_date),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    listOf(0, 1, 2, 7).forEach { offset ->
                        FilterChip(
                            selected = dueOffsetDays == offset,
                            onClick = { dueOffsetDays = offset },
                            label = {
                                Text(
                                    when (offset) {
                                        0 -> stringResource(R.string.tab_today)
                                        1 -> stringResource(R.string.assignment_tomorrow)
                                        7 -> stringResource(R.string.assignment_next_week)
                                        else -> stringResource(R.string.assignment_due_in_days, offset)
                                    }
                                )
                            }
                        )
                    }
                }
                Text(
                    text = dueFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(stringResource(priorityLabelRes(p))) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remind1Day, onCheckedChange = { remind1Day = it })
                    Spacer(modifier = Modifier.width(C1Spacing.xs))
                    Text(stringResource(R.string.assignment_remind_1d))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remind2Hours, onCheckedChange = { remind2Hours = it })
                    Spacer(modifier = Modifier.width(C1Spacing.xs))
                    Text(stringResource(R.string.assignment_remind_2h))
                }
                if (isCrOrTeacher) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = postToWholeSection, onCheckedChange = { postToWholeSection = it })
                        Spacer(modifier = Modifier.width(C1Spacing.xs))
                        Text(stringResource(R.string.assignment_share))
                    }
                }
            }
        },
        confirmButton = {
            C1Button(
                text = stringResource(R.string.assignment_save),
                onClick = {
                    onSave(
                        subjectTag.substringAfterLast("•").trim(),
                        subjectTag,
                        title,
                        desc,
                        priority,
                        dueOffsetDays,
                        dueFormatted,
                        remind1Day,
                        remind2Hours,
                        postToWholeSection
                    )
                },
                enabled = title.isNotBlank()
            )
        },
        dismissButton = {
            C1Button(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

private fun priorityLabelRes(priority: String): Int = when (priority) {
    "HIGH" -> R.string.priority_high
    "LOW" -> R.string.priority_low
    else -> R.string.priority_medium
}

private fun priorityTone(priority: String): C1Tone = when (priority) {
    "HIGH" -> C1Tone.DANGER
    "LOW" -> C1Tone.NEUTRAL
    else -> C1Tone.WARNING
}

// =========================================================================
// PHASE 2.3 — EXAM CALENDAR & SYLLABUS TRACKER
// =========================================================================
@Composable
fun ExamsAndSyllabusScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    var showAddExamDialog by rememberSaveable { mutableStateOf(false) }
    var showAddTopicDialog by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()

    val totalTopics = uiState.syllabusTopics.size
    val completedTopics = uiState.syllabusTopics.count { it.isCompleted }
    val overallProgress = if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExamDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.publish_exam))
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall Syllabus Progress Card
            item {
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.overall_syllabus_progress),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$completedTopics / $totalTopics (${(overallProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    }
                }
            }

            item {
                Text("Exam Schedule & Countdowns", style = MaterialTheme.typography.titleLarge)
            }

            items(uiState.exams, key = { it.id }) { exam ->
                val daysLeft = ((exam.examDateEpochMillis - now) / 86_400_000L).coerceAtLeast(0L)
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${exam.subjectCode} • ${exam.examTitle}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = exam.subjectName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${exam.examDateFormatted} • ${exam.roomOrCenter}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        C1StatusChip(
                            text = "$daysLeft days",
                            tone = if (daysLeft <= 3) C1Tone.WARNING else C1Tone.INFO
                        )
                    }
                }
            }

            // Per-Subject Syllabus Breakdown & Checklists
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Per-Subject Syllabus Checklist", style = MaterialTheme.typography.titleLarge)
                    C1Button(
                        text = "+ Add Topic",
                        onClick = { showAddTopicDialog = true },
                        intent = C1ButtonIntent.TONAL
                    )
                }
            }

            val bySubject = uiState.syllabusTopics.groupBy { it.subjectName }
            bySubject.forEach { (subjectName, topics) ->
                val doneSubj = topics.count { it.isCompleted }
                val subjPct = if (topics.isNotEmpty()) (doneSubj * 100) / topics.size else 0

                item {
                    C1SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = subjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$doneSubj/${topics.size} ($subjPct%)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            LinearProgressIndicator(
                                progress = { subjPct / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                items(topics, key = { it.id }) { topic ->
                    C1SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = topic.isCompleted,
                                onCheckedChange = { viewModel.toggleSyllabusTopic(topic.id, it) }
                            )
                            Column {
                                Text(
                                    text = topic.unitTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(topic.topicTitle, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExamDialog) {
        var code by rememberSaveable { mutableStateOf("SOET-CS-304") }
        var name by rememberSaveable { mutableStateOf("Computer Networks") }
        var dateIso by rememberSaveable { mutableStateOf("23 Nov 2026") }
        var time by rememberSaveable { mutableStateOf("10:00 AM – 01:00 PM") }
        var venue by rememberSaveable { mutableStateOf("Exam Block Hall A") }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text(stringResource(R.string.publish_exam)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(value = code, onValueChange = { code = it }, label = "Subject Code")
                    C1TextField(value = name, onValueChange = { name = it }, label = "Subject Name")
                    C1TextField(value = dateIso, onValueChange = { dateIso = it }, label = "Exam Date")
                    C1TextField(value = time, onValueChange = { time = it }, label = "Time Slot")
                    C1TextField(value = venue, onValueChange = { venue = it }, label = "Exam Hall / Venue")
                }
            },
            confirmButton = {
                C1Button(
                    text = "Save Exam",
                    onClick = {
                        viewModel.addExam(code, name, "End-Semester Exam", dateIso, time, venue, 27)
                        showAddExamDialog = false
                    }
                )
            },
            dismissButton = {
                C1Button(
                    text = "Cancel",
                    onClick = { showAddExamDialog = false },
                    intent = C1ButtonIntent.GHOST
                )
            }
        )
    }

    if (showAddTopicDialog) {
        var subjId by rememberSaveable { mutableStateOf("CS301") }
        var subjName by rememberSaveable { mutableStateOf("Data Structures & Algorithms") }
        var unitTitle by rememberSaveable { mutableStateOf("Unit 5: Sorting & Hashing") }
        var topicTitle by rememberSaveable { mutableStateOf("QuickSort, MergeSort, RadixSort & Open Addressing") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text(stringResource(R.string.publish_syllabus_topic)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(value = subjName, onValueChange = { subjName = it }, label = "Subject Name")
                    C1TextField(value = unitTitle, onValueChange = { unitTitle = it }, label = "Unit Header")
                    C1TextField(value = topicTitle, onValueChange = { topicTitle = it }, label = "Syllabus Topics")
                }
            },
            confirmButton = {
                C1Button(
                    text = "Add Topic",
                    onClick = {
                        viewModel.addSyllabusTopic(subjId, subjName, 5, unitTitle, topicTitle)
                        showAddTopicDialog = false
                    }
                )
            },
            dismissButton = {
                C1Button(
                    text = "Cancel",
                    onClick = { showAddTopicDialog = false },
                    intent = C1ButtonIntent.GHOST
                )
            }
        )
    }
}

// =========================================================================
// PHASE 2.4 — GPA / CGPA / PERCENTAGE CALCULATOR
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GpaCalculatorScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var showAddCourseDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Summary & Scale Selector Card
        item {
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_calculator),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sem SGPA", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = uiState.gpaResult.formattedSgpa,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Cumulative CGPA", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = uiState.gpaResult.formattedCgpa,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text("Percentage", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = uiState.gpaResult.formattedPercentage,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Grading Scale:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradingScaleType.entries.forEach { scale ->
                            FilterChip(
                                selected = uiState.gpaScaleType == scale,
                                onClick = { viewModel.setGradingScale(scale) },
                                label = { Text(scale.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        C1Button(
                            text = "Save Sem History",
                            onClick = { viewModel.saveSemesterToHistory() },
                            modifier = Modifier.weight(1f)
                        )
                        C1Button(
                            text = "Export PDF",
                            onClick = { viewModel.exportGpaPdf(context) },
                            intent = C1ButtonIntent.OUTLINED,
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // 2. Semester Trend Chart Card
        item {
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.semester_trend_chart),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SemesterTrendChart(
                        trend = uiState.gpaResult.semesterTrend,
                        maxScale = uiState.gpaScaleType.maxScalePoint
                    )
                }
            }
        }

        // 3. "What Do I Need?" Target CGPA Calculator Card
        item {
            val req = uiState.targetCgpaRequirement
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.what_do_i_need),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target CGPA: ${String.format(Locale.US, "%.2f", uiState.targetCgpaInput)} (over next ${uiState.upcomingCreditsInput} credits)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Slider(
                        value = uiState.targetCgpaInput.toFloat(),
                        onValueChange = {
                            viewModel.updateTargetCgpa(it.toDouble(), uiState.upcomingCreditsInput)
                        },
                        valueRange = 6.0f..uiState.gpaScaleType.maxScalePoint.toFloat()
                    )
                    val message = when {
                        req.alreadyAchieved -> "You have already achieved your target CGPA of ${String.format(Locale.US, "%.2f", req.targetCgpa)}! 🎉"
                        req.isAchievable -> "You need an SGPA of ${String.format(Locale.US, "%.2f", req.requiredSgpa)} in your next ${req.upcomingCredits} credits to reach ${String.format(Locale.US, "%.2f", req.targetCgpa)} CGPA."
                        else -> "Requires ${String.format(Locale.US, "%.2f", req.requiredSgpa)} SGPA (exceeds max ${req.maxScalePoint}). Try targeting across 2 semesters."
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 4. Current Semester Subject Entry Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Semester Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                C1Button(
                    text = "+ Add Subject",
                    onClick = { showAddCourseDialog = true },
                    intent = C1ButtonIntent.TONAL
                )
            }
        }

        items(uiState.gpaCourses.size) { idx ->
            val course = uiState.gpaCourses[idx]
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${course.subjectCode} • ${course.credits} Credits",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = course.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        C1TextField(
                            value = course.gradePoint.toString(),
                            onValueChange = { viewModel.updateCourseGradePoint(idx, it.toDoubleOrNull() ?: course.gradePoint) },
                            label = "Grade",
                            modifier = Modifier.weight(1f)
                        )
                        C1TextField(
                            value = course.credits.toString(),
                            onValueChange = { viewModel.updateCourseCredits(idx, it.toIntOrNull() ?: course.credits) },
                            label = "Credits",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Points: ${String.format(Locale.US, "%.2f", course.gradePoint)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { viewModel.removeGpaCourse(idx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove subject")
                        }
                    }
                }
            }
        }

        if (uiState.gpaCourses.isEmpty()) {
            item {
                C1EmptyState(
                    icon = Icons.Default.Calculate,
                    title = "No subjects added",
                    message = "Add your current semester subjects to calculate SGPA/CGPA",
                    actionLabel = "+ Add Subject",
                    onAction = { showAddCourseDialog = true }
                )
            }
        }
    }

    if (showAddCourseDialog) {
        var subjCode by rememberSaveable { mutableStateOf("") }
        var subjName by rememberSaveable { mutableStateOf("") }
        var credits by rememberSaveable { mutableIntStateOf(4) }
        var grade by rememberSaveable { mutableStateOf("A") }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Add Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    C1TextField(value = subjCode, onValueChange = { subjCode = it }, label = "Subject Code")
                    C1TextField(value = subjName, onValueChange = { subjName = it }, label = "Subject Name")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        C1TextField(
                            value = credits.toString(),
                            onValueChange = { credits = it.toIntOrNull() ?: 4 },
                            label = "Credits",
                            modifier = Modifier.weight(1f)
                        )
                        C1TextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = "Grade",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                C1Button(
                    text = "Add",
                    onClick = {
                        viewModel.addGpaCourse(subjCode, subjName, credits, grade.toDoubleOrNull() ?: 0.0)
                        showAddCourseDialog = false
                    },
                    enabled = subjCode.isNotBlank() && subjName.isNotBlank()
                )
            },
            dismissButton = {
                C1Button(
                    text = "Cancel",
                    onClick = { showAddCourseDialog = false },
                    intent = C1ButtonIntent.GHOST
                )
            }
        )
    }
}

@Composable
private fun SemesterTrendChart(
    trend: List<SemesterGpaRecord>,
    maxScale: Double
) {
    if (trend.isEmpty()) {
        C1EmptyState(
            icon = Icons.Default.Calculate,
            title = "No semester history",
            message = "Save semester results to see your trend"
        )
        return
    }

    val maxSgpa = trend.maxOfOrNull { it.sgpa } ?: maxScale
    val minSgpa = trend.minOfOrNull { it.sgpa } ?: 0.0
    val range = (maxSgpa - minSgpa).coerceAtLeast(1.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, C1Radius.md)
    ) {
        // Draw chart using Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 20.dp.toPx()
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding

            val points: List<androidx.compose.ui.geometry.Offset> = trend.mapIndexed { index, record ->
                val x: Float = padding + (index.toFloat() / (trend.size - 1).coerceAtLeast(1)) * chartWidth
                val normalizedY = (record.sgpa - minSgpa) / range
                val y: Float = (padding + chartHeight - normalizedY * chartHeight).toFloat()
                androidx.compose.ui.geometry.Offset(x, y)
            }

            // Draw line
            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            drawPath(
                path,
                color = MaterialTheme.colorScheme.primary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = MaterialTheme.colorScheme.primary,
                    center = androidx.compose.ui.geometry.Offset(point.x, point.y),
                    radius = 6.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Privacy Policy & Open Source",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "CampusOne is built with privacy first. No ads, no trackers, no data sold. All data stays on your device unless you explicitly choose to sync. Licensed under MIT — free to use, modify, and distribute.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Source code: github.com/campusone/app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}