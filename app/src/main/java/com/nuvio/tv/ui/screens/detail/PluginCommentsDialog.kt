package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.PluginComment
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PluginCommentsDialog(
    comments: List<PluginComment>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, comments.size, canLoadMore, isLoadingMore, isLoading, error) {
        if (isLoading || !error.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            canLoadMore && !isLoadingMore && lastVisible >= comments.lastIndex - 1
        }.distinctUntilChanged().collect { shouldLoadMore ->
            if (shouldLoadMore) onLoadMore()
        }
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.plugin_comments_title)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(NuvioTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                !error.isNullOrBlank() -> Text(error)
                comments.isEmpty() -> Text(stringResource(R.string.plugin_comments_empty))
                else -> LazyColumn(modifier = Modifier.height(480.dp), state = listState) {
                    items(comments, key = { it.id }) { comment ->
                        Card(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().padding(vertical = NuvioTheme.spacing.xs),
                            colors = CardDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard)
                        ) {
                            Column(modifier = Modifier.padding(NuvioTheme.spacing.md)) {
                                Text(comment.author, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(NuvioTheme.spacing.xs))
                                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (canLoadMore) item {
                        Button(onClick = onLoadMore, enabled = !isLoadingMore) {
                            Text(
                                stringResource(
                                    if (isLoadingMore) R.string.plugin_comments_loading_more
                                    else R.string.plugin_comments_load_more
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
