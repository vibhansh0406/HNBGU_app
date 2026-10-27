package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.config.FeatureFlags
import com.campusone.app.domain.model.PersonalNotice
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.FluidContentStateBox
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.ui.viewmodel.CoreUiState
import com.campusone.app.ui.viewmodel.CoreViewModel

@Composable
fun NoticesScreen(
    coreState: CoreUiState,
    coreViewModel: CoreViewModel
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var expandedNoticeId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val filteredNotices: List<PersonalNotice> = remember(coreState.personalNotices, selectedFilter) {
        when (selectedFilter) {
            1 -> coreState.personalNotices.filter { !it.isRead }
            2 -> coreState.personalNotices.filter { it.isPinned }
            else -> coreState.personalNotices
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    showAddDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.notices_add_btn))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.notices_personal_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedFilter = 0
                        },
                        label = { Text(stringResource(R.string.notices_filter_all)) }
                    )
                }
                item {
                    val unreadCount = coreState.personalNotices.count { !it.isRead }
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedFilter = 1
                        },
                        label = { Text("${stringResource(R.string.notices_filter_unread)} ($unreadCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedFilter = 2
                        },
                        label = { Text(stringResource(R.string.notices_filter_pinned)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!FeatureFlags.CLOUD_FEATURES_ENABLED) {
                InteractiveGlassCard(
                    isHighlighted = false,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.cloud_coming_soon_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val stateKey = if (filteredNotices.isEmpty()) "empty_$selectedFilter" else "list_$selectedFilter"
            FluidContentStateBox(
                stateKey = stateKey,
                data = filteredNotices
            ) { list ->
                if (list.isEmpty()) {
                    InteractiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.notices_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(list, key = { it.id }) { notice ->
                            val isExpanded = expandedNoticeId == notice.id
                            val dotColor by animateColorAsState(
                                targetValue = if (notice.isRead) {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                animationSpec = FluidMotion.microTween(),
                                label = "notice_dot_color"
                            )

                            InteractiveGlassCard(
                                isHighlighted = !notice.isRead || notice.isPinned,
                                onClick = {
                                    expandedNoticeId = if (isExpanded) null else notice.id
                                    if (!notice.isRead) {
                                        coreViewModel.markNoticeRead(notice.id)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            if (notice.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = notice.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (notice.isRead) FontWeight.Medium else FontWeight.Bold
                                            )
                                        }
                                        this@Row.AnimatedVisibility(
                                            visible = !notice.isRead,
                                            enter = FluidMotion.modalEnter(reducedMotion),
                                            exit = FluidMotion.modalExit(reducedMotion)
                                        ) {
                                            AssistChip(
                                                onClick = {
                                                    FluidMotion.performTickHaptic(view, reducedMotion)
                                                    coreViewModel.markNoticeRead(notice.id)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                label = { Text(stringResource(R.string.notices_mark_read)) }
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                FluidMotion.performTickHaptic(view, reducedMotion)
                                                coreViewModel.deletePersonalNotice(notice.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.common_delete)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notice.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = if (isExpanded) 20 else 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var body by rememberSaveable { mutableStateOf("") }
        var pinned by rememberSaveable { mutableStateOf(false) }
        var showError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.notices_add_btn)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedFormValidationBanner(
                        visible = showError,
                        message = stringResource(R.string.form_validation_required)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) showError = false
                        },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Memo / Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                        Text("Pin to top")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isBlank()) {
                        showError = true
                    } else {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        coreViewModel.addPersonalNotice(title, body, pinned)
                        showAddDialog = false
                    }
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
