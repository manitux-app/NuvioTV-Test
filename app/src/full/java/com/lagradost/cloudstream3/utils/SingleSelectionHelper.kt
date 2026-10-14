package com.lagradost.cloudstream3.utils

import android.app.Activity
import com.nuvio.tv.core.plugin.PluginUserMessageBus

/**
 * Compatibility bridge for extensions compiled against Cloudstream's legacy
 * selection dialog helper. Nuvio presents the notice with its TV-safe dialog.
 */
object SingleSelectionHelper {
    @Suppress("UnusedParameter")
    fun showDialog(
        activity: Activity,
        items: List<String>,
        selectedIndex: Int,
        name: String,
        showApply: Boolean,
        dismissCallback: () -> Unit,
        callback: (Int) -> Unit,
    ) {
        val (title, text) = name.toNotice()
        PluginUserMessageBus.show(
            pluginName = "Cloudstream",
            title = title,
            text = text,
            onDismiss = {
                dismissCallback()
                callback(selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)))
            },
        )
    }

    private fun String.toNotice(): Pair<String?, String> {
        val content = removePrefix("!").trim()
        val separator = content.indexOf("\n\n")
        return if (separator < 0) {
            null to content
        } else {
            content.substring(0, separator).trim().ifBlank { null } to
                content.substring(separator + 2).trim()
        }
    }
}
