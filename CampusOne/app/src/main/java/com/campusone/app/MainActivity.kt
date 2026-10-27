package com.campusone.app

import com.campusone.app.R

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campusone.app.domain.usecase.MotionSpecEngine
import com.campusone.app.ui.components.FluidGlassBottomBar
import com.campusone.app.ui.components.FluidNavDestination
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.screens.AttendanceTrackerScreen
import com.campusone.app.ui.screens.FirstLaunchSetupScreen
import com.campusone.app.ui.screens.HomeDashboardScreen
import com.campusone.app.ui.screens.MoreHubScreen
import com.campusone.app.ui.screens.NoticesScreen
import com.campusone.app.ui.screens.TimetableScreen
import com.campusone.app.ui.theme.CampusOneTheme
import com.campusone.app.ui.viewmodel.CoreViewModel
import com.campusone.app.ui.viewmodel.StudyAndCampusViewModel

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CampusOneApp).container

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            val reducedMotion = rememberReducedMotionEnabled()

            val coreViewModel: CoreViewModel = viewModel(
                factory = CoreViewModel.provideFactory(appContainer)
            )
            val studyViewModel: StudyAndCampusViewModel = viewModel(
                factory = StudyAndCampusViewModel.provideFactory(appContainer)
            )

            val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()

            CampusOneTheme {
                val showSetup = !coreState.isLoading && !coreState.profile.setupCompleted
                AnimatedContent(
                    targetState = showSetup,
                    transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                    label = "root_setup_vs_shell"
                ) { isSetupScreen ->
                    if (isSetupScreen) {
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
    val reducedMotion = rememberReducedMotionEnabled()

    val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
    val studyState by studyViewModel.uiState.collectAsStateWithLifecycle()

    val unreadNoticesCount = coreState.personalNotices.count { !it.isRead }

    val destinations = listOf(
        FluidNavDestination("home", R.string.nav_home, Icons.Default.Home, 0),
        FluidNavDestination("attendance", R.string.nav_attendance, Icons.Default.CheckCircle, 0),
        FluidNavDestination("timetable", R.string.nav_timetable, Icons.Default.Schedule, 0),
        FluidNavDestination("notices", R.string.nav_notices, Icons.Default.Notifications, unreadNoticesCount),
        FluidNavDestination("more", R.string.nav_more, Icons.Default.Apps, 0)
    )

    val navigateTo: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                FluidGlassBottomBar(
                    destinations = destinations,
                    currentRoute = currentRoute,
                    onNavigate = navigateTo
                )
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
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val dir = MotionSpecEngine.computeTabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route
                    )
                    FluidMotion.tabEnterTransition(dir, reducedMotion)
                },
                exitTransition = {
                    val dir = MotionSpecEngine.computeTabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route
                    )
                    FluidMotion.tabExitTransition(dir, reducedMotion)
                },
                popEnterTransition = {
                    val dir = MotionSpecEngine.computeTabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route
                    )
                    FluidMotion.tabEnterTransition(dir, reducedMotion)
                },
                popExitTransition = {
                    val dir = MotionSpecEngine.computeTabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route
                    )
                    FluidMotion.tabExitTransition(dir, reducedMotion)
                }
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
                composable("notices") {
                    NoticesScreen(
                        coreState = coreState,
                        coreViewModel = coreViewModel
                    )
                }
                composable("more") {
                    MoreHubScreen(
                        initialSection = 0,
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
