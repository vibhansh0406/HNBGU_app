package `in`.campusone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.UserRole

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onToggleLanguage: () -> Unit,
    onCompleteSetup: (
        name: String,
        email: String,
        collegeName: String,
        courseId: String,
        courseName: String,
        semester: Int,
        section: String,
        role: UserRole
    ) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var name by rememberSaveable { mutableStateOf("Aarav Rawat") }
    var email by rememberSaveable { mutableStateOf("aarav.rawat@hnbgu.ac.in") }
    var isGoogleSignedIn by rememberSaveable { mutableStateOf(false) }

    val colleges = listOf(
        "HNB Garhwal University (Chauras / Birla Campus)",
        "HNBGU BGR Campus, Pauri",
        "HNBGU SRT Campus, Badshahithaul Tehri",
        "DAV (PG) College, Dehradun (HNBGU Affiliated)"
    )
    var selectedCollege by rememberSaveable { mutableStateOf(colleges.first()) }

    val courses = listOf(
        "bsc_cs" to "B.Sc. Computer Science (NEP)",
        "btech_cse" to "B.Tech. Computer Science & Engg.",
        "bca" to "BCA (Bachelor of Computer Applications)",
        "ba_hons" to "B.A. (Hons) Arts & Humanities",
        "bcom" to "B.Com. (NEP)"
    )
    var selectedCourseIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedSemester by rememberSaveable { mutableIntStateOf(3) }
    var selectedSection by rememberSaveable { mutableStateOf("A") }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.STUDENT) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(onClick = onToggleLanguage) {
                    Icon(Icons.Default.Translate, contentDescription = null)
                    Text(
                        text = stringResource(R.string.language_toggle),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Google Sign-In Card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Google / University Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isGoogleSignedIn = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isGoogleSignedIn) Icons.Default.Check else Icons.Default.AccountCircle,
                            contentDescription = null
                        )
                        Text(
                            text = if (isGoogleSignedIn) "Signed in as $email" else stringResource(R.string.sign_in_google),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // 2. College Selection
            Text(
                text = stringResource(R.string.select_college),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colleges.forEach { college ->
                    FilterChip(
                        selected = selectedCollege == college,
                        onClick = { selectedCollege = college },
                        label = { Text(college) }
                    )
                }
            }

            // 3. Course Selection
            Text(
                text = stringResource(R.string.select_course),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                courses.forEachIndexed { index, (_, label) ->
                    FilterChip(
                        selected = selectedCourseIndex == index,
                        onClick = { selectedCourseIndex = index },
                        label = { Text(label) }
                    )
                }
            }

            // 4. Semester & Section
            Text(
                text = "${stringResource(R.string.select_semester)} & ${stringResource(R.string.select_section)}",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..8).forEach { sem ->
                    FilterChip(
                        selected = selectedSemester == sem,
                        onClick = { selectedSemester = sem },
                        label = { Text("Sem $sem") }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("A", "B", "C").forEach { sec ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        label = { Text("Sec $sec") }
                    )
                }
            }

            // 5. Role Selection (Student, CR, Teacher, Club Admin)
            Text(
                text = stringResource(R.string.select_role),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.entries.forEach { role ->
                    val label = when (role) {
                        UserRole.STUDENT -> stringResource(R.string.role_student)
                        UserRole.CR -> stringResource(R.string.role_cr)
                        UserRole.TEACHER -> stringResource(R.string.role_teacher)
                        UserRole.CLUB_ADMIN -> stringResource(R.string.role_club_admin)
                    }
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val (courseId, courseName) = courses[selectedCourseIndex]
                    onCompleteSetup(
                        name,
                        email,
                        selectedCollege,
                        courseId,
                        courseName,
                        selectedSemester,
                        selectedSection,
                        selectedRole
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.complete_setup),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
