package `in`.campusone.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.remote.SupabaseSession
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1NavItem
import `in`.campusone.app.ui.screens.AttendanceScreen
import `in`.campusone.app.ui.screens.FlagshipAttendanceScreen
import `in`.campusone.app.ui.screens.HomeScreen
import `in`.campusone.app.ui.screens.MoreHubScreen
import `in`.campusone.app.ui.screens.MoreSubScreen
import `in`.campusone.app.ui.screens.NoticeBoardScreen
import `in`.campusone.app.ui.screens.OnboardingScreen
import `in`.campusone.app.ui.screens.TimetableScreen
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Motion
import `in`.campusone.app.ui.theme.CampusOneDisplayFamily
import `in`.campusone.app.ui.theme.CampusOneTheme
import `in`.campusone.app.ui.viewmodel.CommunityViewModel
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class BottomNavTab {
    HOME, ATTENDANCE, TIMETABLE, NOTICES, MORE
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var supabaseSync: SupabaseSyncRepository

    /** Notification target delivered while the app is running (warm start via onNewIntent). */
    private var pendingNotificationTarget by mutableStateOf<Pair<String, String>?>(null)

    private val googleSignInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let { data ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    task.result?.let { account ->
                        val idToken = account.idToken
                        if (idToken != null) {
                            lifecycleScope.launch {
                                val result = authManager.signInWithGoogle(idToken)
                                if (result.isSuccess) {
                                    val session = result.getOrNull()
                                    session?.user?.id?.let { uid ->
                                        val profileResult = authManager.ensureUserProfile(
                                            uid = uid,
                                            email = account.email,
                                            name = account.displayName
                                            // Role is assigned server-side by campusone_bootstrap_profile()
                                            // (SECURITY DEFINER, reads the authenticated JWT email). The client
                                            // never decides ADMIN.
                                        )
                                        if (!profileResult.isSuccess) {
                                            android.widget.Toast.makeText(
                                                this@MainActivity,
                                                "Profile save failed: ${profileResult.exceptionOrNull()?.message ?: ""}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        // Register FCM token for push notifications
                                        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                            lifecycleScope.launch {
                                                supabaseSync.registerDeviceToken(
                                                    uid = uid,
                                                    fcmToken = fcmToken,
                                                    devicePlatform = "android",
                                                    deviceName = android.os.Build.MODEL,
                                                    email = account.email ?: ""
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Sign-in failed: ${result.exceptionOrNull()?.message ?: "unknown error"}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                } else {
                    val err = task.exception
                    val code = (err as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Google sign-in failed: code $code ${err?.message ?: ""}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager.initGoogleSignIn()

        // Re-register the current device on every restored/renewed Supabase session.
        // This repairs missed token registration after first launch or transient network failure.
        lifecycleScope.launch {
            authManager.session.collectLatest { session ->
                val uid = session?.user?.id ?: return@collectLatest
                runCatching {
                    // Wait until the Supabase session is usable before calling the
                    // JWT-protected registration endpoint.
                    authManager.ensureFreshToken()
                    val fcmToken = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                    }
                    val registered = supabaseSync.registerDeviceToken(
                        uid = uid,
                        fcmToken = fcmToken,
                        devicePlatform = "android",
                        deviceName = android.os.Build.MODEL,
                        email = authManager.currentUserEmail() ?: ""
                    )
                    android.util.Log.i(
                        "CampusOneFCM",
                        "Device token registration result: $registered"
                    )
                }.onFailure { error ->
                    android.util.Log.e("CampusOneFCM", "Device token registration failed", error)
                }
            }
        }
        val onGoogleSignIn = {
            googleSignInLauncher.launch(authManager.getSignInIntent())
        }
        // Handle notification tap intent from onCreate (cold start)
        pendingNotificationTarget = handleNotificationIntent(intent)
        setContent {
            NotificationPermissionRequest()
            val config = androidx.compose.ui.platform.LocalConfiguration.current
            val ws = when {
                config.screenWidthDp < 600 -> `in`.campusone.app.ui.theme.C1WindowSize.COMPACT
                config.screenWidthDp < 840 -> `in`.campusone.app.ui.theme.C1WindowSize.MEDIUM
                config.screenWidthDp < 1200 -> `in`.campusone.app.ui.theme.C1WindowSize.LARGE
                else -> `in`.campusone.app.ui.theme.C1WindowSize.EXPANDED
            }
            CampusOneTheme(windowSize = ws) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                } else {
                    CampusOneRoot(
                        onGoogleSignIn = onGoogleSignIn,
                        authSession = authManager.session,
                        onSignOut = {
                            lifecycleScope.launch {
                                // Deactivate FCM token on sign-out
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                    lifecycleScope.launch {
                                        supabaseSync.deactivateDeviceToken(fcmToken)
                                    }
                                }
                                authManager.signOut()
                            }
                        },
                        pendingNotificationTarget = pendingNotificationTarget
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = handleNotificationIntent(intent)
        if (target != null) {
            pendingNotificationTarget = target
        }
    }

    /** Extracts notification target from an intent. Returns a Pair of (targetType, targetId) or null. */
    private fun handleNotificationIntent(intent: Intent): Pair<String, String>? {
        if (intent.action == "in.campusone.app.NOTIFICATION_CLICK") {
            val targetType = intent.getStringExtra("target_type") ?: ""
            val targetId = intent.getStringExtra("target_id") ?: ""
            if (targetType.isNotBlank() && targetId.isNotBlank()) {
                return targetType to targetId
            }
        }
        return null
    }
}

@Composable
private fun NotificationPermissionRequest() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun CampusOneRoot(
    onGoogleSignIn: () -> Unit,
    authSession: StateFlow<SupabaseSession?>,
    onSignOut: () -> Unit,
    coreViewModel: CoreViewModel = hiltViewModel(),
    studyCampusViewModel: StudyAndCampusViewModel = hiltViewModel(),
    communityViewModel: CommunityViewModel = hiltViewModel(),
    pendingNotificationTarget: Pair<String, String>? = null
) {
    val authSessionState by authSession.collectAsStateWithLifecycle()
    val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
    val studyCampusState by studyCampusViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val stateHolder = rememberSaveableStateHolder()
    var currentTab by rememberSaveable { mutableStateOf(BottomNavTab.HOME) }
    var pendingMoreScreen by rememberSaveable { mutableStateOf<MoreSubScreen?>(null) }
    var pendingNoticeId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCommunityPostId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAssignmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastNotificationNavAt by remember { mutableLongStateOf(0L) }
    var isSigningIn by remember { mutableStateOf(false) }
    val noticesSyncError by coreViewModel.noticesSyncError.collectAsStateWithLifecycle()
    val unreadNotifications by communityViewModel.unreadNotificationCount.collectAsStateWithLifecycle()

    // Handle FCM notification tap navigation
    val pendingNotification = remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(pendingNotificationTarget) {
        pendingNotification.value = pendingNotificationTarget
    }
    LaunchedEffect(pendingNotification.value) {
        val target = pendingNotification.value
        if (target != null && System.currentTimeMillis() - lastNotificationNavAt > 400) {
            val (targetType, targetId) = target
            lastNotificationNavAt = System.currentTimeMillis()
            when (targetType) {
                "NOTICE" -> {
                    pendingNoticeId = targetId
                    currentTab = BottomNavTab.NOTICES
                }
                "ASSIGNMENT" -> {
                    pendingAssignmentId = targetId
                    pendingMoreScreen = MoreSubScreen.ASSIGNMENTS
                    currentTab = BottomNavTab.MORE
                }
                "EXAM" -> {
                    pendingMoreScreen = MoreSubScreen.EXAMS_SYLLABUS
                    currentTab = BottomNavTab.MORE
                }
                "STUDY_MATERIAL" -> {
                    pendingMoreScreen = MoreSubScreen.NOTES_PYQ
                    currentTab = BottomNavTab.MORE
                }
                "LOST_FOUND" -> {
                    pendingMoreScreen = MoreSubScreen.LOST_FOUND
                    currentTab = BottomNavTab.MORE
                }
                "POST" -> {
                    pendingCommunityPostId = targetId
                    pendingMoreScreen = MoreSubScreen.COMMUNITY
                    currentTab = BottomNavTab.MORE
                }
                "ADMIN" -> {
                    pendingMoreScreen = MoreSubScreen.ADMIN_PANEL
                    currentTab = BottomNavTab.MORE
                }
            }
            pendingNotification.value = null
        }
    }

    BackHandler(enabled = currentTab != BottomNavTab.HOME) {
        currentTab = BottomNavTab.HOME
    }

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

    // If not signed in to Supabase/Google, show sign-in screen
    if (authSessionState == null) {
        val onGoogleSignInWithGuard = {
            if (!isSigningIn) {
                isSigningIn = true
                onGoogleSignIn()
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                SignInScreen(
                    onGoogleSignIn = onGoogleSignInWithGuard,
                    isSigningIn = isSigningIn
                )
            }
        }
        return
    }

    // If not onboarded locally, show onboarding
    if (!coreState.isOnboarded) {
        val signedInEmail = authSessionState?.user?.email ?: coreState.profile.email
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                OnboardingScreen(
                    initialEmail = signedInEmail,
                    isOwner = signedInEmail.equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true),
                    onCompleteSetup = { name, email, collegeName, courseId, courseName, semester, section, role ->
                        coreViewModel.completeOnboarding(
                            name, email, collegeName, courseId, courseName, semester, section, role
                        )
                    }
                )
            }
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    C1NavItem(
                        selected = currentTab == BottomNavTab.HOME,
                        onClick = { currentTab = BottomNavTab.HOME },
                        selectedIcon = Icons.Default.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        label = stringResource(R.string.nav_home)
                    )
                    C1NavItem(
                        selected = currentTab == BottomNavTab.ATTENDANCE,
                        onClick = { currentTab = BottomNavTab.ATTENDANCE },
                        selectedIcon = Icons.Default.CheckCircle,
                        unselectedIcon = Icons.Outlined.CheckCircle,
                        label = stringResource(R.string.nav_attendance)
                    )
                    C1NavItem(
                        selected = currentTab == BottomNavTab.TIMETABLE,
                        onClick = { currentTab = BottomNavTab.TIMETABLE },
                        selectedIcon = Icons.Default.Schedule,
                        unselectedIcon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.nav_timetable)
                    )
                    C1NavItem(
                        selected = currentTab == BottomNavTab.NOTICES,
                        onClick = { currentTab = BottomNavTab.NOTICES },
                        selectedIcon = Icons.Default.Campaign,
                        unselectedIcon = Icons.Outlined.Campaign,
                        label = stringResource(R.string.nav_notices),
                        badgeCount = coreState.unreadNoticeCount
                    )
                    C1NavItem(
                        selected = currentTab == BottomNavTab.MORE,
                        onClick = { currentTab = BottomNavTab.MORE },
                        selectedIcon = Icons.Default.MenuBook,
                        unselectedIcon = Icons.Outlined.MenuBook,
                        label = stringResource(R.string.nav_more),
                        badgeCount = unreadNotifications
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.widthIn(max = 1120.dp).fillMaxSize()
            ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val offset = if (forward) 1 else -1
                    (fadeIn(animationSpec = spring(
                        dampingRatio = C1Motion.springDampingRatio,
                        stiffness = C1Motion.springStiffness
                    )) +
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = C1Motion.springDampingRatio,
                                stiffness = C1Motion.springStiffness
                            )
                        ) { width -> offset * width / 8 })
                        .togetherWith(fadeOut(animationSpec = spring(
                            dampingRatio = C1Motion.springDampingRatio,
                            stiffness = C1Motion.springStiffness
                        )))
                },
                label = "bottomTabTransition"
            ) { tab ->
                stateHolder.SaveableStateProvider(tab.name) {
                    when (tab) {
                        BottomNavTab.HOME -> HomeScreen(
                        uiState = coreState,
                        campusEvents = studyCampusState.events,
                        nextThreeDeadlines = studyCampusState.nextThreeDeadlines,
                        nextUpcomingExam = studyCampusState.nextUpcomingExam,
                        dataRefreshError = noticesSyncError,
                        onOpenProfile = {
                            pendingMoreScreen = MoreSubScreen.PROFILE
                            currentTab = BottomNavTab.MORE
                        },
                        onOpenNotifications = {
                            pendingMoreScreen = MoreSubScreen.NOTIFICATIONS
                            currentTab = BottomNavTab.MORE
                        },
                        onOpenAttendance = { currentTab = BottomNavTab.ATTENDANCE },
                        onOpenTimetable = { currentTab = BottomNavTab.TIMETABLE },
                        onOpenNotices = { currentTab = BottomNavTab.NOTICES },
                        onOpenNotice = { noticeId ->
                            coreViewModel.markNoticeRead(noticeId)
                            currentTab = BottomNavTab.NOTICES
                        },
                        onOpenMore = { screen ->
                            pendingMoreScreen = screen
                            currentTab = BottomNavTab.MORE
                        },
                        onRetryRefresh = coreViewModel::refreshNotices
                    )
                        BottomNavTab.ATTENDANCE -> FlagshipAttendanceScreen(
                        uiState = coreState,
                        onMarkAttendance = coreViewModel::markAttendance,
                        onTargetPctChange = coreViewModel::updateTargetPercentage,
                        onOpenNotifications = {
                            pendingMoreScreen = MoreSubScreen.NOTIFICATIONS
                            currentTab = BottomNavTab.MORE
                        },
                        onOpenProfile = {
                            pendingMoreScreen = MoreSubScreen.PROFILE
                            currentTab = BottomNavTab.MORE
                        }
                        )
                        BottomNavTab.TIMETABLE -> TimetableScreen(
                        uiState = coreState,
                        onUpdateReminderMinutes = coreViewModel::updateReminderLeadMinutes,
                        onRefresh = coreViewModel::refreshTimetable,
                        onOpenNotifications = {
                            pendingMoreScreen = MoreSubScreen.NOTIFICATIONS
                            currentTab = BottomNavTab.MORE
                        },
                        onOpenProfile = {
                            pendingMoreScreen = MoreSubScreen.PROFILE
                            currentTab = BottomNavTab.MORE
                        }
                        )
                        BottomNavTab.NOTICES -> NoticeBoardScreen(
                        uiState = coreState,
                        onMarkNoticeRead = coreViewModel::markNoticeRead,
                        onMarkAllRead = coreViewModel::markAllNoticesRead,
                        onPostNotice = coreViewModel::postNotice,
                        onUploadNoticeAttachment = coreViewModel::uploadNoticeAttachment,
                        dataRefreshError = noticesSyncError,
                        onRetryRefresh = coreViewModel::refreshNotices,
                        focusNoticeId = pendingNoticeId,
                        onFocusConsumed = { pendingNoticeId = null },
                        onOpenNotifications = {
                            pendingMoreScreen = MoreSubScreen.NOTIFICATIONS
                            currentTab = BottomNavTab.MORE
                        },
                        onOpenProfile = {
                            pendingMoreScreen = MoreSubScreen.PROFILE
                            currentTab = BottomNavTab.MORE
                        }
                        )
                        BottomNavTab.MORE -> MoreHubScreen(
                        uiState = studyCampusState,
                        viewModel = studyCampusViewModel,
                        onSignOut = {
                            onSignOut()
                            currentTab = BottomNavTab.HOME
                        },
                        onOpenTimetable = { currentTab = BottomNavTab.TIMETABLE },
                        onOpenAttendance = { currentTab = BottomNavTab.ATTENDANCE },
                        onOpenNotices = { currentTab = BottomNavTab.NOTICES },
                        deepLink = pendingMoreScreen,
                        onDeepLinkConsumed = { pendingMoreScreen = null },
                        communityViewModel = communityViewModel,
                        coreViewModel = coreViewModel,
                        onNotificationNavigate = { targetType, targetId ->
                            when (targetType) {
                                "NOTICE" -> {
                                    pendingNoticeId = targetId
                                    coreViewModel.refreshNotices()
                                    currentTab = BottomNavTab.NOTICES
                                }
                                "ASSIGNMENT" -> {
                                    pendingAssignmentId = targetId
                                    pendingMoreScreen = MoreSubScreen.ASSIGNMENTS
                                    currentTab = BottomNavTab.MORE
                                }
                                "POST" -> {
                                    pendingCommunityPostId = targetId
                                    pendingMoreScreen = MoreSubScreen.COMMUNITY
                                    currentTab = BottomNavTab.MORE
                                }
                                "ADMIN" -> {
                                    pendingMoreScreen = MoreSubScreen.SUPER_ADMIN_MODERATION
                                    currentTab = BottomNavTab.MORE
                                }
                            }
                        },
                        focusPostId = pendingCommunityPostId,
                        onFocusPostConsumed = { pendingCommunityPostId = null },
                        focusAssignmentId = pendingAssignmentId,
                        onFocusAssignmentConsumed = { pendingAssignmentId = null }
                    )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun SignInScreen(onGoogleSignIn: () -> Unit, isSigningIn: Boolean) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(C1Radius.xl)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
            Image(
                painter = painterResource(R.drawable.ic_campusone_c_mark),
                contentDescription = "CampusOne",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(46.dp)
            )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = stringResource(`in`.campusone.app.R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(`in`.campusone.app.R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            C1Button(
                text = if (isSigningIn) "Signing in..." else stringResource(`in`.campusone.app.R.string.sign_in_google),
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isSigningIn
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(`in`.campusone.app.R.string.sign_in_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Initial launch screen. Shows the CampusOne mark with a subtle VEDA Labs
 * developer attribution. Shown only once before the main interface appears.
 */
@Composable
fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_campusone_c_mark),
                contentDescription = "CampusOne",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.splash_veda_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
