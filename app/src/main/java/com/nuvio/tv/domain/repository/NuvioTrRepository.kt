package com.nuvio.tv.domain.repository

import com.nuvio.tv.domain.model.NuvioTrRepositories

interface NuvioTrRepository {
    suspend fun fetchRepositories(): Result<NuvioTrRepositories>
}
