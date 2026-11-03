package com.nexvary.hifzai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.AppAppearance

@Composable
internal fun AppearanceControls(value: AppAppearance, change: (AppAppearance) -> Unit) {
    Text("مظهر التطبيق", style = MaterialTheme.typography.titleLarge)
    Text("المظهر الحالي: ${value.arabic}")
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        AppAppearance.entries.forEach { mode ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .selectable(selected = mode == value, role = Role.RadioButton, onClick = { change(mode) })
                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected = mode == value, onClick = null)
                Text(mode.arabic)
            }
        }
    }
}
