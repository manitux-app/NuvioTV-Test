package com.nuvio.tv.core.plugin

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Host-owned channel for messages requested by either plugin runtime. */
object PluginUserMessageBus {
    data class Message(
        val pluginName: String,
        val title: String?,
        val text: String,
        val onDismiss: (() -> Unit)? = null,
    )

    private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    fun show(
        pluginName: String,
        text: String,
        title: String? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        val safeText = text.trim().take(MAX_TEXT_LENGTH)
        if (safeText.isBlank()) return
        _messages.tryEmit(Message(pluginName, title?.trim()?.take(MAX_TITLE_LENGTH), safeText, onDismiss))
    }

    private const val MAX_TITLE_LENGTH = 120
    private const val MAX_TEXT_LENGTH = 2_000
}
