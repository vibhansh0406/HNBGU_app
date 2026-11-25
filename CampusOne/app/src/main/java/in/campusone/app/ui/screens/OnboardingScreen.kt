package `in`.campusone.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.HnbguAcademicCatalog
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.theme.C1Motion
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.CampusOneBrandGreen

/**
 * Three deliberate onboarding moments:
 * 1. Personal details, 2. Campus and programme, 3. Semester and section.
 * This screen only collects profile fields; persistence and role authority remain
 * in CoreViewModel/AuthManager and the existing Supabase policies.
 */
@Composable
fun OnboardingScreen(
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
    val campuses = HnbguAcademicCatalog.campuses
    var selectedCampus by rememberSaveable { mutableStateOf(campuses.first()) }
    val courses = HnbguAcademicCatalog.courses
    var selectedCourseIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedSemester by rememberSaveable { mutableIntStateOf(3) }
    var selectedSection by rememberSaveable { mutableStateOf("A") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var showCourseMenu by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = (step + 1) / 2f,
        animationSpec = tween(C1Motion.normalMillis, easing = C1Motion.emphasizedEasing),
        label = "onboardingProgress"
    )
    val emailValid = email.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
    val canContinue = step != 0 || (name.isNotBlank() && emailValid)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = C1Spacing.lg)
                    .padding(top = C1Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CampusOneBrandGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_campusone_c_mark),
                                contentDescription = "CampusOne",
                                modifier = Modifier.size(25.dp),
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text("CampusOne", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    }

                Spacer(Modifier.height(C1Spacing.xl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(2) { index ->
                        val segmentColor = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(segmentColor)
                        )
                    }
                }
                Spacer(Modifier.height(C1Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (step == 0) "INSTITUTIONAL IDENTITY" else "ACADEMIC PREFERENCES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text("0${step + 1} / 02", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AnimatedContent(
                targetState = step,
                label = "onboardingStep",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = C1Spacing.lg)
                        .padding(top = C1Spacing.xxl, bottom = C1Spacing.xl),
                    horizontalAlignment = Alignment.Start
                ) {
                    when (currentStep) {
                        0 -> {
                            StepHeading(
                                eyebrow = "STEP 1 · INSTITUTIONAL IDENTITY",
                                title = "Verify your campus identity.",
                                subtitle = "Confirm the account you signed in with, then connect it to your HNBGU campus and programme."
                            )
                            Spacer(Modifier.height(C1Spacing.lg))
                            InfoStrip(
                                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                text = "Google sign-in is active on this device. Confirm the campus contact details below before continuing."
                            )
                            Spacer(Modifier.height(C1Spacing.lg))
                            ProfileField(
                                value = name,
                                onValueChange = { name = it },
                                label = "Student name",
                                placeholder = "Enter your full name",
                                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                keyboardType = KeyboardType.Text
                            )
                            Spacer(Modifier.height(C1Spacing.md))
                            ProfileField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Google account email",
                                placeholder = "name@example.com",
                                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardType = KeyboardType.Email,
                                supportingText = if (email.isNotBlank() && !emailValid) "Enter a valid email address." else null
                            )
                            Spacer(Modifier.height(C1Spacing.xl))
                            FieldLabel("SELECT YOUR HNBGU CAMPUS")
                            Spacer(Modifier.height(C1Spacing.sm))
                            campuses.forEach { campus ->
                                SelectableOptionCard(
                                    title = campus,
                                    subtitle = if (campus.contains("Birla")) "Srinagar · Main campus" else "Srinagar · Chauras campus",
                                    selected = selectedCampus == campus,
                                    onClick = { selectedCampus = campus },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(C1Spacing.sm))
                            }
                            Spacer(Modifier.height(C1Spacing.md))
                            FieldLabel("ENROLLED PROGRAMME")
                            Spacer(Modifier.height(C1Spacing.sm))
                            Box {
                                OutlinedButton(
                                    onClick = { showCourseMenu = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                                    shape = C1Radius.md,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(C1Spacing.sm))
                                    Text(
                                        text = courses.getOrNull(selectedCourseIndex)?.second ?: "Choose your programme",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(
                                    expanded = showCourseMenu,
                                    onDismissRequest = { showCourseMenu = false },
                                    modifier = Modifier.heightIn(max = 420.dp)
                                ) {
                                    courses.forEachIndexed { index, course ->
                                        DropdownMenuItem(
                                            text = { Text(course.second, maxLines = 2) },
                                            onClick = {
                                                selectedCourseIndex = index
                                                showCourseMenu = false
                                            },
                                            leadingIcon = if (selectedCourseIndex == index) {
                                                { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                            } else null
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(C1Spacing.md))
                            Text(
                                "Your campus and programme are used to scope academic information. You can edit them later from your profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            StepHeading(
                                eyebrow = "STEP 2 · ACADEMIC PREFERENCES",
                                title = "Set up your academic space.",
                                subtitle = "Choose your current semester and section so your timetable, attendance and notices stay relevant."
                            )
                            Spacer(Modifier.height(C1Spacing.lg))
                            FlagshipCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text("SELECTED PROGRAMME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Spacer(Modifier.height(C1Spacing.xs))
                                Text(courses.getOrNull(selectedCourseIndex)?.second ?: "Programme not selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(selectedCampus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                            }
                            Spacer(Modifier.height(C1Spacing.xl))
                            FieldLabel("CURRENT SEMESTER")
                            Spacer(Modifier.height(C1Spacing.sm))
                            (1..2).forEach { rowIndex ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ((rowIndex - 1) * 4 + 1..rowIndex * 4).forEach { semester ->
                                        FilterChip(
                                            selected = selectedSemester == semester,
                                            onClick = { selectedSemester = semester },
                                            label = { Text(semester.toString(), fontWeight = FontWeight.SemiBold) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (rowIndex == 1) Spacer(Modifier.height(4.dp))
                            }
                            Spacer(Modifier.height(C1Spacing.xl))
                            FieldLabel("SECTION")
                            Spacer(Modifier.height(C1Spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                                listOf("A", "B", "C").forEach { section ->
                                    FilterChip(
                                        selected = selectedSection == section,
                                        onClick = { selectedSection = section },
                                        label = { Text("Section $section") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(C1Spacing.xl))
                            if (isOwner) {
                                InfoStrip(
                                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                                    text = "Owner account detected. Administrative access is assigned by the server."
                                )
                            } else {
                                FieldLabel("CAMPUS ROLE")
                                Spacer(Modifier.height(C1Spacing.sm))
                                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                                    FilterChip(
                                        selected = selectedRole == UserRole.STUDENT,
                                        onClick = { selectedRole = UserRole.STUDENT },
                                        label = { Text(stringResource(R.string.role_student)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = selectedRole == UserRole.CR,
                                        onClick = { selectedRole = UserRole.CR },
                                        label = { Text(stringResource(R.string.role_cr)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    "CR access requests are reviewed by an administrator. Selecting CR here does not grant permissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = C1Spacing.sm)
                                )
                            }
                            Spacer(Modifier.height(C1Spacing.md))
                            InfoStrip(
                                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                text = "Academic details are saved to your campus profile. Reminder and offline-storage preferences can be changed later in Settings."
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = { step -= 1 },
                                modifier = Modifier.size(54.dp),
                                shape = C1Radius.md,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous step")
                            }
                        }
                        Button(
                            onClick = {
                                if (step < 1) {
                                    step += 1
                                } else {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val (courseId, courseName) = courses[selectedCourseIndex]
                                    onCompleteSetup(
                                        name.trim(),
                                        email.trim(),
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
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = C1Radius.md,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                if (step == 1) "Enter CampusOne" else stringResource(R.string.action_next),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(C1Spacing.sm))
                            Icon(
                                if (step == 1) Icons.Default.Check else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (step == 1) "Your campus profile will be saved securely." else "You can review these details before entering.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = C1Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeading(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: @Composable () -> Unit,
    keyboardType: KeyboardType,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = icon,
            singleLine = true,
            shape = C1Radius.md,
            isError = supportingText != null,
            supportingText = supportingText?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp),
        shape = C1Radius.md,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
                }
            } else {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun InfoStrip(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(C1Radius.md)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(C1Spacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                icon()
            }
        }
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
