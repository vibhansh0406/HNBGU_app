package `in`.campusone.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1FilterChip
import `in`.campusone.app.ui.components.C1IconPosition
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.theme.C1Height
import `in`.campusone.app.ui.theme.C1Motion
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.C1TouchTarget

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
    var selectedCourseIndex = remember { mutableIntStateOf(0) }
    val selectedSemester by rememberSaveable { mutableIntStateOf(3) }
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

    val progress = (step + 1) / 3f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = C1Motion.normalMillis, easing = C1Motion.emphasizedEasing),
        label = "onboardingProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Top bar with logo and language toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                androidx.compose.material3.TextButton(onClick = onToggleLanguage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.language_toggle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Progress indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.sm)
        ) {
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(C1Radius.xs),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Spacer(modifier = Modifier.height(C1Spacing.xs))
            Text(
                text = "Step ${step + 1} of 3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Step content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.xl)
        ) {
            when (step) {
                0 -> OnboardingStepWelcome(
                    name = name,
                    onNameChange = { name = it },
                    email = email,
                    onEmailChange = { email = it }
                )
                1 -> OnboardingStepCampusCourse(
                    campuses = campuses,
                    selectedCampus = selectedCampus,
                    onCampusChange = { selectedCampus = it },
                    courses = courses,
                    selectedCourseIndex = selectedCourseIndex.value,
                    onCourseChange = { selectedCourseIndex.value = it }
                )
                else -> OnboardingStepClass(
                    selectedSemester = selectedSemester,
                    onSemesterChange = { selectedSemester.value = it },
                    selectedSection = selectedSection,
                    onSectionChange = { selectedSection.value = it },
                    isOwner = isOwner,
                    selectableRoles = selectableRoles,
                    selectedRole = selectedRole,
                    onRoleChange = { selectedRole.value = it }
                )
            }
        }

        // Navigation buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    C1Button(
                        text = stringResource(R.string.action_back),
                        onClick = { step -= 1 },
                        intent = C1ButtonIntent.OUTLINED,
                        modifier = Modifier.weight(1f).height(C1Height.button)
                    )
                }
                C1Button(
                    text = if (step == 2) stringResource(R.string.complete_setup) else stringResource(R.string.action_next),
                    onClick = {
                        if (step < 2) {
                            step += 1
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val (courseId, courseName) = courses[selectedCourseIndex.value]
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
                    modifier = Modifier.weight(1f).height(C1Height.button),
                    icon = { if (step == 2) {} else { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                    iconPosition = C1IconPosition.END
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepWelcome(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.xl)
    ) {
        // Hero illustration area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, C1Radius.xl),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = C1Spacing.xl)
            )
        }

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            C1TextField(
                value = name,
                onValueChange = onNameChange,
                label = "Full Name",
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            )
            C1TextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Google / University Email",
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                keyboardType = KeyboardType.Text
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingStepCampusCourse(
    campuses: List<String>,
    selectedCampus: String,
    onCampusChange: (String) -> Unit,
    courses: List<Pair<String, String>>,
    selectedCourseIndex: Int,
    onCourseChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Campus",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(R.string.select_college),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Campus selection
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            campuses.forEach { campus ->
                C1FilterChip(
                    label = campus,
                    selected = selectedCampus == campus,
                    onClick = { onCampusChange(campus) },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // Course selection
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            C1SectionHeader(title = stringResource(R.string.select_course))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                items(courses, key = { it.first }) { (courseId, label) ->
                    val isSelected = selectedCourseIndex == courses.indexOfFirst { it.first == courseId }
                    CourseSelectionCard(
                        label = label,
                        selected = isSelected,
                        onClick = {
                            onCourseChange(courses.indexOfFirst { it.first == courseId })
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingStepClass(
    selectedSemester: Int,
    onSemesterChange: (Int) -> Unit,
    selectedSection: String,
    onSectionChange: (String) -> Unit,
    isOwner: Boolean,
    selectableRoles: List<UserRole>,
    selectedRole: UserRole,
    onRoleChange: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Class",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "${stringResource(R.string.select_semester)} & ${stringResource(R.string.select_section)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Semester selection
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            C1SectionHeader(title = stringResource(R.string.select_semester))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                (1..8).forEach { sem ->
                    C1FilterChip(
                        label = "Sem $sem",
                        selected = selectedSemester == sem,
                        onClick = { onSemesterChange(sem) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // Section selection
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            C1SectionHeader(title = stringResource(R.string.select_section))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                listOf("A", "B", "C").forEach { sec ->
                    C1FilterChip(
                        label = "Sec $sec",
                        selected = selectedSection == sec,
                        onClick = { onSectionChange(sec) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // Role selection or owner notice
        if (isOwner) {
            C1EmptyState(
                icon = Icons.Default.Shield,
                title = "Admin Access",
                message = "Signed in as the app owner. The admin panel will be enabled automatically."
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                C1SectionHeader(title = stringResource(R.string.select_role))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    selectableRoles.forEach { role ->
                        val label = when (role) {
                            UserRole.STUDENT -> stringResource(R.string.role_student)
                            UserRole.CR -> stringResource(R.string.role_cr)
                            UserRole.TEACHER -> stringResource(R.string.role_teacher)
                            UserRole.CLUB_ADMIN -> stringResource(R.string.role_club_admin)
                            else -> role.name
                        }
                        C1FilterChip(
                            label = label,
                            selected = selectedRole == role,
                            onClick = { onRoleChange(role) },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                Text(
                    text = "CR, Teacher, Moderator and Club Admin access is granted after admin approval. A request is sent automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = C1Spacing.sm)
                )
            }
        }
    }
}

/**
 * Course selection card with consistent dimensions and two-line text wrapping.
 * Replaces overflow-prone filter chips for long course names.
 */
@Composable
private fun CourseSelectionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) C1Motion.pressScale else 1f,
        animationSpec = tween(durationMillis = C1Motion.fastMillis),
        label = "courseCardPress"
    )
    C1SurfaceCard(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .graphicsLayer { scaleX = scale; scaleY = scale },
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        contentPadding = PaddingValues(C1Spacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}