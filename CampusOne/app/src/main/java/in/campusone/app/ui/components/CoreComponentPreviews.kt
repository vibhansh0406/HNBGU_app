package `in`.campusone.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.CampusOneTheme

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FoundationPreview() {
    CampusOneTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            C1SectionHeader(title = "Section header", actionLabel = "Action", onAction = {})
            C1Button(text = "Primary", onClick = {}, modifier = Modifier.fillMaxWidth())
            C1Button(text = "Tonal", onClick = {}, intent = C1ButtonIntent.TONAL, modifier = Modifier.fillMaxWidth())
            C1Button(text = "Outlined", onClick = {}, intent = C1ButtonIntent.OUTLINED, modifier = Modifier.fillMaxWidth())
            C1Button(text = "Danger", onClick = {}, intent = C1ButtonIntent.DANGER, modifier = Modifier.fillMaxWidth())
            C1TextField(value = "", onValueChange = {}, label = "Input label")
            C1StatusChip(text = "Safe", tone = C1Tone.SUCCESS)
            C1Banner(text = "Informational banner copy", tone = C1Tone.INFO)
            C1Banner(text = "Warning banner copy", tone = C1Tone.WARNING)
            C1StatTile(label = "Overall attendance", value = "86%")
            C1LoadingState(label = "Loading…")
            C1ErrorState(title = "Could not load", message = "Check your connection.", onRetry = {})
            C1EmptyState(
                icon = Icons.Default.Inbox,
                title = "Nothing here yet",
                message = "Content will appear once published."
            )
        }
    }
}
