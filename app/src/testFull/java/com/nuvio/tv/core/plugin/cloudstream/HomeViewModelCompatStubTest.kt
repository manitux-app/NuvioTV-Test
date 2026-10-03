package com.nuvio.tv.core.plugin.cloudstream

import com.lagradost.cloudstream3.ui.home.HomeViewModel
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelCompatStubTest {

    @Test
    fun `CloudStream HomeViewModel class is resolvable for plugins`() {
        val clazz = Class.forName("com.lagradost.cloudstream3.ui.home.HomeViewModel")
        assertEquals("com.lagradost.cloudstream3.ui.home.HomeViewModel", clazz.name)
        assertNotNull(clazz.getDeclaredClasses().firstOrNull { it.simpleName == "Companion" })
    }

    @Test
    fun `getResumeWatching returns empty instead of throwing`() = runBlocking {
        val result = HomeViewModel.getResumeWatching()
        assertTrue(result.isNullOrEmpty())
    }

    @Test
    fun `CloudStream main page API signature is available to external extensions`() {
        val method = MainAPI::class.java.methods.singleOrNull { method ->
            method.name == "getMainPage" && method.parameterTypes.take(2) == listOf(
                Int::class.javaPrimitiveType,
                MainPageRequest::class.java
            )
        }

        assertNotNull(method)
        // Kotlin suspend functions erase their declared return type to Object and
        // append a Continuation parameter on the JVM.
        assertEquals(Any::class.java, method!!.returnType)
        assertTrue(method.parameterTypes.last().name.startsWith("kotlin.coroutines.Continuation"))
    }
}
