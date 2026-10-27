package com.campusone.app.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusone.app.R
import com.campusone.app.config.FeatureFlags
import com.campusone.app.domain.model.ClubEventPreviewItem
import com.campusone.app.domain.model.RsvpStatus
import com.campusone.app.domain.usecase.CampusLifeEngine
import com.campusone.app.ui.components.InteractiveGlassCard
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import com.campusone.app.util.CampusIntentHelper

@Composable
fun EventsAndClubsScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()

    var events by remember {
        mutableStateOf(
            listOf(
                ClubEventPreviewItem(
                    id = "evt_1",
                    title = "Garhwal Hackathon & Open Source Sprint",
                    clubName = "HNBGU Coding & Robotics Club",
                    category = "TECH",
                    dateTimeLabel = "Saturday, 10:00 AM",
                    venue = "Chauras Campus Auditorium",
                    goingCount = 84,
                    interestedCount = 31,
                    userRsvp = null
                ),
                ClubEventPreviewItem(
                    id = "evt_2",
                    title = "Himalayan Folk Music & Cultural Evening",
                    clubName = "Aarohan Cultural Society",
                    category = "CULTURAL",
                    dateTimeLabel = "Sunday, 04:30 PM",
                    venue = "Birla Campus Amphitheatre",
                    goingCount = 142,
                    interestedCount = 58,
                    userRsvp = null
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!FeatureFlags.CLOUD_FEATURES_ENABLED) {
            item(key = "events_cloud_banner") {
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
                            text = stringResource(R.string.cloud_coming_soon_body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CLOUD_FEATURES_ENABLED = false (On-Device RSVP Preview)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        items(events, key = { it.id }) { event ->
            InteractiveGlassCard(
                isHighlighted = event.userRsvp == RsvpStatus.GOING,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(event.clubName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.title, style = MaterialTheme.typography.titleLarge)
                    Text("${event.dateTimeLabel} • ${event.venue}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedContent(
                        targetState = Pair(event.goingCount, event.interestedCount),
                        transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
                        label = "rsvp_counter_${event.id}"
                    ) { (going, interested) ->
                        Text(
                            text = "Going: $going • Interested: $interested",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RsvpStatus.entries.forEach { status ->
                            FilterChip(
                                selected = event.userRsvp == status,
                                onClick = {
                                    FluidMotion.performTickHaptic(view, reducedMotion)
                                    val updated = CampusLifeEngine.transitionRsvp(
                                        currentGoing = event.goingCount,
                                        currentInterested = event.interestedCount,
                                        previousStatus = event.userRsvp,
                                        newStatus = status
                                    )
                                    events = events.map { item ->
                                        if (item.id == event.id) {
                                            item.copy(
                                                goingCount = updated.goingCount,
                                                interestedCount = updated.interestedCount,
                                                userRsvp = updated.currentStatus
                                            )
                                        } else {
                                            item
                                        }
                                    }
                                },
                                label = { Text(status.name.replace("_", " ")) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            FluidMotion.performTickHaptic(view, reducedMotion)
                            CampusIntentHelper.addExamOrEventToCalendar(
                                context = context,
                                title = event.title,
                                location = event.venue,
                                description = "Organized by ${event.clubName}",
                                startEpochMillis = System.currentTimeMillis() + 86_400_000L
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Event to Phone Calendar")
                    }
                }
            }
        }
    }
}
