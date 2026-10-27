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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.config.FeatureFlags
import com.campusone.app.domain.model.LostFoundPreviewItem
import com.campusone.app.domain.usecase.CampusLifeEngine
import com.campusone.app.ui.components.AnimatedFormValidationBanner
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import java.util.UUID

@Composable
fun LostAndFoundScreen() {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    val now = System.currentTimeMillis()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var itemsList by remember {
        mutableStateOf(
            listOf(
                LostFoundPreviewItem(
                    id = "lf_1",
                    title = "Casio fx-991EX Scientific Calculator",
                    description = "Found near Desk 14 after Discrete Mathematics lecture.",
                    category = "ELECTRONICS",
                    location = "CSE Block Room 102, Chauras Campus",
                    isFoundItem = true,
                    isResolved = false,
                    postedAtMillis = now - 2 * 86_400_000L
                ),
                LostFoundPreviewItem(
                    id = "lf_2",
                    title = "Blue HNBGU Central Library ID Card",
                    description = "Lost between Central Library and Birla Gate.",
                    category = "ID_CARD",
                    location = "Birla Campus Main Walk",
                    isFoundItem = false,
                    isResolved = false,
                    postedAtMillis = now - 5 * 86_400_000L
                )
            )
        )
    }

    val activeItems = remember(itemsList, now) {
        itemsList.filter { !CampusLifeEngine.isLostFoundExpired(it.postedAtMillis, now) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!FeatureFlags.CLOUD_FEATURES_ENABLED) {
            item(key = "lf_cloud_banner") {
                InteractiveGlassCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.cloud_coming_soon_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Privacy-first Lost & Found (auto-archives after 30 days, phone numbers hidden). Local board active below.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item(key = "lf_add_btn") {
            Button(
                onClick = {
                    FluidMotion.performTickHaptic(view, reducedMotion)
                    showAddDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post Lost or Found Item (On-Device)")
            }
        }

        items(activeItems, key = { it.id }) { item ->
            InteractiveGlassCard(
                isHighlighted = !item.isResolved,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(if (item.isFoundItem) "FOUND" else "LOST") }
                        )
                        Text(
                            text = if (item.isResolved) "RESOLVED" else "OPEN",
                            fontWeight = FontWeight.Bold,
                            color = if (item.isResolved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Location: ${item.location} • Category: ${item.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    this@Column.AnimatedVisibility(
                        visible = !item.isResolved,
                        enter = FluidMotion.expandCollapseEnter(reducedMotion),
                        exit = FluidMotion.expandCollapseExit(reducedMotion)
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    FluidMotion.performTickHaptic(view, reducedMotion)
                                    itemsList = itemsList.map { current ->
                                        if (current.id == item.id) current.copy(isResolved = true) else current
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark as Resolved")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        var location by rememberSaveable { mutableStateOf("Chauras Campus") }
        var showError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Post Lost or Found Item") },
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
                        label = { Text("Item Title") },
                        singleLine = true
                    )
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isBlank()) {
                        showError = true
                    } else {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        itemsList = listOf(
                            LostFoundPreviewItem(
                                id = "lf_${UUID.randomUUID()}",
                                title = title.trim(),
                                description = desc.trim(),
                                category = "GENERAL",
                                location = location.trim(),
                                isFoundItem = false,
                                isResolved = false,
                                postedAtMillis = System.currentTimeMillis()
                            )
                        ) + itemsList
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
