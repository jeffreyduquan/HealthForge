package de.healthforge.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Unified search bar — used in ALL Essen sub-tabs (Lebensmittel, Rezepte, Supplements).
 * Replaces the 3 different search implementations.
 *
 * @param query Current search text
 * @param onQueryChange Called when text changes
 * @param placeholder Placeholder text (e.g. "Apfel, Brot, Tomate…")
 * @param showFilterIcon Whether to show a filter icon button (Lebensmittel has filters)
 * @param onFilterClick Called when filter icon is tapped (nullable = icon hidden)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HfSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Suchen…",
    showFilterIcon: Boolean = false,
    onFilterClick: (() -> Unit)? = null,
    filterCount: Int = 0,
) {
    val hm = LocalHmTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.weight(1f),
        )
        if (showFilterIcon && onFilterClick != null) {
            IconButton(onClick = onFilterClick) {
                androidx.compose.foundation.layout.Box {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        modifier = Modifier.size(24.dp),
                    )
                    if (filterCount > 0) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(hm.ambientViolet),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$filterCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = hm.fgPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
