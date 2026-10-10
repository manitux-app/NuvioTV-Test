package com.nuvio.tv.data.remote.api

import com.nuvio.tv.data.remote.dto.NuvioTrRepositoriesDto
import retrofit2.http.GET
import retrofit2.http.Url
import okhttp3.ResponseBody

interface NuvioTrApi {
    @GET
    suspend fun getRepositories(@Url url: String): NuvioTrRepositoriesDto

    @GET
    suspend fun getCollection(@Url url: String): ResponseBody
}
