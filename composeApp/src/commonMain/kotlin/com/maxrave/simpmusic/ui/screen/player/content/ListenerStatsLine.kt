package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.maxrave.domain.data.model.analytics.ListenerStats
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.ui.screen.home.analytics.monthShortName
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.listener_first_play
import simpmusic.composeapp.generated.resources.listener_last_heard_days_ago
import simpmusic.composeapp.generated.resources.listener_last_heard_on
import simpmusic.composeapp.generated.resources.listener_last_heard_today
import simpmusic.composeapp.generated.resources.listener_last_heard_yesterday
import simpmusic.composeapp.generated.resources.listener_plays

/**
 * The one line on the player that comes from this listener rather than from YouTube:
 * "Play 12 · last heard yesterday", or "Your first play". Every style's track row places it
 * under the artist; the shell gates it on local tracking by leaving [stats] null.
 *
 * Tinted with the app accent on purpose — it is the fork's own sentence on a screen that is
 * otherwise artwork-coloured, and the accent is what says so.
 */
@Composable
fun ListenerStatsLine(
    stats: ListenerStats?,
    modifier: Modifier = Modifier,
    color: Color = seed,
) {
    if (stats == null) return
    Text(
        text = listenerStatsText(stats),
        style = typo().bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun listenerStatsText(stats: ListenerStats): String {
    // The play in progress is already logged, so a count of one IS the first play.
    if (stats.playCount <= 1) return stringResource(Res.string.listener_first_play)
    val today = now().date
    val days = stats.lastPlayedAt.date.daysUntil(today)
    val lastHeard =
        when {
            days <= 0 -> stringResource(Res.string.listener_last_heard_today)
            days == 1 -> stringResource(Res.string.listener_last_heard_yesterday)
            days < 7 -> stringResource(Res.string.listener_last_heard_days_ago, days.toString())
            else ->
                stringResource(
                    Res.string.listener_last_heard_on,
                    "${stats.lastPlayedAt.day} ${monthShortName(stats.lastPlayedAt.month)}",
                )
        }
    return stringResource(Res.string.listener_plays, stats.playCount.toString()) + " · " + lastHeard
}
