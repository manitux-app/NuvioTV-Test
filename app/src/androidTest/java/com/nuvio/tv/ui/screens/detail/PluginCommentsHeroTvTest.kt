package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.ui.theme.NuvioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PluginCommentsHeroTvTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun commentsCapabilityShowsActionAndInvokesHandler() {
        var openCommentsCalls = 0
        compose.setContent {
            NuvioTheme {
                Box(Modifier.fillMaxSize().background(NuvioTheme.colors.Background)) {
                    HeroContentSection(
                        meta = Meta(
                            id = "plugin-item",
                            type = ContentType.MOVIE,
                            name = "Plugin movie",
                            poster = null,
                            posterShape = PosterShape.POSTER,
                            background = null,
                            logo = null,
                            description = null,
                            releaseInfo = null,
                            imdbRating = null,
                            genres = emptyList(),
                            runtime = null,
                            director = emptyList(),
                            cast = emptyList(),
                            videos = emptyList(),
                            country = null,
                            awards = null,
                            language = null,
                            links = emptyList(),
                        ),
                        nextEpisode = null,
                        nextToWatch = null,
                        onPlayClick = {},
                        isInLibrary = false,
                        onToggleLibrary = {},
                        onLibraryLongPress = {},
                        isMovieWatched = false,
                        isMovieWatchedPending = false,
                        onToggleMovieWatched = {},
                        showPluginComments = true,
                        onShowPluginComments = { openCommentsCalls++ },
                    )
                }
            }
        }

        compose.onNodeWithContentDescription(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.plugin_comments_title),
        ).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, openCommentsCalls) }
    }
}
