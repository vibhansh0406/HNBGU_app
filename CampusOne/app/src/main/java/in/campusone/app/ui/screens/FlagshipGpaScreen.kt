package `in`.campusone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.domain.usecase.CourseGradeInput
import `in`.campusone.app.domain.usecase.GradingScaleType
import `in`.campusone.app.domain.usecase.GpaCalculator
import `in`.campusone.app.ui.viewmodel.StudyAndCampusUiState
import `in`.campusone.app.ui.viewmodel.StudyAndCampusViewModel
import java.util.Locale

private val GpaGreen = Color(0xFF004A35)
private val GpaMint = Color(0xFFDCECE1)
private val GpaCanvas = Color(0xFFF9FAF5)
private val GpaInk = Color(0xFF17231F)
private val GpaMuted = Color(0xFF66766D)
private val GpaLine = Color(0xFFE2E8E1)
private val GpaOchre = Color(0xFFF5EAD5)

@Composable
fun FlagshipGpaCalculatorScreen(
    uiState: StudyAndCampusUiState,
    viewModel: StudyAndCampusViewModel
) {
    val context = LocalContext.current
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val past = uiState.gpaResult.semesterTrend.filter { it.semesterNumber != uiState.profile.semester }
    val pastCredits = past.sumOf { it.totalCredits }
    val pastPoints = past.sumOf { it.totalCredits * it.sgpa }
    val baselineCgpa = if (pastCredits > 0) pastPoints / pastCredits.toDouble() else null
    val current = uiState.gpaResult
    val maximum = uiState.gpaScaleType.maxScalePoint.toFloat()

    Column(modifier = Modifier.fillMaxSize().background(GpaCanvas)) {
        FlagshipAppHeader(uiState.profile.collegeName, uiState.profile.name, showActions = false)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FlagshipPageTitle(
                    "GPA & CGPA Simulator",
                    "Project your semester grade point average and cumulative standing",
                    "ACADEMIC AUDIT",
                    trailing = { FlagshipPill("Sem " + uiState.profile.semester) }
                )
            }
            item {
                FlagshipCard(containerColor = Color(0xFFE9F0E9)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Baseline standing", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = GpaInk)
                        FlagshipPill(if (past.isNotEmpty()) past.size.toString() + " saved semesters" else "Awaiting history")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GpaStatCard("Prior CGPA", baselineCgpa?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—", pastCredits.toString() + " credits", Modifier.weight(1f))
                        GpaStatCard("Earned credits", pastCredits.toString(), if (past.isEmpty()) "No saved semesters" else past.size.toString() + " records", Modifier.weight(1f))
                        GpaStatCard("Current load", uiState.gpaCourses.sumOf { it.credits }.toString(), "Credits entered", Modifier.weight(1f))
                    }
                }
            }
            item {
                FlagshipCard(containerColor = GpaGreen) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LIVE FORECAST", color = Color(0xFFC7E4D4), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        FlagshipPill("Calculated from inputs", Color(0xFF286B4E), Color.White)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Projected SGPA", fontSize = 11.sp, color = Color(0xFFD9E9DF))
                            Text(current.formattedSgpa, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Scale " + uiState.gpaScaleType.maxScalePoint.toInt(), fontSize = 10.sp, color = Color(0xFFC7E4D4))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cumulative CGPA", fontSize = 11.sp, color = Color(0xFFD9E9DF))
                            Text(current.formattedCgpa, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text(current.totalCredits.toString() + " credits included", fontSize = 10.sp, color = Color(0xFFC7E4D4))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Equivalent percentage", modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color(0xFFD9E9DF))
                        Text(current.formattedPercentage, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            item {
                FlagshipCard {
                    FlagshipSectionTitle("Grading scale", "Choose the scale your institution uses")
                    Spacer(Modifier.height(7.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GradingScaleType.entries.forEach { scale ->
                            FilterChip(
                                selected = uiState.gpaScaleType == scale,
                                onClick = { viewModel.setGradingScale(scale) },
                                label = { Text(scale.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
            item {
                val requirement = uiState.targetCgpaRequirement
                FlagshipCard(containerColor = Color(0xFFEAF2EC)) {
                    FlagshipSectionTitle("Target CGPA planner", "What grade point do you need in upcoming credits?")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Target " + String.format(Locale.getDefault(), "%.2f", uiState.targetCgpaInput), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GpaGreen, modifier = Modifier.weight(1f))
                        Text(uiState.upcomingCreditsInput.toString() + " upcoming credits", fontSize = 10.sp, color = GpaMuted)
                    }
                    Slider(
                        value = uiState.targetCgpaInput.toFloat().coerceIn(0f, maximum),
                        onValueChange = { viewModel.updateTargetCgpa(it.toDouble(), uiState.upcomingCreditsInput) },
                        valueRange = 0f..maximum
                    )
                    val message = when {
                        requirement.alreadyAchieved -> "Your current standing already meets this target."
                        requirement.isAchievable -> "Required upcoming SGPA: " + String.format(Locale.getDefault(), "%.2f", requirement.requiredSgpa)
                        else -> "This target is outside the maximum possible grade point for the selected scale."
                    }
                    Text(message, fontSize = 12.sp, color = GpaGreen, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Semester " + uiState.profile.semester + " course matrix", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GpaInk)
                        Text(uiState.gpaCourses.size.toString() + " subjects · " + uiState.gpaCourses.sumOf { it.credits } + " credits entered", fontSize = 11.sp, color = GpaMuted)
                    }
                    Button(onClick = { showAdd = true }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = GpaGreen)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add subject", fontSize = 11.sp)
                    }
                }
            }
            if (uiState.gpaCourses.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No semester subjects entered", fontWeight = FontWeight.Bold, color = GpaInk)
                        Text("Add your current subjects and credit values. The projection updates as you change grades.", fontSize = 12.sp, color = GpaMuted)
                        Spacer(Modifier.height(8.dp))
                        FlagshipPrimaryButton("Add first subject", onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                items(uiState.gpaCourses.size, key = { uiState.gpaCourses[it].id.ifBlank { uiState.gpaCourses[it].subjectCode + "_" + it } }) { index ->
                    val course = uiState.gpaCourses[index]
                    FlagshipCourseGradeCard(
                        course = course,
                        scale = uiState.gpaScaleType,
                        onGrade = { viewModel.updateCourseGradePoint(index, it) },
                        onCredits = { viewModel.updateCourseCredits(index, it) },
                        onRemove = { viewModel.removeGpaCourse(index) }
                    )
                }
            }
            item {
                FlagshipCard {
                    FlagshipSectionTitle("Grade point reference", "Standard grade values available in this calculator")
                    Spacer(Modifier.height(9.dp))
                    val grades = GpaCalculator.standardGradeScale.toList()
                    grades.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { pair ->
                                Surface(modifier = Modifier.weight(1f), color = Color(0xFFF0F3EE), shape = RoundedCornerShape(8.dp)) {
                                    Column(modifier = Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(pair.value.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GpaGreen)
                                        Text(pair.key.substringBefore(" "), fontSize = 9.sp, color = GpaMuted)
                                    }
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }
            item {
                FlagshipCard(containerColor = Color(0xFFF0F3EE)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GpaMuted, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Calculation note", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GpaInk)
                            Text("This is a student-maintained projection based on the selected grade-point scale and entered credit values. It is not an official transcript or university result; verify final calculations against the applicable ordinance and examination records.", fontSize = 11.sp, lineHeight = 16.sp, color = GpaMuted)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlagshipPrimaryButton("Save projection", onClick = viewModel::saveSemesterToHistory, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            for (index in uiState.gpaCourses.indices.reversed()) viewModel.removeGpaCourse(index)
                        },
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }
                OutlinedButton(onClick = { viewModel.exportGpaPdf(context) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Export academic projection PDF")
                }
            }
        }
    }

    if (showAdd) {
        var code by rememberSaveable { mutableStateOf("") }
        var name by rememberSaveable { mutableStateOf("") }
        var credits by rememberSaveable { mutableStateOf("4") }
        var grade by rememberSaveable { mutableStateOf("8") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add semester subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(code, { code = it }, label = { Text("Subject code") }, singleLine = true)
                    OutlinedTextField(name, { name = it }, label = { Text("Subject name") }, singleLine = true)
                    OutlinedTextField(credits, { credits = it.filter(Char::isDigit) }, label = { Text("Credits") }, singleLine = true)
                    OutlinedTextField(grade, { grade = it }, label = { Text("Grade point") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = code.isNotBlank() && name.isNotBlank() && (credits.toIntOrNull() ?: 0) in 1..10 && (grade.toDoubleOrNull() ?: -1.0) in 0.0..uiState.gpaScaleType.maxScalePoint,
                    onClick = {
                        viewModel.addGpaCourse(code.trim(), name.trim(), credits.toInt(), grade.toDouble())
                        showAdd = false
                    }
                ) { Text("Add subject") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GpaStatCard(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(11.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 9.sp, color = GpaMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = GpaGreen)
            Text(detail, fontSize = 9.sp, color = GpaMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FlagshipCourseGradeCard(
    course: CourseGradeInput,
    scale: GradingScaleType,
    onGrade: (Double) -> Unit,
    onCredits: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by rememberSaveable(course.id) { mutableStateOf(false) }
    var creditsText by rememberSaveable(course.id) { mutableStateOf(course.credits.toString()) }
    val options: List<Pair<String, Double>> = when (scale) {
        GradingScaleType.TEN_POINT_INDIAN -> GpaCalculator.standardGradeScale.map { it.key to it.value.toDouble() }
        GradingScaleType.FOUR_POINT_WES -> listOf("A (4)" to 4.0, "A- (3.7)" to 3.7, "B+ (3.3)" to 3.3, "B (3)" to 3.0, "C (2)" to 2.0, "D (1)" to 1.0, "F (0)" to 0.0)
        GradingScaleType.DIRECT_PERCENTAGE -> listOf("90–100" to 95.0, "80–89" to 85.0, "70–79" to 75.0, "60–69" to 65.0, "50–59" to 55.0, "40–49" to 45.0, "Below 40" to 0.0)
    }
    val gradeLabel = options.minByOrNull { kotlin.math.abs(it.second - course.gradePoint) }?.first ?: course.gradePoint.toString()
    FlagshipCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.subjectCode, fontSize = 10.sp, color = Color(0xFF367F8D), fontWeight = FontWeight.Bold)
                Text(course.subjectName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GpaInk)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(Locale.getDefault(), "%.1f", course.gradePoint), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GpaGreen)
                Text("Grade point", fontSize = 9.sp, color = GpaMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Anticipated grade", fontSize = 10.sp, color = GpaMuted)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp)) {
                        Text(gradeLabel, modifier = Modifier.weight(1f), fontSize = 11.sp, color = GpaInk)
                        Text("⌄", color = GpaGreen)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(text = { Text(option.first) }, onClick = { onGrade(option.second); expanded = false })
                        }
                    }
                }
            }
            Column(modifier = Modifier.width(88.dp)) {
                Text("Credits", fontSize = 10.sp, color = GpaMuted)
                OutlinedTextField(
                    value = creditsText,
                    onValueChange = {
                        creditsText = it.filter(Char::isDigit).take(2)
                        creditsText.toIntOrNull()?.takeIf { number -> number in 1..10 }?.let(onCredits)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove subject", tint = Color(0xFF9C641F)) }
        }
        LinearProgressIndicator(
            progress = { (course.gradePoint / scale.maxScalePoint).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = GpaGreen,
            trackColor = GpaLine
        )
    }
}
