package com.nuvio.tv.core.plugin.cloudstream

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.HomePageResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalCatalogAdapterTest {

    @Test
    fun `catalog requests use declared main pages even when hasMainPage is false`() {
        val api = object : MainAPI() {
            override val name = "Provider"
            override val mainPage = listOf(MainPageData("Popular", "popular", false))
        }

        val requests = api.catalogRequests("repo", "scraper", "provider")

        assertEquals(1, requests.size)
        assertEquals("Provider", requests.single().providerName)
        assertEquals("Popular", requests.single().request.name)
    }

    @Test
    fun `catalog requests ignore MainAPI blank placeholder`() {
        val requests = object : MainAPI() {}.catalogRequests("repo", "scraper", "provider")

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `catalog requests retain a named page with empty provider data`() {
        val api = object : MainAPI() {
            override val mainPage = listOf(MainPageData("Popular", "", false))
        }

        val requests = api.catalogRequests("repo", "scraper", "provider")

        assertEquals(1, requests.size)
    }

    @Test
    fun `catalog requests use the default request for a getMainPage override without metadata`() {
        val api = object : MainAPI() {
            override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? = null
        }

        val requests = api.catalogRequests("repo", "scraper", "provider")

        assertEquals(1, requests.size)
        assertEquals("", requests.single().request.name)
        assertEquals("", requests.single().request.data)
    }
}
