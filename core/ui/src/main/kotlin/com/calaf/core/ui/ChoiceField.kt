package com.calaf.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun <T> ChoiceField(
    label: String,
    value: T,
    values: List<T>,
    display: @Composable (T) -> String,
    onChange: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { open = true }) { Text(display(value)) }
            DropdownMenu(open, { open = false }) {
                values.forEach { item ->
                    DropdownMenuItem(text = { Text(display(item)) }, onClick = { onChange(item); open = false })
                }
            }
        }
    }
}
