package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.config.SupabaseRuntimeConfig
import com.campusone.app.domain.model.AccountStatus
import com.campusone.app.domain.model.CampusAppRole
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.model.RemoteProfileDto
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel

@Composable
fun GoogleLoginSelectionScreen(
    runtimeConfig: SupabaseRuntimeConfig,
    isAuthenticating: Boolean,
    authErrorMessage: String?,
    onSelectGoogleLogin: (LoginIntentPath) -> Unit,
    onSaveRuntimeConfig: (url: String, publishableKey: String, googleClientId: String) -> Unit,
    onContinueOfflineSetup: () -> Unit
) {
    var showConfigEditor by rememberSaveable {
        mutableStateOf(!runtimeConfig.isSupabaseConfigured || !runtimeConfig.isGoogleAuthConfigured)
    }
    var sbUrl by rememberSaveable(runtimeConfig.supabaseUrl) { mutableStateOf(runtimeConfig.supabaseUrl) }
    var sbKey by rememberSaveable(runtimeConfig.supabasePublishableKey) {
        mutableStateOf(runtimeConfig.supabasePublishableKey)
    }
    var googleClientId by rememberSaveable(runtimeConfig.googleWebClientId) {
        mutableStateOf(runtimeConfig.googleWebClientId)
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InteractiveGlassCard(
                    isHighlighted = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.auth_welcome_title),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = stringResource(R.string.auth_welcome_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnimatedFormValidationBanner(
                    visible = !authErrorMessage.isNullOrBlank(),
                    message = authErrorMessage ?: ""
                )
            }

            // Choice 1: Continue as Student
            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auth_student_card_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = stringResource(R.string.auth_student_card_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            enabled = !isAuthenticating,
                            onClick = { onSelectGoogleLogin(LoginIntentPath.STUDENT) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(stringResource(R.string.auth_continue_student_btn))
                        }
                    }
                }
            }

            // Choice 2: Admin Login
            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auth_admin_card_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = stringResource(R.string.auth_admin_card_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            enabled = !isAuthenticating,
                            onClick = { onSelectGoogleLogin(LoginIntentPath.ADMIN) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(stringResource(R.string.auth_admin_login_btn))
                        }
                    }
                }
            }

            // Configuration Status & Runtime Override Card
            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (runtimeConfig.isSupabaseConfigured && runtimeConfig.isGoogleAuthConfigured) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = if (runtimeConfig.isSupabaseConfigured && runtimeConfig.isGoogleAuthConfigured) {
                                        Color(0xFF059669)
                                    } else {
                                        Color(0xFFD97706)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.auth_config_status_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            TextButton(onClick = { showConfigEditor = !showConfigEditor }) {
                                Text(if (showConfigEditor) "Hide" else "Configure")
                            }
                        }

                        Text(
                            text = runtimeConfig.missingConfigDiagnosis(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = showConfigEditor) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = sbUrl,
                                    onValueChange = { sbUrl = it },
                                    label = { Text("SUPABASE_URL (https://xxx.supabase.co)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = sbKey,
                                    onValueChange = { sbKey = it },
                                    label = { Text("SUPABASE_PUBLISHABLE_KEY (anon/publishable)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = googleClientId,
                                    onValueChange = { googleClientId = it },
                                    label = { Text("GOOGLE_WEB_CLIENT_ID (*.apps.googleusercontent.com)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onSaveRuntimeConfig(sbUrl, sbKey, googleClientId)
                                        showConfigEditor = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.common_save))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onContinueOfflineSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.auth_offline_fallback_btn))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessDeniedScreen(
    title: String,
    reason: String,
    email: String,
    roleLabel: String,
    canSwitchToStudentView: Boolean,
    onSwitchToStudentView: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InteractiveGlassCard(
                isHighlighted = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Signed in as: $email • Trusted Role: $roleLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (canSwitchToStudentView) {
                        Button(
                            onClick = onSwitchToStudentView,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.auth_switch_to_student_btn))
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_sign_out_btn))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRoleConsoleScreen(
    coreState: CoreUiState,
    viewModel: CoreViewModel
) {
    val context = LocalContext.current
    val remoteProfile = coreState.remoteProfile
    val role = remoteProfile?.parsedRole ?: CampusAppRole.STUDENT

    var noticeTitle by rememberSaveable { mutableStateOf("") }
    var noticeBody by rememberSaveable { mutableStateOf("") }
    var noticePriority by rememberSaveable { mutableStateOf("HIGH") }
    var noticePinned by rememberSaveable { mutableStateOf(false) }
    var publishImmediately by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(role) {
        if (role == CampusAppRole.SUPER_ADMIN) {
            viewModel.loadAdminUsersAndAuditLogs()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            InteractiveGlassCard(
                isHighlighted = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CampusOne Admin Console (${role.displayLabel})",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${remoteProfile?.email.orEmpty()} • Dept ID: ${remoteProfile?.departmentId ?: "All Departments"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButtonOnAdmin(
                            onRefresh = {
                                viewModel.refreshSupabaseSync(context)
                                if (role == CampusAppRole.SUPER_ADMIN) {
                                    viewModel.loadAdminUsersAndAuditLogs()
                                }
                            },
                            onSignOut = { viewModel.signOutOfSupabase(context) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.switchLoginIntentPath(LoginIntentPath.STUDENT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Student App View")
                    }
                }
            }
        }

        if (!coreState.exportBannerMessage.isNullOrBlank()) {
            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(coreState.exportBannerMessage ?: "", modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearExportBanner() }) {
                            Text(stringResource(R.string.common_close))
                        }
                    }
                }
            }
        }

        // Notice Composer for super_admin, department_admin, notice_admin
        if (role in setOf(CampusAppRole.SUPER_ADMIN, CampusAppRole.DEPARTMENT_ADMIN, CampusAppRole.NOTICE_ADMIN)) {
            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Publish Official Department / Campus Notice",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = noticeTitle,
                            onValueChange = { noticeTitle = it },
                            label = { Text("Notice Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = noticeBody,
                            onValueChange = { noticeBody = it },
                            label = { Text("Notice Content / Circular Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("LOW", "MEDIUM", "HIGH", "URGENT")) { p ->
                                FilterChip(
                                    selected = noticePriority == p,
                                    onClick = { noticePriority = p },
                                    label = { Text(p) }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = noticePinned, onCheckedChange = { noticePinned = it })
                            Text("Pin Notice")
                            Spacer(modifier = Modifier.width(12.dp))
                            Checkbox(checked = publishImmediately, onCheckedChange = { publishImmediately = it })
                            Text("Publish Immediately")
                        }
                        Button(
                            onClick = {
                                if (noticeTitle.isNotBlank() && noticeBody.isNotBlank()) {
                                    viewModel.publishNoticeAsAdmin(
                                        title = noticeTitle,
                                        content = noticeBody,
                                        priority = noticePriority,
                                        isPinned = noticePinned,
                                        publishImmediately = publishImmediately
                                    )
                                    noticeTitle = ""
                                    noticeBody = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (publishImmediately) "Publish Notice to Supabase" else "Save Draft Notice")
                        }
                    }
                }
            }
        }

        // Super Admin Role & Account Status Management
        if (role == CampusAppRole.SUPER_ADMIN) {
            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Super Admin: Manage Accounts & Roles (Server-Side RPC)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (coreState.adminManagedProfiles.isEmpty()) {
                            Text(
                                text = "Tap Refresh above to load registered profiles from Supabase.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            coreState.adminManagedProfiles.take(15).forEach { prof ->
                                AdminProfileRowCard(
                                    profile = prof,
                                    onAssignRole = { newRole, newStatus ->
                                        viewModel.assignRoleAsSuperAdmin(
                                            targetUserId = prof.id,
                                            newRole = newRole.dbValue,
                                            newDepartmentId = prof.departmentId,
                                            newAccountStatus = newStatus.dbValue
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recent Administrative Audit Logs (Immutable)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (coreState.adminAuditLogs.isEmpty()) {
                            Text(
                                text = "No audit logs recorded yet.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            coreState.adminAuditLogs.take(10).forEach { log ->
                                Text(
                                    text = "• [${log.action}] by ${log.actorEmail ?: "admin"} on ${log.entityType} (${log.createdAt?.take(19) ?: ""})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconButtonOnAdmin(
    onRefresh: () -> Unit,
    onSignOut: () -> Unit
) {
    Row {
        AssistChip(
            onClick = onRefresh,
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Sync") }
        )
        Spacer(modifier = Modifier.width(6.dp))
        AssistChip(
            onClick = onSignOut,
            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Logout") }
        )
    }
}

@Composable
private fun AdminProfileRowCard(
    profile: RemoteProfileDto,
    onAssignRole: (CampusAppRole, AccountStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "${profile.fullName.ifBlank { "User" }} (${profile.email})",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Current Role: ${profile.role} • Status: ${profile.accountStatus}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CampusAppRole.entries) { r ->
                FilterChip(
                    selected = profile.parsedRole == r,
                    onClick = { onAssignRole(r, profile.parsedStatus) },
                    label = { Text(r.dbValue) }
                )
            }
            item {
                val toggleStatus = if (profile.parsedStatus == AccountStatus.SUSPENDED) {
                    AccountStatus.ACTIVE
                } else {
                    AccountStatus.SUSPENDED
                }
                AssistChip(
                    onClick = { onAssignRole(profile.parsedRole, toggleStatus) },
                    label = {
                        Text(
                            if (profile.parsedStatus == AccountStatus.SUSPENDED) "Reactivate"
                            else "Suspend"
                        )
                    }
                )
            }
        }
    }
}
