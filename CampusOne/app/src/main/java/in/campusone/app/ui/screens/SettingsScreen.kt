package `in`.campusone.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.campusone.app.BuildConfig
import `in`.campusone.app.domain.model.HnbguAcademicCatalog
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val language by viewModel.languageTag.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val announcements by viewModel.announcementsEnabled.collectAsStateWithLifecycle()
    val notices by viewModel.noticesEnabled.collectAsStateWithLifecycle()
    val examUpdates by viewModel.examUpdatesEnabled.collectAsStateWithLifecycle()
    val assignmentUpdates by viewModel.assignmentUpdatesEnabled.collectAsStateWithLifecycle()
    val studyUpdates by viewModel.studyMaterialUpdatesEnabled.collectAsStateWithLifecycle()
    val classReminders by viewModel.classRemindersEnabled.collectAsStateWithLifecycle()
    val assignmentReminders by viewModel.assignmentRemindersEnabled.collectAsStateWithLifecycle()
    val examReminders by viewModel.examRemindersEnabled.collectAsStateWithLifecycle()
    val eventReminders by viewModel.eventRemindersEnabled.collectAsStateWithLifecycle()
    var showSignOut by rememberSaveable { mutableStateOf(false) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    var editedName by rememberSaveable { mutableStateOf(profile.name) }
    var editedCampus by rememberSaveable { mutableStateOf(profile.collegeName) }
    var editedCourseId by rememberSaveable { mutableStateOf(profile.courseId) }
    var editedCourseName by rememberSaveable { mutableStateOf(profile.courseName) }
    var editedSemester by rememberSaveable { mutableStateOf(profile.semester.toString()) }
    var editedSection by rememberSaveable { mutableStateOf(profile.section) }
    val context = LocalContext.current
    var systemNotificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = C1Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.lg)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                    C1SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(44.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile.name.ifBlank { "CampusOne student" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    profile.email.ifBlank { "Account details unavailable" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                                )
                                Text(
                                    profile.role.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                editedName = profile.name
                                editedCampus = profile.collegeName
                                editedCourseId = profile.courseId
                                editedCourseName = profile.courseName
                                editedSemester = profile.semester.toString()
                                editedSection = profile.section
                                showEditProfile = true
                            }
                        ) {
                            Text("Edit academic profile", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            item {
                C1SectionHeader(title = "Preferences", subtitle = "Language and appearance")
            }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg)) {
                    SettingsChoiceRow(
                        title = "Language",
                        subtitle = "App language",
                        options = listOf("en" to "English", "hi" to "हिन्दी"),
                        selected = language,
                        onSelect = { viewModel.setLanguage(it) }
                    )
                }
            }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg)) {
                    SettingsChoiceRow(
                        title = "Appearance",
                        subtitle = "Light, dark or follow your device",
                        options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                        selected = themeMode,
                        onSelect = { viewModel.setThemeMode(it) }
                    )
                }
            }
            item {
                C1SectionHeader(title = "Notifications", subtitle = "Choose which updates reach this device")
            }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsSwitchRow("All notifications", "Master switch for CampusOne notifications", Icons.Default.Notifications, notifications) {
                        viewModel.setNotificationsEnabled(it)
                    }
                    SettingsSwitchRow("College announcements", "University-wide updates", Icons.Default.Campaign, announcements, notifications) {
                        viewModel.setNotificationCategory("announcement", it)
                    }
                    SettingsSwitchRow("Section notices", "Updates from your class and faculty", Icons.Default.School, notices, notifications) {
                        viewModel.setNotificationCategory("notice", it)
                    }
                    SettingsSwitchRow("Examination updates", "Exam schedules and changes", Icons.Default.Shield, examUpdates, notifications) {
                        viewModel.setNotificationCategory("exam", it)
                    }
                    SettingsSwitchRow("Assignment updates", "New assignments and deadlines", Icons.AutoMirrored.Filled.Assignment, assignmentUpdates, notifications) {
                        viewModel.setNotificationCategory("assignment", it)
                    }
                    SettingsSwitchRow("Study material", "New notes and resource uploads", Icons.AutoMirrored.Filled.MenuBook, studyUpdates, notifications) {
                        viewModel.setNotificationCategory("study_material", it)
                    }
                    SettingsActionRow(
                        title = if (systemNotificationsEnabled) "Pop-up notifications are enabled" else "Enable pop-up notifications",
                        subtitle = if (systemNotificationsEnabled) {
                            "Android allows CampusOne alerts. Set each channel to Alerting for heads-up banners."
                        } else {
                            "Android is blocking CampusOne alerts on this device. Open system settings to allow them."
                        },
                        icon = Icons.Default.Notifications,
                        onClick = {
                            val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        danger = !systemNotificationsEnabled
                    )
                }
            }
            item {
                C1SectionHeader(title = "Academic reminders", subtitle = "Helpful reminders generated on your device")
            }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsSwitchRow("Upcoming classes", "Remind me before a scheduled class", Icons.Default.Schedule, classReminders, notifications) {
                        viewModel.setLocalReminder("class", it)
                    }
                    SettingsSwitchRow("Assignment deadlines", "Reminders for pending work", Icons.AutoMirrored.Filled.Assignment, assignmentReminders, notifications) {
                        viewModel.setLocalReminder("assignment", it)
                    }
                    SettingsSwitchRow("Exams", "Reminders before examinations", Icons.Default.School, examReminders, notifications) {
                        viewModel.setLocalReminder("exam", it)
                    }
                    SettingsSwitchRow("Campus events", "Reminders for events you joined", Icons.Default.Event, eventReminders, notifications) {
                        viewModel.setLocalReminder("event", it)
                    }
                }
            }
            item { C1SectionHeader(title = "App updates", subtitle = "Get the verified Android installer from the official release page") }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsActionRow(
                        title = "Download latest CampusOne version",
                        subtitle = "Open the official GitHub Releases page. Install only the signed APK asset.",
                        icon = Icons.Default.Download,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/vibhansh0406/HNBGU_app/releases/latest")
                            )
                            context.startActivity(Intent.createChooser(intent, "Open CampusOne releases"))
                        }
                    )
                }
            }
            item { C1SectionHeader(title = "Privacy & support", subtitle = "Get help and understand your data") }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsActionRow(
                        title = "Send feedback",
                        subtitle = "Share feedback using an app on your device",
                        icon = Icons.Default.Feedback,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "CampusOne feedback")
                                putExtra(Intent.EXTRA_TEXT, "CampusOne " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\\n\\nFeedback:\\n")
                            }
                            context.startActivity(Intent.createChooser(intent, "Send feedback"))
                        }
                    )
                    SettingsActionRow("Privacy policy", "How CampusOne handles your information", Icons.Default.PrivacyTip, onOpenPrivacy)
                }
            }
            item { C1SectionHeader(title = "Account", subtitle = "Manage access to this device") }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsActionRow("Sign out", "Sign out of this device", Icons.AutoMirrored.Filled.Logout, { showSignOut = true }, danger = true)
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = C1Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(`in`.campusone.app.R.drawable.ic_campusone_brand_lockup),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(C1Spacing.xs))
                    Text("CampusOne", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Made for your campus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.height(C1Spacing.xl)) }
        }

    if (showEditProfile) {
        val courseOptions = (HnbguAcademicCatalog.courses + (profile.courseId to profile.courseName))
            .distinctBy { it.first }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit academic profile") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
                ) {
                    Text(
                        "Your changes are saved to your HNBGU account and restored on your next install.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    C1TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = "Full name",
                        modifier = Modifier.fillMaxWidth()
                    )
                    ProfileDropdownField(
                        label = "Campus",
                        selectedValue = editedCampus,
                        options = HnbguAcademicCatalog.campuses.map { it to it },
                        onSelected = { editedCampus = it }
                    )
                    ProfileDropdownField(
                        label = "Course",
                        selectedValue = editedCourseId,
                        options = courseOptions,
                        onSelected = { id ->
                            editedCourseId = id
                            editedCourseName = courseOptions.firstOrNull { it.first == id }?.second.orEmpty()
                        }
                    )
                    ProfileDropdownField(
                        label = "Semester",
                        selectedValue = editedSemester,
                        options = (1..8).map { it.toString() to "Semester $it" },
                        onSelected = { editedSemester = it }
                    )
                    ProfileDropdownField(
                        label = "Section",
                        selectedValue = editedSection,
                        options = listOf("A" to "Section A", "B" to "Section B", "C" to "Section C"),
                        onSelected = { editedSection = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editedName.isNotBlank() && editedCourseId.isNotBlank(),
                    onClick = {
                        onSaveProfile(
                            profile.copy(
                                name = editedName.trim(),
                                collegeName = editedCampus,
                                courseId = editedCourseId,
                                courseName = editedCourseName,
                                semester = editedSemester.toIntOrNull() ?: profile.semester,
                                section = editedSection
                            )
                        )
                        showEditProfile = false
                    }
                ) { Text("Save changes") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You can sign in again at any time. Your campus data remains linked to your account.") },
            confirmButton = {
                TextButton(onClick = { showSignOut = false; onSignOut() }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOut = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileDropdownField(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue
    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(selectedLabel, maxLines = 2)
                    Text("Change")
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
            options.forEach { (value, label) ->
                FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = C1Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = C1Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
