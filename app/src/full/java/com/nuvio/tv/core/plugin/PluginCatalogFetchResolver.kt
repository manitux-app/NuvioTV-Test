package com.nuvio.tv.core.plugin

import android.util.Log
import com.nuvio.tv.core.network.IPv4FirstDns
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.Closeable
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal class PluginCatalogFetchResolver(
    private val primaryClient: OkHttpClient = defaultClient(),
    private val permissiveClient: OkHttpClient = permissiveClient()
) {
    fun execute(
        request: Request,
        followRedirects: Boolean,
        inFlightCalls: MutableSet<Call>
    ): ResponseHandle {
        val primary = primaryClient.withRedirectPolicy(followRedirects)
        val permissive = permissiveClient.withRedirectPolicy(followRedirects)
        return try {
            execute(primary, request, inFlightCalls)
        } catch (error: SSLException) {
            if (!request.url.isHttps) throw error
            Log.w(TAG, "Catalog TLS validation failed for ${request.url.host}; retrying with catalog fallback", error)
            execute(permissive, request, inFlightCalls)
        }
    }

    private fun execute(
        client: OkHttpClient,
        request: Request,
        inFlightCalls: MutableSet<Call>
    ): ResponseHandle {
        val call = client.newCall(request)
        inFlightCalls.add(call)
        return try {
            ResponseHandle(call.execute(), call, inFlightCalls)
        } catch (error: Exception) {
            inFlightCalls.remove(call)
            throw error
        }
    }

    private fun OkHttpClient.withRedirectPolicy(followRedirects: Boolean): OkHttpClient =
        if (followRedirects) this else newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

    internal class ResponseHandle(
        val response: Response,
        private val call: Call,
        private val inFlightCalls: MutableSet<Call>
    ) : Closeable {
        override fun close() {
            try {
                response.close()
            } finally {
                inFlightCalls.remove(call)
            }
        }
    }

    private companion object {
        private const val TAG = "PluginCatalogFetch"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .dns(IPv4FirstDns())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .proxy(java.net.Proxy.NO_PROXY)
            .build()

        fun permissiveClient(): OkHttpClient {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            }
            return defaultClient().newBuilder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        }
    }
}
