package `in`.campusone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.ui.screens.AttendanceScreen
import `in`.campusone.app.ui.screens.HomeScreen
import `in`.campusone.app.ui.screens.MoreHubScreen
import `in`.campusone.app.ui.screens.NoticeBoardScreen
import `in`.campusone.app.ui.screens.OnboardingScreen
import `in`.campusone.app.ui.screens.TimetableScreen
import `in`.campusone.app.ui.theme.CampusOneTheme
import `in`.campusone.app.ui.viewmodel.CoreViewModel
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
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
                                        authManager.ensureUserProfile(
                                            uid = uid,
                                            email = account.email,
                                            name = account.displayName,
                                            role = "STUDENT"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager.initGoogleSignIn()
        setContent {
            CampusOneTheme {
                CampusOneRoot(
                    onGoogleSignIn = {
                        googleSignInLauncher.launch(authManager.getSignInIntent())
                    },
                    isSignedIn = { authManager.currentSession() != null },
                    onSignOut = {
                        lifecycleScope.launch { authManager.signOut() }
                    }
                )
            }
        }
    }
}

@Composable
fun CampusOneRoot(
    onGoogleSignIn: () -> Unit,
    isSignedIn: () -> Boolean,
    onSignOut: () -> Unit,
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

    // If not signed in to Supabase/Google, show sign-in screen
    if (!isSignedIn()) {
        SignInScreen(onGoogleSignIn = onGoogleSignIn)
        return
    }

    // If not onboarded locally, show onboarding
    if (!coreState.isOnboarded) {
        OnboardingScreen(
            onToggleLanguage = coreViewModel::toggleLanguage,
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
                    viewModel = studyCampusViewModel
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(`in`.campusone.app.R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(`in`.campusone.app.R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(`in`.campusone.app.R.string.sign_in_google),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sign in with Google to sync your data permanently across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
