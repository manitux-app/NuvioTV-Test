package com.nuvio.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.plugin.PluginUserMessageBus

/** Presents plugin notices with Nuvio's TV-safe dialog instead of plugin-owned UI. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PluginMessageDialogHost() {
    val queue = remember { mutableStateListOf<PluginUserMessageBus.Message>() }
    var current by remember { mutableStateOf<PluginUserMessageBus.Message?>(null) }

    LaunchedEffect(Unit) {
        PluginUserMessageBus.messages.collect { message ->
            if (current == null) current = message else queue += message
        }
    }

    val dismiss: () -> Unit = {
        val dismissed = current
        current = queue.firstOrNull()
        if (queue.isNotEmpty()) queue.removeAt(0)
        dismissed?.onDismiss?.invoke()
        Unit
    }
    current?.let { message ->
        NuvioDialog(
            onDismiss = dismiss,
            title = message.title?.takeIf { it.isNotBlank() } ?: message.pluginName,
            subtitle = message.text
        ) {
            Button(
                onClick = dismiss,
                modifier = Modifier
                    .width(128.dp)
                    .align(Alignment.End),
            ) {
                //Text(stringResource(R.string.plugin_message_confirm))
                Text(
                    text = stringResource(R.string.plugin_message_confirm),
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
