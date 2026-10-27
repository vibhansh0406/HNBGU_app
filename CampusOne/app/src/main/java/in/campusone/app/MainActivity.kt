package `in`.campusone.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.campusone.app.R
import `in`.campusone.app.ui.screens.AttendanceTrackerScreen
import `in`.campusone.app.ui.screens.CampusHubScreen
import `in`.campusone.app.ui.screens.FirstLaunchSetupScreen
import `in`.campusone.app.ui.screens.HomeDashboardScreen
import `in`.campusone.app.ui.screens.StudyHubScreen
import `in`.campusone.app.ui.screens.TimetableScreen
import `in`.campusone.app.ui.theme.CampusOneTheme
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CampusOneApp).container

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            val coreViewModel: CoreViewModel = viewModel(
                factory = CoreViewModel.provideFactory(appContainer)
            )
            val studyViewModel: StudyAndCampusViewModel = viewModel(
                factory = StudyAndCampusViewModel.provideFactory(appContainer)
            )

            val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
            val studyState by studyViewModel.uiState.collectAsStateWithLifecycle()

            CampusOneTheme {
                if (!coreState.isLoading && !coreState.profile.setupCompleted) {
                    FirstLaunchSetupScreen(
                        onCompleteSetup = { name, college, course, sem, section, target ->
                            coreViewModel.completeSetup(name, college, course, sem, section, target)
                        }
                    )
                } else {
                    CampusOneAdaptiveShell(
                        useNavRail = useNavRail,
                        coreViewModel = coreViewModel,
                        studyViewModel = studyViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun CampusOneAdaptiveShell(
    useNavRail: Boolean,
    coreViewModel: CoreViewModel,
    studyViewModel: StudyAndCampusViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
    val studyState by studyViewModel.uiState.collectAsStateWithLifecycle()

    val destinations = listOf(
        TopLevelDestination("home", R.string.nav_home, Icons.Default.Home),
        TopLevelDestination("attendance", R.string.nav_attendance, Icons.Default.CheckCircle),
        TopLevelDestination("timetable", R.string.nav_timetable, Icons.Default.Schedule),
        TopLevelDestination("study", R.string.nav_study, Icons.Default.MenuBook),
        TopLevelDestination("campus", R.string.nav_campus, Icons.Default.AccountBalance)
    )

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                NavigationBar {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigateTo(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavRail) {
                NavigationRail {
                    destinations.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigateTo(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("home") {
                    HomeDashboardScreen(
                        coreState = coreState,
                        upcomingAssignments = studyState.assignments,
                        upcomingExams = studyState.exams,
                        onNavigateToTab = navigateTo
                    )
                }
                composable("attendance") {
                    AttendanceTrackerScreen(
                        coreState = coreState,
                        viewModel = coreViewModel
                    )
                }
                composable("timetable") {
                    TimetableScreen(
                        coreState = coreState,
                        viewModel = coreViewModel
                    )
                }
                composable("study") {
                    StudyHubScreen(
                        state = studyState,
                        viewModel = studyViewModel
                    )
                }
                composable("campus") {
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
}
