package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal data class NavigationDestination(val label: String, val icon: ImageVector,
    val selected: Boolean, val select: () -> Unit)

/** Keep full labels and 48dp targets, including at large font on a narrow window. */
@Composable
internal fun AdaptiveBottomNavigation(destinations: List<NavigationDestination>) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints {
        // A 12sp Arabic label needs more room as the system font grows. Do not
        // shrink the font or hide unselected destinations to fit five columns.
        val fitting = (maxWidth.value / (68f * fontScale)).toInt().coerceIn(1, destinations.size)
        val columns = if (fitting == 4 && destinations.size == 5) 3 else fitting
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .testTag("bottom-navigation")) {
                destinations.chunked(columns).forEach { row ->
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height((64f + 12f * fontScale).dp)) {
                        row.forEach { destination ->
                            NavigationBarItem(selected = destination.selected, onClick = destination.select,
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) })
                        }
                    }
                }
            }
        }
    }
}
