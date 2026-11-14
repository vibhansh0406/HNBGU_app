package `in`.campusone.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.remote.SupabaseSession
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.ui.screens.AttendanceScreen
import `in`.campusone.app.ui.screens.HomeScreen
import `in`.campusone.app.ui.screens.MoreHubScreen
import `in`.campusone.app.ui.screens.NoticeBoardScreen
import `in`.campusone.app.ui.screens.OnboardingScreen
import `in`.campusone.app.ui.screens.TimetableScreen
import `in`.campusone.app.ui.theme.CampusOneTheme
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class BottomNavTab {
    HOME, ATTENDANCE, TIMETABLE, NOTICES, MORE
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

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
                                            name = account.displayName,
                                            role = if (account.email?.trim()
                                                    .equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true)
                                            ) "ADMIN" else "STUDENT"
                                        )
                                        if (!profileResult.isSuccess) {
                                            android.widget.Toast.makeText(
                                                this@MainActivity,
                                                "Profile save failed: ${profileResult.exceptionOrNull()?.message ?: ""}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
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
        setContent {
            NotificationPermissionRequest()
            CampusOneTheme {
                CampusOneRoot(
                    onGoogleSignIn = {
                        googleSignInLauncher.launch(authManager.getSignInIntent())
                    },
                    authSession = authManager.session,
                    onSignOut = {
                        lifecycleScope.launch { authManager.signOut() }
                    }
                )
            }
        }
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
    studyCampusViewModel: StudyAndCampusViewModel = hiltViewModel()
) {
    val authSessionState by authSession.collectAsStateWithLifecycle()
    val coreState by coreViewModel.uiState.collectAsStateWithLifecycle()
    val introSeen by coreViewModel.introSeen.collectAsStateWithLifecycle()
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

    // First launch: immersive intro before sign-in
    if (introSeen == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {}
        return
    }
    if (introSeen == false) {
        IntroScreen(onFinish = coreViewModel::markIntroSeen)
        return
    }

    // If not signed in to Supabase/Google, show sign-in screen
    if (authSessionState == null) {
        SignInScreen(onGoogleSignIn = onGoogleSignIn)
        return
    }

    // If not onboarded locally, show onboarding
    if (!coreState.isOnboarded) {
        val signedInEmail = authSessionState?.user?.email ?: coreState.profile.email
        OnboardingScreen(
            onToggleLanguage = coreViewModel::toggleLanguage,
            initialEmail = signedInEmail,
            isOwner = signedInEmail.equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true),
            onCompleteSetup = { name, email, collegeName, courseId, courseName, semester, section, role ->
                coreViewModel.completeOnboarding(
                    name, email, collegeName, courseId, courseName, semester, section, role
                )
            }
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
                    viewModel = studyCampusViewModel,
                    onSignOut = {
                        onSignOut()
                        currentTab = BottomNavTab.HOME
                    }
                )
            }
        }
    }
}

@Composable
fun SignInScreen(onGoogleSignIn: () -> Unit) {
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
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(44.dp))
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(`in`.campusone.app.R.string.sign_in_google),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Offline first. Synced only for you.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class IntroPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            IntroPage(
                Icons.Default.CheckCircle,
                "Attendance that never slips",
                "Track every class in one tap. Know exactly how many lectures you can miss and still stay safe."
            ),
            IntroPage(
                Icons.Default.Schedule,
                "Your day, always in sync",
                "Live timetable, classroom alerts and notice board updates from your section and the university."
            ),
            IntroPage(
                Icons.Default.MenuBook,
                "Everything campus, one app",
                "Study material, exams, clubs, events, lost & found and the campus directory — all offline-first."
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text("Skip")
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 22.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.lastIndex) "Get started" else "Next",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
