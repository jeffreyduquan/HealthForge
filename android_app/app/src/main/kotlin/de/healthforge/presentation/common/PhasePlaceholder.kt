package de.healthforge.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Reusable placeholder for screens whose full implementation is deferred to a later
 * phase. REQ-NAV-003/004. Keeps look/feel consistent across Plan/Log/etc.
 *
 * @param title Screen title (e.g. "Plan").
 * @param description Short German user-facing message about what's coming.
 * @param icon Material icon shown above the title.
 * @param phaseLabel Optional tag like "P2" or "P3" rendered subtly under the description.
 */
@Composable
fun PhasePlaceholder(
    title: String,
    description: String,
    icon: ImageVector,
    phaseLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (phaseLabel != null) {
            Text(
                phaseLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
