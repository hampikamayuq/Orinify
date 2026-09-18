package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artist_mix
import simpmusic.composeapp.generated.resources.artist_mixes
import simpmusic.composeapp.generated.resources.loading

/**
 * One radio per artist the listener plays most, as a horizontal shelf.
 *
 * A shelf rather than a sixth tile in [LibraryTilingBox]: the other five open onto ONE list, and
 * there is no single "artist mix" to open — each artist is its own queue, so the artists have to
 * be on screen for the row to mean anything.
 *
 * Drawn here rather than through [LibraryItem] because none of that component's branches fit: its
 * rows take `RecentlyType` or `PlaylistType`, and an artist mix is neither — it is an
 * [ArtistEntity] that plays instead of navigating. Tapping a card starts the radio in place; it
 * deliberately does NOT open the artist page, which is what the same artwork does everywhere else
 * in the app.
 *
 * Renders nothing at all when there are no mixes. An empty shelf with a heading would tell a new
 * listener they are missing something, when the truthful answer is that this needs a few weeks of
 * listening first.
 */
@Composable
fun ArtistMixShelf(
    artists: List<ArtistEntity>,
    isLoading: Boolean,
    // The channel id currently being resolved, or null. Only one can be in flight.
    loadingChannelId: String?,
    onClick: (ArtistEntity) -> Unit,
) {
    if (artists.isEmpty() && !isLoading) return
    Column {
        Text(
            text = stringResource(Res.string.artist_mixes),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, start = 10.dp, end = 10.dp)
                    .heightIn(min = 35.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically),
        )
        Crossfade(targetState = isLoading && artists.isEmpty(), label = "ArtistMixLoading") { loading ->
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(Modifier.wrapContentSize())
                }
            } else {
                LazyRow(Modifier.padding(top = 10.dp)) {
                    items(items = artists, key = { it.channelId }) { artist ->
                        ArtistMixItem(
                            artist = artist,
                            isResolving = artist.channelId == loadingChannelId,
                            onClick = { onClick(artist) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistMixItem(
    artist: ArtistEntity,
    isResolving: Boolean,
    onClick: () -> Unit,
) {
    // On the clickable, not the overlay: the clickable merges its descendants and a child's
    // stateDescription does not survive that merge.
    val loadingLabel = stringResource(Res.string.loading)
    Column(
        modifier =
            Modifier
                .width(140.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .then(if (isResolving) Modifier.semantics { stateDescription = loadingLabel } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(124.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(artist.thumbnails)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artist.thumbnails)
                        .crossfade(550)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                // The name is the Text below, inside the same clickable; naming it here too
                // reads it twice.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            // Over the artwork, not beside it: the shelf must not change size while a mix
            // resolves, or every card after this one shifts under the listener's finger.
            // Fully qualified: the enclosing Column puts `ColumnScope.AnimatedVisibility` in
            // scope, and inside this Box there is no ColumnScope to call it on.
            androidx.compose.animation.AnimatedVisibility(
                visible = isResolving,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                    )
                }
            }
        }
        Text(
            text = artist.name,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
            text = stringResource(Res.string.artist_mix),
            style = typo().bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
