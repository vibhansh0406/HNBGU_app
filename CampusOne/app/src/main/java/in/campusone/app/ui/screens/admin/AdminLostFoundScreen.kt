package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.ui.components.C1AppBar
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1IconButton
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLostFoundScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            C1AppBar(
                title = "Lost & Found",
                onBack = onBack
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = C1Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            items(uiState.lostFound, key = { it.id }) { item ->
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(modifier = Modifier.padding(C1Spacing.lg)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "${if (item.type == "LOST") "🔍" else "📦"}  ${item.itemTitle}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "${item.type} • ${item.category}", style = MaterialTheme.typography.bodyMedium, color = extendedColors.brandForeground)
                                Text(text = item.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = item.dateFormatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                                if (!item.isResolved) {
                                    C1Button(
                                        text = "Resolve",
                                        onClick = { viewModel.resolveLostFound(item.id) },
                                        intent = C1ButtonIntent.PRIMARY
                                    )
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Resolved", tint = extendedColors.brandForeground)
                                }
                                C1IconButton(
                                    onClick = { viewModel.hideLostFound(item.id) },
                                    content = {
                                        Icon(Icons.Default.FindInPage, contentDescription = "Hide")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
