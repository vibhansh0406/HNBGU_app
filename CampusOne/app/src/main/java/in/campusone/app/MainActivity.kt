package `in`.campusone.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import `in`.campusone.app.ui.screens.AttendanceScreen
import `in`.campusone.app.ui.screens.HomeScreen
import `in`.campusone.app.ui.screens.MoreHubScreen
import `in`.campusone.app.ui.screens.NoticeBoardScreen
import `in`.campusone.app.ui.screens.OnboardingScreen
import `in`.campusone.app.ui.screens.TimetableScreen
import `in`.campusone.app.ui.theme.CampusOneTheme
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel

enum class BottomNavTab {
    HOME, ATTENDANCE, TIMETABLE, NOTICES, MORE
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusOneTheme {
                CampusOneRoot()
            }
        }
    }
}

@Composable
fun CampusOneRoot(
    coreViewModel: CoreViewModel = hiltViewModel(),
    studyCampusViewModel: StudyAndCampusViewModel = hiltViewModel()
) {
    val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
    val studyCampusState by studyCampusViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(BottomNavTab.HOME) }

    LaunchedEffect(Unit) {
        coreViewModel.uiMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        studyCampusViewModel.statusMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (!coreState.isOnboarded) {
        OnboardingScreen(
            onToggleLanguage = coreViewModel::toggleLanguage,
            onCompleteSetup = coreViewModel::completeOnboarding
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.HOME,
                    onClick = { currentTab = BottomNavTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.ATTENDANCE,
                    onClick = { currentTab = BottomNavTab.ATTENDANCE },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_attendance)) }
                )
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.TIMETABLE,
                    onClick = { currentTab = BottomNavTab.TIMETABLE },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_timetable)) }
                )
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.NOTICES,
                    onClick = { currentTab = BottomNavTab.NOTICES },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (coreState.unreadNoticeCount > 0) {
                                    Badge { Text(coreState.unreadNoticeCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(R.string.nav_notices)) }
                )
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.MORE,
                    onClick = { currentTab = BottomNavTab.MORE },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_more)) }
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                BottomNavTab.HOME -> HomeScreen(
                    uiState = coreState,
                    nextThreeDeadlines = studyCampusState.nextThreeDeadlines,
                    nextUpcomingExam = studyCampusState.nextUpcomingExam,
                    onToggleLanguage = coreViewModel::toggleLanguage,
                    onOpenAttendance = { currentTab = BottomNavTab.ATTENDANCE },
                    onOpenTimetable = { currentTab = BottomNavTab.TIMETABLE },
                    onOpenNotices = { currentTab = BottomNavTab.NOTICES },
                    onOpenStudyHub = { currentTab = BottomNavTab.MORE }
                )
                BottomNavTab.ATTENDANCE -> AttendanceScreen(
                    uiState = coreState,
                    onMarkAttendance = coreViewModel::markAttendance,
                    onTargetPctChange = coreViewModel::updateTargetPercentage
                )
                BottomNavTab.TIMETABLE -> TimetableScreen(
                    uiState = coreState,
                    onUpdateReminderMinutes = coreViewModel::updateReminderLeadMinutes,
                    onPublishSlot = coreViewModel::publishTimetableSlot
                )
                BottomNavTab.NOTICES -> NoticeBoardScreen(
                    uiState = coreState,
                    onMarkNoticeRead = coreViewModel::markNoticeRead,
                    onMarkAllRead = coreViewModel::markAllNoticesRead,
                    onPostNotice = coreViewModel::postNotice
                )
                BottomNavTab.MORE -> MoreHubScreen(
                    uiState = studyCampusState,
                    viewModel = studyCampusViewModel
                )
            }
        }
    }
}
