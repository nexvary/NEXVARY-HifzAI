package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal data class NavigationDestination(val label: String, val icon: ImageVector,
    val selected: Boolean, val select: () -> Unit)

/** Full labels and 48dp targets without imposing a fixed height on scaled text. */
@Composable
internal fun AdaptiveBottomNavigation(destinations: List<NavigationDestination>) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints {
        val fitting = (maxWidth.value / (68f * fontScale)).toInt().coerceIn(1, destinations.size)
        val columns = if (fitting == 4 && destinations.size == 5) 3 else fitting
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .testTag("bottom-navigation").selectableGroup()) {
                destinations.chunked(columns).forEach { row ->
                    // Intrinsic height lets the text and font padding determine
                    // row height. NavigationBarItem constrains its label at 2x.
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { destination ->
                            Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp)
                                .selectable(selected = destination.selected, role = Role.Tab,
                                    onClick = destination.select).padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Surface(shape = RoundedCornerShape(50),
                                    color = if (destination.selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent) {
                                    Icon(destination.icon, contentDescription = null,
                                        tint = if (destination.selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).size(24.dp))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(destination.label, style = MaterialTheme.typography.labelMedium,
                                    color = if (destination.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
