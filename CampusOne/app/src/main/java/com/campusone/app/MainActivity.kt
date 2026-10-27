package com.campusone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.usecase.AuthorizationDecision
import com.campusone.app.domain.usecase.MotionSpecEngine
import com.campusone.app.domain.usecase.RoleAuthorizationEngine
import com.campusone.app.ui.components.FluidGlassBottomBar
import com.campusone.app.ui.components.FluidNavDestination
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.screens.AccessDeniedScreen
import com.campusone.app.ui.screens.AdminRoleConsoleScreen
import com.campusone.app.ui.screens.AttendanceTrackerScreen
import com.campusone.app.ui.screens.FirstLaunchSetupScreen
import com.campusone.app.ui.screens.GoogleLoginSelectionScreen
import com.campusone.app.ui.screens.HomeDashboardScreen
import com.campusone.app.ui.screens.MoreHubScreen
import com.campusone.app.ui.screens.NoticesScreen
import com.campusone.app.ui.screens.TimetableScreen
import com.campusone.app.ui.theme.CampusOneTheme
import com.campusone.app.ui.viewmodel.CoreViewModel
import com.campusone.app.ui.viewmodel.StudyAndCampusViewModel

class MainActivity : AppCompatActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)
    private var pendingSubjectId by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractDeepLinkIntent(intent)

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
            var showOfflineSetupWizard by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(pendingSubjectId) {
                if (!pendingSubjectId.isNullOrBlank()) {
                    coreViewModel.setHighlightedSubject(pendingSubjectId)
                    pendingSubjectId = null
                }
            }

            CampusOneTheme {
                val rootScreenKey = when {
                    coreState.isLoading -> "loading"
                    coreState.remoteProfile != null -> {
                        when (
                            RoleAuthorizationEngine.evaluateLoginGate(
                                intentPath = coreState.loginIntentPath,
                                profile = coreState.remoteProfile
                            )
                        ) {
                            is AuthorizationDecision.AccessDenied -> "access_denied"
                            is AuthorizationDecision.AuthorizedAdmin -> "admin_console"
                            is AuthorizationDecision.AuthorizedStudent -> "student_shell"
                        }
                    }
                    showOfflineSetupWizard -> "offline_setup"
                    !coreState.profile.setupCompleted -> "login_selection"
                    else -> "student_shell"
                }

                AnimatedContent(
                    targetState = rootScreenKey,
                    transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                    label = "root_auth_gate"
                ) { screenKey ->
                    when (screenKey) {
                        "login_selection" -> {
                            GoogleLoginSelectionScreen(
                                runtimeConfig = coreState.runtimeConfig,
                                isAuthenticating = coreState.isAuthenticating,
                                authErrorMessage = coreState.authErrorMessage,
                                onSelectGoogleLogin = { path ->
                                    coreViewModel.signInWithGoogle(this@MainActivity, path)
                                },
                                onSaveRuntimeConfig = { url, key, googleId ->
                                    coreViewModel.saveRuntimeSupabaseConfig(url, key, googleId)
                                },
                                onContinueOfflineSetup = {
                                    showOfflineSetupWizard = true
                                }
                            )
                        }
                        "offline_setup" -> {
                            FirstLaunchSetupScreen(
                                onCompleteSetup = { name, college, course, sem, section, target ->
                                    showOfflineSetupWizard = false
                                    coreViewModel.completeSetup(name, college, course, sem, section, target)
                                }
                            )
                        }
                        "access_denied" -> {
                            val decision = RoleAuthorizationEngine.evaluateLoginGate(
                                intentPath = coreState.loginIntentPath,
                                profile = coreState.remoteProfile
                            ) as? AuthorizationDecision.AccessDenied
                            AccessDeniedScreen(
                                title = decision?.title ?: "Access Denied",
                                reason = decision?.reason ?: "Your account is not authorized for this portal.",
                                email = coreState.remoteProfile?.email.orEmpty(),
                                roleLabel = coreState.remoteProfile?.role ?: "student",
                                canSwitchToStudentView = decision?.canFallbackToStudentDashboard == true,
                                onSwitchToStudentView = {
                                    coreViewModel.switchLoginIntentPath(LoginIntentPath.STUDENT)
                                },
                                onSignOut = {
                                    coreViewModel.signOutOfSupabase(this@MainActivity)
                                }
                            )
                        }
                        "admin_console" -> {
                            AdminRoleConsoleScreen(
                                coreState = coreState,
                                viewModel = coreViewModel
                            )
                        }
                        else -> {
                            CampusOneAdaptiveShell(
                                useNavRail = useNavRail,
                                coreViewModel = coreViewModel,
                                studyViewModel = studyViewModel,
                                requestedRoute = pendingRoute,
                                onRouteConsumed = { pendingRoute = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinkIntent(intent)
    }

    private fun extractDeepLinkIntent(intent: Intent?) {
        val route = intent?.getStringExtra("open_route")
        val subjectId = intent?.getStringExtra("open_subject_id")
        if (!route.isNullOrBlank()) {
            pendingRoute = route
        }
        if (!subjectId.isNullOrBlank()) {
            pendingSubjectId = subjectId
        }
    }
}

@Composable
private fun CampusOneAdaptiveShell(
    useNavRail: Boolean,
    coreViewModel: CoreViewModel,
    studyViewModel: StudyAndCampusViewModel,
    requestedRoute: String?,
    onRouteConsumed: () -> Unit
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
        FluidNavDestination("timetable", R.string.nav_timetable, Icons.Default.DateRange, 0),
        FluidNavDestination("notices", R.string.nav_notices, Icons.Default.Notifications, unreadNoticesCount),
        FluidNavDestination("more", R.string.nav_more, Icons.Default.Menu, 0)
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

    LaunchedEffect(requestedRoute) {
        if (!requestedRoute.isNullOrBlank()) {
            navigateTo(requestedRoute)
            onRouteConsumed()
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
