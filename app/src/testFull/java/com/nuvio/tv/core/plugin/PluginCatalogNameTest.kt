package com.nuvio.tv.core.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginCatalogNameTest {

    @Test
    fun `decodes named HTML entities in catalog titles`() {
        assertEquals(
            "Örümcek Adam: Yepyeni Bir Gün",
            decodePluginCatalogName("&Ouml;r&uuml;mcek Adam: Yepyeni Bir G&uuml;n")
        )
        assertEquals("Saklambaç 2", decodePluginCatalogName("Saklamba&ccedil; 2"))
    }
}
