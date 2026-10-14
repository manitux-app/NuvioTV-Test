package com.nuvio.tv.domain.model

data class PluginSettingsSchema(
    val scraperId: String,
    val title: String,
    val fields: List<PluginSettingField>,
    val values: Map<String, String>
)

data class PluginSettingField(
    val key: String,
    val label: String,
    val type: String = "text",
    val defaultValue: String = "",
    val options: List<String> = emptyList()
)
