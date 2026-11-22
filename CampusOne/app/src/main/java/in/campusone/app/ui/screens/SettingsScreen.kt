package `in`.campusone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.campusone.app.BuildConfig
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.languageTag.collectAsStateWithLifecycle()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(insets)
                .padding(horizontal = C1Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.lg)
        ) {
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name.ifBlank { "CampusOne student" }, style = MaterialTheme.typography.titleLarge)
                            Text(profile.email.ifBlank { "Account details unavailable" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(profile.role.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            item { C1SectionHeader(title = "Appearance") }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg)) {
                    SettingsChoiceRow(
                        title = "Theme",
                        subtitle = "Choose how CampusOne looks on this device",
                        options = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark"),
                        selected = theme,
                        onSelect = viewModel::setThemeMode
                    )
                }
            }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.lg)) {
                    SettingsChoiceRow(
                        title = "Language",
                        subtitle = "App language",
                        options = listOf("en" to "English", "hi" to "हिन्दी"),
                        selected = language,
                        onSelect = viewModel::setLanguage
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                    C1SectionHeader(title = "Notifications")
                    Text("Control the updates CampusOne sends to this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                    SettingsSwitchRow("Assignment updates", "New assignments and deadlines", Icons.Default.Assignment, assignmentUpdates, notifications) {
                        viewModel.setNotificationCategory("assignment", it)
                    }
                    SettingsSwitchRow("Study material", "New notes and resource uploads", Icons.Default.Info, studyUpdates, notifications) {
                        viewModel.setNotificationCategory("study_material", it)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                    C1SectionHeader(title = "Academic reminders")
                    Text("Local reminders generated on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsSwitchRow("Upcoming classes", "Remind me before a scheduled class", Icons.Default.Schedule, classReminders, notifications) {
                        viewModel.setLocalReminder("class", it)
                    }
                    SettingsSwitchRow("Assignment deadlines", "Reminders for pending work", Icons.Default.Assignment, assignmentReminders, notifications) {
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
            item { C1SectionHeader(title = "Privacy & support") }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsActionRow("Privacy policy", "How CampusOne handles your information", Icons.Default.PrivacyTip, onOpenPrivacy)
                }
            }
            item { C1SectionHeader(title = "Account") }
            item {
                C1SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(C1Spacing.md)) {
                    SettingsActionRow("Sign out", "Sign out of this device", Icons.Default.Logout, { showSignOut = true }, danger = true)
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = C1Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(C1Spacing.xs))
                    Text("CampusOne", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Made for your campus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.height(C1Spacing.xl)) }
        }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = C1Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
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
    androidx.compose.foundation.clickable
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).then(
            Modifier.padding(horizontal = C1Spacing.xs)
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) { Text("Open") }
    }
}
