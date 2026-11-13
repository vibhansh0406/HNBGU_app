package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@Composable
fun AdminUsersScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(uiState.profile.name) }
    var email by rememberSaveable { mutableStateOf(uiState.profile.email) }
    var collegeName by rememberSaveable { mutableStateOf(uiState.profile.collegeName) }
    var courseName by rememberSaveable { mutableStateOf(uiState.profile.courseName) }
    var semester by rememberSaveable { mutableIntStateOf(uiState.profile.semester) }
    var section by rememberSaveable { mutableStateOf(uiState.profile.section) }
    var selectedRole by rememberSaveable { mutableStateOf(uiState.profile.role) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users & Roles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Current Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = uiState.profile.name.ifEmpty { "Student" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = uiState.profile.email.ifEmpty { "student@college.edu" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text(text = "Edit Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = collegeName, onValueChange = { collegeName = it }, label = { Text("College") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = courseName, onValueChange = { courseName = it }, label = { Text("Course") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = semester.toString(), onValueChange = { semester = it.toIntOrNull() ?: 1 }, label = { Text("Semester") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Role:", style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.DropdownMenu(
                            expanded = true,
                            onDismissRequest = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            UserRole.entries.forEach { role ->
                                androidx.compose.material3.DropdownMenuItem(
                                    onClick = { selectedRole = role },
                                    text = { Text(role.name) }
                                )
                            }
                        }
                        Text(selectedRole.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            androidx.compose.material3.Button(
                onClick = {
                    viewModel.saveProfile(
                        uiState.profile.copy(
                            name = name,
                            email = email,
                            collegeName = collegeName,
                            courseName = courseName,
                            semester = semester,
                            section = section,
                            role = selectedRole
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile")
            }
        }
    }
}
