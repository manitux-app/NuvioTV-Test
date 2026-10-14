package com.nuvio.tv.ui.screens.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.PluginSettingsSchema
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PluginSettingsDialog(schema: PluginSettingsSchema, onDismiss: () -> Unit, onSave: (PluginSettingsSchema, Map<String, String>) -> Unit) {
    var values by remember(schema) { mutableStateOf(schema.values) }
    NuvioDialog(onDismiss = onDismiss, title = schema.title) {
        schema.fields.forEach { field ->
            Text(field.label)
            if (field.type == "boolean") {
                Button(onClick = { values = values + (field.key to (!(values[field.key]?.toBoolean() ?: false)).toString()) }) {
                    Text(if (values[field.key]?.toBoolean() == true) "Enabled" else "Disabled")
                }
            } else {
                BasicTextField(
                    value = values[field.key].orEmpty(),
                    onValueChange = { values = values + (field.key to it) },
                    singleLine = true,
                    cursorBrush = SolidColor(NuvioTheme.colors.Primary),
                    modifier = Modifier.fillMaxWidth().background(NuvioTheme.colors.Surface).padding(12.dp)
                )
            }
        }
        Button(onClick = { onSave(schema, values) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
    }
}
