package de.healthforge.presentation.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Unified TopAppBar for ALL detail screens.
 * Consistent styling: hm.background, hm.fgPrimary title, back arrow.
 *
 * @param title Screen title (e.g. ingredient name, recipe title)
 * @param onBack Back navigation callback
 * @param actions Additional action buttons (e.g. edit, report, add-to-plan)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HfDetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val hm = LocalHmTokens.current
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = hm.background,
            titleContentColor = hm.fgPrimary,
            navigationIconContentColor = hm.fgPrimary,
            actionIconContentColor = hm.fgPrimary,
        ),
        modifier = modifier,
    )
}
