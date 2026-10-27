package com.campusone.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.domain.model.DirectoryContactItem
import com.campusone.app.domain.usecase.CampusLifeEngine
import com.campusone.app.ui.components.FluidContentStateBox
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.util.CampusIntentHelper

@Composable
fun CampusDirectoryScreen(
    contacts: List<DirectoryContactItem>
) {
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    val categories = remember { listOf("ALL", "FACULTY", "EMERGENCY", "ADMIN", "HOSTEL", "CANTEEN") }

    val filtered = remember(contacts, query, selectedCategory) {
        CampusLifeEngine.filterDirectory(contacts, query, selectedCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.directory_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it }) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        if (selectedCategory != cat) {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            selectedCategory = cat
                        }
                    },
                    label = { Text(cat) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        FluidContentStateBox(
            stateKey = "dir_${selectedCategory}_${filtered.isEmpty()}",
            data = filtered
        ) { list ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(list, key = { it.id }) { contact ->
                    InteractiveGlassCard(
                        isHighlighted = contact.category == "EMERGENCY",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(contact.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${contact.designation} • ${contact.department}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Office: ${contact.officeLocation}", style = MaterialTheme.typography.bodyMedium)
                            Text("Hours: ${contact.officeHours}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        FluidMotion.performTickHaptic(view, reducedMotion)
                                        CampusIntentHelper.dialPhone(context, contact.phone)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.directory_call))
                                }
                                OutlinedButton(
                                    onClick = {
                                        FluidMotion.performTickHaptic(view, reducedMotion)
                                        CampusIntentHelper.sendEmail(context, contact.email)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.directory_email))
                                }
                                IconButton(
                                    onClick = {
                                        FluidMotion.performTickHaptic(view, reducedMotion)
                                        CampusIntentHelper.copyToClipboard(context, contact.name, contact.phone)
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.directory_copy))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    FluidMotion.performTickHaptic(view, reducedMotion)
                                    CampusIntentHelper.openInMaps(
                                        context = context,
                                        lat = contact.latitude,
                                        lng = contact.longitude,
                                        label = "${contact.name} (${contact.officeLocation})"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.directory_open_maps))
                            }
                        }
                    }
                }
            }
        }
    }
}
