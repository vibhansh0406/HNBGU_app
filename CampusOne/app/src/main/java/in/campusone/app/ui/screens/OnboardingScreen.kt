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
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private const val TOTAL_STEPS = 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onToggleLanguage: () -> Unit,
    initialEmail: String = "",
    isOwner: Boolean = false,
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
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(initialEmail) }

    val campuses = listOf(
        "Birla Campus, Srinagar (Garhwal)",
        "Chauras Campus, Srinagar (Garhwal)"
    )
    var selectedCampus by rememberSaveable { mutableStateOf(campuses.first()) }

    val courses = listOf(
        "bsc_cs" to "B.Sc. (Hons) Computer Science",
        "bsc_life" to "B.Sc. (Hons) Life Science",
        "bsc_phy" to "B.Sc. (Hons) Physics",
        "bsc_chem" to "B.Sc. (Hons) Chemistry",
        "bsc_math" to "B.Sc. (Hons) Mathematics",
        "bsc_bot" to "B.Sc. (Hons) Botany",
        "bsc_zoo" to "B.Sc. (Hons) Zoology",
        "bsc_micro" to "B.Sc. (Hons) Microbiology",
        "bsc_biochem" to "B.Sc. (Hons) Biochemistry",
        "bsc_biotech" to "B.Sc. (Hons) Biotechnology",
        "bsc_geology" to "B.Sc. (Hons) Geology",
        "bsc_stats" to "B.Sc. (Hons) Statistics",
        "bsc_geog" to "B.Sc. (Hons) Geography",
        "bsc_anthro" to "B.Sc. (Hons) Anthropology",
        "bsc_econ" to "B.Sc. (Hons) Economics",
        "bsc_defence" to "B.Sc. (Hons) Defence Studies",
        "bsc_yogic" to "B.Sc. (Hons) Yogic Science",
        "bsc_forestry" to "B.Sc. Forestry",
        "bsc_horti" to "B.Sc. Horticulture",
        "ba_hons" to "B.A. (Hons) Arts & Humanities",
        "ba_jmc" to "B.A. (Hons) Journalism & Mass Comm.",
        "bcom" to "B.Com. (Hons)",
        "bba" to "BBA",
        "btech_cse" to "B.Tech. Computer Science & Engg.",
        "btech_ece" to "B.Tech. Electronics & Comm. Engg.",
        "btech_it" to "B.Tech. Information Technology",
        "btech_me" to "B.Tech. Mechanical Engineering",
        "btech_ie" to "B.Tech. Instrumentation Engineering",
        "bpharm" to "B.Pharm.",
        "bhm" to "BHM & Catering Technology",
        "bed" to "B.Ed.",
        "bped" to "B.P.Ed.",
        "blib" to "B.Lib.I.Sc."
    )
    var selectedCourseIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedSemester by rememberSaveable { mutableIntStateOf(3) }
    var selectedSection by rememberSaveable { mutableStateOf("A") }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.STUDENT) }

    val selectableRoles = listOf(
        UserRole.STUDENT,
        UserRole.CR,
        UserRole.TEACHER,
        UserRole.CLUB_ADMIN
    )

    val canContinue = when (step) {
        0 -> name.isNotBlank() && email.isNotBlank()
        else -> true
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            LinearProgressIndicator(
                progress = { (step + 1) / TOTAL_STEPS.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Step ${step + 1} of $TOTAL_STEPS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.welcome_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.welcome_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Your campus",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.select_college),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            campuses.forEach { campus ->
                                FilterChip(
                                    selected = selectedCampus == campus,
                                    onClick = { selectedCampus = campus },
                                    label = { Text(campus) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                    }

                    else -> {
                        Text(
                            text = "Your class",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isOwner) {
                            Text(
                                text = "Admin access",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Signed in as the app owner. The admin panel will be enabled automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.select_role),
                                style = MaterialTheme.typography.titleMedium
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectableRoles.forEach { role ->
                                    val label = when (role) {
                                        UserRole.STUDENT -> stringResource(R.string.role_student)
                                        UserRole.CR -> stringResource(R.string.role_cr)
                                        UserRole.TEACHER -> stringResource(R.string.role_teacher)
                                        UserRole.CLUB_ADMIN -> stringResource(R.string.role_club_admin)
                                        else -> role.name
                                    }
                                    FilterChip(
                                        selected = selectedRole == role,
                                        onClick = { selectedRole = role },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    TextButton(
                        onClick = { step -= 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < TOTAL_STEPS - 1) {
                            step += 1
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val (courseId, courseName) = courses[selectedCourseIndex]
                            onCompleteSetup(
                                name,
                                email,
                                selectedCampus,
                                courseId,
                                courseName,
                                selectedSemester,
                                selectedSection,
                                selectedRole
                            )
                        }
                    },
                    enabled = canContinue,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = if (step == TOTAL_STEPS - 1) stringResource(R.string.complete_setup) else "Next",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
