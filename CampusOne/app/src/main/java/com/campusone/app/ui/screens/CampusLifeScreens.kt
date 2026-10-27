package com.campusone.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout. padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel
import com.campusone.app.ui.viewmodel.StudyAndCampusUiState
import com.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import com.campusone.app.util.CampusIntentHelper

/**
 * Unified "More" Hub (Study + Campus Life + Backup/Settings) with smooth directional transitions.
 */
@Composable
fun MoreHubScreen(
    initialSection: Int = 0, // 0 = Study Hub, 1 = Campus Life Hub
    coreState: CoreUiState,
    studyState: StudyAndCampusUiState,
    coreViewModel: CoreViewModel,
    studyViewModel: StudyAndCampusViewModel
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    var activeSection by rememberSaveable { mutableIntStateOf(initialSection) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = activeSection == 0,
                onClick = {
                    if (activeSection != 0) {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        activeSection = 0
                    }
                },
                label = { Text(stringResource(R.string.nav_study)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = activeSection == 1,
                onClick = {
                    if (activeSection != 1) {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        activeSection = 1
                    }
                },
                label = { Text(stringResource(R.string.nav_campus)) },
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedContent(
            targetState = activeSection,
            transitionSpec = {
                val dir = if (targetState >= initialState) 1 else -1
                FluidMotion.directionalContentTransform(dir, reducedMotion)
            },
            label = "more_hub_section_switch"
        ) { section ->
            if (section == 0) {
                StudyHubScreen(state = studyState, viewModel = studyViewModel)
            } else {
                CampusHubScreen(
                    coreState = coreState,
                    studyState = studyState,
                    coreViewModel = coreViewModel,
                    studyViewModel = studyViewModel
                )
            }
        }
    }
}

@Composable
fun CampusHubScreen(
    coreState: CoreUiState,
    studyState: StudyAndCampusUiState,
    coreViewModel: CoreViewModel,
    studyViewModel: StudyAndCampusViewModel
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.campus_tab_directory),
        stringResource(R.string.campus_tab_events),
        stringResource(R.string.campus_tab_lost_found),
        stringResource(R.string.campus_tab_settings)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        if (selectedTab != index) {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedTab = index
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        AnimatedVisibility(
            visible = studyState.statusBannerMessage != null,
            enter = FluidMotion.expandCollapseEnter(reducedMotion),
            exit = FluidMotion.expandCollapseExit(reducedMotion)
        ) {
            InteractiveGlassCard(
                isHighlighted = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(studyState.statusBannerMessage ?: "", modifier = Modifier.weight(1f))
                    TextButton(onClick = { studyViewModel.clearBanner() }) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val dir = if (targetState >= initialState) 1 else -1
                FluidMotion.directionalContentTransform(dir, reducedMotion)
            },
            label = "campus_hub_subtab"
        ) { tabIdx ->
            when (tabIdx) {
                0 -> CampusDirectoryScreen(contacts = studyState.directoryContacts)
                1 -> EventsAndClubsScreen()
                2 -> LostAndFoundScreen()
                else -> BackupAndSettingsSubScreen(studyState, coreViewModel, studyViewModel)
            }
        }
    }
}

@Composable
private fun BackupAndSettingsSubScreen(
    studyState: StudyAndCampusUiState,
    coreViewModel: CoreViewModel,
    studyViewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        studyViewModel.refreshCrashLog(context)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            studyViewModel.exportBackupToUri(uri)
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            studyViewModel.importBackupFromUri(uri)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { coreViewModel.toggleLanguage() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_language_toggle))
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_notifications_btn))
                        }
                    }
                }
            }
        }

        item {
            InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                createBackupLauncher.launch("CampusOne_Backup_${System.currentTimeMillis()}.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_export_json))
                        }
                        OutlinedButton(
                            onClick = {
                                openBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_import_json))
                        }
                    }
                }
            }
        }

        item {
            InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_crash_log_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    val crashText = studyState.lastCrashLog
                    if (crashText.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.settings_no_crashes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = crashText.take(1500),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    CampusIntentHelper.copyToClipboard(context, "CampusOne Crash Log", crashText)
                                }
                            ) {
                                Text(stringResource(R.string.settings_copy_crash))
                            }
                            OutlinedButton(
                                onClick = { studyViewModel.clearCrashLog(context) }
                            ) {
                                Text(stringResource(R.string.settings_clear_crash))
                            }
                        }
                    }
                }
            }
        }
    }
}
